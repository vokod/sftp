package com.awolity.sftpteszt.ssh;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.awolity.sftpteszt.AppExecutors;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
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
        Log.d(TAG, "connect() called with: context = [" + context + "], data = [" + data + "], listener = [" + listener + "]");
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

    public void listDirectory(final SSHClient sshClient, final String path, final ListDirectoryListener listener) {
        Log.d(TAG, "listDirectory() called with: sshClient = [" + sshClient + "], listener = [" + listener + "]");
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
                    try (SFTPClient sftp = sshClient.newSFTPClient()) {
                        List<RemoteResourceInfo> remoteFiles = sftp.ls(path);
                        listener.onDirectoryListed(remoteFiles);
                    } catch (IOException e) {
                        listener.onError(new SshException("IO exception error", e));
                    }
                }
            }
        });
    }

    public void downloadFile(final SSHClient sshClient, final RemoteResourceInfo remoteFile,
                             final File inputDir, final DownloadListener listener) {
        AppExecutors.getInstance().network().execute(new Runnable() {
            @Override
            public void run() {
                File inFile = new File(inputDir, remoteFile.getName());
                if (remoteFile.isDirectory() || !remoteFile.isRegularFile()) {
                    listener.onError(new SshException("Remote file is a directory!", null));
                    return;
                }
                if (sshClient == null) {
                    listener.onError(new SshException("Client not initialised", null));
                } else if (!sshClient.isConnected()) {
                    listener.onError(new SshException("Client not connected", null));
                } else if (!sshClient.isAuthenticated()) {
                    listener.onError(new SshException("Client not authenticated", null));
                } else {
                    try (SFTPClient sftp = sshClient.newSFTPClient()) {
                        sftp.get(remoteFile.getName(), new FileSystemFile(inFile));
                    } catch (IOException e) {
                        listener.onError(new SshException("IO exception error", e));
                    }
                }
                listener.onFileDownloaded(inFile);
            }
        });
    }

    public void deleteFile(final SSHClient sshClient, final RemoteResourceInfo remoteFile,
                           final DeleteListener listener) {
        AppExecutors.getInstance().network().execute(new Runnable() {
            @Override
            public void run() {

                if (sshClient == null) {
                    listener.onError(new SshException("Client not initialised", null));
                    return;
                } else if (!sshClient.isConnected()) {
                    listener.onError(new SshException("Client not connected", null));
                    return;
                } else if (!sshClient.isAuthenticated()) {
                    listener.onError(new SshException("Client not authenticated", null));
                    return;
                }
                if (remoteFile.isDirectory()) {
                    try (SFTPClient sftp = sshClient.newSFTPClient()) {
                        sftp.rmdir(remoteFile.getName());
                        listener.onFileDeleted(remoteFile.getName());
                    } catch (IOException e) {
                        listener.onError(new SshException("IO exception error", e));
                    }
                } else {
                    try (SFTPClient sftp = sshClient.newSFTPClient()) {
                        sftp.rm(remoteFile.getPath());
                        listener.onFileDeleted(remoteFile.getName());
                    } catch (IOException e) {
                        listener.onError(new SshException("IO exception error", e));
                    }
                }
            }
        });
    }

    public void uploadFile(final SSHClient sshClient, final File localFile, final String remotePath,
                           final UploadListener listener) {
        AppExecutors.getInstance().network().execute(new Runnable() {
            @Override
            public void run() {
                if (sshClient == null) {
                    listener.onError(new SshException("Client not initialised", null));
                    return;
                } else if (!sshClient.isConnected()) {
                    listener.onError(new SshException("Client not connected", null));
                    return;
                } else if (!sshClient.isAuthenticated()) {
                    listener.onError(new SshException("Client not authenticated", null));
                    return;
                }
                try (SFTPClient sftp = sshClient.newSFTPClient()) {
                    sftp.put(new FileSystemFile(localFile.getAbsolutePath()), remotePath + localFile.getName());
                    listener.onFileUploaded(localFile.getName());
                } catch (IOException e) {
                    listener.onError(new SshException("IO exception error", e));
                }
            }

        });
    }



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
        void onDirectoryListed(@NonNull List<RemoteResourceInfo> remoteFiles);

        void onError(Exception e);
    }

    public interface DownloadListener {
        void onFileDownloaded(@NonNull File file);

        void onError(@NonNull Exception e);
    }

    public interface DeleteListener {
        void onFileDeleted(String name);

        void onError(@NonNull Exception e);
    }

    public interface UploadListener {
        void onFileUploaded(@NonNull String result);

        void onError(@NonNull Exception e);
    }
}
