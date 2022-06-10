/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.impl.idea.openapi.fileEditor.impl.tabActions;

import consulo.application.AllIcons;
import consulo.ide.IdeBundle;
import consulo.application.ui.UISettings;
import consulo.ide.impl.idea.openapi.fileEditor.ex.FileEditorManagerEx;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.ui.ShadowAction;
import consulo.ui.ex.action.*;
import consulo.util.lang.BitUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.fileEditor.FileEditorWindow;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.InputEvent;

public class CloseTab extends AnAction implements DumbAware {
  private final Project myProject;
  private final VirtualFile myFile;
  private final FileEditorWindow myEditorWindow;

  public CloseTab(JComponent c, Project project, VirtualFile file, FileEditorWindow editorWindow) {
    myProject = project;
    myFile = file;
    myEditorWindow = editorWindow;
    new ShadowAction(this, ActionManager.getInstance().getAction(IdeActions.ACTION_CLOSE), c);
  }

  public CloseTab(Component c, Project project, VirtualFile file, FileEditorWindow editorWindow) {
    myProject = project;
    myFile = file;
    myEditorWindow = editorWindow;
    // TODO [VISTALL] unsupported new ShadowAction(this, ActionManager.getInstance().getAction(IdeActions.ACTION_CLOSE), c);
  }

  @RequiredUIAccess
  @Override
  public void update(final AnActionEvent e) {
    boolean pinned = isPinned();

    e.getPresentation().setIcon(pinned ? PlatformIconGroup.actionsPintab() : AllIcons.Actions.Close);
    e.getPresentation().setHoveredIcon(pinned ? PlatformIconGroup.actionsPintab() : AllIcons.Actions.CloseHovered);
    e.getPresentation().setVisible(UISettings.getInstance().getShowCloseButton() || pinned);
    if (pinned) {
      e.getPresentation().setText(IdeBundle.message("action.unpin.tab"));
    }
    else {
      e.getPresentation().setText("Close. Alt-click to close others.");
    }
  }

  private boolean isPinned() {
    return myEditorWindow.isFilePinned(myFile);
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull final AnActionEvent e) {
    if (isPinned() && ActionPlaces.EDITOR_TAB.equals(e.getPlace())) {
      myEditorWindow.setFilePinned(myFile, false);
      return;
    }

    final FileEditorManagerEx mgr = FileEditorManagerEx.getInstanceEx(myProject);
    FileEditorWindow window;
    final VirtualFile file = myFile;
    if (ActionPlaces.EDITOR_TAB.equals(e.getPlace())) {
      window = myEditorWindow;
    }
    else {
      window = mgr.getCurrentWindow();
    }

    if (window != null) {
      if (BitUtil.isSet(e.getModifiers(), InputEvent.ALT_DOWN_MASK)) {
        window.closeAllExcept(file);
      }
      else {
        if (window.findFileComposite(file) != null) {
          mgr.closeFile(file, window);
        }
      }
    }
  }
}
