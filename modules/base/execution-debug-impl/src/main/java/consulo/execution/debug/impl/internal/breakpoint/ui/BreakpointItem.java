/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.breakpoint.ui;

import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.codeEditor.util.popup.DetailView;
import consulo.codeEditor.util.popup.ItemWrapper;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.execution.debug.ui.DebuggerColors;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;

import javax.swing.*;
import java.util.Objects;

public abstract class BreakpointItem extends ItemWrapper implements Comparable<BreakpointItem>, Navigatable {
    public static final Key<Object> EDITOR_ONLY = Key.create("EditorOnly");

    public abstract void saveState();

    public abstract Object getBreakpoint();

    public abstract boolean isEnabled();

    public abstract void setEnabled(boolean state);

    public abstract boolean isDefaultBreakpoint();

    protected static void showInEditor(DetailView panel, VirtualFile virtualFile, int line) {
        TextAttributes attributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(DebuggerColors.BREAKPOINT_ATTRIBUTES);

        DetailView.PreviewEditorState state = DetailView.PreviewEditorState.create(virtualFile, line, attributes);

        if (state.equals(panel.getEditorState())) {
            return;
        }

        panel.navigateInPreviewEditor(state);

        TextAttributes softerAttributes = attributes.clone();
        ColorValue backgroundColor = softerAttributes.getBackgroundColor();
        if (backgroundColor != null) {
            // FIXME [VISTALL] softer is not supported softerAttributes.setBackgroundColor(ColorUtil.softer(backgroundColor));
        }

        Editor editor = panel.getEditor();
        MarkupModel editorModel = editor.getMarkupModel();
        MarkupModel documentModel = DocumentMarkupModel.forDocument(editor.getDocument(), editor.getProject(), false);

        for (RangeHighlighter highlighter : documentModel.getAllHighlighters()) {
            if (Objects.equals(highlighter.getUserData(DebuggerColors.BREAKPOINT_HIGHLIGHTER_KEY), Boolean.TRUE)) {
                int line1 = editor.offsetToLogicalPosition(highlighter.getStartOffset()).line;
                if (line1 != line) {
                    editorModel.addLineHighlighter(line1, DebuggerColors.BREAKPOINT_HIGHLIGHTER_LAYER + 1, softerAttributes);
                }
            }
        }
    }

    @Override
    public void updateAccessoryView(JComponent component) {
        JCheckBox checkBox = (JCheckBox) component;
        checkBox.setSelected(isEnabled());
    }

    @Override
    public void setupRenderer(ColoredTextContainer renderer, Project project, boolean selected) {
        boolean plainView = true;
        if (renderer instanceof ColoredTreeCellRenderer treeCellRenderer) {
            plainView = false;
        }
        setupGenericRenderer(renderer, plainView);
    }

    public abstract void setupGenericRenderer(ColoredTextContainer renderer, boolean plainView);

    public abstract Image getIcon();

    public abstract String getDisplayText();

    protected void dispose() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BreakpointItem item = (BreakpointItem) o;

        if (getBreakpoint() != null ? !getBreakpoint().equals(item.getBreakpoint()) : item.getBreakpoint() != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return getBreakpoint() != null ? getBreakpoint().hashCode() : 0;
    }
}
