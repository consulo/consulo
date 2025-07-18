// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.util.windows.defender;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ApplicationPropertiesComponent;
import consulo.application.internal.ApplicationInfo;
import consulo.container.boot.ContainerPathManager;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.diagnostic.DiagnosticBundle;
import consulo.logging.Logger;
import consulo.platform.Platform;
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
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.ManagingFS;
import consulo.virtualFileSystem.internal.PersistentFS;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class WindowsDefenderChecker {
    private static final Logger LOG = Logger.getInstance(WindowsDefenderChecker.class);

    private static final Pattern WINDOWS_ENV_VAR_PATTERN = Pattern.compile("%([^%]+?)%");
    private static final Pattern WINDOWS_DEFENDER_WILDCARD_PATTERN = Pattern.compile("[?*]");
    private static final int WMIC_COMMAND_TIMEOUT_MS = 10000;
    private static final int POWERSHELL_COMMAND_TIMEOUT_MS = 10000;
    private static final int MAX_POWERSHELL_STDERR_LENGTH = 500;
    private static final String IGNORE_VIRUS_CHECK = "ignore.virus.scanning.warn.message";

    public enum RealtimeScanningStatus {
        SCANNING_DISABLED,
        SCANNING_ENABLED,
        ERROR
    }

    public static class CheckResult {
        public final RealtimeScanningStatus status;

        // Value in the map is true if the path is excluded, false otherwise
        public final Map<Path, Boolean> pathStatus;

        public CheckResult(RealtimeScanningStatus status, Map<Path, Boolean> pathStatus) {
            this.status = status;
            this.pathStatus = pathStatus;
        }
    }

    @Nonnull
    private final Application myApplication;

    @Inject
    public WindowsDefenderChecker(@Nonnull Application application) {
        myApplication = application;
    }

    public static WindowsDefenderChecker getInstance() {
        return ServiceManager.getService(WindowsDefenderChecker.class);
    }

    public boolean isVirusCheckIgnored(Project project) {
        return ApplicationPropertiesComponent.getInstance().isTrueValue(IGNORE_VIRUS_CHECK) || ProjectPropertiesComponent.getInstance(project)
            .isTrueValue(
                IGNORE_VIRUS_CHECK);
    }

    public CheckResult checkWindowsDefender(@Nonnull Project project) {
        Boolean windowsDefenderActive = isWindowsDefenderActive();
        if (windowsDefenderActive == null || !windowsDefenderActive) {
            LOG.info("Windows Defender status: not used");
            return new CheckResult(RealtimeScanningStatus.SCANNING_DISABLED, Collections.emptyMap());
        }

        RealtimeScanningStatus scanningStatus = getRealtimeScanningEnabled();
        if (scanningStatus == RealtimeScanningStatus.SCANNING_ENABLED) {
            Collection<String> excludedProcesses = getExcludedProcesses();
            List<File> processesToCheck = getProcessesToCheck();
            if (excludedProcesses != null && ContainerUtil.all(processesToCheck,
                (exe) -> excludedProcesses.contains(exe.getName()
                    .toLowerCase(Locale.ENGLISH))) && excludedProcesses
                .contains("java.exe")) {
                LOG.info("Windows Defender status: all relevant processes excluded from real-time scanning");
                return new CheckResult(RealtimeScanningStatus.SCANNING_DISABLED, Collections.emptyMap());
            }

            List<Pattern> excludedPatterns = getExcludedPatterns();
            if (excludedPatterns != null) {
                Map<Path, Boolean> pathStatuses = checkPathsExcluded(getImportantPaths(project), excludedPatterns);
                boolean anyPathNotExcluded = !ContainerUtil.all(pathStatuses.values(), Boolean::booleanValue);
                if (anyPathNotExcluded) {
                    LOG.info("Windows Defender status: some relevant paths not excluded from real-time scanning, notifying user");
                }
                else {
                    LOG.info("Windows Defender status: all relevant paths excluded from real-time scanning");
                }
                return new CheckResult(scanningStatus, pathStatuses);
            }
        }
        if (scanningStatus == RealtimeScanningStatus.ERROR) {
            LOG.info("Windows Defender status: failed to detect");
        }
        else {
            LOG.info("Windows Defender status: real-time scanning disabled");
        }
        return new CheckResult(scanningStatus, Collections.emptyMap());
    }

    @Nonnull
    protected List<File> getProcessesToCheck() {
        List<File> result = new ArrayList<>();
        File ideStarter = new File(ContainerPathManager.get().getAppHomeDirectory(), getExecutableOnWindows());
        if (ideStarter.exists()) {
            result.add(ideStarter);
        }

        PersistentFS fs = (PersistentFS) ManagingFS.getInstance();

        Path fsNotifier = fs.getFileWatcherExecutablePath();
        if (fsNotifier != null) {
            result.add(fsNotifier.toFile());
        }
        return result;
    }

    /**
     * @return full path to consulo.exe or consulo64.exe
     */
    @Nonnull
    private static String getExecutableOnWindows() {
        return Platform.current().mapWindowsExecutable(ApplicationInfo.getInstance().getName().toLowerCase(Locale.ROOT), "exe");
    }

    private static Boolean isWindowsDefenderActive() {
        try {
            ProcessOutput output = CapturingProcessUtil.execAndGetOutput(new GeneralCommandLine("wmic",
                    "/Namespace:\\\\root\\SecurityCenter2",
                    "Path",
                    "AntivirusProduct",
                    "Get",
                    "displayName,productState"),
                WMIC_COMMAND_TIMEOUT_MS);
            if (output.getExitCode() == 0) {
                return parseWindowsDefenderProductState(output);
            }
            else {
                LOG.warn("wmic Windows Defender check exited with status " + output.getExitCode() + ": " + StringUtil.first(output.getStderr(),
                    MAX_POWERSHELL_STDERR_LENGTH,
                    false));
            }
        }
        catch (ExecutionException e) {
            LOG.warn("wmic Windows Defender check failed", e);
        }
        return null;
    }

    private static Boolean parseWindowsDefenderProductState(ProcessOutput output) {
        String[] lines = StringUtil.splitByLines(output.getStdout());
        for (String line : lines) {
            if (line.startsWith("Windows Defender")) {
                String productStateString = StringUtil.substringAfterLast(line, " ");
                int productState;
                try {
                    productState = Integer.parseInt(productStateString);
                    return (productState & 0x1000) != 0;
                }
                catch (NumberFormatException e) {
                    LOG.info("Unexpected wmic output format: " + line);
                    return null;
                }
            }
        }
        return false;
    }

    /**
     * Runs a powershell command to list the paths that are excluded from realtime scanning by Windows Defender. These
     * <p>
     * paths can contain environment variable references, as well as wildcards ('?', which matches a single character, and
     * '*', which matches any sequence of characters (but cannot match multiple nested directories; i.e., "foo\*\bar" would
     * match foo\baz\bar but not foo\baz\quux\bar)). The behavior of wildcards with respect to case-sensitivity is undocumented.
     * Returns a list of patterns, one for each exclusion path, that emulate how Windows Defender would interpret that path.
     */
    @Nullable
    private static List<Pattern> getExcludedPatterns() {
        Collection<String> paths = getWindowsDefenderProperty("ExclusionPath");
        if (paths == null) {
            return null;
        }
        return ContainerUtil.map(paths, path -> wildcardsToRegex(expandEnvVars(path)));
    }

    @Nullable
    private static Collection<String> getExcludedProcesses() {
        Collection<String> processes = getWindowsDefenderProperty("ExclusionProcess");
        if (processes == null) {
            return null;
        }
        return ContainerUtil.map(processes, process -> process.toLowerCase());
    }

    /**
     * Runs a powershell command to determine whether realtime scanning is enabled or not.
     */
    @Nonnull
    private static RealtimeScanningStatus getRealtimeScanningEnabled() {
        Collection<String> output = getWindowsDefenderProperty("DisableRealtimeMonitoring");
        if (output == null) {
            return RealtimeScanningStatus.ERROR;
        }
        if (output.size() > 0 && output.iterator().next().startsWith("False")) {
            return RealtimeScanningStatus.SCANNING_ENABLED;
        }
        return RealtimeScanningStatus.SCANNING_DISABLED;
    }

    @Nullable
    private static Collection<String> getWindowsDefenderProperty(String propertyName) {
        try {
            GeneralCommandLine cmd = new GeneralCommandLine();
            cmd.setExePath("powershell");
            cmd.addParameters("-inputformat", "none", "-outputformat", "text", "-NonInteractive", "-Command");
            cmd.addParameter("Get-MpPreference | select -ExpandProperty '" + propertyName + "'");

            ProcessOutput output = CapturingProcessUtil.execAndGetOutput(cmd, POWERSHELL_COMMAND_TIMEOUT_MS);
            if (output.getExitCode() == 0) {
                return output.getStdoutLines();
            }
            else {
                LOG.warn("Windows Defender " + propertyName + " check exited with status " + output.getExitCode() + ": " + StringUtil.first(output.getStderr(),
                    MAX_POWERSHELL_STDERR_LENGTH,
                    false));
            }
        }
        catch (ExecutionException e) {
            LOG.warn("Windows Defender " + propertyName + " check failed", e);
        }
        return null;
    }

    /**
     * Returns a list of paths that might impact build performance if Windows Defender were configured to scan them.
     */
    @Nonnull
    protected List<Path> getImportantPaths(@Nonnull Project project) {
        String homeDir = System.getProperty("user.home");
        String gradleUserHome = System.getenv("GRADLE_USER_HOME");
        String projectDir = project.getBasePath();

        List<Path> paths = new ArrayList<>();
        if (projectDir != null) {
            paths.add(Paths.get(projectDir));
        }
        paths.add(Paths.get(ContainerPathManager.get().getSystemPath()));
        if (gradleUserHome != null) {
            paths.add(Paths.get(gradleUserHome));
        }
        else {
            paths.add(Paths.get(homeDir, ".gradle"));
        }

        return paths;
    }


    /**
     * Expands references to environment variables (strings delimited by '%') in 'path'
     */
    @Nonnull
    private static String expandEnvVars(@Nonnull String path) {
        Matcher m = WINDOWS_ENV_VAR_PATTERN.matcher(path);
        StringBuffer result = new StringBuffer();
        while (m.find()) {
            String value = System.getenv(m.group(1));
            if (value != null) {
                m.appendReplacement(result, Matcher.quoteReplacement(value));
            }
        }
        m.appendTail(result);
        return result.toString();
    }

    /**
     * Produces a {@link Pattern} that approximates how Windows Defender interprets the exclusion path {@link path}.
     * The path is split around wildcards; the non-wildcard portions are quoted, and regex equivalents of
     * the wildcards are inserted between them. See
     * https://docs.microsoft.com/en-us/windows/security/threat-protection/windows-defender-antivirus/configure-extension-file-exclusions-windows-defender-antivirus
     * for more details.
     */
    @Nonnull
    private static Pattern wildcardsToRegex(@Nonnull String path) {
        Matcher m = WINDOWS_DEFENDER_WILDCARD_PATTERN.matcher(path);
        StringBuilder sb = new StringBuilder();
        int previousWildcardEnd = 0;
        while (m.find()) {
            sb.append(Pattern.quote(path.substring(previousWildcardEnd, m.start())));
            if (m.group().equals("?")) {
                sb.append("[^\\\\]");
            }
            else {
                sb.append("[^\\\\]*");
            }
            previousWildcardEnd = m.end();
        }
        sb.append(Pattern.quote(path.substring(previousWildcardEnd)));
        sb.append(".*"); // technically this should only be appended if the path refers to a directory, not a file. This is difficult to determine.
        return Pattern.compile(sb.toString(),
            Pattern.CASE_INSENSITIVE); // CASE_INSENSITIVE is overly permissive. Being precise with this is more work than it's worth.
    }

    /**
     * Checks whether each of the given paths in {@link paths} is matched by some pattern in {@link excludedPatterns},
     * returning a map of the results.
     */
    @Nonnull
    private static Map<Path, Boolean> checkPathsExcluded(@Nonnull List<Path> paths, @Nonnull List<Pattern> excludedPatterns) {
        Map<Path, Boolean> result = new HashMap<>();
        for (Path path : paths) {
            if (!path.toFile().exists()) {
                continue;
            }

            try {
                String canonical = path.toRealPath().toString();
                boolean found = false;
                for (Pattern pattern : excludedPatterns) {
                    if (pattern.matcher(canonical).matches()) {
                        found = true;
                        result.put(path, true);
                        break;
                    }
                }
                if (!found) {
                    result.put(path, false);
                }
            }
            catch (IOException e) {
                LOG.warn("Windows Defender exclusion check couldn't get real path for " + path, e);
            }
        }
        return result;
    }

    public void configureActions(Project project, WindowsDefenderNotification notification) {
        notification.addAction(new WindowsDefenderFixAction(myApplication, notification.getPaths()));

        notification.addAction(new NotificationAction(ExternalServiceLocalize.virusScanningDontShowAgain()) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e, @Nonnull Notification notification) {
                notification.expire();
                ApplicationPropertiesComponent.getInstance().setValue(IGNORE_VIRUS_CHECK, "true");
            }
        });
        notification.addAction(new NotificationAction(ExternalServiceLocalize.virusScanningDontShowAgainThisProject()) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e, @Nonnull Notification notification) {
                notification.expire();
                ProjectPropertiesComponent.getInstance(project).setValue(IGNORE_VIRUS_CHECK, "true");
            }
        });
    }

    public String getConfigurationInstructionsUrl() {
        return "https://intellij-support.jetbrains.com/hc/en-us/articles/360006298560";
    }

    public boolean runExcludePathsCommand(Project project, Collection<Path> paths) {
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
