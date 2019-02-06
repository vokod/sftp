package com.awolity.sftpteszt.ssh;

public class SshException extends Exception {

    private final String message;

    SshException(String message, Throwable cause) {
        super(cause);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
