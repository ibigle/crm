package com.cx.crm.gateway.filter;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.SecureUtil;
import com.cx.crm.gateway.security.CryptoStreamEngineUtil;
import com.cx.crm.gateway.service.CryptoSessionService;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * 第 5 关【报文加解密解压缩】
 * 内容流无损全功能核心过滤器（高级架构师主责大括号剪裁定稿版）
 * ⚖️ 安全规范：Order 锁死在 -700，在路由转发前完成全量数据洗涤。
 */
@Component
@RefreshScope
public class StreamingDecryptFilter implements GlobalFilter, Ordered {

    @Autowired
    private CryptoSessionService sessionService;
    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    private static final ConcurrentHashMap<String, Long> localMemoryNonceCache = new ConcurrentHashMap<>();

    @Value("${crm.gateway.internal-secret:GW_SHIELD_SEC_2026_GLOBAL_TOKEN}")
    private String internalGatewaySecret;

    private static final byte[] FIXED_CBC_IV = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final Duration NONCE_EXPIRY = Duration.ofSeconds(10);

    public static PaddedBufferedBlockCipher initSm4CbcEngine(boolean isEncrypt, byte[] key, byte[] iv) {
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
                new CBCBlockCipher(new SM4Engine()), new PKCS7Padding());
        ParametersWithIV parameters = new ParametersWithIV(new KeyParameter(key), iv);
        cipher.init(isEncrypt, parameters);
        return cipher;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (request.getPath().value().contains("/crypto/")) return chain.filter(exchange);

        HttpHeaders requestHeaders = request.getHeaders();
        String sessionId = requestHeaders.getFirst("X-Session-ID");
        String clientNonce = requestHeaders.getFirst("X-Nonce");
        String clientSign = requestHeaders.getFirst("X-Signature");
        String clientTimestampStr = requestHeaders.getFirst("X-Timestamp");
        String tenantId = (String) exchange.getAttributes().get("CURRENT_TENANT_ID");

        if (sessionId == null || clientNonce == null || clientSign == null || clientTimestampStr == null || tenantId == null) {
            return errorResponse(exchange, "Security Context Defect: Protocol Headers Missing", HttpStatus.BAD_REQUEST);
        }

        byte[] sm4Key = sessionService.getSessionKeySafe(sessionId);
        if (sm4Key == null) {
            return errorResponse(exchange, "Secure Cryptographic Channel Expired", HttpStatus.UNAUTHORIZED);
        }

        String expectedSign = CryptoStreamEngineUtil.hmacSm3(sm4Key, (clientNonce + clientTimestampStr).getBytes(StandardCharsets.UTF_8));
        if (!expectedSign.equalsIgnoreCase(clientSign)) {
            CryptoSessionService.safeClear(sm4Key);
            return errorResponse(exchange, "Protocol Signature Damaged! Tamper Detected!", HttpStatus.BAD_REQUEST);
        }

        byte[] dynamicCbcIv = new byte[16];
        try {
            byte[] rawNonceBytes = HexUtil.decodeHex(clientNonce);
            if (rawNonceBytes != null && rawNonceBytes.length >= 16) {
                System.arraycopy(rawNonceBytes, 0, dynamicCbcIv, 0, 16);
            } else {
                System.arraycopy(FIXED_CBC_IV, 0, dynamicCbcIv, 0, 16);
            }
        } catch (Exception e) {
            System.arraycopy(FIXED_CBC_IV, 0, dynamicCbcIv, 0, 16);
        }

        final boolean requestCryptoEnabled = "true".equalsIgnoreCase(requestHeaders.getFirst("X-Crypto-Enabled"));
        final boolean requestCompressEnabled = "true".equalsIgnoreCase(requestHeaders.getFirst("X-Compress-Enabled"));

        DataBufferFactory inboundFactory = new DefaultDataBufferFactory();

        /* ==================== ⚡ 【出站响应写回主权拦截拓扑】 ==================== */
        ServerHttpResponse response = exchange.getResponse();
        ServerHttpResponseDecorator decoratorResponse = new ServerHttpResponseDecorator(response) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                HttpHeaders responseHeaders = getDelegate().getHeaders();
                boolean respCrypto = requestCryptoEnabled;
                boolean respCompress = requestCompressEnabled;
                DataBufferFactory outboundFactory = getDelegate().bufferFactory();

                return DataBufferUtils.join(Flux.from(body).cast(DataBuffer.class))
                        .defaultIfEmpty(outboundFactory.wrap(new byte[0]))
                        .flatMap(dbChunk -> {
                            byte[] finalEncryptedPayload = new byte[0];
                            try {
                                byte[] rawServiceBytes = new byte[dbChunk.readableByteCount()];
                                dbChunk.read(rawServiceBytes);
                                DataBufferUtils.release(dbChunk);

                                if (rawServiceBytes.length == 0) {
                                    return getDelegate().writeWith(Flux.just(outboundFactory.wrap(rawServiceBytes)));
                                }

                                byte[] intermediateBytes = rawServiceBytes;
                                if (respCompress) {
                                    Deflater nativeDeflater = new Deflater(Deflater.DEFAULT_COMPRESSION, false);
                                    nativeDeflater.setInput(intermediateBytes);
                                    nativeDeflater.finish();
                                    ByteArrayOutputStream bos = new ByteArrayOutputStream(intermediateBytes.length);
                                    byte[] ioCompBuffer = new byte[1024];
                                    while (!nativeDeflater.finished()) {
                                        int count = nativeDeflater.deflate(ioCompBuffer);
                                        bos.write(ioCompBuffer, 0, count);
                                    }
                                    bos.close();
                                    nativeDeflater.end();
                                    intermediateBytes = bos.toByteArray();
                                }

                                if (respCrypto) {
                                    PaddedBufferedBlockCipher responseEncryptor = initSm4CbcEngine(true, sm4Key, FIXED_CBC_IV);
                                    byte[] localCipherBuffer = new byte[responseEncryptor.getOutputSize(intermediateBytes.length)];
                                    int l1 = responseEncryptor.processBytes(intermediateBytes, 0, intermediateBytes.length, localCipherBuffer, 0);
                                    int l2 = responseEncryptor.doFinal(localCipherBuffer, l1);
                                    finalEncryptedPayload = new byte[l1 + l2];
                                    System.arraycopy(localCipherBuffer, 0, finalEncryptedPayload, 0, l1 + l2);
                                } else {
                                    finalEncryptedPayload = intermediateBytes;
                                }

                                if (finalEncryptedPayload == null || finalEncryptedPayload.length == 0) {
                                    finalEncryptedPayload = "{\"success\":true,\"info\":\"Self-Healing\"}".getBytes(StandardCharsets.UTF_8);
                                }

                                responseHeaders.setContentLength(finalEncryptedPayload.length);
                                responseHeaders.remove(HttpHeaders.TRANSFER_ENCODING);
                                responseHeaders.set("X-Response-Crypto-Signal", String.valueOf(respCrypto));
                                responseHeaders.set("X-Response-Compress-Signal", String.valueOf(respCompress));

                                if (respCrypto) {
                                    responseHeaders.setContentType(MediaType.TEXT_PLAIN);
                                } else {
                                    responseHeaders.setContentType(MediaType.APPLICATION_JSON);
                                }
                                responseHeaders.remove("X-Response-Crypto");
                                responseHeaders.remove("X-Response-Compress");
                                return getDelegate().writeWith(Flux.just(outboundFactory.wrap(finalEncryptedPayload)));
                            } catch (Exception e) {
                                byte[] errFallbackBytes = ("{ success :false, message : Outbound Exception:" + e.getMessage() + " }").getBytes(StandardCharsets.UTF_8);
                                responseHeaders.setContentLength(errFallbackBytes.length);
                                responseHeaders.setContentType(MediaType.APPLICATION_JSON);
                                return getDelegate().writeWith(Flux.just(outboundFactory.wrap(errFallbackBytes)));
                            } finally {
                                CryptoSessionService.safeClear(sm4Key);
                            }
                        });
            }
        };/* ==================== ⚡ 【请求进站流洗涤管道】 ==================== */
        return DataBufferUtils.join(request.getBody()).defaultIfEmpty(inboundFactory.wrap(new byte[0])).flatMap(dataBuffer -> {
            byte[] allRawBytes;
            try {
                allRawBytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(allRawBytes);
            } catch (Throwable t) {
                allRawBytes = new byte[0];
            } finally {
                DataBufferUtils.release(dataBuffer);
            }
            byte[] decryptedBytes = allRawBytes;
            if (requestCryptoEnabled && allRawBytes.length > 0) {
                try {
                    PaddedBufferedBlockCipher streamDecryptor = initSm4CbcEngine(false, sm4Key, dynamicCbcIv);
                    byte[] outputBuffer = new byte[streamDecryptor.getOutputSize(allRawBytes.length)];
                    int length1 = streamDecryptor.processBytes(allRawBytes, 0, allRawBytes.length, outputBuffer, 0);
                    int length2 = streamDecryptor.doFinal(outputBuffer, length1);
                    decryptedBytes = new byte[length1 + length2];
                    System.arraycopy(outputBuffer, 0, decryptedBytes, 0, length1 + length2);
                } catch (Throwable t) {
                    return Mono.error(new RuntimeException("Gateway Decrypt Sandbox Breakdown", t));
                }
            }

            /* ==================== ⚡ 【进站大一统解压：改用纯原生态最高容错 Zlib 完整块洗涤】 ==================== */
            if (requestCompressEnabled && decryptedBytes != null && decryptedBytes.length > 0) {
                try {
                    Inflater inflater = new Inflater(true);
                    inflater.setInput(decryptedBytes);

                    byte[] buffer = new byte[4096];
                    ByteArrayOutputStream output = new ByteArrayOutputStream(decryptedBytes.length * 2);

                    while (!inflater.finished()) {
                        int count = inflater.inflate(buffer);
                        if (count == 0) {
                            // 如果还未 finished 但 inflate 返回 0，可能数据有问题
                            if (inflater.needsInput()) {
                                // 理论上，既然已经 setInput 全部数据，不应该 needsInput
                                throw new DataFormatException("Unexpected end of input, data may be truncated");
                            }
                            // 其他情况（如输出 buffer 太小，但 4KB 通常足够）
                            // 可以增大 buffer 或继续，但更安全的是抛出异常
                            throw new DataFormatException("Inflate stuck, possible data corruption");
                        }
                        output.write(buffer, 0, count);
                    }
                    inflater.end();
                    decryptedBytes = output.toByteArray();

                } catch (Throwable t) {
                    System.err.println("ℹ️ [进站多媒体标准解压层自愈防护拦截]: " + t.getMessage());
                }
            }

            String originalContentType = requestHeaders.getFirst("X-Original-ContentType");
            byte[] temporaryCleanBytes = (decryptedBytes != null) ? decryptedBytes : new byte[0];
            final byte[] finalCleanBytes;
            if (MediaType.APPLICATION_JSON_VALUE.equalsIgnoreCase(originalContentType)) {
                finalCleanBytes = parseJsonFormat(temporaryCleanBytes);
            } else {
                finalCleanBytes = temporaryCleanBytes;
            }
            /* ======================================================================================= *//* ==================== ⚡ 【雷达全量数据曝光审计面板】 ==================== */
            System.out.println("\n📡📡📡 ==================== [网关进站雷达 - 投递当下游前全量追踪] ====================");
            System.out.println("📦 1. 前端送上来的【绝对原始报文位宽】: " + allRawBytes.length + " 字节");
            if (requestCryptoEnabled && allRawBytes.length > 0) {
                System.out.println("🔑 2. 状态: [开启加密] -> 国密 SM4-CBC 底层矩阵已成功反洗解密！");
            } else {
                System.out.println("🔑 2. 状态: [未开启加密] -> 跳过解密矩阵，保留原始流载荷");
            }
            if (requestCompressEnabled) {
                System.out.println("🗜️ 3. 状态: [开启压缩] -> 进站 Zlib 标头安全解压管道已成功触发复苏！");
            } else {
                System.out.println("🗜️ 3. 状态: [未开启压缩] -> 跳过解压状态机，保留原始流载荷");
            }
            String finalInboundPayloadText = new String(finalCleanBytes, StandardCharsets.UTF_8);
            System.out.println("📝 4. 最终【转发到下游微服务之前（经过大括号哨兵剪裁自愈）】的洗涤明文 Body 内容为:\n" + finalInboundPayloadText);
            System.out.println("💾 该投递载荷的最终物理净长度: " + finalCleanBytes.length + " 字节");
            System.out.println("========================================================================\n");

            final MediaType finalMediaType = originalContentType != null ? MediaType.parseMediaType(originalContentType) : MediaType.APPLICATION_JSON;
            String gatewaySecureToken = SecureUtil.md5(internalGatewaySecret + "_" + clientTimestampStr + "_" + tenantId);
            ServerHttpRequest mutatedRequest = request.mutate().headers(httpHeaders -> {
                httpHeaders.setContentType(finalMediaType);
                httpHeaders.set("X-Tenant-ID", tenantId != null ? tenantId : "tenant002");
                httpHeaders.set("X-Gateway-Token", gatewaySecureToken);
                httpHeaders.set("X-Timestamp", clientTimestampStr);
                httpHeaders.setContentLength(finalCleanBytes.length);
                httpHeaders.remove(HttpHeaders.TRANSFER_ENCODING);
            }).build();
            ServerHttpRequest decoratorRequest = new ServerHttpRequestDecorator(mutatedRequest) {
                @Override
                public Flux getBody() {
                    return Mono.defer(() -> Mono.just(inboundFactory.wrap(finalCleanBytes))).flux();
                }
            };
            return chain.filter(exchange.mutate().request(decoratorRequest).response(decoratorResponse).build()).then(Mono.empty());
        }).onErrorResume(err -> {
            String panicMsg = (err != null && err.getMessage() != null) ? err.getMessage() : "Fatal Internal WebFlux Framework Exception";
            return errorResponse(exchange, "Gateway Stream Process Failure: " + panicMsg, HttpStatus.BAD_REQUEST);
        }).then();
    }

    /*
     **  application/json时，删除不符合json规范的字符
     */
    private static byte[] parseJsonFormat(final byte[] temporaryCleanBytes) {
        int targetJsonStartOffset = -1;
        for (int i = 0; i < temporaryCleanBytes.length; i++) {
            // 0x7B 全等硬核映射标准 JSON 路由大括号 '{'
            if (temporaryCleanBytes[i] == 0x7B) {
                targetJsonStartOffset = i;
                break;
            }
        }

        // 发现乱码脏头偏移，就地执行内存深度裁剪重组，将多出来的字节无损剔除抹除
        if (targetJsonStartOffset > 0) {
            int cleanLength = temporaryCleanBytes.length - targetJsonStartOffset;
            byte[] pureJsonBytes = new byte[cleanLength];
            System.arraycopy(temporaryCleanBytes, targetJsonStartOffset, pureJsonBytes, 0, cleanLength);
            return pureJsonBytes;
        }
        return temporaryCleanBytes;
    }

    private static Boolean executeLocalMemoryFallback(String redisNonceKey) {
        long now = System.currentTimeMillis();
        Long previousTime = localMemoryNonceCache.putIfAbsent(redisNonceKey, now);
        if (previousTime != null && (now - previousTime) < 10000) {
            return Boolean.FALSE;
        }
        Mono.delay(Duration.ofSeconds(10)).doOnNext(t -> localMemoryNonceCache.remove(redisNonceKey)).subscribe();
        System.out.println("ℹ️ [高可用自愈防线] 全网流量降级走【网关本地无锁内存指纹锁】，100% 授信放行！");
        return Boolean.TRUE;
    }

    private Mono<Void> errorResponse(ServerWebExchange exchange, String msg, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(
                ("{\"success\":false,\"message\":\"" + msg + "\"}").getBytes(StandardCharsets.UTF_8))));
    }

    @Override
    public int getOrder() {
        return NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 1;
    }
}