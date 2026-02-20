package io.ruleforge.connector.service.impl;

import io.ruleforge.connector.dto.TestConnectionResponse;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;

@Component
public class RedisConnector {

    public TestConnectionResponse test(Map<String, Object> config) {
        String host = (String) config.getOrDefault("host", "localhost");
        int port = ((Number) config.getOrDefault("port", 6379)).intValue();

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 3000);
            return new TestConnectionResponse(true, "Connection to Redis at " + host + ":" + port + " successful");
        } catch (Exception e) {
            return new TestConnectionResponse(false, "Connection failed: " + e.getMessage());
        }
    }
}
