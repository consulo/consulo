// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.util.windows.defender;

import com.sun.jna.Memory;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.COM.COMException;
import com.sun.jna.platform.win32.COM.Wbemcli;
import com.sun.jna.platform.win32.COM.WbemcliUtil;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.KnownFolders;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ApplicationPropertiesComponent;
import consulo.application.util.WindowsDefenderCheckerExcludePathProvider;
import consulo.container.boot.ContainerPathManager;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.ide.impl.idea.diagnostic.DiagnosticBundle;
import consulo.logging.Logger;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.util.CapturingProcessUtil;
import consulo.process.util.ProcessOutput;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.jna.JnaLoader;
import consulo.util.lang.StringUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * Sources:
 * <a href="https://learn.microsoft.com/en-us/microsoft-365/security/defender-endpoint/configure-extension-file-exclusions-microsoft-defender-antivirus">Defender Settings</a>,
 * <a href="https://learn.microsoft.com/en-us/powershell/module/defender/">Defender PowerShell Module</a>.
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class WindowsDefenderChecker {
    private static final Logger LOG = Logger.getInstance(WindowsDefenderChecker.class);

    private static final String IGNORE_VIRUS_CHECK = "ignore.virus.scanning.warn.message";
    private static final int WMI_QUERY_TIMEOUT_MS = 10_000;

    private final Application myApplication;

    @Inject
    public WindowsDefenderChecker(Application application) {
        myApplication = application;
    }

    public static WindowsDefenderChecker getInstance() {
        return Application.get().getInstance(WindowsDefenderChecker.class);
    }

    public boolean isVirusCheckIgnored(@Nullable Project project) {
        return ApplicationPropertiesComponent.getInstance().isTrueValue(IGNORE_VIRUS_CHECK)
            || (project != null && ProjectPropertiesComponent.getInstance(project).isTrueValue(IGNORE_VIRUS_CHECK));
    }

    /**
     * {@link Boolean#TRUE} means Defender is present, active, and real-time protection check is enabled.
     * {@link Boolean#FALSE} means something from the above list is not true.
     * {@code null} means the IDE cannot detect the status.
     */
    public @Nullable Boolean isRealTimeProtectionEnabled() {
        if (!JnaLoader.isLoaded()) {
            LOG.debug("isRealTimeProtectionEnabled: JNA is not loaded");
            return null;
        }

        try {
            WinNT.HRESULT comInit = Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_APARTMENTTHREADED);
            if (LOG.isDebugEnabled()) LOG.debug("CoInitializeEx: " + comInit);

            WbemcliUtil.WmiQuery<AntivirusProduct> avQuery =
                new WbemcliUtil.WmiQuery<>("Root\\SecurityCenter2", "AntivirusProduct", AntivirusProduct.class);
            WbemcliUtil.WmiResult<AntivirusProduct> avResult = avQuery.execute(WMI_QUERY_TIMEOUT_MS);
            if (LOG.isDebugEnabled()) LOG.debug(avQuery.getWmiClassName() + ": " + avResult.getResultCount());
            for (int i = 0; i < avResult.getResultCount(); i++) {
                Object name = avResult.getValue(AntivirusProduct.DisplayName, i);
                if (LOG.isDebugEnabled()) LOG.debug("DisplayName[" + i + "]: " + name + " (" + name.getClass().getName() + ')');
                if (name instanceof String s && (s.contains("Windows Defender") || s.contains("Microsoft Defender"))) {
                    Object state = avResult.getValue(AntivirusProduct.ProductState, i);
                    if (LOG.isDebugEnabled()) LOG.debug("ProductState: " + state + " (" + state.getClass().getName() + ')');
                    boolean enabled = state instanceof Integer intState && (intState.intValue() & 0x1000) != 0;
                    if (!enabled) return false;
                    break;
                }
            }

            WbemcliUtil.WmiQuery<MpComputerStatus> statusQuery =
                new WbemcliUtil.WmiQuery<>("Root\\Microsoft\\Windows\\Defender", "MSFT_MpComputerStatus", MpComputerStatus.class);
            WbemcliUtil.WmiResult<MpComputerStatus> statusResult = statusQuery.execute(WMI_QUERY_TIMEOUT_MS);
            if (LOG.isDebugEnabled()) LOG.debug(statusQuery.getWmiClassName() + ": " + statusResult.getResultCount());
            if (statusResult.getResultCount() != 1) return false;
            Object rtProtection = statusResult.getValue(MpComputerStatus.RealTimeProtectionEnabled, 0);
            if (LOG.isDebugEnabled()) LOG.debug("RealTimeProtectionEnabled: " + rtProtection + " (" + rtProtection.getClass().getName() + ')');
            return Boolean.TRUE.equals(rtProtection);
        }
        catch (COMException e) {
            // reference: https://learn.microsoft.com/en-us/windows/win32/wmisdk/wmi-error-constants
            if (e.matchesErrorCode(Wbemcli.WBEM_E_INVALID_NAMESPACE)) return false;  // Microsoft Defender not installed
            String message = "WMI Microsoft Defender check failed";
            WinNT.HRESULT hresult = e.getHresult();
            if (hresult != null) message += " [0x" + Integer.toHexString(hresult.intValue()) + ']';
            LOG.warn(message, e);
            return null;
        }
        catch (Exception e) {
            LOG.warn("WMI Microsoft Defender check failed", e);
            return null;
        }
    }

    private enum AntivirusProduct {DisplayName, ProductState}

    private enum MpComputerStatus {RealTimeProtectionEnabled}

    public boolean isUntrustworthyLocation(Path path) {
        String tempVar = System.getenv("TEMP");
        if (tempVar != null && path.startsWith(Paths.get(tempVar))) {
            return true;
        }

        Path downloadDir = null;
        if (JnaLoader.isLoaded()) {
            try {
                downloadDir = Paths.get(Shell32Util.getKnownFolderPath(KnownFolders.FOLDERID_Downloads));
            }
            catch (Exception e) {
                LOG.warn("download dir detection failed", e);
            }
        }
        if (downloadDir == null) {
            downloadDir = Paths.get(System.getProperty("user.home"), "Downloads");
        }
        if (path.startsWith(downloadDir)) {
            return true;
        }

        return false;
    }

    /**
     * Returns a list of paths that might impact build performance if Windows Defender were configured to scan them.
     */
    public List<Path> getPathsToExclude(@Nullable Project project) {
        TreeSet<Path> paths = new TreeSet<>();
        paths.add(Paths.get(ContainerPathManager.get().getSystemPath()));

        String basePath = project == null ? null : project.getBasePath();
        Path projectPath = basePath == null ? null : Paths.get(basePath);

        myApplication.getExtensionPoint(WindowsDefenderCheckerExcludePathProvider.class).forEach(provider -> {
            provider.collectPaths(project, projectPath, path -> paths.add(path.toAbsolutePath()));
        });

        if (projectPath != null) {
            paths.add(projectPath);
        }

        return new ArrayList<>(paths);
    }

    public List<Path> filterDevDrivePaths(List<Path> paths) {
        if (paths.isEmpty()) return paths;

        if (!JnaLoader.isLoaded()) {
            LOG.debug("filterDevDrivePaths: JNA is not loaded");
            return paths;
        }

        Long buildNumber = getWinBuildNumber();
        if (buildNumber == null || buildNumber < 22621) {
            if (LOG.isDebugEnabled()) LOG.debug("DevDrive feature is not supported on " + buildNumber);
            return paths;
        }

        try (FILE_FS_PERSISTENT_VOLUME_INFORMATION volInfo = new FILE_FS_PERSISTENT_VOLUME_INFORMATION()) {
            return paths.stream().filter(path -> !isOnDevDrive(path, volInfo)).toList();
        }
        catch (Exception e) {
            LOG.warn("DevDrive detection failed", e);
            return paths;
        }
    }

    private static @Nullable Long getWinBuildNumber() {
        String osVersion = System.getProperty("os.version");
        if (osVersion == null) return null;
        try {
            String[] parts = osVersion.split("\\.");
            if (parts.length >= 3) return Long.parseLong(parts[2]);
        }
        catch (NumberFormatException ignored) {
        }
        return null;
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static final int FSCTL_QUERY_PERSISTENT_VOLUME_STATE = 0x9023C;
    private static final int PERSISTENT_VOLUME_STATE_DEV_VOLUME = 0x00002000;
    private static final int PERSISTENT_VOLUME_STATE_TRUSTED_VOLUME = 0x00004000;

    @SuppressWarnings({"unused", "FieldMayBeFinal"})
    @Structure.FieldOrder({"VolumeFlags", "FlagMask", "Version", "Reserved"})
    public static final class FILE_FS_PERSISTENT_VOLUME_INFORMATION extends Structure implements AutoCloseable {
        public int VolumeFlags;
        public int FlagMask;
        public int Version;
        public int Reserved;

        @Override
        public void close() {
            if (getPointer() instanceof Memory m) m.close();
        }
    }

    // https://learn.microsoft.com/en-us/windows-hardware/drivers/ddi/ntifs/ns-ntifs-_file_fs_persistent_volume_information
    private static boolean isOnDevDrive(Path path, FILE_FS_PERSISTENT_VOLUME_INFORMATION volInfo) {
        WinNT.HANDLE handle = Kernel32.INSTANCE.CreateFile(
            path.toString(), WinNT.FILE_READ_ATTRIBUTES, WinNT.FILE_SHARE_READ | WinNT.FILE_SHARE_WRITE, null, WinNT.OPEN_EXISTING,
            WinNT.FILE_FLAG_BACKUP_SEMANTICS, null);
        if (handle == WinBase.INVALID_HANDLE_VALUE) {
            int err = Kernel32.INSTANCE.GetLastError();
            LOG.warn("CreateFile(" + path + "): " + err + ": " + Kernel32Util.formatMessageFromLastErrorCode(err));
            return false;
        }
        try {
            volInfo.FlagMask = PERSISTENT_VOLUME_STATE_DEV_VOLUME | PERSISTENT_VOLUME_STATE_TRUSTED_VOLUME;
            volInfo.Version = 1;
            volInfo.write();
            if (Kernel32.INSTANCE.DeviceIoControl(handle, FSCTL_QUERY_PERSISTENT_VOLUME_STATE,
                volInfo.getPointer(), volInfo.size(), volInfo.getPointer(), volInfo.size(), null, null)) {
                volInfo.read();
                if (LOG.isDebugEnabled()) LOG.debug(path + ": 0x" + Integer.toHexString(volInfo.VolumeFlags));
                return volInfo.VolumeFlags == (PERSISTENT_VOLUME_STATE_DEV_VOLUME | PERSISTENT_VOLUME_STATE_TRUSTED_VOLUME);
            }
            else {
                if (LOG.isDebugEnabled()) {
                    int err = Kernel32.INSTANCE.GetLastError();
                    LOG.debug("DeviceIoControl(" + path + "): " + err + ": " + Kernel32Util.formatMessageFromLastErrorCode(err));
                }
                return false;
            }
        }
        finally {
            Kernel32.INSTANCE.CloseHandle(handle);
        }
    }

    public void configureActions(Project project, WindowsDefenderNotification notification) {
        notification.addAction(new WindowsDefenderFixAction(myApplication, notification.getPaths()));

        notification.addAction(new NotificationAction(ExternalServiceLocalize.virusScanningDontShowAgain()) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e, Notification notification) {
                notification.expire();
                ApplicationPropertiesComponent.getInstance().setValue(IGNORE_VIRUS_CHECK, "true");
            }
        });
        notification.addAction(new NotificationAction(ExternalServiceLocalize.virusScanningDontShowAgainThisProject()) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e, Notification notification) {
                notification.expire();
                ProjectPropertiesComponent.getInstance(project).setValue(IGNORE_VIRUS_CHECK, "true");
            }
        });
    }

    public String getConfigurationInstructionsUrl() {
        return "https://intellij-support.jetbrains.com/hc/en-us/articles/360006298560";
    }

    public boolean runExcludePathsCommand(@Nullable Project project, Collection<Path> paths) {
        try {
            ProcessOutput output = CapturingProcessUtil.execAndGetOutput(
                new GeneralCommandLine("powershell",
                    "-Command",
                    "Add-MpPreference",
                    "-ExclusionPath",
                    StringUtil.join(paths, (path) -> StringUtil.wrapWithDoubleQuote(path.toString()), ",")).withSudo(""));
            return output.getExitCode() == 0;
        }
        catch (ExecutionException e) {
            UIUtil.invokeLaterIfNeeded(() -> Messages.showErrorDialog(project,
                DiagnosticBundle.message("virus.scanning.fix.failed", e.getMessage()),
                DiagnosticBundle.message("virus.scanning.fix.title")));
        }
        return false;
    }
}
