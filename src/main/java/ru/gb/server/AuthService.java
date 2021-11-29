package ru.gb.server;

import java.sql.SQLException;

public interface AuthService {
    String getNickByLoginAndPassword(String login, String password);
    void updateTable(String login,String name) throws SQLException;

}

