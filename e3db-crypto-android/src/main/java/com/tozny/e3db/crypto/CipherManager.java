package com.tozny.e3db.crypto;

/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * 
 * Copyright (c) 2018 
 * 
 * All rights reserved.
 * 
 * e3db-java
 * 
 * Created by Lilli Szafranski on 4/9/18.
 * 
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.security.KeyStore;
import java.security.SecureRandom;

class CipherManager {

    interface GetCipher {
        Cipher getCipher(Context context, String identifier, SecretKey key) throws Exception;
    }

    private static void saveInitializationVector(Context context, String fileName, byte[] bytes) throws Exception {
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(new File(FileSystemManager.getInitializationVectorFilePath(context, fileName)));
            fos.write(bytes);
            fos.flush();

        } finally {
            if (fos != null) fos.close();
        }
    }

    private static byte[] loadInitializationVector(Context context, String fileName) throws Exception {
//        FileInputStream fis = null;
//        byte[] bytes;
//
//        try {
//            File file = new File(FileSystemManager.getInitializationVectorFilePath(context, fileName));
//            int fileSize = (int) file.length();
//            bytes = new byte[fileSize];
//            fis = new FileInputStream(file);
//            fis.read(bytes, 0, fileSize);
//
//        } finally {
//            if (fis != null) fis.close();
//        }
//
//        return bytes;

        byte[] bytes = new byte[12];

        if (!new File(FileSystemManager.getInitializationVectorFilePath(context, fileName)).exists()) {

            SecureRandom random = new SecureRandom();
            random.nextBytes(bytes);

            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(FileSystemManager.getInitializationVectorFilePath(context, fileName)));
            bos.write(bytes);
            bos.flush();
            bos.close();

        } else {
            FileInputStream inputStream = new FileInputStream(FileSystemManager.getInitializationVectorFilePath(context, fileName));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int read = 0;
            while ((read = inputStream.read(bytes, 0, bytes.length)) != -1) {
                baos.write(bytes, 0, read);
            }
            baos.flush();
        }

        return bytes;
    }

    static void deleteInitializationVector(Context context, String fileName) throws Exception {
        if (new File(FileSystemManager.getInitializationVectorFilePath(context, fileName)).exists()) {
            File file = new File(FileSystemManager.getInitializationVectorFilePath(context, fileName));
            file.delete();
        }
    }

    static class SaveCipherGetter implements GetCipher {

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public Cipher getCipher(Context context, String identifier, SecretKey key) throws Exception {

            GCMParameterSpec params = new GCMParameterSpec(128, loadInitializationVector(context, identifier)); // Use SecureRandom to get 12 random bytes

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, params);

            

            //Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            //cipher.init(Cipher.ENCRYPT_MODE, key);

            //IvParameterSpec ivParams = cipher.getParameters().getParameterSpec(IvParameterSpec.class);

            //saveInitializationVector(context, identifier, ivParams.getIV());

            // TODO: Log b64 IV to make sure is new every time

            return cipher;
        }
    }

    static class LoadCipherGetter implements GetCipher {

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public Cipher getCipher(Context context, String identifier, SecretKey key) throws Exception {
            GCMParameterSpec params = new GCMParameterSpec(128, loadInitializationVector(context, identifier)); // TODO: Lilli, do we know that it's always 128?

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, params);

            return cipher;
        }
    }

    static GetCipher saveCipherGetter() {
        return new SaveCipherGetter();
    }

    static GetCipher loadCipherGetter() {
        return new LoadCipherGetter();
    }
}
