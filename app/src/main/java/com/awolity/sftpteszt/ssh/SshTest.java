package com.awolity.sftpteszt.ssh;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.awolity.sftpteszt.AppExecutors;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.userauth.UserAuthException;
import net.schmizz.sshj.xfer.FileSystemFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SshTest {

    private static final String TAG = "SshTest";

    public void connect(final Context context, final ConnectionData data,
                        final ConnectListener listener) {
        AppExecutors.getInstance().network().execute(new Runnable() {
            @Override
            public void run() {
                SSHClient sshClient = new SSHClient(new AndroidConfig());
                try {
                    sshClient.loadKnownHosts(new File(context.getFilesDir(), "known_hosts"));
                } catch (final IOException e) {
                    listener.onVerifyError(e);
                }

                try {
                    Log.d(TAG, "Connect:");
                    sshClient.connect(data.getHost(), data.getPn());
                    Log.d(TAG, "client connected: " + sshClient.isConnected());
                    Log.d(TAG, "client authenticated: " + sshClient.isAuthenticated());
                    sshClient.authPassword(data.getUsername(), data.getPassword());
                    Log.d(TAG, "-----");
                    Log.d(TAG, "StartSession");
                    sshClient.startSession();
                    Log.d(TAG, "client connected: " + sshClient.isConnected());
                    Log.d(TAG, "client authenticated: " + sshClient.isAuthenticated());
                    listener.onConnected(sshClient);
                } catch (final ConnectionException e) {
                    listener.onConnectionError(new SshException("Connection error", e));
                } catch (final TransportException e) {
                    listener.onConnectionError(new SshException("Transport exception error", e));
                    // on TransportException: [HOST_KEY_NOT_VERIFIABLE]
                } catch (final UserAuthException e) {
                    listener.onConnectionError(new SshException("User authentication error", e));
                    // on UserAuthException
                } catch (final IOException e) {
                    // on Host unreachable
                    // on Network unreachable
                    // on ConnectException
                    listener.onConnectionError(new SshException("IO exception error", e));
                }
            }
        });
    }

    public void listDirectory(final SSHClient sshClient, final ListDirectoryListener listener) {
        AppExecutors.getInstance().network().execute(new Runnable() {
            @Override
            public void run() {
                if (sshClient == null) {
                    listener.onError(new SshException("Client not initialised", null));
                } else if (!sshClient.isConnected()) {
                    listener.onError(new SshException("Client not connected", null));
                } else if (!sshClient.isAuthenticated()) {
                    listener.onError(new SshException("Client not authenticated", null));
                } else {
                    try {
                        final Session session = sshClient.startSession();
                        Session.Command cmd = session.exec("ls");
                        String result = IOUtils.readFully(cmd.getInputStream()).toString();
                        RemoteFile[] files = getFileList(sshClient, result);
                        listener.onDirectoryListed(files);
                    } catch (UserAuthException e) {
                        listener.onError(new SshException("User authentication error", e));
                    } catch (TransportException e) {
                        listener.onError(new SshException("Transport exception error", e));
                    } catch (IOException e) {
                        listener.onError(new SshException("IO exception error", e));
                    }
                }
            }
        });
    }

    private RemoteFile[] getFileList(final SSHClient sshClient, String resultString) throws IOException {
        String lines[] = resultString.split("\\r?\\n");
        RemoteFile[] files = new RemoteFile[lines.length];
       for (int i = 0; i < files.length; i++) {
            files[i] = new RemoteFile(lines[i], isDirectory(sshClient, lines[i]));
        }
        return files;
    }

    private boolean isDirectory(final SSHClient sshClient, String filename) throws IOException {
        Log.d(TAG, "isDirectory() called with: sshClient = [" + sshClient + "], filename = [" + filename + "]");
        Session session = sshClient.startSession();
        Session.Command cmd = session.exec("cd " + filename);
        String result = IOUtils.readFully(cmd.getInputStream()).toString();
        Log.d(TAG, "result: " + result);
        if (result.equals("cd: No such directory.\n")) {
            return false;
        } else {
            session = sshClient.startSession();
            session.exec("cd ..");
            return true;
        }
    }

/*    public void download(final Context context, final SSHClient sshClient, final DownloadListener listener) {
        AppExecutors.getInstance().network().execute(new Runnable() {
            @Override
            public void run() {
                File inFile = null;
                String result = "\nDownloading file \"1.txt\"...";
                if (sshClient == null) {
                    result += "\nNot yet connected!";
                } else {
                    try (SFTPClient sftp = sshClient.newSFTPClient()) {
                        inFile = new File(context.getFilesDir(), "1.txt");
                        sftp.get("1.txt", new FileSystemFile(inFile));
                        result += "\nsuccess";
                    } catch (IOException e) {
                        e.printStackTrace();
                        result += "\n" + e.getLocalizedMessage();
                    }
                }
                final File finalInFile = inFile;
                final String finalResult = result;
                AppExecutors.getInstance().mainThread().execute(new Runnable() {
                    @Override
                    public void run() {
                        listener.onFileDownloaded(finalInFile, finalResult);
                    }
                });
            }

        });
    }*/

  /*  public void upload(final File file, final UploadListener listener) {
        AppExecutors.getInstance().network().execute(new Runnable() {
            @Override
            public void run() {
                String result = "\nUploading file file \"" + file.getName() + "\"...";
                if (sshClient == null) {
                    result += "\nNot yet connected!";
                } else {
                    try (SFTPClient sftp = sshClient.newSFTPClient()) {
                        sftp.put(new FileSystemFile(file.getAbsolutePath()), "/" + file.getName());
                        result += "\nsuccess";
                    } catch (IOException e) {
                        e.printStackTrace();
                        result += "\n" + e.getLocalizedMessage();
                    }
                }
                final String finalResult = result;
                AppExecutors.getInstance().mainThread().execute(new Runnable() {
                    @Override
                    public void run() {
                        listener.onFileUploaded(finalResult);
                    }
                });
            }
        });

    }*/

    public void disconnect(final SSHClient sshClient, final DisconnectListener listener) {
        AppExecutors.getInstance().network().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    sshClient.disconnect();
                    listener.onDisconnected();
                } catch (final IOException e) {
                    listener.onError(new SshException("IO error exception", e));
                }
            }
        });
    }

    public interface ConnectListener {
        void onConnected(@NonNull SSHClient client);

        void onVerifyError(@NonNull Exception e);

        void onConnectionError(@NonNull Exception e);
    }

    public interface DisconnectListener {
        void onDisconnected();

        void onError(Exception e);
    }

    public interface ListDirectoryListener {
        void onDirectoryListed(@NonNull RemoteFile[] files);

        void onError(Exception e);
    }

    public interface DownloadListener {
        void onFileDownloaded(File file, String result);
    }

    public interface UploadListener {
        void onFileUploaded(String result);
    }
}
