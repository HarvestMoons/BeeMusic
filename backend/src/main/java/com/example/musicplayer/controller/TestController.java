package com.example.musicplayer.controller;

import org.springframework.web.bind.annotation.*;
import javax.sql.DataSource;
import java.sql.Connection;

@RestController
@RequestMapping("/api/public/test")
public class TestController {

    private final DataSource dataSource;

    public TestController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 测试数据库连接是否成功
     * @return 连接状态
     */
    @GetMapping("/db")
    public String testDatabaseConnection() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                return "✅ 数据库连接成功: " + conn.getMetaData().getURL();
            } else {
                return "❌ 数据库连接失败（连接为空或已关闭）";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 数据库连接异常: " + e.getMessage();
        }
    }

    /**
     * 一个简单的后端健康检查接口
     */
    @GetMapping("/ping")
    public String ping() {
        return "Spring Boot 运行正常 ✅";
    }
}
