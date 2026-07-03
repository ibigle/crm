package com.cx.crm.order.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

@RestController
@RequestMapping("/order/v2")
public class OrderFileController {
    private static final Tika TIKA = new Tika();
    private static final MimeTypes MIME_TYPES = MimeTypes.getDefaultMimeTypes();

    /**
     * 💡 针对解密后的高性能大文件流专用物理流式接收接口
     * 完美兼容网关透传过来的 chunked 传输编码
     */
    @PostMapping("/uploadBillFile")
    @ResponseBody
    public ResponseEntity<String> uploadBillFile(HttpServletRequest request, @RequestHeader("X-Tenant-ID") String tenantId) {
//        // 1. MyBatis-Plus 多租户上下文已由前文的拦截器安全注入 ThreadLocal
//        System.out.println("🚀 [下游微服务] 收到网关透传解密后的纯净大文件流... 当前租户上下文: " + TenantContext.getTenantId());
//
//        // 💡 2. 极致性能点：严禁使用 MultipartFile 包装！直接通过底层的原生的网络输入流进行分块对刷
//        try (InputStream nettyInputStream = request.getInputStream();
//             // 模拟持久化到对应租户的物理气隙隔离目录下 (例如: /data/tenant001/bills/)
//             FileOutputStream fileOutputStream = new FileOutputStream("C:\\BIGLE-FILES\\logs\\data\\" + tenantId + "\\bills/bill_" + System.currentTimeMillis() + ".png")) {
//
//            byte[] buffer = new byte[8192]; // 8KB 标准高效物理缓冲区
//            int bytesRead;
//            long totalBytes = 0;
//
//            // 💡 边读网关发过来的明文流、边原地写入物理磁盘/或直接上传给远端的 MinIO
//            while ((bytesRead = nettyInputStream.read(buffer)) != -1) {
//                fileOutputStream.write(buffer, 0, bytesRead);
//                totalBytes += bytesRead;
//            }
//
//            System.out.println("🎉 [下游微服务] 租户 " + tenantId + " 的大文件流无损落地成功！全链路零大对象常驻，总计接收: " + totalBytes + " 字节。");
//            return ResponseEntity.ok("File uploaded successfully. Total size: " + totalBytes + " bytes");
//
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body("File storage streaming exception: " + e.getMessage());
//        }
        System.out.println("\n=== 📥 [order-service] 成功接收到前端透传的本地 2.png 图片流 ===");

        /* 1. 指定还原图片的存放目录（建议本地联调时建好，或者自动生成） */
        String storageDirPath = "C:/BIGLE-FILES/uploaded-images/";
        File storageDir = new File(storageDirPath);
        if (!storageDir.exists()) {
            storageDir.mkdirs(); /* 自动联动创建物理目录 */
        }

        /* 2. 动态生成高散列、绝对唯一的物理图片文件名，防高并发同名覆盖 */
        String targetFileName = "restored_" + UUID.randomUUID().toString().substring(0, 8) + ".xlsx";
        File targetFile = new File(storageDir, targetFileName);

        /* ==================== ⚡ 【核心洗涤拓扑：Socket 流字节内存零拷贝写出】 ==================== */
        try (InputStream inputStream = request.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(targetFile)) {

            byte[] ioBuffer = new byte[4096]; /* 🌟 4KB 黄金高吞吐缓冲区 */
            int bytesRead;
            long totalBytesRead = 0;

            /* 循环从 Socket 原生输入流中吞入二进制图片块，直到流结束 */
            while ((bytesRead = inputStream.read(ioBuffer)) != -1) {
                outputStream.write(ioBuffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }

            outputStream.flush(); /* 强力将物理缓存区数据冲刷锁死至硬盘 */

            System.out.println("🎉 [无损还原大胜利] 物理图片成功落盘存储！");
            System.out.println("🖼️ 存储路径: " + targetFile.getAbsolutePath());
            System.out.println("💾 实际接收到物理发票图片的字节数: " + totalBytesRead + " 字节");

            /* 返回符合前端期待的标准明文 JSON 文本响应 */
            return ResponseEntity.ok("File uploaded successfully. Total size: " + totalBytesRead + " bytes");

        } catch (Exception e) {
            System.err.println("🚨 物理发票图片数据落盘崩溃，原因: " + e.getMessage());
            return ResponseEntity.status(500).body("File storage streaming exception: " + e.getMessage());
        }
    }

    /**
     * 🌟 顶级架构修正：
     * 1. 声明 consumes 接收标准的二进制流（APPLICATION_OCTET_STREAM_VALUE），完美承接网关冲刷过来的图片。
     * 2. 直接注入原生的 HttpServletRequest，彻底跳过任何 Spring 自带的反序列化 Converter，防范图片魔数被污染。
     */
    @PostMapping(value = "/order/v2/uploadBillFile", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public String uploadBillFile(HttpServletRequest request) {
        System.out.println("\n=== 📥 [order-service] 成功接收到前端透传的本地 2.png 图片流 ===");

        /* 1. 指定还原图片的存放目录（建议本地联调时建好，或者自动生成） */
        String storageDirPath = "C:/BIGLE-FILES/uploaded-images/";
        File storageDir = new File(storageDirPath);
        if (!storageDir.exists()) {
            storageDir.mkdirs(); /* 自动联动创建物理目录 */
        }

        /* 2. 动态生成高散列、绝对唯一的物理图片文件名，防高并发同名覆盖 */
        String targetFileName = "restored_" + UUID.randomUUID().toString().substring(0, 8) + ".png";
        File targetFile = new File(storageDir, targetFileName);

        /* ==================== ⚡ 【核心洗涤拓扑：Socket 流字节内存零拷贝写出】 ==================== */
        try (InputStream inputStream = request.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(targetFile)) {

            byte[] ioBuffer = new byte[4096]; /* 🌟 4KB 黄金高吞吐缓冲区 */
            int bytesRead;
            long totalBytesRead = 0;

            /* 循环从 Socket 原生输入流中吞入二进制图片块，直到流结束 */
            while ((bytesRead = inputStream.read(ioBuffer)) != -1) {
                outputStream.write(ioBuffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }

            outputStream.flush(); /* 强力将物理缓存区数据冲刷锁死至硬盘 */

            System.out.println("🎉 [无损还原大胜利] 物理图片成功落盘存储！");
            System.out.println("🖼️ 存储路径: " + targetFile.getAbsolutePath());
            System.out.println("💾 实际接收到物理发票图片的字节数: " + totalBytesRead + " 字节");

            /* 返回符合前端期待的标准明文 JSON 文本响应 */
            return "{\"success\":true,\"message\":\"Picture Restored Successfully! Location: " + targetFileName + "\"}";

        } catch (Exception e) {
            System.err.println("🚨 物理发票图片数据落盘崩溃，原因: " + e.getMessage());
            return "{\"success\":false,\"message\":\"Order-Service IO Error: " + e.getMessage() + "\"}";
        }
    }
}

