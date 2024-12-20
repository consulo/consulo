/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.editor.ui.awt;

import consulo.application.ui.wm.ApplicationIdeFocusManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.event.FocusChangeListener;
import consulo.document.Document;
import consulo.project.Project;
import consulo.ui.ex.awt.JBUI;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

/**
 * User: spLeaner
 */
public class ComboboxEditorTextField extends EditorTextField {
    public ComboboxEditorTextField(@Nonnull String text, Project project, FileType fileType) {
        super(text, project, fileType);
        setOneLineMode(true);
        putClientProperty("JComboBox.isTableCellEditor", true);
        setBorder(JBUI.Borders.empty());
    }

    public ComboboxEditorTextField(Document document, Project project, FileType fileType) {
        this(document, project, fileType, false);
        setOneLineMode(true);
        putClientProperty("JComboBox.isTableCellEditor", true);
        setBorder(JBUI.Borders.empty());
    }

    public ComboboxEditorTextField(Document document, Project project, FileType fileType, boolean isViewer) {
        super(document, project, fileType, isViewer);
        setOneLineMode(true);
        putClientProperty("JComboBox.isTableCellEditor", true);
        setBorder(JBUI.Borders.empty());
    }

    @Nonnull
    @Override
    protected Color getBackgroundColor(boolean enabled) {
        return enabled ? super.getBackgroundColor(enabled) : UIManager.getColor("ComboBox.disabledBackground");
    }

    @Override
    protected void addFocusHacks() {
    }

    @Override
    protected EditorEx createEditor() {
        final EditorEx result = super.createEditor();

        result.addFocusListener(new FocusChangeListener() {
            @Override
            public void focusGained(Editor editor) {
                repaintComboBox();
            }

            @Override
            public void focusLost(Editor editor) {
                repaintComboBox();
            }
        });

        return result;
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    private void repaintComboBox() {
        ApplicationIdeFocusManager.getInstance().getInstanceForProject(getProject()).doWhenFocusSettlesDown(() -> {
            final Container parent = getParent();
            if (parent != null) {
                parent.repaint();
            }
        });
    }
}
