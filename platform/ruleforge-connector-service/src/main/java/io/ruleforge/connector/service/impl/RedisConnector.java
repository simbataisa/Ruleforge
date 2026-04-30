package io.ruleforge.connector.service.impl;

import io.ruleforge.connector.dto.TestConnectionResponse;
import org.springframework.stereotype.Component;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;

@Component
public class RedisConnector {

    private final SocketFactory plainSocketFactory;
    private final SSLSocketFactory sslSocketFactory;

    public RedisConnector() {
        this(SocketFactory.getDefault(), (SSLSocketFactory) SSLSocketFactory.getDefault());
    }

    RedisConnector(SSLSocketFactory sslSocketFactory) {
        this(SocketFactory.getDefault(), sslSocketFactory);
    }

    RedisConnector(SocketFactory plainSocketFactory, SSLSocketFactory sslSocketFactory) {
        this.plainSocketFactory = plainSocketFactory;
        this.sslSocketFactory = sslSocketFactory;
    }

    public TestConnectionResponse test(Map<String, Object> config) {
        String host = (String) config.getOrDefault("host", "localhost");
        int port = ((Number) config.getOrDefault("port", 6379)).intValue();
        boolean useTls = Boolean.TRUE.equals(config.get("useTls"));
        String mode = useTls ? "TLS Redis socket" : "plain Redis socket";

        try (Socket socket = createSocket(useTls)) {
            socket.connect(new InetSocketAddress(host, port), 3000);
            return new TestConnectionResponse(true, "Connection to Redis at " + host + ":" + port + " successful using " + mode);
        } catch (Exception e) {
            return new TestConnectionResponse(false, "Connection failed using " + mode + ": " + e.getMessage());
        }
    }

    private Socket createSocket(boolean useTls) throws java.io.IOException {
        return useTls ? sslSocketFactory.createSocket() : plainSocketFactory.createSocket();
    }
}
