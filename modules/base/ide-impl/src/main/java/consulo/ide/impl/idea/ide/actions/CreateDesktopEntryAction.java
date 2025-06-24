/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.actions;

import consulo.application.Application;
import consulo.application.impl.internal.start.ApplicationStarter;
import consulo.application.localize.ApplicationLocalize;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.util.SystemInfo;
import consulo.container.boot.ContainerPathManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.local.ExecUtil;
import consulo.process.util.CapturingProcessUtil;
import consulo.project.Project;
import consulo.project.ui.notification.Notifications;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public class CreateDesktopEntryAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(CreateDesktopEntryAction.class);

    public static boolean isAvailable() {
        return Platform.current().os().isUnix() && SystemInfo.hasXdgOpen();
    }

    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent event) {
        boolean enabled = isAvailable();
        Presentation presentation = event.getPresentation();
        presentation.setEnabled(enabled);
        presentation.setVisible(enabled);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent event) {
        if (!isAvailable()) {
            return;
        }

        Project project = event.getData(Project.KEY);
        CreateDesktopEntryDialog dialog = new CreateDesktopEntryDialog(project);
        if (!dialog.showAndGet()) {
            return;
        }

        boolean globalEntry = dialog.myGlobalEntryCheckBox.isSelected();
        ProgressManager.getInstance().run(new Task.Backgroundable(project, event.getPresentation().getText()) {
            @Override
            public void run(@Nonnull ProgressIndicator indicator) {
                createDesktopEntry((Project)getProject(), indicator, globalEntry);
            }
        });
    }

    public static void createDesktopEntry(@Nullable Project project, @Nonnull ProgressIndicator indicator, boolean globalEntry) {
        if (!isAvailable()) {
            return;
        }
        double step = (1.0 - indicator.getFraction()) / 3.0;

        try {
            indicator.setTextValue(ApplicationLocalize.desktopEntryChecking());
            check();
            indicator.setFraction(indicator.getFraction() + step);

            indicator.setTextValue(ApplicationLocalize.desktopEntryPreparing());
            File entry = prepare();
            indicator.setFraction(indicator.getFraction() + step);

            indicator.setTextValue(ApplicationLocalize.desktopEntryInstalling());
            install(entry, globalEntry);
            indicator.setFraction(indicator.getFraction() + step);

            Notifications.SYSTEM_MESSAGES_GROUP.newInfo()
                .title(LocalizeValue.localizeTODO("Desktop entry created"))
                .content(ApplicationLocalize.desktopEntrySuccess(Application.get().getName()))
                .notify(null);
        }
        catch (Exception e) {
            String message = e.getMessage();
            if (!StringUtil.isEmptyOrSpaces(message)) {
                LOG.warn(e);
                Notifications.SYSTEM_MESSAGES_GROUP.newError()
                    .title(LocalizeValue.localizeTODO("Failed to create desktop entry"))
                    .content(LocalizeValue.localizeTODO(message))
                    .notify(project);
            }
            else {
                LOG.error(e);
            }
        }
    }

    private static void check() throws ExecutionException, InterruptedException {
        int result = new GeneralCommandLine("which", "xdg-desktop-menu").createProcess().waitFor();
        if (result != 0) {
            throw new RuntimeException(ApplicationLocalize.desktopEntryXdgMissing().get());
        }
    }

    private static File prepare() throws IOException {
        File distributionDirectory = ContainerPathManager.get().getAppHomeDirectory();
        String name = Application.get().getName().toString();

        Path iconPath = ContainerPathManager.get().findIconInAppHomeDirectory();
        if (iconPath == null) {
            throw new RuntimeException(ApplicationLocalize.desktopEntryIconMissing(distributionDirectory.getPath()).get());
        }

        File execPath = new File(distributionDirectory, "consulo.sh");
        if (!execPath.exists()) {
            throw new RuntimeException(ApplicationLocalize.desktopEntryScriptMissing(distributionDirectory.getPath()).get());
        }

        String wmClass = ApplicationStarter.getFrameClass();

        String content = ExecUtil.loadTemplate(
            CreateDesktopEntryAction.class.getClassLoader(),
            "entry.desktop",
            consulo.ide.impl.idea.util.containers.ContainerUtil.newHashMap(
                Arrays.asList("$NAME$", "$SCRIPT$", "$ICON$", "$WM_CLASS$"),
                Arrays.asList(name, execPath.getPath(), iconPath.toAbsolutePath().toString(), wmClass)
            )
        );

        String entryName = wmClass + ".desktop";
        File entryFile = new File(FileUtil.getTempDirectory(), entryName);
        FileUtil.writeToFile(entryFile, content);
        entryFile.deleteOnExit();
        return entryFile;
    }

    private static void install(File entryFile, boolean globalEntry)
        throws IOException, ExecutionException, InterruptedException {
        if (globalEntry) {
            File script = ExecUtil.createTempExecutableScript(
                "sudo",
                ".sh",
                "#!/bin/sh\n" +
                    "xdg-desktop-menu install --mode system --novendor \"" +
                    entryFile.getAbsolutePath() +
                    "\"\n" +
                    "RV=$?\n" +
                    "xdg-desktop-menu forceupdate --mode system\n" +
                    "exit $RV\n"
            );
            script.deleteOnExit();
            LocalizeValue prompt = ApplicationLocalize.desktopEntrySudoPrompt();
            int result =
                CapturingProcessUtil.execAndGetOutput(new GeneralCommandLine(script.getPath()).withSudo(prompt.get())).getExitCode();
            if (result != 0) {
                throw new RuntimeException("'" + script.getAbsolutePath() + "' : " + result);
            }
        }
        else {
            int result =
                new GeneralCommandLine(
                    "xdg-desktop-menu",
                    "install",
                    "--mode",
                    "user",
                    "--novendor",
                    entryFile.getAbsolutePath()
                ).createProcess().waitFor();
            if (result != 0) {
                throw new RuntimeException("'" + entryFile.getAbsolutePath() + "' : " + result);
            }
            new GeneralCommandLine("xdg-desktop-menu", "forceupdate", "--mode", "user").createProcess().waitFor();
        }
    }

    public static class CreateDesktopEntryDialog extends DialogWrapper {
        private JPanel myContentPane;
        private JLabel myLabel;
        private JCheckBox myGlobalEntryCheckBox;

        public CreateDesktopEntryDialog(Project project) {
            super(project);
            init();
            setTitle("Create Desktop Entry");
            myLabel.setText(myLabel.getText().replace("$APP_NAME$", Application.get().getName().toString()));
        }

        @Override
        protected JComponent createCenterPanel() {
            return myContentPane;
        }
    }
}
