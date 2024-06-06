package consulo.remoteServer.impl.internal.ui;

import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.remoteServer.runtime.ServerConnection;
import consulo.remoteServer.runtime.ui.RemoteServersView;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.toolWindow.ToolWindow;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author nik
 */
@Singleton
@ServiceImpl
public class RemoteServersViewImpl extends RemoteServersView {
  @Nonnull
  private final Project myProject;

  @Inject
  public RemoteServersViewImpl(@Nonnull Project project) {
    myProject = project;
  }

  @Override
  public void showServerConnection(@Nonnull final ServerConnection<?> connection) {
    final ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(ServersToolWindowManager.ID);
    if (toolWindow != null) {
      toolWindow.activate(new Runnable() {
        @Override
        public void run() {
          ServersToolWindowContent content = getServersViewComponent(toolWindow);
          if (content != null) {
            content.select(connection);
          }
        }
      });
    }
  }

  private static ServersToolWindowContent getServersViewComponent(ToolWindow toolWindow) {
    //todo[nik] register ServersToolWindowContent as project service?
    return UIUtil.findComponentOfType(toolWindow.getComponent(), ServersToolWindowContent.class);
  }

  @Override
  public void showDeployment(@Nonnull final ServerConnection<?> connection, @Nonnull final String deploymentName) {
    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
    final ToolWindow toolWindow = toolWindowManager.getToolWindow(ServersToolWindowManager.ID);
    if (toolWindow != null) {
      toolWindowManager.invokeLater(new Runnable() {
        @Override
        public void run() {
          ServersToolWindowContent component = getServersViewComponent(toolWindow);
          if (component != null) {
            component.select(connection, deploymentName);
          }
        }
      });
    }
  }
}
