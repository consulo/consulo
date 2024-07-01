/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.desktop.startup;

import consulo.application.impl.internal.start.CommandLineArgs;
import consulo.builtinWebServer.impl.http.BuiltInServer;
import consulo.builtinWebServer.impl.http.ImportantFolderLockerViaBuiltInServer;
import consulo.builtinWebServer.impl.http.MessageDecoder;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.application.JetBrainsProtocolHandler;
import consulo.ide.impl.idea.openapi.util.io.FileUtilRt;
import consulo.ide.impl.idea.util.NotNullProducer;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.logging.Logger;
import consulo.ide.localize.IdeLocalize;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.Notifications;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.MultiMap;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * @author mike
 */
public final class DesktopImportantFolderLocker implements ImportantFolderLockerViaBuiltInServer {
  private static final String PORT_FILE = "port";
  private static final String PORT_LOCK_FILE = "port.lock";
  private static final String TOKEN_FILE = "token";

  private static final String ACTIVATE_COMMAND = "activate ";
  private static final String PID_COMMAND = "pid";
  private static final String PATHS_EOT_RESPONSE = "---";
  private static final String OK_RESPONSE = "ok";

  private final String myConfigPath;
  private final String mySystemPath;
  private final AtomicReference<Consumer<CommandLineArgs>> myActivateListener = new AtomicReference<>();
  private String myToken;
  private BuiltInServer myServer;

  public DesktopImportantFolderLocker(@Nonnull String configPath, @Nonnull String systemPath) {
    myConfigPath = canonicalPath(configPath);
    mySystemPath = canonicalPath(systemPath);
  }

  @Override
  public void dispose() {
    log("enter: dispose()");

    BuiltInServer server = myServer;
    if (server == null) return;

    try {
      Disposer.dispose(server);
    }
    finally {
      try {
        underLocks(() -> {
          FileUtil.delete(new File(myConfigPath, PORT_FILE));
          FileUtil.delete(new File(mySystemPath, PORT_FILE));
          FileUtil.delete(new File(mySystemPath, TOKEN_FILE));
          return null;
        });
      }
      catch (Exception e) {
        Logger.getInstance(DesktopImportantFolderLocker.class).warn(e);
      }
    }
  }

  @Override
  @Nullable
  public BuiltInServer getServer() {
    return myServer;
  }

  @Override
  public void setExternalInstanceListener(Consumer<CommandLineArgs> argsConsumer) {
    myActivateListener.set(argsConsumer);
  }

  @Override
  @Nonnull
  public ActivateStatus lock(@Nonnull String[] args) throws Exception {
    log("enter: lock(config=%s system=%s)", myConfigPath, mySystemPath);

    return underLocks(() -> {
      File portMarkerC = new File(myConfigPath, PORT_FILE);
      File portMarkerS = new File(mySystemPath, PORT_FILE);

      MultiMap<Integer, String> portToPath = MultiMap.createSmart();
      addExistingPort(portMarkerC, myConfigPath, portToPath);
      addExistingPort(portMarkerS, mySystemPath, portToPath);
      if (!portToPath.isEmpty()) {
        for (Map.Entry<Integer, Collection<String>> entry : portToPath.entrySet()) {
          ActivateStatus status = tryActivate(entry.getKey(), entry.getValue(), args);
          if (status != ActivateStatus.NO_INSTANCE) {
            log("exit: lock(): " + status);
            return status;
          }
        }
      }

      if (isShutdownCommand()) {
        System.exit(0);
      }

      myToken = UUID.randomUUID().toString();
      String[] lockedPaths = {myConfigPath, mySystemPath};
      int workerCount = 1;
      NotNullProducer<ChannelHandler> handler = () -> new MyChannelInboundHandler(lockedPaths, myActivateListener, myToken);
      myServer = BuiltInServer.startNioOrOio(workerCount, 6942, 50, false, handler);

      byte[] portBytes = Integer.toString(myServer.getPort()).getBytes(StandardCharsets.UTF_8);
      FileUtil.writeToFile(portMarkerC, portBytes);
      FileUtil.writeToFile(portMarkerS, portBytes);

      File tokenFile = new File(mySystemPath, TOKEN_FILE);
      FileUtil.writeToFile(tokenFile, myToken.getBytes(StandardCharsets.UTF_8));
      PosixFileAttributeView view = Files.getFileAttributeView(tokenFile.toPath(), PosixFileAttributeView.class);
      if (view != null) {
        try {
          view.setPermissions(ContainerUtil.newHashSet(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        }
        catch (IOException e) {
          log(e);
        }
      }

      log("exit: lock(): succeed");
      return ActivateStatus.NO_INSTANCE;
    });
  }

  private <V> V underLocks(@Nonnull Callable<V> action) throws Exception {
    FileUtil.createDirectory(new File(myConfigPath));
    try (@SuppressWarnings("unused") FileOutputStream lock1 = new FileOutputStream(new File(myConfigPath, PORT_LOCK_FILE), true)) {
      FileUtil.createDirectory(new File(mySystemPath));
      try (@SuppressWarnings("unused") FileOutputStream lock2 = new FileOutputStream(new File(mySystemPath, PORT_LOCK_FILE), true)) {
        return action.call();
      }
    }
  }

  private static void addExistingPort(@Nonnull File portMarker, @Nonnull String path, @Nonnull MultiMap<Integer, String> portToPath) {
    if (portMarker.exists()) {
      try {
        portToPath.putValue(Integer.parseInt(FileUtilRt.loadFile(portMarker)), path);
      }
      catch (Exception e) {
        log(e);
        // don't delete - we overwrite it on write in any case
      }
    }
  }

  @Nonnull
  private ActivateStatus tryActivate(int portNumber, @Nonnull Collection<String> paths, @Nonnull String[] args) {
    log("trying: port=%s", portNumber);
    args = checkForJetBrainsProtocolCommand(args);
    try {
      try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), portNumber)) {
        socket.setSoTimeout(1000);

        boolean result = false;
        @SuppressWarnings("IOResourceOpenedButNotSafelyClosed") DataInputStream in = new DataInputStream(socket.getInputStream());
        while (true) {
          try {
            String path = in.readUTF();
            log("read: path=%s", path);
            if (PATHS_EOT_RESPONSE.equals(path)) {
              break;
            }
            else if (paths.contains(path)) {
              result = true;  // don't break - read all input
            }
          }
          catch (IOException e) {
            log("read: %s", e.getMessage());
            break;
          }
        }

        if (result) {
          try {
            String token = FileUtilRt.loadFile(new File(mySystemPath, TOKEN_FILE));
            @SuppressWarnings("IOResourceOpenedButNotSafelyClosed") DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF(ACTIVATE_COMMAND + token + "\0" + new File(".").getAbsolutePath() + "\0" + StringUtil.join(args, "\0"));
            out.flush();
            String response = in.readUTF();
            log("read: response=%s", response);
            if (response.equals(OK_RESPONSE)) {
              if (isShutdownCommand()) {
                printPID(portNumber);
              }
              return ActivateStatus.ACTIVATED;
            }
          }
          catch (IOException e) {
            log(e);
          }

          return ActivateStatus.CANNOT_ACTIVATE;
        }
      }
    }
    catch (ConnectException e) {
      log("%s (stale port file?)", e.getMessage());
    }
    catch (IOException e) {
      log(e);
    }

    return ActivateStatus.NO_INSTANCE;
  }

  @SuppressWarnings("ALL")
  private static void printPID(int port) {
    try {
      Socket socket = new Socket(InetAddress.getLoopbackAddress(), port);
      socket.setSoTimeout(1000);
      @SuppressWarnings("IOResourceOpenedButNotSafelyClosed") DataOutputStream out = new DataOutputStream(socket.getOutputStream());
      out.writeUTF(PID_COMMAND);
      DataInputStream in = new DataInputStream(socket.getInputStream());
      int pid = 0;
      while (true) {
        try {
          String s = in.readUTF();
          if (Pattern.matches("[0-9]+@.*", s)) {
            pid = Integer.parseInt(s.substring(0, s.indexOf('@')));
            System.err.println(pid);
          }
        }
        catch (IOException e) {
          break;
        }
      }
    }
    catch (Exception ignore) {
    }
  }

  private static boolean isShutdownCommand() {
    return "shutdown".equals(JetBrainsProtocolHandler.getCommand());
  }

  private static String[] checkForJetBrainsProtocolCommand(String[] args) {
    final String jbUrl = System.getProperty(JetBrainsProtocolHandler.class.getName());
    if (jbUrl != null) {
      return new String[]{jbUrl};
    }
    return args;
  }

  private static class MyChannelInboundHandler extends MessageDecoder {
    private enum State {
      HEADER,
      CONTENT
    }

    private final String[] myLockedPaths;
    private final AtomicReference<Consumer<CommandLineArgs>> myActivateListener;
    private final String myToken;
    private State myState = State.HEADER;

    public MyChannelInboundHandler(@Nonnull String[] lockedPaths, @Nonnull AtomicReference<Consumer<CommandLineArgs>> activateListener, @Nonnull String token) {
      myLockedPaths = lockedPaths;
      myActivateListener = activateListener;
      myToken = token;
    }

    @Override
    public void channelActive(ChannelHandlerContext context) throws Exception {
      ByteBuf buffer = context.alloc().ioBuffer(1024);
      boolean success = false;
      try {
        ByteBufOutputStream out = new ByteBufOutputStream(buffer);
        for (String path : myLockedPaths) out.writeUTF(path);
        out.writeUTF(PATHS_EOT_RESPONSE);
        out.close();
        success = true;
      }
      finally {
        if (!success) {
          buffer.release();
        }
      }
      context.writeAndFlush(buffer);
    }

    @Override
    protected void messageReceived(@Nonnull ChannelHandlerContext context, @Nonnull ByteBuf input) throws Exception {
      while (true) {
        switch (myState) {
          case HEADER: {
            ByteBuf buffer = getBufferIfSufficient(input, 2, context);
            if (buffer == null) {
              return;
            }

            contentLength = buffer.readUnsignedShort();
            if (contentLength > 8192) {
              context.close();
              return;
            }
            myState = State.CONTENT;
          }
          break;

          case CONTENT: {
            CharSequence command = readChars(input);
            if (command == null) {
              return;
            }

            if (StringUtil.startsWith(command, PID_COMMAND)) {
              ByteBuf buffer = context.alloc().ioBuffer();
              ByteBufOutputStream out = new ByteBufOutputStream(buffer);
              String name = ManagementFactory.getRuntimeMXBean().getName();
              out.writeUTF(name);
              out.close();
              context.writeAndFlush(buffer);
            }

            if (StringUtil.startsWith(command, ACTIVATE_COMMAND)) {
              String data = command.subSequence(ACTIVATE_COMMAND.length(), command.length()).toString();
              List<String> args = StringUtil.split(data, data.contains("\0") ? "\0" : "\uFFFD");

              boolean tokenOK = !args.isEmpty() && myToken.equals(args.get(0));
              if (!tokenOK) {
                log(new UnsupportedOperationException("unauthorized request: " + command));
                Notifications.Bus.notify(new Notification(
                  Notifications.SYSTEM_MESSAGES_GROUP,
                  IdeLocalize.activationAuthTitle().get(),
                  IdeLocalize.activationAuthMessage().get(),
                  NotificationType.WARNING
                ));
              }
              else {
                Consumer<CommandLineArgs> listener = myActivateListener.get();
                if (listener != null) {
                  listener.accept(CommandLineArgs.parse(ArrayUtil.toStringArray(args)));
                }
              }

              ByteBuf buffer = context.alloc().ioBuffer(4);
              ByteBufOutputStream out = new ByteBufOutputStream(buffer);
              out.writeUTF(OK_RESPONSE);
              out.close();
              context.writeAndFlush(buffer);
            }
            context.close();
          }
          break;
        }
      }
    }
  }

  @Nonnull
  private static String canonicalPath(@Nonnull String configPath) {
    try {
      return new File(configPath).getCanonicalPath();
    }
    catch (IOException ignore) {
      return configPath;
    }
  }

  private static void log(Exception e) {
    Logger.getInstance(DesktopImportantFolderLocker.class).warn(e);
  }

  private static void log(String format, Object... args) {
    Logger logger = Logger.getInstance(DesktopImportantFolderLocker.class);
    if (logger.isDebugEnabled()) {
      logger.debug(String.format(format, args));
    }
  }
}