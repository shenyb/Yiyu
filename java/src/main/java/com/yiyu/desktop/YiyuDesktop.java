package com.yiyu.desktop;

import com.yiyu.YiyuApplication;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.Environment;

public class YiyuDesktop extends Application {

    private ConfigurableApplicationContext springContext;

    /**
     * 入口：双击 jar / start.bat 时调用
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        // 在非 JavaFX 线程启动 Spring Boot，阻塞等待就绪
        springContext = SpringApplication.run(YiyuApplication.class);
    }

    @Override
    public void start(Stage stage) {
        Environment env = springContext.getEnvironment();
        String port = env.getProperty("server.port", "8080");
        String url = "http://localhost:" + port;

        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);
        engine.load(url);

        // 窗口标题显示实际页面标题
        engine.titleProperty().addListener((obs, old, title) -> {
            if (title != null && !title.isEmpty()) {
                stage.setTitle("医语 — " + title);
            }
        });

        Scene scene = new Scene(webView, 1200, 800);
        stage.setScene(scene);
        stage.setTitle("医语 — AI 助手");
        stage.setMinWidth(800);
        stage.setMinHeight(500);
        stage.show();

        // 窗口关闭时停止 Spring Boot
        stage.setOnCloseRequest(e -> {
            Platform.exit();
        });
    }

    @Override
    public void stop() {
        if (springContext != null) {
            Platform.runLater(() -> {
                springContext.close();
            });
        }
    }
}
