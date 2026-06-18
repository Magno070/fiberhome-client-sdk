package dev.magno.fiberhome.connection;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Tl1Connection {
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    
    private Socket socket;
    private BufferedReader reader;
    private OutputStream writer;
    private boolean connected;
    private int ctagCounter = 1;

    private Tl1Connection(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.username = builder.username;
        this.password = builder.password;
    }

    public synchronized void connect() {
        if (connected) {
            return;
        }
        try {
            this.socket = new Socket(host, port);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = socket.getOutputStream();
            this.connected = true;

            // TL1 login command: ACT-USER::[username]:[ctag]::[password];
            String loginCmd = String.format("ACT-USER::%s:%d::%s;", username, nextCtag(), password);
            String loginResponse = execute(loginCmd);
            if (loginResponse == null || !loginResponse.contains("COMPLD")) {
                disconnect();
                throw new RuntimeException("TL1 login failed: " + loginResponse);
            }
        } catch (IOException e) {
            disconnect();
            throw new RuntimeException("Failed to connect to TL1 server: " + e.getMessage(), e);
        }
    }

    public synchronized void disconnect() {
        if (!connected) {
            return;
        }
        try {
            if (writer != null && socket != null && !socket.isClosed()) {
                // Ignore failure on logout
                try {
                    String logoutCmd = String.format("CANC-USER::%s:%d;", username, nextCtag());
                    writer.write((logoutCmd + "\r\n").getBytes(StandardCharsets.UTF_8));
                    writer.flush();
                } catch (Exception ignored) {}
            }
        } finally {
            connected = false;
            closeQuietly(reader);
            closeQuietly(writer);
            closeQuietly(socket);
            reader = null;
            writer = null;
            socket = null;
        }
    }

    public synchronized String execute(String command) {
        if (!connected) {
            throw new IllegalStateException("Not connected to TL1 server.");
        }
        try {
            String sanitized = command.trim();
            if (!sanitized.endsWith(";")) {
                sanitized += ";";
            }
            writer.write((sanitized + "\r\n").getBytes(StandardCharsets.UTF_8));
            writer.flush();

            return readResponse();
        } catch (IOException e) {
            connected = false;
            throw new RuntimeException("Error executing TL1 command: " + e.getMessage(), e);
        }
    }

    private String readResponse() throws IOException {
        StringBuilder responseBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            responseBuilder.append(line).append("\n");
            // TL1 response is terminated by a line containing ';' or ending with it
            if (line.trim().equals(";")) {
                break;
            }
        }
        return responseBuilder.toString();
    }

    private synchronized int nextCtag() {
        return ctagCounter++;
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {}
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public static class Builder {
        private String host;
        private int port = 3083; // Default TL1 port
        private String username;
        private String password;

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Tl1Connection build() {
            if (host == null || host.isEmpty()) {
                throw new IllegalStateException("Host must be set.");
            }
            if (username == null || username.isEmpty()) {
                throw new IllegalStateException("Username must be set.");
            }
            if (password == null || password.isEmpty()) {
                throw new IllegalStateException("Password must be set.");
            }
            return new Tl1Connection(this);
        }
    }
}
