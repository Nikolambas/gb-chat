package ru.gb.server;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ServerRunner {
    static Logger logger = LogManager.getLogger(ServerRunner.class.getName());

    public static void main(String[] args) {
        logger.info("Сервер загрузился");
        new ChatServer().run();
    }
}
