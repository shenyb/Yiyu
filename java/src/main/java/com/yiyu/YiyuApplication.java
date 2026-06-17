package com.yiyu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class YiyuApplication {

    public static void main(String[] args) {
        suppressMacOsImkNoise();
        if (shouldLaunchDesktop(args)) {
            com.yiyu.desktop.YiyuDesktop.main(args);
        } else {
            SpringApplication.run(YiyuApplication.class, args);
        }
    }

    /**
     * macOS JDK 会向 stderr 直接输出 "error messaging the mach port for IMK..."，
     * 不经过任何 Java logging 框架，无法用 logback 过滤。
     * 用自定义 PrintStream 包装 stderr，过滤掉这类噪音行。
     */
    private static void suppressMacOsImkNoise() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("mac")) return;
        java.io.PrintStream originalErr = System.err;
        System.setErr(new java.io.PrintStream(originalErr, true) {
            @Override
            public void println(String x) {
                if (x != null && x.contains("error messaging the mach port for IMK")) return;
                super.println(x);
            }
            @Override
            public void print(String s) {
                if (s != null && s.contains("error messaging the mach port for IMK")) return;
                super.print(s);
            }
        });
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
