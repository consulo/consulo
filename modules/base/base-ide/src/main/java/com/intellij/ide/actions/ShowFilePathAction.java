/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.ide.actions;

import com.intellij.CommonBundle;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.util.ExecUtil;
import com.intellij.ide.DataManager;
import com.intellij.ide.IdeBundle;
import com.intellij.idea.ActionsBundle;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.SystemProperties;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef;
import consulo.annotation.UsedInPlugin;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.vfs.ArchiveFileSystem;
import consulo.vfs.util.ArchiveVfsUtil;
import org.jetbrains.ide.PooledThreadExecutor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ShowFilePathAction extends AnAction {
  private static final Logger LOG = Logger.getInstance(ShowFilePathAction.class);

  @UsedInPlugin
  public static final NotificationListener FILE_SELECTING_LISTENER = new NotificationListener.Adapter() {
    @Override
    protected void hyperlinkActivated(@Nonnull Notification notification, @Nonnull HyperlinkEvent e) {
      URL url = e.getURL();
      if (url != null) {
        try {
          openFile(new File(url.toURI()));
        }
        catch (URISyntaxException ex) {
          LOG.warn("invalid URL: " + url, ex);
        }
      }
      notification.expire();
    }
  };

  private static final NullableLazyValue<String> fileManagerApp = new AtomicNullableLazyValue<String>() {
    @Override
    protected String compute() {
      return readDesktopEntryKey("Exec").map(line -> line.split(" ")[0]).filter(exec -> exec.endsWith("nautilus") || exec.endsWith("pantheon-files")).orElse(null);
    }
  };

  private static final NotNullLazyValue<String> fileManagerName = new AtomicNotNullLazyValue<String>() {
    @Nonnull
    @Override
    protected String compute() {
      if (SystemInfo.isMac) return "Finder";
      if (SystemInfo.isWindows) return "Explorer";
      return readDesktopEntryKey("Name").orElse("File Manager");
    }
  };

  private static Optional<String> readDesktopEntryKey(String key) {
    if (SystemInfo.hasXdgMime()) {
      String appName = ExecUtil.execAndReadLine(new GeneralCommandLine("xdg-mime", "query", "default", "inode/directory"));
      if (appName != null && appName.endsWith(".desktop")) {
        return Stream.of(getXdgDataDirectories().split(":")).map(dir -> new File(dir, "applications/" + appName)).filter(File::exists).findFirst().map(file -> readDesktopEntryKey(file, key));
      }
    }

    return Optional.empty();
  }

  private static String readDesktopEntryKey(File file, String key) {
    LOG.debug("looking for '" + key + "' in " + file);
    String prefix = key + '=';
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      return reader.lines().filter(l -> l.startsWith(prefix)).map(l -> l.substring(prefix.length())).findFirst().orElse(null);
    }
    catch (IOException | UncheckedIOException e) {
      LOG.info("Cannot read: " + file, e);
      return null;
    }
  }

  private static String getXdgDataDirectories() {
    String dataHome = System.getenv("XDG_DATA_HOME");
    String dataDirs = System.getenv("XDG_DATA_DIRS");
    return StringUtil.defaultIfEmpty(dataHome, SystemProperties.getUserHome() + "/.local/share") + ':' + StringUtil.defaultIfEmpty(dataDirs, "/usr/local/share:/usr/share");
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    boolean visible = !SystemInfo.isMac && isSupported();
    e.getPresentation().setVisible(visible);
    if (visible) {
      VirtualFile file = getFile(e);
      e.getPresentation().setEnabled(file != null);
      e.getPresentation().setText(ActionsBundle.message("action.ShowFilePath.tuned", file != null && file.isDirectory() ? 1 : 0));
    }
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    show(getFile(e), popup -> DataManager.getInstance().getDataContextFromFocus().doWhenDone(popup::showInBestPositionFor));
  }

  public static void show(final VirtualFile file, final MouseEvent e) {
    show(file, popup -> {
      if (e.getComponent().isShowing()) {
        popup.show(new RelativePoint(e));
      }
    });
  }

  public static void show(final VirtualFile file, final Consumer<ListPopup> action) {
    if (!isSupported()) return;

    List<VirtualFile> files = new ArrayList<>();
    List<String> fileUrls = new ArrayList<>();
    VirtualFile eachParent = file;
    while (eachParent != null) {
      int index = files.size();
      files.add(index, eachParent);
      fileUrls.add(index, getPresentableUrl(eachParent));
      if (eachParent.getParent() == null && eachParent.getFileSystem() instanceof ArchiveFileSystem) {
        eachParent = ArchiveVfsUtil.getVirtualFileForArchive(eachParent);
        if (eachParent == null) break;
      }
      eachParent = eachParent.getParent();
    }

    Platform.FileSystem fs = Platform.current().fs();
    Application.get().executeOnPooledThread(() -> {
      List<Image> icons = new ArrayList<>();
      for (String url : fileUrls) {
        File ioFile = new File(url);
        icons.add(ioFile.exists() ? fs.getImage(ioFile) : Image.empty(16));
      }

      Application.get().invokeLater(() -> action.accept(createPopup(files, icons)));
    });
  }

  private static String getPresentableUrl(final VirtualFile eachParent) {
    String url = eachParent.getPresentableUrl();
    if (eachParent.getParent() == null && SystemInfo.isWindows) {
      url += "\\";
    }
    return url;
  }

  private static ListPopup createPopup(List<VirtualFile> files, List<Image> icons) {
    final BaseListPopupStep<VirtualFile> step = new BaseListPopupStep<VirtualFile>("File Path", files, icons) {
      @Nonnull
      @Override
      public String getTextFor(final VirtualFile value) {
        return value.getPresentableName();
      }

      @Override
      public PopupStep onChosen(final VirtualFile selectedValue, final boolean finalChoice) {
        final File selectedFile = new File(getPresentableUrl(selectedValue));
        if (selectedFile.exists()) {
          Application.get().executeOnPooledThread((Runnable)() -> openFile(selectedFile));
        }
        return FINAL_CHOICE;
      }
    };

    return JBPopupFactory.getInstance().createListPopup(step);
  }

  public static boolean isSupported() {
    return SystemInfo.isWindows || SystemInfo.isMac || SystemInfo.hasXdgOpen() || Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN);
  }

  @Nonnull
  public static String getFileManagerName() {
    return fileManagerName.getValue();
  }

  /**
   * Shows system file manager with given file's parent directory open and the file highlighted in it<br/>
   * (note that not all platforms support highlighting).
   *
   * @param file a file or directory to show and highlight in a file manager.
   */
  public static void openFile(@Nonnull File file) {
    if (!file.exists()) return;
    file = file.getAbsoluteFile();
    File parent = file.getParentFile();
    if (parent == null) {
      return;
    }
    try {
      doOpen(parent, file);
    }
    catch (Exception e) {
      LOG.warn(e);
    }
  }

  /**
   * Shows system file manager with given directory open in it.
   *
   * @param directory a directory to show in a file manager.
   */
  @SuppressWarnings("UnusedDeclaration")
  public static void openDirectory(@Nonnull final File directory) {
    if (!directory.isDirectory()) return;
    try {
      doOpen(directory, null);
    }
    catch (Exception e) {
      LOG.warn(e);
    }
  }

  private static void doOpen(@Nonnull File _dir, @Nullable File _toSelect) throws IOException, ExecutionException {
    String dir = FileUtil.toSystemDependentName(FileUtil.toCanonicalPath(_dir.getPath()));
    String toSelect = _toSelect != null ? FileUtil.toSystemDependentName(FileUtil.toCanonicalPath(_toSelect.getPath())) : null;

    if (SystemInfo.isWindows) {
      String cmd = toSelect != null ? "explorer /select,\"" + shortPath(toSelect) + '"' : "explorer /root,\"" + shortPath(dir) + '"';
      LOG.debug(cmd);
      Process process = Runtime.getRuntime().exec(cmd);  // no advanced quoting/escaping is needed
      new CapturingProcessHandler(process, null, cmd).runProcess().checkSuccess(LOG);
    }
    else if (SystemInfo.isMac) {
      GeneralCommandLine cmd = toSelect != null ? new GeneralCommandLine("open", "-R", toSelect) : new GeneralCommandLine("open", dir);
      LOG.debug(cmd.toString());
      ExecUtil.execAndGetOutput(cmd).checkSuccess(LOG);
    }
    else if (fileManagerApp.getValue() != null) {
      schedule(new GeneralCommandLine(fileManagerApp.getValue(), toSelect != null ? toSelect : dir));
    }
    else if (SystemInfo.hasXdgOpen()) {
      schedule(new GeneralCommandLine("xdg-open", dir));
    }
    else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
      LOG.debug("opening " + dir + " via desktop API");
      Desktop.getDesktop().open(new File(dir));
    }
    else {
      Messages.showErrorDialog("This action isn't supported on the current platform", "Cannot Open File");
    }
  }

  private static String shortPath(String path) {
    if (path.contains("  ")) {
      // On the way from Runtime.exec() to CreateProcess(), a command line goes through couple rounds of merging and splitting
      // which breaks paths containing a sequence of two or more spaces.
      // Conversion to a short format is an ugly hack allowing to open such paths in Explorer.
      char[] result = new char[WinDef.MAX_PATH];
      if (Kernel32.INSTANCE.GetShortPathName(path, result, result.length) <= result.length) {
        return Native.toString(result);
      }
    }

    return path;
  }

  private static void schedule(GeneralCommandLine cmd) {
    PooledThreadExecutor.INSTANCE.submit(() -> {
      try {
        LOG.debug(cmd.toString());
        ExecUtil.execAndGetOutput(cmd).checkSuccess(LOG);
      }
      catch (Exception e) {
        LOG.warn(e);
      }
    });
  }

  @Nullable
  private static VirtualFile getFile(final AnActionEvent e) {
    return e.getData(CommonDataKeys.VIRTUAL_FILE);
  }

  public static Boolean showDialog(Project project, String message, String title, File file) {
    final Boolean[] ref = new Boolean[1];
    final DialogWrapper.DoNotAskOption option = new DialogWrapper.DoNotAskOption() {
      @Override
      public boolean isToBeShown() {
        return true;
      }

      @Override
      public void setToBeShown(boolean value, int exitCode) {
        if (!value) {
          ref[0] = exitCode == 0;
        }
      }

      @Override
      public boolean canBeHidden() {
        return true;
      }

      @Override
      public boolean shouldSaveOptionsOnCancel() {
        return true;
      }

      @Nonnull
      @Override
      public String getDoNotShowMessage() {
        return CommonBundle.message("dialog.options.do.not.ask");
      }
    };
    showDialog(project, message, title, file, option);
    return ref[0];
  }

  public static void showDialog(Project project, String message, String title, File file, DialogWrapper.DoNotAskOption option) {
    if (Messages.showOkCancelDialog(project, message, title, RevealFileAction.getActionName(), IdeBundle.message("action.close"), Messages.getInformationIcon(), option) == Messages.OK) {
      openFile(file);
    }
  }

  @Nullable
  public static VirtualFile findLocalFile(@Nullable VirtualFile file) {
    if (file == null) return null;

    if (file.isInLocalFileSystem()) {
      return file;
    }

    VirtualFileSystem fs = file.getFileSystem();
    if (fs instanceof ArchiveFileSystem && file.getParent() == null) {
      return ((ArchiveFileSystem)fs).getLocalVirtualFileFor(file);
    }

    return null;
  }
}
