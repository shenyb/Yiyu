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
        // 系统属性或环境变量可强制控制
        if ("true".equals(System.getProperty("yiyu.headless"))
                || "true".equals(System.getenv("YIYU_HEADLESS"))) {
            return false;
        }
        if ("true".equals(System.getProperty("yiyu.desktop"))
                || "true".equals(System.getenv("YIYU_DESKTOP"))) {
            return true;
        }
        // 命令行参数
        for (String arg : args) {
            if ("--headless".equals(arg) || "-h".equals(arg)) return false;
            if ("--desktop".equals(arg) || "-d".equals(arg)) return true;
        }
        // 自动检测
        try {
            Class.forName("javafx.application.Application");
            boolean headless = java.awt.GraphicsEnvironment.isHeadless();
            return !headless;
        } catch (Exception e) {
            return false;
        }
    }
}
