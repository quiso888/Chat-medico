package org.aguzman.hilos.ejemplotimer.Proyecto;
import java.io.Serializable;

public class User implements Serializable {
    private String username;
    private String password;
    private boolean isDoctor;

    public User(String username, String password, boolean isDoctor) {
        this.username = username;
        this.password = password;
        this.isDoctor = isDoctor;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isDoctor() {
        return isDoctor;
    }
}
