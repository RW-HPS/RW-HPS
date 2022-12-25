/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.util.encryption

import net.rwhps.server.data.global.Data
import net.rwhps.server.util.log.Log.error
import java.io.IOException
import java.security.*
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException

class Rsa {
    /*
        public static void main(String [] args) throws Exception {
            // 获取钥匙
            KeyPair keyPair = buildKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();
            String publicKeyString = base64Encode(publicKey.getEncoded());
            System.out.println("publicKeyString="+publicKeyString +"\n");
            // 加密
            byte [] encrypted = encrypt(publicKey, "This is a secret message");
            System.out.println(base64Encode(encrypted));  // <<encrypted message>>
            // 解密
            byte[] secret = decrypt(privateKey, encrypted);
            System.out.println(new String(secret, UTF8));     // This is a secret message
        }
    */

    @Throws(NoSuchAlgorithmException::class)
    fun buildKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(4096)
        return keyPairGenerator.genKeyPair()
    }

    companion object {
        @Throws(BadPaddingException::class, IllegalBlockSizeException::class)
        fun encrypt(publicKey: PublicKey?, message: String): ByteArray {
            var cipher: Cipher? = null
            try {
                cipher = Cipher.getInstance("RSA")
                /**
                 * 这一部分的cache是没用的
                 * NoSuchPaddingException : 无法获取实例
                 * NoSuchAlgorithmException : 不支持的加密方法
                 * InvalidKeyException : 大于ASE128
                 */
            } catch (e: NoSuchPaddingException) {
            } catch (e: NoSuchAlgorithmException) {
                error(error("UNSUPPORTED_ENCRYPTION"), e)
            }
            try {
                cipher!!.init(Cipher.ENCRYPT_MODE, publicKey)
            } catch (e: InvalidKeyException) {
                error(error("DOES_NOT_SUPPORT_AES_256"), e)
            }
            return cipher!!.doFinal(message.toByteArray(Data.UTF_8))
        }

        @Throws(BadPaddingException::class, IllegalBlockSizeException::class)
        fun decrypt(privateKey: PrivateKey?, encrypted: ByteArray?): ByteArray {
            var cipher: Cipher? = null
            try {
                cipher = Cipher.getInstance("RSA")
                /**
                 * 这一部分的cache是没用的
                 * NoSuchPaddingException : 无法获取实例
                 * NoSuchAlgorithmException : 不支持的加密方法
                 * InvalidKeyException : 大于ASE128
                 */
            } catch (e: NoSuchPaddingException) {
            } catch (e: NoSuchAlgorithmException) {
                error(error("UNSUPPORTED_ENCRYPTION"), e)
            }
            try {
                cipher!!.init(Cipher.DECRYPT_MODE, privateKey)
            } catch (e: InvalidKeyException) {
                error(error("DOES_NOT_SUPPORT_AES_256"), e)
            }
            return cipher!!.doFinal(encrypted)
        }

        /**
         * 从字符串中加载公/私钥
         */
        fun loadPublicKey(publicKeyStr: String): RSAPublicKey {
            return try {
                val buffer = Base64.decode(publicKeyStr)
                val keyFactory = KeyFactory.getInstance("RSA")
                val keySpec = X509EncodedKeySpec(buffer)
                keyFactory.generatePublic(keySpec) as RSAPublicKey
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)
            } catch (e: InvalidKeySpecException) {
                throw RuntimeException(e)
            }
        }

        fun loadPrivateKey(privateKeyStr: String): RSAPrivateKey {
            return try {
                val buffer = Base64.decode(privateKeyStr)
                val keySpec = PKCS8EncodedKeySpec(buffer)
                val keyFactory = KeyFactory.getInstance("RSA")
                keyFactory.generatePrivate(keySpec) as RSAPrivateKey
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)
            } catch (e: InvalidKeySpecException) {
                throw RuntimeException(e)
            }
        }

        @Throws(IOException::class)
        fun getPublicKey(publicKey: PublicKey): String {
            return Base64.encode(publicKey.encoded)
        }

        @Throws(IOException::class)
        fun getPrivateKey(privateKey: PrivateKey): String {
            return Base64.encode(privateKey.encoded)
        }
    }
}
