package io.ruleforge.connector.service.impl;

import io.ruleforge.connector.dto.TestConnectionResponse;
import org.junit.jupiter.api.Test;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisConnectorTest {

    @Test
    void useTlsTrueUsesConfiguredSslSocketFactory() {
        RecordingSocketFactory socketFactory = new RecordingSocketFactory();
        RedisConnector connector = new RedisConnector(socketFactory);

        TestConnectionResponse response = connector.test(Map.of(
                "host", "redis.example.test",
                "port", 6380,
                "useTls", true
        ));

        assertTrue(socketFactory.createSocketCalled);
        assertTrue(response.message().contains("TLS"));
    }

    @Test
    void useTlsFalseUsesPlainSocketForLocalDevelopment() {
        RecordingSocketFactory socketFactory = new RecordingSocketFactory();
        RedisConnector connector = new RedisConnector(socketFactory);

        TestConnectionResponse response = connector.test(Map.of(
                "host", "127.0.0.1",
                "port", 6379,
                "useTls", false
        ));

        assertFalse(socketFactory.createSocketCalled);
        assertFalse(response.message().contains("TLS"));
    }

    private static class RecordingSocketFactory extends SSLSocketFactory {
        private boolean createSocketCalled;

        @Override
        public String[] getDefaultCipherSuites() {
            return new String[0];
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return new String[0];
        }

        @Override
        public Socket createSocket() throws IOException {
            createSocketCalled = true;
            return SocketFactory.getDefault().createSocket();
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
            createSocketCalled = true;
            return SocketFactory.getDefault().createSocket();
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            createSocketCalled = true;
            return SocketFactory.getDefault().createSocket();
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            createSocketCalled = true;
            return SocketFactory.getDefault().createSocket();
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            createSocketCalled = true;
            return SocketFactory.getDefault().createSocket();
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            createSocketCalled = true;
            return SocketFactory.getDefault().createSocket();
        }
    }
}
