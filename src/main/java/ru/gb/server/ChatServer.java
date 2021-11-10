package ru.gb.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatServer {

    private final AuthService authService;
    private final List<ClientHandler> clients;

    public ChatServer() {
        this.authService = new SimpleAuthService();
        this.clients = new ArrayList<>();

        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            while (true) {
                System.out.println("Wait client connection...");
                final Socket socket = serverSocket.accept();
                new ClientHandler(socket, this);
                System.out.println("Client connected");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AuthService getAuthService() {
        return authService;
    }

    public boolean isNickBusy(String nick) {
        for (ClientHandler client : clients) {
            if (client.getNick().equals(nick)) {
                return true;
            }
        }
        return false;
    }

    public void subscribe(ClientHandler client) {
        clients.add(client);
    }

    public void unsubscribe(ClientHandler client) {
        clients.remove(client);
    }


    public void broadcast(String msg) {
        for (ClientHandler client : clients) {
            client.sendMessage(msg);
        }
    }
}
