/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.qt.editor.impl.internal;

import consulo.codeEditor.EditorFactory;
import consulo.language.editor.ui.EditorComboBox;
import consulo.language.editor.ui.internal.EditorBoxOptions;
import consulo.project.ProjectManager;
import consulo.ui.annotation.RequiredUIAccess;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.List;

class DesktopQtEditorComboBoxImpl extends DesktopQtEditorBoxBase<DesktopQtEditorComboBoxFrame> implements EditorComboBox {
    private List<String> myItems = List.of();

    DesktopQtEditorComboBoxImpl(EditorBoxOptions options, EditorFactory editorFactory, ProjectManager projectManager) {
        super(options, editorFactory, projectManager);
    }

    @Override
    protected DesktopQtEditorComboBoxFrame createHost(QWidget parent) {
        return new DesktopQtEditorComboBoxFrame(parent);
    }

    @RequiredUIAccess
    @Override
    protected void initialize(DesktopQtEditorComboBoxFrame component) {
        super.initialize(component);

        fillItems(component);

        component.activated.connect(this::itemActivated);
    }

    @RequiredUIAccess
    private void itemActivated(int index) {
        if (index >= 0 && index < myItems.size()) {
            setValue(myItems.get(index));
            mySupport.moveCaretToEnd();
        }
    }

    private void fillItems(DesktopQtEditorComboBoxFrame comboBox) {
        comboBox.clear();
        comboBox.addItems(myItems);
        syncSelection(comboBox);
    }

    private void syncSelection(DesktopQtEditorComboBoxFrame comboBox) {
        comboBox.setCurrentIndex(myItems.indexOf(getValue()));
    }

    @RequiredUIAccess
    @Override
    protected void valueChanged() {
        DesktopQtEditorComboBoxFrame comboBox = myComponent;
        if (comboBox != null && !comboBox.isDisposed()) {
            syncSelection(comboBox);
        }

        super.valueChanged();
    }

    @Override
    public List<String> getItems() {
        return myItems;
    }

    @RequiredUIAccess
    @Override
    public void setItems(List<String> items) {
        myItems = List.copyOf(items);

        DesktopQtEditorComboBoxFrame comboBox = myComponent;
        if (comboBox != null && !comboBox.isDisposed()) {
            fillItems(comboBox);
        }
    }

    @Override
    public @Nullable String getSelectedItem() {
        String value = getValue();
        return myItems.contains(value) ? value : null;
    }

    @RequiredUIAccess
    @Override
    public void setSelectedItem(@Nullable String item) {
        setValue(item);
    }
}
