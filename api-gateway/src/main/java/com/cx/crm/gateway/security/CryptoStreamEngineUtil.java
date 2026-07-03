package com.cx.crm.gateway.security;

import cn.hutool.core.util.HexUtil;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class CryptoStreamEngineUtil {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /* ==================== ⚡ 【后端 Java 侧：标准商用国密 CBC 核心引擎】 ==================== */
    public static PaddedBufferedBlockCipher initSm4CbcEngine(boolean isEncrypt, byte[] key, byte[] iv) {
        /* 采用高可用、全生态全兼容的 CBCBlockCipher 嵌套国密 SM4 核心，外覆 PKCS7 强力填充 */
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
                new CBCBlockCipher(new SM4Engine()), new PKCS7Padding());

        ParametersWithIV parameters = new ParametersWithIV(new KeyParameter(key), iv);
        cipher.init(isEncrypt, parameters);
        return cipher;
    }

    /**
     * 💡 终极安全整改 1：全网首推流式 SM4/GCM AEAD 认证加密引擎 (彻底免疫 Padding 侧信道攻击)
     *
     * @param isEncrypt true-加密, false-解密
     * @param iv        GCM模式要求固定12字节的高熵唯一IV
     */
    public static AEADBlockCipher initSm4GcmEngine(boolean isEncrypt, byte[] key, byte[] iv) {
        // 使用定稿版标准的 GCMBlockCipher 嵌套 SM4 核心
        AEADBlockCipher cipher = GCMBlockCipher.newInstance(new SM4Engine());

        // GCM 认证标签（Mac Tag）标准长度锁死为 128 位 (16字节)
        AEADParameters parameters = new AEADParameters(new KeyParameter(key), 128, iv);
        cipher.init(isEncrypt, parameters);
        return cipher;
    }

    /**
     * 💡 2. 认证流切片增量运算
     */
    public static byte[] processGcmChunk(AEADBlockCipher cipher, byte[] inputChunk) {
        if (inputChunk == null || inputChunk.length == 0) return new byte[0];
        byte[] outputBuffer = new byte[cipher.getOutputSize(inputChunk.length)];
        int processedLen = cipher.processBytes(inputChunk, 0, inputChunk.length, outputBuffer, 0);
        return processedLen > 0 ? Arrays.copyOf(outputBuffer, processedLen) : new byte[0];
    }

    /**
     * 💡 3. 认证流最终冲刷（GCM模式在此处会进行数学强制验签核对，若被篡改直接报错，不予输出）
     */
    public static byte[] finalizeGcmStream(org.bouncycastle.crypto.modes.AEADBlockCipher cipher) throws Exception {
        if (cipher == null) return new byte[0];
        byte[] outputBuffer = new byte[cipher.getOutputSize(0)];
        int finalLen = cipher.doFinal(outputBuffer, 0);
        return finalLen > 0 ? java.util.Arrays.copyOf(outputBuffer, finalLen) : new byte[0];
    }

    // 💡 集中式加解密底座及流式压缩机（基于 4096 内存页，保持上一轮无损补齐状态）
    public static byte[] streamDecompressChunk(Inflater inflater, byte[] compressedChunk, boolean isLast) throws Exception {
        inflater.setInput(compressedChunk);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(compressedChunk.length * 2)) {
            byte[] buffer = new byte[4096];
            while (!inflater.needsInput()) {
                int count = inflater.inflate(buffer);
                if (count > 0) bos.write(buffer, 0, count);
                else break;
            }
            if (isLast) inflater.end();
            return bos.toByteArray();
        }
    }

    public static byte[] streamCompressChunk(Deflater deflater, byte[] rawChunk, boolean isLast) throws Exception {
        deflater.setInput(rawChunk);
        if (isLast) deflater.finish();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(rawChunk.length)) {
            byte[] buffer = new byte[4096];
            while (!deflater.needsInput() && (!isLast || !deflater.finished())) {
                int count = deflater.deflate(buffer);
                if (count > 0) bos.write(buffer, 0, count);
                else break;
            }
            if (isLast) deflater.end();
            return bos.toByteArray();
        }
    }

    public static String encryptBySm4(byte[] keyBytes, String plainText) throws Exception {
        SecretKeySpec sm4Key = new SecretKeySpec(keyBytes, "SM4");
        Cipher cipher = Cipher.getInstance("SM4/ECB/PKCS5Padding", BouncyCastleProvider.PROVIDER_NAME);
        cipher.init(Cipher.ENCRYPT_MODE, sm4Key);
        return HexUtil.encodeHexStr(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
    }

    public static String hmacSm3(byte[] key, byte[] data) {
        HMac hmac = new HMac(new SM3Digest());
        hmac.init(new KeyParameter(key));
        hmac.update(data, 0, data.length);
        byte[] result = new byte[hmac.getMacSize()];
        hmac.doFinal(result, 0);
        return Hex.toHexString(result);
    }
}
