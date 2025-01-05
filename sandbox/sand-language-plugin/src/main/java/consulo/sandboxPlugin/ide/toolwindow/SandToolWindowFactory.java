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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.ide.impl.idea.ide.actions.ToolWindowTabRenameActionBase;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 19.03.14
 */
@ExtensionImpl
public class SandToolWindowFactory implements ToolWindowFactory {
  @Nonnull
  @Override
  public String getId() {
    return "Sand";
  }

  @RequiredUIAccess
  @Override
  public void createToolWindowContent(@Nonnull Project project, @Nonnull ToolWindow toolWindow) {
    ContentFactory contentFactory = ContentFactory.getInstance();

    Content content = contentFactory.createUIContent(Label.create(LocalizeValue.localizeTODO("test")), "Test", false);
    toolWindow.getContentManager().addContent(content);

    toolWindow.setTitleActions(new AnAction("Expand All", null, AllIcons.Actions.Expandall) {
      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        Messages.showInfoMessage("Expand All", Application.get().getName().get());
      }
    });

    toolWindow.setTabDoubleClickActions(new ToolWindowTabRenameActionBase("Sand", "Enter new session name"));

    toolWindow.setTabActions(new AnAction("Add Tab", null, AllIcons.General.Add) {
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

  @Override
  public boolean canCloseContents() {
    return true;
  }

  @Nonnull
  @Override
  public ToolWindowAnchor getAnchor() {
    return ToolWindowAnchor.RIGHT;
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return PlatformIconGroup.toolwindowsToolwindowpalette();
  }

  @Nonnull
  @Override
  public LocalizeValue getDisplayName() {
    return LocalizeValue.localizeTODO("Sand");
  }
}
