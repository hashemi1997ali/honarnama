package ir.hashemi.market.model;

import java.io.Serializable;
import java.util.Date;

public class User implements Serializable {

    public Long id;
    public String name;
    public String username;
    public String password;
    public String created_at;

    public User() {
    }

    public User(String name, String username, String password) {
        this.name = name;
        this.username = username;
        this.password = password;
    }
}
