/*
 * Copyright 2013 Consulo.org
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
package com.intellij.remoteServer.impl.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.configuration.ServerConfiguration;
import com.intellij.remoteServer.impl.runtime.log.LoggingHandlerImpl;
import com.intellij.remoteServer.runtime.ServerConnection;
import com.intellij.remoteServer.runtime.deployment.DeploymentLogManager;
import com.intellij.remoteServer.runtime.ui.RemoteServersView;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.impl.ContentImpl;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 16:08/28.09.13
 */
public class RemoteServersViewImpl extends RemoteServersView {
  private Project myProject;

  public RemoteServersViewImpl(Project project) {
    myProject = project;
  }

  @Override
  public void showServerConnection(@NotNull ServerConnection<?> connection) {
    ServerType<? extends ServerConfiguration> type = connection.getServer().getType();

    createOrGetToolWindow(type);
  }

  @Override
  public void showDeployment(@NotNull final ServerConnection<?> connection, @NotNull String deploymentName) {
    DeploymentLogManager logManager = connection.getLogManager(deploymentName);
    if(logManager == null) {
      return;
    }

    final ToolWindow toolWindow = createOrGetToolWindow(connection.getServer().getType());

    final LoggingHandlerImpl mainLoggingHandler = (LoggingHandlerImpl)logManager.getMainLoggingHandler();


    ToolbarDecorator t = new ToolbarDecorator() {
      @Override
      protected JComponent getComponent() {
        return mainLoggingHandler.getConsole().getComponent();
      }

      @Override
      protected void updateButtons() {
      }

      @Override
      protected void installDnDSupport() {
      }

      @Override
      protected boolean isModelEditable() {
        return false;
      }
    };

    t.disableUpDownActions();
    t.disableAddAction();
    t.disableRemoveAction();
    t.addExtraAction(new AnActionButton("Close", AllIcons.Actions.Cancel) {
      @Override
      public void actionPerformed(AnActionEvent e) {
        int contentCount = toolWindow.getContentManager().getContentCount();
        if(contentCount == 1) {
          ToolWindowManager.getInstance(myProject).unregisterToolWindow(connection.getServer().getType().getPresentableName());
        }
        else {
          Content selectedContent = toolWindow.getContentManager().getSelectedContent();
          if(selectedContent == null) {
            return;
          }

          toolWindow.getContentManager().removeContent(selectedContent, true);
        }
      }
    });
    t.setToolbarPosition(ActionToolbarPosition.LEFT);

    toolWindow.getContentManager().addContent(new ContentImpl(t.createPanel(), deploymentName, false));
  }

  private ToolWindow createOrGetToolWindow(ServerType<? extends ServerConfiguration> type) {
    ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(type.getPresentableName());
    if(toolWindow == null) {
      toolWindow = ToolWindowManager.getInstance(myProject)
        .registerToolWindow(type.getPresentableName(), true, ToolWindowAnchor.BOTTOM);
      toolWindow.setIcon(type.getIcon());
    }
    return toolWindow;
  }
}
