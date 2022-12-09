/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.encrypt.strategy.impl;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.shardingsphere.encrypt.strategy.spi.Encryptor;
import org.apache.shardingsphere.encrypt.utils.XorUtil;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

/**
 * AES encryptor.
 *
 * @author binyu.wu
 * @date 2022/11/7 15:40
 */
@Getter
@Setter
@Slf4j
public final class AESEncryptor implements Encryptor {

    private static final String AES_KEY = "aes.key.value";

    private static final String DEFAULT_ALGORITHM = "AES/GCM/NoPadding";
    private static final String BASE64_IV = "T3ZdQ3VBJwBEC1BXI1AKPA==";

    private Properties properties = new Properties();

    @Override
    public String getType() {
        return "AES";
    }

    @Override
    public void init() {
    }

    @Override
    @SneakyThrows
    public String encrypt(final Object plaintext) {
        if (null == plaintext) {
            return null;
        }
        byte[] result = getCipher(Cipher.ENCRYPT_MODE).doFinal(StringUtils.getBytesUtf8(String.valueOf(plaintext)));
        String encrypt = Base64.encodeBase64String(result);
        if (null != encrypt && encrypt.length() >= 190) {
            log.info("encryptjdbc aes encrypt result length more than 190, so choose plaintext");
            return String.valueOf(plaintext);
        } else {
            return encrypt;
        }
    }

    @Override
    @SneakyThrows
    public Object decrypt(final String ciphertext) {
        if (null == ciphertext) {
            return null;
        }
        try {
            byte[] result = getCipher(Cipher.DECRYPT_MODE).doFinal(Base64.decodeBase64(ciphertext));
            return new String(result, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("encryptjdbc aes decrypt error, check cipher is plain", e);
            return ciphertext;
        }
    }

    private Cipher getCipher(final int decryptMode) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, XorUtil.decrypt(BASE64_IV).getBytes(StandardCharsets.UTF_8));
        Cipher result = Cipher.getInstance(DEFAULT_ALGORITHM);
        result.init(decryptMode, new SecretKeySpec(createSecretKey(), getType()), gcmParameterSpec);
        return result;
    }

    private byte[] createSecretKey() {
        Preconditions.checkArgument(null != properties.get(AES_KEY), String.format("%s can not be null.", AES_KEY));
        return XorUtil.decrypt(properties.get(AES_KEY).toString()).getBytes(StandardCharsets.UTF_8);
    }

}
