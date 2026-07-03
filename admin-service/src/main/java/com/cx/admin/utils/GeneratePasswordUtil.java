package com.cx.admin.utils;

import org.jasypt.util.text.AES256TextEncryptor;

public class GeneratePasswordUtil {
    public static void main(String[] args) {
        AES256TextEncryptor encryptor = new AES256TextEncryptor();
        // 🌟 锁死：大防线解密唯一根密钥（盐值），绝对禁止写死在代码中，后面通过容器环境变量传入
        encryptor.setPassword("CRM_BIGLE_ROOT_SALT_KEY_2026");

        String myRawDbPassword = "926101xyXY"; // 真实的数据库明文密码
        String secretCiphertext = encryptor.encrypt(myRawDbPassword);

        System.out.println("🔒 物理计算完成！密文结果为: " + secretCiphertext);
        // 假设输出为: gX8hN2mW9qZ3...
    }
}
