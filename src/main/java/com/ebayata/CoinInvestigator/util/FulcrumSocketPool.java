package com.ebayata.CoinInvestigator.util;

import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class FulcrumSocketPool {

    private final BlockingQueue<SocketResources> pool;
    private final int poolSize = 1;

    public FulcrumSocketPool() throws IOException {
        this.pool = new LinkedBlockingQueue<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            pool.add(createSocketResources());
        }
    }

    private SocketResources createSocketResources() throws IOException {
        Socket socket = new Socket("127.0.0.1", 50001);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        return new SocketResources(socket, writer, reader);
    }

    public SocketResources borrow() throws InterruptedException {
        return pool.take(); // waits if empty
    }

    public void release(SocketResources socketResources) {
        if (socketResources != null) {
            pool.offer(socketResources);
        }
    }

    @PreDestroy
    public void shutdown() {
        for (SocketResources resources : pool) {
            try {
                resources.close();
            } catch (IOException ignored) {}
        }
    }

    @Getter
    public static class SocketResources implements Closeable {
        private final Socket socket;
        private final BufferedWriter writer;
        private final BufferedReader reader;

        public SocketResources(Socket socket, BufferedWriter writer, BufferedReader reader) {
            this.socket = socket;
            this.writer = writer;
            this.reader = reader;
        }

        @Override
        public void close() throws IOException {
            writer.close();
            reader.close();
            socket.close();
        }
    }
}
