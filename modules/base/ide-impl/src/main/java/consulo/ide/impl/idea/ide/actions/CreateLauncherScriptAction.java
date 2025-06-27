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
package consulo.ide.impl.idea.ide.actions;

import consulo.application.Application;
import consulo.application.localize.ApplicationLocalize;
import consulo.container.boot.ContainerPathManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.local.ExecUtil;
import consulo.process.util.CapturingProcessUtil;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.Notifications;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import static consulo.ide.impl.idea.util.containers.ContainerUtil.newHashMap;
import static consulo.util.lang.Pair.pair;

/**
 * @author yole
 */
public class CreateLauncherScriptAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(CreateLauncherScriptAction.class);
    private static final String CONTENTS = "/Contents";

    public static boolean isAvailable() {
        return Platform.current().os().isUnix();
    }

    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent e) {
        boolean canCreateScript = isAvailable();
        Presentation presentation = e.getPresentation();
        presentation.setVisible(canCreateScript);
        presentation.setEnabled(canCreateScript);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        if (!isAvailable()) {
            return;
        }

        Project project = e.getData(Project.KEY);
        CreateLauncherScriptDialog dialog = new CreateLauncherScriptDialog(project);
        if (!dialog.showAndGet()) {
            return;
        }

        String path = dialog.myPathField.getText();
        assert path != null;
        if (!path.startsWith("/")) {
            String home = System.getenv("HOME");
            if (home != null && new File(home).isDirectory()) {
                if (path.startsWith("~")) {
                    path = home + path.substring(1);
                }
                else {
                    path = home + "/" + path;
                }
            }
        }

        String name = dialog.myNameField.getText();
        assert name != null;
        File target = new File(path, name);
        if (target.exists()) {
            LocalizeValue message = ApplicationLocalize.launcherScriptOverwrite(target);
            LocalizeValue title = ApplicationLocalize.launcherScriptTitle();
            if (Messages.showOkCancelDialog(project, message.get(), title.get(), UIUtil.getQuestionIcon()) != Messages.OK) {
                return;
            }
        }

        createLauncherScript(project, target.getAbsolutePath());
    }

    public static void createLauncherScript(Project project, String pathName) {
        if (!isAvailable()) {
            return;
        }

        try {
            File scriptFile = createLauncherScriptFile();
            File scriptTarget = new File(pathName);

            File scriptTargetDir = scriptTarget.getParentFile();
            assert scriptTargetDir != null;
            if (!(scriptTargetDir.exists() || scriptTargetDir.mkdirs()) || !scriptFile.renameTo(scriptTarget)) {
                String scriptTargetDirPath = scriptTargetDir.getCanonicalPath();
                // copy file and change ownership to root (UID 0 = root, GID 0 = root (wheel on Macs))
                String installationScriptSrc =
                    "#!/bin/sh\n" +
                        "mkdir -p \"" + scriptTargetDirPath + "\"\n" +
                        "install -g 0 -o 0 \"" + scriptFile.getCanonicalPath() + "\" \"" + pathName + "\"";
                File installationScript = ExecUtil.createTempExecutableScript("launcher_installer", ".sh", installationScriptSrc);
                LocalizeValue prompt = ApplicationLocalize.launcherScriptSudoPrompt(scriptTargetDirPath);
                CapturingProcessUtil.execAndGetOutput(new GeneralCommandLine(installationScript.getPath()).withSudo(prompt.get()));
            }
        }
        catch (Exception e) {
            String message = e.getMessage();
            if (!StringUtil.isEmptyOrSpaces(message)) {
                LOG.warn(e);
                NotificationService.getInstance()
                    .newError(Notifications.SYSTEM_MESSAGES_GROUP)
                    .title(LocalizeValue.localizeTODO("Failed to create launcher script"))
                    .content(LocalizeValue.localizeTODO(message))
                    .notify(project);
            }
            else {
                LOG.error(e);
            }
        }
    }

    private static File createLauncherScriptFile() throws IOException, ExecutionException {
        String runPath = ContainerPathManager.get().getHomePath();
        String productName = Application.get().getName().get().toLowerCase(Locale.US);
        if (!Platform.current().os().isMac()) {
            runPath += "/bin/" + productName + ".sh";
        }
        else if (runPath.endsWith(CONTENTS)) {
            runPath = runPath.substring(0, runPath.length() - CONTENTS.length());
        }

        ClassLoader loader = CreateLauncherScriptAction.class.getClassLoader();
        assert loader != null;
        Map<String, String> variables =
            newHashMap(pair("$CONFIG_PATH$", ContainerPathManager.get().getConfigPath()), pair("$RUN_PATH$", runPath));
        String launcherContents = StringUtil.convertLineSeparators(ExecUtil.loadTemplate(loader, "launcher.py", variables));

        return ExecUtil.createTempExecutableScript("launcher", "", launcherContents);
    }

    public static String defaultScriptName() {
        return "consulo";
    }

    public static class CreateLauncherScriptDialog extends DialogWrapper {
        private JPanel myMainPanel;
        private JTextField myNameField;
        private JTextField myPathField;
        private JLabel myTitle;

        protected CreateLauncherScriptDialog(Project project) {
            super(project);
            init();
            setTitle(ApplicationLocalize.launcherScriptTitle());
            LocalizeValue productName = Application.get().getName();
            myTitle.setText(myTitle.getText().replace("$APP_NAME$", productName.get()));
            myNameField.setText(defaultScriptName());
        }

        @Override
        protected JComponent createCenterPanel() {
            return myMainPanel;
        }
    }
}
