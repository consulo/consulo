package com.intellij.remoteServer.impl.runtime.ui;

import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

/**
 * Created by IntelliJ IDEA.
 * User: michael.golubev
 */
public class ServersToolWindowOpener extends AbstractProjectComponent {

  public ServersToolWindowOpener(final Project project) {
    super(project);
  }

  @Override
  public void projectOpened() {
    StartupManager.getInstance(myProject).registerPostStartupActivity(new Runnable() {
      @Override
      public void run() {
        ToolWindowManager.getInstance(myProject).invokeLater(new Runnable() {
          @Override
          public void run() {
            new ServersToolWindow(myProject);
          }
        });
      }
    });
  }

  @Override
  @NotNull
  public String getComponentName() {
    return "ServersToolWindowOpener";
  }
}
