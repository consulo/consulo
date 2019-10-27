package com.intellij.remoteServer.impl.runtime.ui;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.wm.ToolWindowManager;
import consulo.platform.Platform;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by IntelliJ IDEA.
 * User: michael.golubev
 */
@Singleton
public class ServersToolWindowOpener implements ProjectComponent {

  private Project myProject;

  @Inject
  public ServersToolWindowOpener(final Project project) {
    myProject = project;
  }

  @Override
  public void projectOpened() {
    Platform.runIfDesktopPlatform(() -> StartupManager.getInstance(myProject).registerPostStartupActivity(() -> ToolWindowManager.getInstance(myProject).invokeLater(() -> new ServersToolWindow(myProject))));
  }
}
