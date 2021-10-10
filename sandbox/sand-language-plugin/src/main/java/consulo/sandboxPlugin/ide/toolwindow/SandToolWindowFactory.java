/*
 * Copyright 2013-2016 consulo.io
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
package consulo.sandboxPlugin.ide.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.ToolWindowTabRenameActionBase;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import consulo.localize.LocalizeValue;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 19.03.14
 */
public class SandToolWindowFactory implements ToolWindowFactory {
  @RequiredUIAccess
  @Override
  public void createToolWindowContent(@Nonnull Project project, @Nonnull ToolWindow toolWindow) {
    ContentFactory contentFactory = ContentFactory.getInstance();

    Content content = contentFactory.createUIContent(Label.create(LocalizeValue.localizeTODO("test")), "Test", false);
    toolWindow.getContentManager().addContent(content);

    ((ToolWindowEx)toolWindow).setTitleActions(new AnAction("Expand All", null, AllIcons.Actions.Expandall) {
      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        Messages.showInfoMessage("Expand All", "Consulo");
      }
    });

    ((ToolWindowEx)toolWindow).setTabDoubleClickActions(new ToolWindowTabRenameActionBase("Sand", "Enter new session name"));

    ((ToolWindowEx)toolWindow).setTabActions(new AnAction("Add Tab", null, AllIcons.General.Add) {
      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        Content content = contentFactory.createUIContent(Label.create("test"), "Test", false);
        content.setCloseable(true);

        toolWindow.getContentManager().addContent(content);
      }
    });
  }

  @Override
  public boolean isUnified() {
    return true;
  }
}
