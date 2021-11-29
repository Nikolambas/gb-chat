package ru.gb.server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SimpleAuthService implements AuthService {

    private Connection connection;
    private Statement statement;


    private  List<UserData> users;

    public SimpleAuthService() {
        users = new ArrayList<>();
//        for (int i = 0; i < 5; i++) {
//            users.add(new UserData("login" + i, "pass" + i, "nick" + i));
//        }

        try {
            connect();
            createTable();
            setArray(users);
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            try {
                if (connection.isClosed()){
                    disconnect();
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }

    }

    private void createTable() throws SQLException {
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS users (\n"+
                        "[key]    INTEGER PRIMARY KEY AUTOINCREMENT UNIQUE NOT NULL,\n"+
                "login    STRING  UNIQUE NOT NULL,\n"+
                "password STRING  NOT NULL,\n"+
                "name             UNIQUE NOT NULL\n"+
        ");");
    }

    private void disconnect()  {
        if (statement!=null){
            try {
                statement.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        if (connection!=null){
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }

    private void connect() throws SQLException {
        connection= DriverManager.getConnection("jdbc:sqlite:users.db");
        statement=connection.createStatement();
    }


    @Override
    public void updateTable(String name,String newName) throws SQLException {
        statement.executeUpdate("UPDATE users SET name ='"+newName+"' WHERE name='"+name+"';");
    }

    private void insertIntoTable(UserData userData) throws SQLException {
        statement.executeUpdate(" INSERT INTO users (login,password,name)" +
                " VALUES('"+userData.login+"','"+userData.password+"','"+userData.nick+"');");
    }

    private void setArray(List list) throws SQLException {
        try (ResultSet rs = statement.executeQuery("SELECT * FROM users;")) {
            while (rs.next()){
                String login = rs.getString("login");
                String password = rs.getString("password");
                String nick = rs.getString("name");
                list.add(new UserData(login,password,nick));
            }
        }
    }

    @Override
    public String getNickByLoginAndPassword(String login, String password) {
        for (UserData user : users) {
            if (user.login.equals(login) && user.password.equals(password)) {
                return user.nick;
            }
        }
        return null;
    }

    private static class UserData {
        private final String login;
        private final String password;
        private final String nick;

        public UserData(String login, String password, String nick) {
            this.login = login;
            this.password = password;
            this.nick = nick;
        }
    }

}
