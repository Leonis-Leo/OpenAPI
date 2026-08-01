package com.openapi.sdk.example;

import com.openapi.sdk.OpenApiClient;

import java.util.Map;

/**
 * SDK 使用示例。
 *
 * <p>运行前：启动 MySQL/Redis、backend(:8101)、gateway(:8080)，
 * 并确保 app 表中存在对应的 AccessKey / SecretKey（可用 db/init.sql 中的演示密钥）。</p>
 */
public class DemoInvokeMain {

    public static void main(String[] args) {
        OpenApiClient client = new OpenApiClient(
                "http://localhost:8080",
                "demo-access-key",
                "demo-secret-key");

        // GET 调用：随机名称
        String name = client.get("/api/demo/name", Map.of("prefix", "Hi-"));
        System.out.println("GET /api/demo/name => " + name);

        // POST 调用：参数回显
        String echo = client.post("/api/demo/echo", Map.of("hello", "world"));
        System.out.println("POST /api/demo/echo => " + echo);
    }
}
