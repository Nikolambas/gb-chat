package ru.gb.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientHandler {
    private static final String COMMAND_PREFIX = "/";
    private static final String SEND_MESSAGE_TO_CLIENT_COMMAND = COMMAND_PREFIX + "w";
    private static final String END_COMMAND = COMMAND_PREFIX + "end";
    private static final String UPDATE_NICK = COMMAND_PREFIX + "upd";
    private final Socket socket;
    private final ChatServer server;
    private final DataInputStream in;
    private final DataOutputStream out;
    private String nick;
    private String login;
    static Logger logger = LogManager.getLogger(ClientHandler.class.getName());
    ExecutorService executorService;

    public ClientHandler(Socket socket, ChatServer server) {
        try {
            this.nick = "";
            this.socket = socket;
            this.server = server;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            executorService = Executors.newSingleThreadExecutor();
            executorService.execute(() -> {
                try {
                    authenticate();
                    logger.info("Клиент подключился");
                    if (!socket.isClosed()){
                        readMessages();
                    }
                } finally {
                    closeConnection();
                    logger.info("Клиент отключился");
                }
            });
            executorService.shutdown();

        } catch (IOException e) {
            throw  new RuntimeException();
        }

    }

    private void closeConnection() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            logger.error(e);
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            logger.error(e);
        }
        try {
            if (socket != null) {
                server.unsubscribe(this);
                socket.close();
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private void authenticate() {
        AtomicBoolean connect = new AtomicBoolean();
        connect.set(true);
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                Thread.sleep(120000);
                if (nick.equals("")) {
                    connect.set(false);
                    ClientHandler.this.closeConnection();
                    logger.info("Пользователь не успел авторизоваться");
                }
            } catch (InterruptedException e) {
                logger.error(e);
            }
        });
        executorService.shutdown();
        while (connect.get()) {
            try {
                final String str = in.readUTF();
                if (str.startsWith("/auth")) {
                    final String[] split = str.split(" ");
                    final String login = split[1];
                    final String password = split[2];
                    final String nick = server.getAuthService().getNickByLoginAndPassword(login, password);
                    if (nick != null) {
                        if (server.isNickBusy(nick)) {
                            sendMessage("Пользователь уже авторизован");
                            continue;
                        }
                        sendMessage("/authok " + nick + " " + login);
                        this.nick = nick;
                        this.login=login;
                        server.broadcast("Пользователь " + nick + " зашел в чат");
                        String fileName = "C:"+File.separator+"Program Files"+File.separator+"apache-maven-3.6.3-src"+
                                File.separator+"gb-chat"+File.separator+"history_"+login+".txt";
                        File file = new File(fileName);
                        if (!file.exists()){
                            file.createNewFile();
                        }
                        BufferedReader bw = new BufferedReader(new FileReader("history_"+login+".txt"));
                        int count = 100;
                        String msg;
                        while ((msg = bw.readLine())!=null&&count!=0){
                            sendMessage(msg);
                            count--;
                        }
                        bw.close();
                        logger.info("Пользователю "+nick+" загрузили историю переписки");
                        server.subscribe(this);
                        break;
                    } else {
                        sendMessage("Неверные логин и пароль");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void sendMessage(String message) {
        try {
            logger.info("SERVER: Send message to " + nick);
            out.writeUTF(message);
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private void readMessages() {
        try {
            while (true) {
                final String msg = in.readUTF();
                BufferedWriter bw = new BufferedWriter(new FileWriter("history_"+login+".txt",true));
                bw.write("\n"+msg);
                bw.flush();
                logger.info("Receive message: " + msg);
                if (msg.startsWith(COMMAND_PREFIX)) {
                    if (END_COMMAND.equals(msg)) {
                        break;
                    }
                    if (msg.startsWith(SEND_MESSAGE_TO_CLIENT_COMMAND)) { // /w nick1 dkfjslkfj dskj
                        final String[] token = msg.split(" ");
                        final String nick = token[1];
                        server.sendMessageToClient(this, nick, msg.substring(SEND_MESSAGE_TO_CLIENT_COMMAND.length() + 2 + nick.length()));
                        logger.info("Пользователь "+nick+" вышел из чата");
                    }
                    if (msg.startsWith(UPDATE_NICK)){           // /upd nick1
                        String[]strings=msg.split(" ");
                        String newNick=strings[1];
                        if (strings.length==2){
                            try {
                                server.getAuthService().updateTable(nick,newNick);
                                sendMessage("/upd "+ newNick+" "+nick);
                                this.nick = newNick;
                                logger.info("Пользователь "+nick+" поменял ник на "+newNick);
                            } catch (SQLException throwables) {
                                logger.error(throwables);
                            }
                        }
                    }
                    continue;
                }
                server.broadcast(nick + ": " + msg);
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }

    public String getNick() {
        return nick;
    }
}
