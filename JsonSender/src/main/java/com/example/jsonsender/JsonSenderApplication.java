package com.example.jsonsender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

@SpringBootApplication
public class JsonSenderApplication {

    private static FileLock lock;
    private static RandomAccessFile lockFile;

    public static void main(String[] args) {
        File file = new File("JsonSender.lock");
        try {
            lockFile = new RandomAccessFile(file, "rw");
            lock = lockFile.getChannel().tryLock();

            if (lock == null) {
                System.err.println("Application is already running.");
                System.exit(1);
            }

            // Add shutdown hook to release the lock
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (lock != null) {
                        lock.release();
                    }
                    if (lockFile != null) {
                        lockFile.close();
                    }
                    file.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));

            SpringApplication.run(JsonSenderApplication.class, args);
        } catch (Exception e) {
            System.err.println("Failed to create lock file: " + e.getMessage());
            System.exit(1);
        }
    }

}
