package com.awolity.sftpteszt.ssh;

public class RemoteFile {

    private final boolean isDirectory;
    private final String name;

    public RemoteFile(String name, boolean isDirectory) {
        this.name = name;

        this.isDirectory = isDirectory;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public String getName() {
        return name;
    }
}
