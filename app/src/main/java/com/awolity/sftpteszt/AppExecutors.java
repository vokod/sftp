package com.awolity.sftpteszt;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Global executor pools for the whole application.
 * <p>
 * Grouping tasks like this avoids the effects of task starvation (e.g. disk reads don't wait behind
 * webservice requests).
 */
public class AppExecutors {

  private static AppExecutors INSTANCE = null;

  private final Executor diskIO;
  private final Executor cpu;
  private final Executor network;
  private final Executor mainThread;

  public static synchronized AppExecutors getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new AppExecutors();
    }
    return INSTANCE;
  }

  private AppExecutors() {
    this.diskIO = Executors.newSingleThreadExecutor();
    this.network = Executors.newSingleThreadExecutor();
    this.cpu = Executors.newFixedThreadPool(3);
    this.mainThread = new MainThreadExecutor();
  }

  public Executor diskIO() {
    return diskIO;
  }

  public Executor cpu() {
    return cpu;
  }

  public Executor mainThread() {
    return mainThread;
  }

  public Executor network() {
    return network;
  }

  private static class MainThreadExecutor implements Executor {
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    @Override
    public void execute(@NonNull Runnable command) {
      mainThreadHandler.post(command);
    }
  }
}