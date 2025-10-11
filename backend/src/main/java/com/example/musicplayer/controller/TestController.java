package com.example.musicplayer.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Collections;

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

    @GetMapping("/auth")
    public ResponseEntity<String> test(Authentication authentication, HttpSession session) {
        String username = authentication != null ? authentication.getName() : "Not authenticated";
        String roles = authentication != null ? authentication.getAuthorities().toString() : "No roles";
        System.out.println("Session ID: " + session.getId());
        System.out.println("Authenticated user: " + username);
        System.out.println("Roles: " + roles);
        System.out.println("Session attributes: " + Collections.list(session.getAttributeNames()));
        System.out.println("SPRING_SECURITY_CONTEXT: " + session.getAttribute("SPRING_SECURITY_CONTEXT"));
        return ResponseEntity.ok("User: " + username + ", Roles: " + roles + ", Session: " + session.getId());
    }
}
