/*
 * Copyright 2013-2020 consulo.io
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
package consulo.desktop.awt.wm.impl;

import consulo.application.Application;
import consulo.application.impl.internal.start.ApplicationStarter;
import consulo.application.impl.internal.start.CommandLineArgs;
import consulo.dataContext.DataManager;
import consulo.ide.impl.actions.AboutManager;
import consulo.ide.impl.idea.ide.CommandLineProcessor;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.Window;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * @author VISTALL
 * @since 2020-10-19
 * <p>
 * Just register Desktop actions if can. At current moment - only macOS impl this methods
 */
public class TopMenuInitializer {
  public static void register(Application application) {
    if (!Desktop.isDesktopSupported()) {
      return;
    }

    Desktop desktop = Desktop.getDesktop();

    if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
      desktop.setAboutHandler(e -> {
        DataManager dataManager = DataManager.getInstance();

        Window window = WindowManager.getInstance().suggestParentWindow(dataManager.getDataContext().getData(Project.KEY));

        AboutManager aboutManager = application.getComponent(AboutManager.class);

        aboutManager.showAsync(window);
      });
    }

    if (desktop.isSupported(Desktop.Action.APP_PRINT_FILE)) {
      desktop.setPreferencesHandler(e -> {
        final Project project = getNotNullProject();
        final ShowSettingsUtil showSettingsUtil = ShowSettingsUtil.getInstance();
        if (!showSettingsUtil.isAlreadyShown(project)) {
          showSettingsUtil.showSettingsDialog(project);
        }
      });
    }

    if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
      desktop.setQuitHandler((e, response) -> {
        application.exit();
      });
    }

    if (desktop.isSupported(Desktop.Action.APP_OPEN_FILE)) {
      desktop.setOpenFileHandler(e -> {
        List<File> files = e.getFiles();

        if (files != null) {
          File file = files.get(0);

          CommandLineArgs args = new CommandLineArgs();
          args.setFile(file.getPath());

          CommandLineProcessor.processExternalCommandLine(args, null).doWhenDone(project1 -> {
            ApplicationStarter.getInstance().setPerformProjectLoad(false);
          });
        }
      });
    }
  }

  @Nonnull
  private static Project getNotNullProject() {
    Project project = getProject();
    return project == null ? ProjectManager.getInstance().getDefaultProject() : project;
  }

  @SuppressWarnings("deprecation")
  private static Project getProject() {
    return DataManager.getInstance().getDataContext().getData(Project.KEY);
  }
}
