// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.event.EditorEventMulticaster;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.fileEditor.statusBar.EditorBasedWidget;
import consulo.ide.impl.idea.openapi.editor.ex.EditorEventMulticasterEx;
import consulo.project.Project;
import consulo.project.ui.wm.CustomStatusBarWidget;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.ui.ex.awt.FocusUtil;
import consulo.ui.ex.localize.UILocalize;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;

import javax.swing.*;

/**
 * @author cdr
 */
public class ColumnSelectionModePanel extends EditorBasedWidget
    implements StatusBarWidget.Multiframe, CustomStatusBarWidget, PropertyChangeListener {
    private final TextPanel myTextPanel = new TextPanel();

    public ColumnSelectionModePanel(@Nonnull Project project, @Nonnull StatusBarWidgetFactory factory) {
        super(project, factory);
        myTextPanel.setVisible(false);
    }

    @Override
    public WidgetPresentation getPresentation() {
        return null;
    }

    @Override
    public StatusBarWidget copy() {
        return new ColumnSelectionModePanel(getProject(), myFactory);
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
        return myTextPanel;
    }

    @Override
    public void install(@Nonnull StatusBar statusBar) {
        super.install(statusBar);

        FocusUtil.addFocusOwnerListener(this, evt -> updateStatus());
        EditorEventMulticaster multicaster = EditorFactory.getInstance().getEventMulticaster();
        if (multicaster instanceof EditorEventMulticasterEx multicasterEx) {
            multicasterEx.addPropertyChangeListener(this, this);
        }
    }

    private void updateStatus() {
        if (!myProject.isDisposed()) {
            return;
        }
        Editor editor = getFocusedEditor();
        if (editor != null && !isOurEditor(editor)) {
            return;
        }
        if (editor == null || !editor.isColumnMode()) {
            myTextPanel.setVisible(false);
        }
        else {
            myTextPanel.setVisible(true);
            myTextPanel.setText(UILocalize.statusBarColumnStatusText().get());
            myTextPanel.setToolTipText(UILocalize.statusBarColumnStatusTooltipText().get());
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
        if (EditorEx.PROP_INSERT_MODE.equals(propertyName) || EditorEx.PROP_COLUMN_MODE.equals(propertyName)) {
            updateStatus();
        }
    }

    @Override
    public void selectionChanged(@Nonnull FileEditorManagerEvent event) {
        updateStatus();
    }
}
