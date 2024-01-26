package com.zyniel.apps.westiemosaic.models.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CoreHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreHelper.class);

    public static String createSHAHash(String input) throws NoSuchAlgorithmException {

        String hashtext = null;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] messageDigest =
                md.digest(input.getBytes(StandardCharsets.UTF_8));

        hashtext = convertToHex(messageDigest);
        return hashtext;
    }

    private static String convertToHex(final byte[] messageDigest) {
        BigInteger bigint = new BigInteger(1, messageDigest);
        String hexText = bigint.toString(16);
        while (hexText.length() < 32) {
            hexText = "0".concat(hexText);
        }
        return hexText;
    }

    public static void removeFilesByExtension(Path dir, String extension) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*" + extension)) {
            for (Path entry : stream) {
                Files.delete(entry);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to delete file from folder", e);
        }
    }

}
