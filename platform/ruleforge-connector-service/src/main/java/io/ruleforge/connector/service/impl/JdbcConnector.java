package io.ruleforge.connector.service.impl;

import io.ruleforge.connector.dto.TestConnectionResponse;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;

@Component
public class JdbcConnector {

    public TestConnectionResponse test(Map<String, Object> config, String credentials) {
        String url = (String) config.get("url");
        String username = (String) config.getOrDefault("username", "");
        String password = credentials != null ? credentials : (String) config.getOrDefault("password", "");

        if (url == null) {
            return new TestConnectionResponse(false, "Missing 'url' in config");
        }

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            conn.createStatement().executeQuery("SELECT 1");
            return new TestConnectionResponse(true, "Connection successful");
        } catch (Exception e) {
            return new TestConnectionResponse(false, "Connection failed: " + e.getMessage());
        }
    }
}
