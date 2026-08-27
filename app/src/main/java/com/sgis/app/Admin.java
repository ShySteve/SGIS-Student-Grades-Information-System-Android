package com.sgis.app;

public class Admin {
    public final String username;
    public final String password;
    public final PersonName name;

    public Admin(String username, String password, PersonName name) {
        this.username = username;
        this.password = password;
        this.name = name;
    }
}
