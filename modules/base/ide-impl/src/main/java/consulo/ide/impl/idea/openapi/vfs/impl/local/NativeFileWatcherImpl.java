// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.vfs.impl.local;

import consulo.application.Application;
import consulo.application.ApplicationBundle;
import consulo.application.ApplicationManager;
import consulo.application.util.SystemInfo;
import consulo.component.util.NativeFileLoader;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.process.ProcessOutputTypes;
import consulo.process.internal.OSProcessHandler;
import consulo.process.io.BaseDataReader;
import consulo.process.io.BaseOutputReader;
import consulo.util.dataholder.Key;
import consulo.util.io.CharsetToolkit;
import consulo.util.lang.Pair;
import consulo.util.lang.ShutDownTracker;
import consulo.util.lang.TimeoutUtil;
import consulo.virtualFileSystem.ManagingFS;
import consulo.virtualFileSystem.impl.internal.local.FileWatcherNotificationSink;
import consulo.virtualFileSystem.impl.internal.local.PluggableFileWatcher;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author dslomov
 */
public class NativeFileWatcherImpl extends PluggableFileWatcher {
  private static final Logger LOG = Logger.getInstance(NativeFileWatcherImpl.class);

  private static final String PROPERTY_WATCHER_DISABLED = "consulo.filewatcher.disabled";
  private static final String PROPERTY_WATCHER_EXECUTABLE_PATH = "consulo.filewatcher.executable.path";
  private static final Path PLATFORM_NOT_SUPPORTED = Path.of("(platform not supported)");
  private static final String ROOTS_COMMAND = "ROOTS";
  private static final String EXIT_COMMAND = "EXIT";
  private static final int MAX_PROCESS_LAUNCH_ATTEMPT_COUNT = 10;

  private FileWatcherNotificationSink myNotificationSink;
  private Path myExecutable;

  private volatile MyProcessHandler myProcessHandler;
  private final AtomicInteger myStartAttemptCount = new AtomicInteger(0);
  private volatile boolean myIsShuttingDown;
  private final AtomicInteger mySettingRoots = new AtomicInteger(0);
  private volatile List<String> myRecursiveWatchRoots = Collections.emptyList();
  private volatile List<String> myFlatWatchRoots = Collections.emptyList();
  private final String[] myLastChangedPaths = new String[2];
  private int myLastChangedPathIndex;

  @Override
  public void initialize(@Nonnull ManagingFS managingFS, @Nonnull FileWatcherNotificationSink notificationSink) {
    myNotificationSink = notificationSink;

    boolean disabled = isDisabled();
    myExecutable = getExecutablePath();

    if (disabled) {
      LOG.info("Native file watcher is disabled");
    }
    else if (myExecutable == PLATFORM_NOT_SUPPORTED) {
      notifyOnFailure(ApplicationBundle.message("watcher.exe.not.exists"));
    }
    else if (!Files.exists(myExecutable)) {
      notifyOnFailure(ApplicationBundle.message("watcher.exe.not.found"));
    }
    else if (!Files.isExecutable(myExecutable)) {
      String message = ApplicationBundle.message("watcher.exe.not.exe", myExecutable);
      notifyOnFailure(message);
    }
    else {
      try {
        startupProcess(false);
        LOG.info("Native file watcher is operational.");
      }
      catch (IOException e) {
        LOG.warn(e.getMessage());
        notifyOnFailure(ApplicationBundle.message("watcher.failed.to.start"));
      }
    }
  }

  @Override
  public void dispose() {
    myIsShuttingDown = true;
    shutdownProcess();
  }

  @Override
  public boolean isOperational() {
    return myProcessHandler != null;
  }

  @Override
  public boolean isSettingRoots() {
    return isOperational() && mySettingRoots.get() > 0;
  }

  @Override
  public void setWatchRoots(@Nonnull List<String> recursive, @Nonnull List<String> flat) {
    setWatchRoots(recursive, flat, false);
  }

  /**
   * Subclasses should override this method if they want to use custom logic to disable their file watcher.
   */
  protected boolean isDisabled() {
    if (Boolean.getBoolean(PROPERTY_WATCHER_DISABLED)) return true;
    Application app = ApplicationManager.getApplication();
    return app.isCommandLine() || app.isUnitTestMode();
  }

  /**
   * Subclasses should override this method to provide a custom binary to run.
   */
  @Nonnull
  public static Path getExecutablePath() {
    Path path = getExecutablePathImpl();
    if (path == null) {
      return PLATFORM_NOT_SUPPORTED;
    }
    return path.toAbsolutePath();
  }

  @Nullable
  public static Path getExecutablePathImpl() {
    String execPath = System.getProperty(PROPERTY_WATCHER_EXECUTABLE_PATH);
    if (execPath != null) return Path.of(execPath);

    Platform platform = Platform.current();
    return NativeFileLoader.findExecutablePath(platform.mapAnyExecutableName("fsnotifier"));
  }

  /* internal stuff */

  private void notifyOnFailure(String cause) {
    myNotificationSink.notifyUserOnFailure(cause);
  }

  private void startupProcess(boolean restart) throws IOException {
    if (myIsShuttingDown) {
      return;
    }
    if (ShutDownTracker.isShutdownHookRunning()) {
      myIsShuttingDown = true;
      return;
    }

    if (myStartAttemptCount.incrementAndGet() > MAX_PROCESS_LAUNCH_ATTEMPT_COUNT) {
      notifyOnFailure(ApplicationBundle.message("watcher.failed.to.start"));
      return;
    }

    if (restart) {
      shutdownProcess();
    }

    LOG.info("Starting file watcher: " + myExecutable);
    ProcessBuilder processBuilder = new ProcessBuilder(myExecutable.toString());
    Process process = processBuilder.start();
    myProcessHandler = new MyProcessHandler(process, myExecutable.getFileName().toString());
    myProcessHandler.startNotify();

    if (restart) {
      List<String> recursive = myRecursiveWatchRoots;
      List<String> flat = myFlatWatchRoots;
      if (recursive.size() + flat.size() > 0) {
        setWatchRoots(recursive, flat, true);
      }
    }
  }

  private void shutdownProcess() {
    OSProcessHandler processHandler = myProcessHandler;
    if (processHandler != null) {
      if (!processHandler.isProcessTerminated()) {
        try {
          writeLine(EXIT_COMMAND);
        }
        catch (IOException ignore) {
        }
        if (!processHandler.waitFor(10)) {
          ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (!processHandler.waitFor(500)) {
              LOG.warn("File watcher is still alive. Doing a force quit.");
              processHandler.destroyProcess();
            }
          });
        }
      }

      myProcessHandler = null;
    }
  }

  private void setWatchRoots(List<String> recursive, List<String> flat, boolean restart) {
    if (myProcessHandler == null || myProcessHandler.isProcessTerminated()) return;

    if (ApplicationManager.getApplication().isDisposeInProgress()) {
      recursive = flat = Collections.emptyList();
    }

    if (!restart && myRecursiveWatchRoots.equals(recursive) && myFlatWatchRoots.equals(flat)) {
      return;
    }

    mySettingRoots.incrementAndGet();
    myRecursiveWatchRoots = recursive;
    myFlatWatchRoots = flat;

    try {
      writeLine(ROOTS_COMMAND);
      for (String path : recursive) {
        writeLine(path);
      }
      for (String path : flat) {
        writeLine("|" + path);
      }
      writeLine("#");
    }
    catch (IOException e) {
      LOG.warn(e);
    }
  }

  private void writeLine(String line) throws IOException {
    if (LOG.isTraceEnabled()) LOG.trace("<< " + line);
    MyProcessHandler processHandler = myProcessHandler;
    if (processHandler != null) {
      processHandler.writeLine(line);
    }
  }

  @Override
  public void resetChangedPaths() {
    synchronized (myLastChangedPaths) {
      myLastChangedPathIndex = 0;
      Arrays.fill(myLastChangedPaths, null);
    }
  }

  private static final Charset CHARSET =
    SystemInfo.isWindows || SystemInfo.isMac ? StandardCharsets.UTF_8 : CharsetToolkit.getPlatformCharset();

  private static final BaseOutputReader.Options READER_OPTIONS = new BaseOutputReader.Options() {
    @Override
    public BaseDataReader.SleepingPolicy policy() {
      return BaseDataReader.SleepingPolicy.BLOCKING;
    }

    @Override
    public boolean sendIncompleteLines() {
      return false;
    }

    @Override
    public boolean withSeparators() {
      return false;
    }
  };

  private enum WatcherOp {
    GIVEUP,
    RESET,
    UNWATCHEABLE,
    REMAP,
    MESSAGE,
    CREATE,
    DELETE,
    STATS,
    CHANGE,
    DIRTY,
    RECDIRTY
  }

  private class MyProcessHandler extends OSProcessHandler {
    private final BufferedWriter myWriter;
    private WatcherOp myLastOp;
    private final List<String> myLines = new ArrayList<>();

    private MyProcessHandler(@Nonnull Process process, @Nonnull String commandLine) {
      super(process, commandLine, CHARSET);
      myWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), CHARSET));
    }

    private void writeLine(String line) throws IOException {
      myWriter.write(line);
      myWriter.newLine();
      myWriter.flush();
    }

    @Nonnull
    @Override
    protected BaseOutputReader.Options readerOptions() {
      return READER_OPTIONS;
    }

    @Override
    protected void notifyProcessTerminated(int exitCode) {
      super.notifyProcessTerminated(exitCode);

      String message = "Watcher terminated with exit code " + exitCode;
      if (myIsShuttingDown) {
        LOG.info(message);
      }
      else {
        LOG.warn(message);
      }

      myProcessHandler = null;

      try {
        startupProcess(true);
      }
      catch (IOException e) {
        shutdownProcess();
        LOG.warn("Watcher terminated and attempt to restart has failed. Exiting watching thread.", e);
      }
    }

    @Override
    public void notifyTextAvailable(@Nonnull String line, @Nonnull Key outputType) {
      if (outputType == ProcessOutputTypes.STDERR) {
        LOG.warn(line);
      }
      if (outputType != ProcessOutputTypes.STDOUT) {
        return;
      }

      if (LOG.isTraceEnabled()) LOG.trace(">> " + line);

      if (myLastOp == null) {
        WatcherOp watcherOp;
        try {
          watcherOp = WatcherOp.valueOf(line);
        }
        catch (IllegalArgumentException e) {
          String message = "Illegal watcher command: '" + line + "'";
          if (line.length() <= 20) message += " " + Arrays.toString(line.chars().toArray());
          LOG.error(message);
          return;
        }

        if (watcherOp == WatcherOp.GIVEUP) {
          notifyOnFailure(ApplicationBundle.message("watcher.gave.up"));
          myIsShuttingDown = true;
        }
        else if (watcherOp == WatcherOp.RESET) {
          myNotificationSink.notifyReset(null);
        }
        else {
          myLastOp = watcherOp;
        }
      }
      else if (myLastOp == WatcherOp.MESSAGE) {
        LOG.warn(line);
        notifyOnFailure(line);
        myLastOp = null;
      }
      else if (myLastOp == WatcherOp.REMAP || myLastOp == WatcherOp.UNWATCHEABLE) {
        if ("#".equals(line)) {
          if (myLastOp == WatcherOp.REMAP) {
            processRemap();
          }
          else {
            mySettingRoots.decrementAndGet();
            processUnwatchable();
          }
          myLines.clear();
          myLastOp = null;
        }
        else {
          myLines.add(line);
        }
      }
      else {
        String path = StringUtil.trimEnd(line.replace('\0', '\n'), File.separator);  // unescape
        processChange(path, myLastOp);
        myLastOp = null;
      }
    }

    private void processRemap() {
      Set<Pair<String, String>> pairs = new HashSet<>();
      for (int i = 0; i < myLines.size() - 1; i += 2) {
        pairs.add(Pair.create(myLines.get(i), myLines.get(i + 1)));
      }
      myNotificationSink.notifyMapping(pairs);
    }

    private void processUnwatchable() {
      myNotificationSink.notifyManualWatchRoots(myLines);
    }

    private void processChange(@Nonnull String path, @Nonnull WatcherOp op) {
      if (SystemInfo.isWindows && op == WatcherOp.RECDIRTY) {
        myNotificationSink.notifyReset(path);
        return;
      }

      if ((op == WatcherOp.CHANGE || op == WatcherOp.STATS) && isRepetition(path)) {
        if (LOG.isTraceEnabled()) LOG.trace("repetition: " + path);
        return;
      }

      if (SystemInfo.isMac) {
        path = Normalizer.normalize(path, Normalizer.Form.NFC);
      }

      switch (op) {
        case STATS:
        case CHANGE:
          myNotificationSink.notifyDirtyPath(path);
          break;

        case CREATE:
        case DELETE:
          myNotificationSink.notifyPathCreatedOrDeleted(path);
          break;

        case DIRTY:
          myNotificationSink.notifyDirtyDirectory(path);
          break;

        case RECDIRTY:
          myNotificationSink.notifyDirtyPathRecursive(path);
          break;

        default:
          LOG.error("Unexpected op: " + op);
      }
    }
  }

  protected boolean isRepetition(String path) {
    // collapse subsequent change file change notifications that happen once we copy large file,
    // this allows reduction of path checks at least 20% for Windows
    synchronized (myLastChangedPaths) {
      for (int i = 0; i < myLastChangedPaths.length; ++i) {
        int last = myLastChangedPathIndex - i - 1;
        if (last < 0) last += myLastChangedPaths.length;
        String lastChangedPath = myLastChangedPaths[last];
        if (lastChangedPath != null && lastChangedPath.equals(path)) {
          return true;
        }
      }

      myLastChangedPaths[myLastChangedPathIndex++] = path;
      if (myLastChangedPathIndex == myLastChangedPaths.length) myLastChangedPathIndex = 0;
    }

    return false;
  }

  @Override
  @TestOnly
  public void startup() throws IOException {
    Application app = ApplicationManager.getApplication();
    assert app != null && app.isUnitTestMode() : app;

    myIsShuttingDown = false;
    myStartAttemptCount.set(0);
    startupProcess(false);
  }

  @Override
  @TestOnly
  public void shutdown() throws InterruptedException {
    Application app = ApplicationManager.getApplication();
    assert app != null && app.isUnitTestMode() : app;

    MyProcessHandler processHandler = myProcessHandler;
    if (processHandler != null) {
      myIsShuttingDown = true;
      shutdownProcess();

      long t = System.currentTimeMillis();
      while (!processHandler.isProcessTerminated()) {
        if (System.currentTimeMillis() - t > 5000) {
          throw new InterruptedException("Timed out waiting watcher process to terminate");
        }
        TimeoutUtil.sleep(100);
      }
    }
  }
}