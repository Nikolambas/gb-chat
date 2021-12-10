package ru.gb.server;

import jdk.nashorn.internal.runtime.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    private final AuthService authService;
    private final Map<String, ClientHandler> clients;
    static Logger logger = LogManager.getLogger(ChatServer.class.getName());


    public ChatServer() {
        this.authService = new SimpleAuthService();
        this.clients = new HashMap<>();
    }

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            while (true) {
                logger.info("Wait client connection...");
                final Socket socket = serverSocket.accept();
                new ClientHandler(socket, this);
                logger.info("Client connected");
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }

    public AuthService getAuthService() {
        return authService;
    }

    public boolean isNickBusy(String nick) {
        return clients.containsKey(nick);
    }

    public void subscribe(ClientHandler client) {
        clients.put(client.getNick(), client);
        broadcastClientsList();
    }

    public void unsubscribe(ClientHandler client) {
        clients.remove(client.getNick());
        broadcastClientsList();
    }

    public void sendMessageToClient(ClientHandler from, String nickTo, String msg) {
        final ClientHandler client = clients.get(nickTo);
        if (client != null) {
            client.sendMessage("от " + from.getNick() + ": " + msg);
            from.sendMessage("участнику " + nickTo + ": " + msg);
            return;
        }
        from.sendMessage("Участника с ником " + nickTo + " нет в чат-комнате");
    }


    public void broadcastClientsList() {
        StringBuilder clientsCommand = new StringBuilder("/clients ");
        for (ClientHandler client : clients.values()) {
            clientsCommand.append(client.getNick()).append(" ");
        }
        broadcast(clientsCommand.toString());
    }

    public void broadcast(String msg) {
        for (ClientHandler client : clients.values()) {
            client.sendMessage(msg);
        }
    }
}
