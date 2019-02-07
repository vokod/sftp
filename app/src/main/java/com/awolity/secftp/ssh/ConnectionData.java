package com.awolity.secftp.ssh;

public class ConnectionData {

    private final int pn;
    private final String host;
    private final String username;
    private final String password;

    public ConnectionData(String host, int pn, String username, String password) {
        this.host = host;
        this.pn = pn;
        this.username = username;
        this.password = password;
    }

    public int getPn() {
        return pn;
    }

    public String getHost() {
        return host;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
