/*
 * Copyright 2013-2025 consulo.io
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
package consulo.desktop.awt.platform.impl;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import consulo.application.util.*;
import consulo.application.util.concurrent.PooledThreadExecutor;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.internal.CapturingProcessHandler;
import consulo.process.io.ProcessIOExecutorService;
import consulo.process.local.ExecUtil;
import consulo.process.util.CapturingProcessUtil;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.util.io.FileUtil;
import consulo.util.jna.JnaLoader;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.io.*;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author VISTALL
 * @since 2025-09-05
 */
public class FileManagerProxy {
    private static final Logger LOG = Logger.getInstance(FileManagerProxy.class);

    private interface Shell32Ex extends StdCallLibrary {
        Shell32Ex INSTANCE = Native.load("shell32", Shell32Ex.class, W32APIOptions.DEFAULT_OPTIONS);

        Pointer ILCreateFromPath(String path);

        void ILFree(Pointer pIdl);

        WinNT.HRESULT SHOpenFolderAndSelectItems(Pointer pIdlFolder, WinDef.UINT cIdl, Pointer[] apIdl, WinDef.DWORD dwFlags);
    }

    private static final NullableLazyValue<String> fileManagerApp = new AtomicNullableLazyValue<>() {
        @Override
        protected String compute() {
            return readDesktopEntryKey("Exec").map(line -> line.split(" ")[0])
                .filter(exec -> exec.endsWith("nautilus") || exec.endsWith("pantheon-files"))
                .orElse(null);
        }
    };

    private static final NotNullLazyValue<String> fileManagerName = new AtomicNotNullLazyValue<>() {
        @Nonnull
        @Override
        protected String compute() {
            if (Platform.current().os().isMac()) {
                return "Finder";
            }
            if (Platform.current().os().isWindows()) {
                return "Explorer";
            }
            return readDesktopEntryKey("Name").orElse("File Manager");
        }
    };

    private static Optional<String> readDesktopEntryKey(String key) {
        if (SystemInfo.hasXdgMime()) {
            String appName = ExecUtil.execAndReadLine(new GeneralCommandLine("xdg-mime", "query", "default", "inode/directory"));
            if (appName != null && appName.endsWith(".desktop")) {
                return Stream.of(getXdgDataDirectories().split(":"))
                    .map(dir -> new File(dir, "applications/" + appName))
                    .filter(File::exists)
                    .findFirst()
                    .map(file -> readDesktopEntryKey(file, key));
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
        return StringUtil.defaultIfEmpty(dataHome, Platform.current().user().homePath() + "/.local/share") +
            ':' + StringUtil.defaultIfEmpty(dataDirs, "/usr/local/share:/usr/share");
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
    public static void openFile(@Nonnull File file, @Nonnull UIAccess uiAccess) {
        if (!file.exists()) {
            return;
        }
        file = file.getAbsoluteFile();
        File parent = file.getParentFile();
        if (parent == null) {
            return;
        }
        try {
            doOpen(parent, file, uiAccess);
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
    public static void openDirectory(@Nonnull File directory, @Nonnull UIAccess uiAccess) {
        if (!directory.isDirectory()) {
            return;
        }
        try {
            doOpen(directory, null, uiAccess);
        }
        catch (Exception e) {
            LOG.warn(e);
        }
    }

    private static void doOpen(@Nonnull File _dir, @Nullable File _toSelect, @Nonnull UIAccess uiAccess) throws IOException, ExecutionException {
        String dir = FileUtil.toSystemDependentName(FileUtil.toCanonicalPath(_dir.getPath()));
        String toSelect = _toSelect != null ? FileUtil.toSystemDependentName(FileUtil.toCanonicalPath(_toSelect.getPath())) : null;

        if (Platform.current().os().isWindows()) {
            if (JnaLoader.isLoaded()) {
                openViaShellApi(dir, toSelect);
            }
            else {
                openViaExplorerCall(dir, toSelect);
            }
        }
        else if (Platform.current().os().isMac()) {
            GeneralCommandLine cmd =
                toSelect != null ? new GeneralCommandLine("open", "-R", toSelect) : new GeneralCommandLine("open", dir);
            schedule(cmd);
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
            uiAccess.give(() -> Messages.showErrorDialog("This action isn't supported on the current platform", "Cannot Open File"));
        }
    }


    private static void openViaShellApi(String dir, @Nullable String toSelect) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("shell open: dir=" + dir + " toSelect=" + toSelect);
        }

        ProcessIOExecutorService.INSTANCE.execute(() -> {
            Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_APARTMENTTHREADED);

            if (toSelect == null) {
                var res = Shell32.INSTANCE.ShellExecute(null, "explore", dir, null, null, WinUser.SW_NORMAL);
                if (res.intValue() <= 32) {
                    LOG.warn("ShellExecute(" + dir + "): " + res.intValue() + " GetLastError=" + Kernel32.INSTANCE.GetLastError());
                    openViaExplorerCall(dir, null);
                }
            }
            else {
                var pIdl = Shell32Ex.INSTANCE.ILCreateFromPath(dir);
                var apIdl = new Pointer[]{Shell32Ex.INSTANCE.ILCreateFromPath(toSelect)};
                var cIdl = new WinDef.UINT(apIdl.length);
                try {
                    var res = Shell32Ex.INSTANCE.SHOpenFolderAndSelectItems(pIdl, cIdl, apIdl, new WinDef.DWORD(0));
                    if (!WinError.S_OK.equals(res)) {
                        LOG.warn("SHOpenFolderAndSelectItems(" + dir + ',' + toSelect + "): 0x" + Integer.toHexString(res.intValue()));
                        openViaExplorerCall(dir, toSelect);
                    }
                }
                finally {
                    Shell32Ex.INSTANCE.ILFree(pIdl);
                    Shell32Ex.INSTANCE.ILFree(apIdl[0]);
                }
            }
        });
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
        PooledThreadExecutor.getInstance().submit(() -> {
            try {
                LOG.debug(cmd.toString());
                CapturingProcessUtil.execAndGetOutput(cmd).checkSuccess(LOG);
            }
            catch (Exception e) {
                LOG.warn(e);
            }
        });
    }

    public static boolean isSupported() {
        return Platform.current().os().isWindows()
            || Platform.current().os().isMac()
            || SystemInfo.hasXdgOpen()
            || Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN);
    }

    private static void openViaExplorerCall(String dir, @Nullable String toSelect) {
        String cmd = toSelect != null ? "explorer /select,\"" + shortPath(toSelect) + '"' : "explorer /root,\"" + shortPath(dir) + '"';
        LOG.debug(cmd);
        PooledThreadExecutor.getInstance().execute(() -> {
            try {
                Process process = new ProcessBuilder(cmd).start();  // no advanced quoting/escaping is needed

                new CapturingProcessHandler(process, null, cmd).runProcess().checkSuccess(LOG);
            }
            catch (IOException e) {
                LOG.warn(e);
            }
        });
    }
}
