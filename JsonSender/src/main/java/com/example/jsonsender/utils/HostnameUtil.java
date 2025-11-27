package com.example.jsonsender.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostnameUtil {
    private static final Logger logger = LoggerFactory.getLogger(HostnameUtil.class);

    @SuppressWarnings("deprecation")
    public static String getHostname() {
        String hostname = "localhost"; // デフォルト値
        try {
            Process process = Runtime.getRuntime().exec("hostname");
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            hostname = reader.readLine();
            process.waitFor();
        } catch (java.io.IOException | InterruptedException e) {
            logger.error("ホスト名の取得中にエラーが発生しました: " + e.getMessage());
        }
        return hostname;
    }
}
