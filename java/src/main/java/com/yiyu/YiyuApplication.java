package com.yiyu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class YiyuApplication {

    public static void main(String[] args) {
        if (shouldLaunchDesktop(args)) {
            com.yiyu.desktop.YiyuDesktop.main(args);
        } else {
            SpringApplication.run(YiyuApplication.class, args);
        }
    }

    private static boolean shouldLaunchDesktop(String[] args) {
        for (String arg : args) {
            if ("--headless".equals(arg) || "-h".equals(arg)) {
                return false;
            }
        }
        try {
            Class.forName("javafx.application.Application");
            // 有桌面环境才弹窗口（spring-boot:run 在终端里不弹）
            return !java.awt.GraphicsEnvironment.isHeadless();
        } catch (Exception e) {
            return false;
        }
    }
}
