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

import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.ide.impl.idea.execution.util.ExecUtil;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.Notifications;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.ui.ex.action.Presentation;
import consulo.application.ApplicationBundle;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.ApplicationNamesInfo;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.application.util.SystemInfo;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.ui.AppUIUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.container.boot.ContainerPathManager;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.internal.AppIconUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class CreateDesktopEntryAction extends DumbAwareAction {
  private static final Logger LOG = Logger.getInstance(CreateDesktopEntryAction.class);

  public static boolean isAvailable() {
    return SystemInfo.isUnix && SystemInfo.hasXdgOpen();
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull final AnActionEvent event) {
    final boolean enabled = isAvailable();
    final Presentation presentation = event.getPresentation();
    presentation.setEnabled(enabled);
    presentation.setVisible(enabled);
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull final AnActionEvent event) {
    if (!isAvailable()) return;

    final Project project = event.getData(CommonDataKeys.PROJECT);
    final CreateDesktopEntryDialog dialog = new CreateDesktopEntryDialog(project);
    if (!dialog.showAndGet()) {
      return;
    }

    final boolean globalEntry = dialog.myGlobalEntryCheckBox.isSelected();
    ProgressManager.getInstance().run(new Task.Backgroundable(project, event.getPresentation().getText()) {
      @Override
      public void run(@Nonnull final ProgressIndicator indicator) {
        createDesktopEntry((Project)getProject(), indicator, globalEntry);
      }
    });
  }

  public static void createDesktopEntry(@Nullable final Project project, @Nonnull final ProgressIndicator indicator, final boolean globalEntry) {
    if (!isAvailable()) return;
    final double step = (1.0 - indicator.getFraction()) / 3.0;

    try {
      indicator.setText(ApplicationBundle.message("desktop.entry.checking"));
      check();
      indicator.setFraction(indicator.getFraction() + step);

      indicator.setText(ApplicationBundle.message("desktop.entry.preparing"));
      final File entry = prepare();
      indicator.setFraction(indicator.getFraction() + step);

      indicator.setText(ApplicationBundle.message("desktop.entry.installing"));
      install(entry, globalEntry);
      indicator.setFraction(indicator.getFraction() + step);

      final String message = ApplicationBundle.message("desktop.entry.success", ApplicationNamesInfo.getInstance().getProductName());
      if (ApplicationManager.getApplication() != null) {
        Notifications.Bus.notify(new Notification(Notifications.SYSTEM_MESSAGES_GROUP_ID, "Desktop entry created", message, NotificationType.INFORMATION));
      }
    }
    catch (Exception e) {
      if (ApplicationManager.getApplication() == null) {
        throw new RuntimeException(e);
      }
      final String message = e.getMessage();
      if (!StringUtil.isEmptyOrSpaces(message)) {
        LOG.warn(e);
        Notifications.Bus
                .notify(new Notification(Notifications.SYSTEM_MESSAGES_GROUP_ID, "Failed to create desktop entry", message, NotificationType.ERROR), project);
      }
      else {
        LOG.error(e);
      }
    }
  }

  private static void check() throws ExecutionException, InterruptedException {
    int result = new GeneralCommandLine("which", "xdg-desktop-menu").createProcess().waitFor();
    if (result != 0) throw new RuntimeException(ApplicationBundle.message("desktop.entry.xdg.missing"));
  }

  private static File prepare() throws IOException {
    File distributionDirectory = ContainerPathManager.get().getAppHomeDirectory();
    String name = ApplicationNamesInfo.getInstance().getFullProductName();

    final String iconPath = AppIconUtil.findIcon(distributionDirectory.getPath());
    if (iconPath == null) {
      throw new RuntimeException(ApplicationBundle.message("desktop.entry.icon.missing", distributionDirectory.getPath()));
    }

    final File execPath = new File(distributionDirectory, "consulo.sh");
    if (!execPath.exists()) {
      throw new RuntimeException(ApplicationBundle.message("desktop.entry.script.missing", distributionDirectory.getPath()));
    }

    final String wmClass = AppUIUtil.getFrameClass();

    final String content = ExecUtil.loadTemplate(CreateDesktopEntryAction.class.getClassLoader(), "entry.desktop", ContainerUtil
            .newHashMap(Arrays.asList("$NAME$", "$SCRIPT$", "$ICON$", "$WM_CLASS$"), Arrays.asList(name, execPath.getPath(), iconPath, wmClass)));

    final String entryName = wmClass + ".desktop";
    final File entryFile = new File(FileUtil.getTempDirectory(), entryName);
    FileUtil.writeToFile(entryFile, content);
    entryFile.deleteOnExit();
    return entryFile;
  }

  private static void install(File entryFile, boolean globalEntry) throws IOException, ExecutionException, InterruptedException {
    if (globalEntry) {
      File script = ExecUtil.createTempExecutableScript("sudo", ".sh", "#!/bin/sh\n" +
                                                                       "xdg-desktop-menu install --mode system --novendor \"" +
                                                                       entryFile.getAbsolutePath() +
                                                                       "\"\n" +
                                                                       "RV=$?\n" +
                                                                       "xdg-desktop-menu forceupdate --mode system\n" +
                                                                       "exit $RV\n");
      script.deleteOnExit();
      String prompt = ApplicationBundle.message("desktop.entry.sudo.prompt");
      int result = ExecUtil.sudoAndGetOutput(new GeneralCommandLine(script.getPath()), prompt).getExitCode();
      if (result != 0) throw new RuntimeException("'" + script.getAbsolutePath() + "' : " + result);
    }
    else {
      int result = new GeneralCommandLine("xdg-desktop-menu", "install", "--mode", "user", "--novendor", entryFile.getAbsolutePath()).createProcess().waitFor();
      if (result != 0) throw new RuntimeException("'" + entryFile.getAbsolutePath() + "' : " + result);
      new GeneralCommandLine("xdg-desktop-menu", "forceupdate", "--mode", "user").createProcess().waitFor();
    }
  }

  public static class CreateDesktopEntryDialog extends DialogWrapper {
    private JPanel myContentPane;
    private JLabel myLabel;
    private JCheckBox myGlobalEntryCheckBox;

    public CreateDesktopEntryDialog(final Project project) {
      super(project);
      init();
      setTitle("Create Desktop Entry");
      myLabel.setText(myLabel.getText().replace("$APP_NAME$", ApplicationNamesInfo.getInstance().getProductName()));
    }

    @Override
    protected JComponent createCenterPanel() {
      return myContentPane;
    }
  }
}
