// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.event.EditorEventMulticaster;
import consulo.ide.impl.idea.openapi.editor.ex.EditorEventMulticasterEx;
import consulo.codeEditor.EditorEx;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.project.ui.wm.CustomStatusBarWidget;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.ui.ex.UIBundle;
import consulo.disposer.Disposer;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

/**
 * @author cdr
 */
public class ColumnSelectionModePanel extends EditorBasedWidget implements StatusBarWidget.Multiframe, CustomStatusBarWidget, PropertyChangeListener {
  private final TextPanel myTextPanel = new TextPanel();

  public ColumnSelectionModePanel(@Nonnull Project project) {
    super(project);
    myTextPanel.setVisible(false);
  }

  @Override
  @Nonnull
  public String ID() {
    return StatusBar.StandardWidgets.COLUMN_SELECTION_MODE_PANEL;
  }

  @Override
  public WidgetPresentation getPresentation() {
    return null;
  }

  @Override
  public StatusBarWidget copy() {
    return new ColumnSelectionModePanel(getProject());
  }

  @Nonnull
  @Override
  public JComponent getComponent() {
    return myTextPanel;
  }

  @Override
  public void install(@Nonnull StatusBar statusBar) {
    super.install(statusBar);
    KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addPropertyChangeListener(SWING_FOCUS_OWNER_PROPERTY, evt -> propertyChange(new PropertyChangeEvent(evt.getSource(), evt.getPropertyName(), evt.getOldValue(), evt.getNewValue())));
    Disposer.register(this, () -> KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .removePropertyChangeListener(SWING_FOCUS_OWNER_PROPERTY, evt -> propertyChange(new PropertyChangeEvent(evt.getSource(), evt.getPropertyName(), evt.getOldValue(), evt.getNewValue()))));
    EditorEventMulticaster multicaster = EditorFactory.getInstance().getEventMulticaster();
    if (multicaster instanceof EditorEventMulticasterEx) {
      ((EditorEventMulticasterEx)multicaster).addPropertyChangeListener(this, this);
    }
  }

  private void updateStatus() {
    if (!myProject.isDisposed()) return;
    final Editor editor = getFocusedEditor();
    if (editor != null && !isOurEditor(editor)) return;
    if (editor == null || !editor.isColumnMode()) {
      myTextPanel.setVisible(false);
    }
    else {
      myTextPanel.setVisible(true);
      myTextPanel.setText(UIBundle.message("status.bar.column.status.text"));
      myTextPanel.setToolTipText("Column selection mode");
    }
  }

  @Override
  public void fileOpened(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {
    updateStatus();
  }

  @Override
  public void fileClosed(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {
    updateStatus();
  }

  @Override
  public void propertyChange(@Nonnull PropertyChangeEvent evt) {
    String propertyName = evt.getPropertyName();
    if (EditorEx.PROP_INSERT_MODE.equals(propertyName) || EditorEx.PROP_COLUMN_MODE.equals(propertyName) || SWING_FOCUS_OWNER_PROPERTY.equals(propertyName)) {
      updateStatus();
    }
  }

  @Override
  public void selectionChanged(@Nonnull FileEditorManagerEvent event) {
    updateStatus();
  }
}
