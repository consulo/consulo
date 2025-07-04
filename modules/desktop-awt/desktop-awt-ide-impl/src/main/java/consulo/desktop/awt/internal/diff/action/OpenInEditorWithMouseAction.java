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
package consulo.desktop.awt.internal.diff.action;

import consulo.codeEditor.Editor;
import consulo.codeEditor.event.EditorMouseEventArea;
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.navigation.Navigatable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

public abstract class OpenInEditorWithMouseAction extends AnAction implements DumbAware {
  @Nonnull
  private List<? extends Editor> myEditors = Collections.emptyList();

  public OpenInEditorWithMouseAction() {
    AnAction navigateAction = ActionManager.getInstance().getAction(IdeActions.ACTION_GOTO_DECLARATION); // null in MPS
    setShortcutSet(navigateAction != null ?
                   navigateAction.getShortcutSet() :
                   new CustomShortcutSet(new MouseShortcut(MouseEvent.BUTTON1, InputEvent.CTRL_DOWN_MASK, 1)));
  }

  public void install(@Nonnull List<? extends Editor> editors) {
    myEditors = editors;
    for (Editor editor : editors) {
      registerCustomShortcutSet(getShortcutSet(), ((EditorGutterComponentEx)editor.getGutter()).getComponent());
    }
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    InputEvent inputEvent = e.getInputEvent();
    if (!(inputEvent instanceof MouseEvent)) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    if (e.getData(Project.KEY) == null) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    if (e.getData(OpenInEditorAction.KEY) == null) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    Component component = inputEvent.getComponent();
    if (component == null) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    Point point = ((MouseEvent)inputEvent).getPoint();
    Component componentAt = SwingUtilities.getDeepestComponentAt(component, point.x, point.y);
    if (!(componentAt instanceof EditorGutterComponentEx)) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    Editor editor = getEditor(componentAt);
    if (editor == null) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    MouseEvent convertedEvent = SwingUtilities.convertMouseEvent(inputEvent.getComponent(), (MouseEvent)inputEvent, componentAt);
    EditorMouseEventArea area = editor.getMouseEventArea(convertedEvent);
    if (area != EditorMouseEventArea.LINE_NUMBERS_AREA) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    e.getPresentation().setEnabledAndVisible(true);
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    MouseEvent inputEvent = (MouseEvent)e.getInputEvent();
    OpenInEditorAction openInEditorAction = e.getRequiredData(OpenInEditorAction.KEY);
    Project project = e.getRequiredData(Project.KEY);

    Component component = inputEvent.getComponent();
    Point point = inputEvent.getPoint();
    Component componentAt = SwingUtilities.getDeepestComponentAt(component, point.x, point.y);
    MouseEvent convertedEvent = SwingUtilities.convertMouseEvent(inputEvent.getComponent(), inputEvent, componentAt);

    Editor editor = getEditor(componentAt);
    assert editor != null;

    int line = editor.xyToLogicalPosition(convertedEvent.getPoint()).line;

    Navigatable navigatable = getNavigatable(editor, line);
    if (navigatable == null) return;

    openInEditorAction.openEditor(project, navigatable);
  }

  @Nullable
  private Editor getEditor(@Nonnull Component component) {
    for (Editor editor : myEditors) {
      if (editor != null && editor.getGutter() == component) {
        return editor;
      }
    }
    return null;
  }

  @Nullable
  protected abstract Navigatable getNavigatable(@Nonnull Editor editor, int line);
}
