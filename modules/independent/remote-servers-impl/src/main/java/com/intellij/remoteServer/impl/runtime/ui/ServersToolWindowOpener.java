package com.intellij.remoteServer.impl.runtime.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.wm.ToolWindowManager;
import consulo.annotation.inject.NotLazy;
import consulo.platform.Platform;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by IntelliJ IDEA.
 * User: michael.golubev
 */
@Singleton
@NotLazy
public class ServersToolWindowOpener {
  private Project myProject;

  @Inject
  public ServersToolWindowOpener(final Project project) {
    myProject = project;

    if (myProject.isDefault()) {
      return;
    }

    Platform.onlyAtDesktop(() -> StartupManager.getInstance(myProject).registerPostStartupActivity(() -> ToolWindowManager.getInstance(myProject).invokeLater(() -> new ServersToolWindow(myProject))));
  }
}
