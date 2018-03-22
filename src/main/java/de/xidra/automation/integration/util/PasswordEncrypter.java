/*
 *  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *
 *       THIS MATERIAL IS PROPRIETARY  TO  EDUARD VAN DEN BONGARD
 *       AND IS  NOT TO BE REPRODUCED, USED OR  DISCLOSED  EXCEPT
 *       IN ACCORDANCE  WITH  PROGRAM  LICENSE  OR  UPON  WRITTEN
 *       AUTHORIZATION  OF  EDUARD VAN DEN BONGARD, AM STIRKENBEND 20
 *       41352 KORSCHENBROICH, GERMANY.
 *
 *       COPYRIGHT (C) 2013-17 EDUARD VAN DEN BONGARD
 *
 *  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 */

package de.xidra.automation.integration.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * This class encryptes a given clear text password. The encrypted password can then be places in the deliveryprocessors
 * yaml configuration file (and in the respective localproperties file
 */
public class PasswordEncrypter
{
    public static final String VERSION = "0.0.16-RC04";
    private static String PASSWORD = null;

    private static Logger log = LoggerFactory.getLogger(PasswordEncrypter.class);

    private byte[] salt;

    // Decreasing this speeds down startup time and can be useful during testing, but it also makes it easier for brute force attackers
    private static int iterationCount = 2000;
    // Other values give me java.security.InvalidKeyException: Illegal key size or default parameters
    private static int keyLength = 128;

    private String key;


    public PasswordEncrypter()
    {
        this(null,null);
    }

    public PasswordEncrypter(String key,String salt)
    {
        setKey(key);
        setSalt(salt);
    }

    public String encrypt(String password) throws Exception
    {
        SecretKeySpec key = createSecretKey(this.key.toCharArray(),
                salt, iterationCount, keyLength);
        String encryptedPassword = encrypt(password, key);

        log.debug("password encrypted");
        return encryptedPassword;
    }

    public String decrypt(String password) throws Exception
    {
        SecretKeySpec key = createSecretKey(this.key.toCharArray(),
                salt, iterationCount, keyLength);
        String decryptedPassword = decrypt(password, key);

        log.debug("password decrypted");
        return decryptedPassword;
    }


    private SecretKeySpec createSecretKey(char[] password, byte[] salt, int iterationCount, int keyLength) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterationCount, keyLength);
        SecretKey keyTmp = keyFactory.generateSecret(keySpec);
        return new SecretKeySpec(keyTmp.getEncoded(), "AES");
    }

    private String encrypt(String property, SecretKeySpec key) throws GeneralSecurityException, UnsupportedEncodingException
    {
        Cipher pbeCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        pbeCipher.init(Cipher.ENCRYPT_MODE, key);
        AlgorithmParameters parameters = pbeCipher.getParameters();
        IvParameterSpec ivParameterSpec = parameters.getParameterSpec(IvParameterSpec.class);
        byte[] cryptoText = pbeCipher.doFinal(property.getBytes("UTF-8"));
        byte[] iv = ivParameterSpec.getIV();
        return base64Encode(iv) + ":" + base64Encode(cryptoText);
    }

    private String base64Encode(byte[] bytes)
    {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String decrypt(String string, SecretKeySpec key) throws GeneralSecurityException, IOException
    {
        String iv = string.split(":")[0];
        String property = string.split(":")[1];
        Cipher pbeCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        pbeCipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(base64Decode(iv)));
        return new String(pbeCipher.doFinal(base64Decode(property)), "UTF-8");
    }

    private byte[] base64Decode(String property) throws IOException
    {
        return Base64.getDecoder().decode(property);
    }

    public void setKey(String key)
    {
        if(key == null)
            log.warn("Key for PasswordEncrypter cannot be null, setting to default value");

        this.key = getValueOrDefault(key, "DevOps-in-a-Box");
    }

    public void setSalt(String salt) {

        if(salt == null)
            log.warn("Salt for PasswordEncrytper cannot be null, setting to default value");

        this.salt = getValueOrDefault(salt,"LALALAND").getBytes();
    }

    private <T> T getValueOrDefault(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }
}
