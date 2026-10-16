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
package consulo.desktop.awt.editor.impl.internal;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.document.Document;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.language.editor.ui.EditorComboBox;
import consulo.language.editor.ui.awt.AWTLanguageEditorUtil;
import consulo.language.editor.ui.awt.ComboboxEditorTextField;
import consulo.language.editor.ui.internal.EditorBoxOptions;
import consulo.language.editor.ui.internal.EditorBoxSupport;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import javax.swing.ComboBoxEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.function.Consumer;

class DesktopEditorComboBoxImpl extends SwingComponentDelegate<DesktopEditorComboBoxImpl.MyComboBox> implements EditorComboBox {
    class MyComboBox extends ComboBox<String> implements FromSwingComponentWrapper {
        @Override
        public void setMinimumSize(@Nullable Dimension minimumSize) {
            super.setMinimumSize(minimumSize);
            setMinimumAndPreferredWidth(minimumSize == null ? -1 : minimumSize.width);
        }

        @Override
        public Component toUIComponent() {
            return DesktopEditorComboBoxImpl.this;
        }
    }

    class MyEditorTextField extends ComboboxEditorTextField {
        private MyEditorTextField() {
            super(myOptions.document(), myOptions.project(), myOptions.fileType(), myOptions.viewer());
        }

        @Override
        protected EditorEx createEditor() {
            EditorEx editor = super.createEditor();
            for (Consumer<EditorEx> customization : myOptions.customizations()) {
                customization.accept(editor);
            }
            return editor;
        }

        @Override
        public void uiDataSnapshot(DataSink sink) {
            super.uiDataSnapshot(sink);

            UiDataProvider provider = DesktopEditorComboBoxImpl.this.getUserData(UiDataProvider.KEY);
            if (provider != null) {
                provider.uiDataSnapshot(sink);
            }
        }
    }

    class MyComboBoxEditor implements ComboBoxEditor {
        @Override
        public java.awt.Component getEditorComponent() {
            return myTextField;
        }

        @Override
        @RequiredUIAccess
        public void setItem(@Nullable Object item) {
            if (myUpdating || item == null || item.equals(myTextField.getText())) {
                return;
            }
            Editor editor = myTextField.getEditor();
            EditorBoxSupport.replaceText(myOptions.project(), myTextField.getDocument(), editor, (String) item);
            if (editor != null) {
                editor.getCaretModel().moveToOffset(editor.getDocument().getTextLength());
            }
        }

        @Override
        public Object getItem() {
            return myTextField.getText();
        }

        @Override
        public void selectAll() {
            myTextField.selectAll();
        }

        @Override
        public void addActionListener(ActionListener l) {
        }

        @Override
        public void removeActionListener(ActionListener l) {
        }
    }

    private final EditorBoxOptions myOptions;

    private final MyEditorTextField myTextField;

    private List<String> myItems = List.of();

    private @Nullable Component mySuffixComponent;

    private boolean myFireListeners = true;

    private boolean myUpdating;

    DesktopEditorComboBoxImpl(EditorBoxOptions options) {
        myOptions = options;
        myTextField = new MyEditorTextField();
    }

    @Override
    protected MyComboBox createComponent() {
        MyEditorTextField field = myTextField;
        if (myOptions.editorFont()) {
            field.setFontInheritedFromLAF(false);
            field.setFont(AWTLanguageEditorUtil.getEditorFont());
        }

        field.setPlaceholder(myOptions.placeholder().getNullIfEmpty());

        field.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(DocumentEvent event) {
                syncSelection();

                if (myFireListeners) {
                    fireValueChanged();
                }
            }
        });

        MyComboBox comboBox = new MyComboBox();
        comboBox.setEditable(true);
        comboBox.setEditor(new MyComboBoxEditor());
        return comboBox;
    }

    private void syncSelection() {
        MyComboBox comboBox = toAWTComponent();
        myUpdating = true;
        try {
            comboBox.getModel().setSelectedItem(getSelectedItem());
        }
        finally {
            myUpdating = false;
        }
    }

    @SuppressWarnings("unchecked")
    @RequiredUIAccess
    private void fireValueChanged() {
        getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, getValue()));
    }

    @Override
    public List<String> getItems() {
        return myItems;
    }

    @RequiredUIAccess
    @Override
    public void setItems(List<String> items) {
        myItems = List.copyOf(items);

        MyComboBox comboBox = toAWTComponent();
        myUpdating = true;
        try {
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(myItems.toArray(String[]::new));
            model.setSelectedItem(getSelectedItem());
            comboBox.setModel(model);
        }
        finally {
            myUpdating = false;
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

    @Override
    public Document getDocument() {
        return myTextField.getDocument();
    }

    @RequiredUIAccess
    @Override
    public void setDocument(Document document, FileType fileType) {
        myTextField.setNewDocumentAndFileType(fileType, document);
    }

    @Override
    public @Nullable Editor getEditor() {
        return myTextField.getEditor();
    }

    @RequiredUIAccess
    @Override
    public void selectAll() {
        myTextField.selectAll();
    }

    @Override
    public void setPlaceholder(LocalizeValue text) {
        myTextField.setPlaceholder(text.getNullIfEmpty());
    }

    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        mySuffixComponent = suffixComponent;

        myTextField.setSuffixComponent(suffixComponent == null ? null : (JComponent) TargetAWT.to(suffixComponent));
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }

    @Override
    public String getValue() {
        return myTextField.getText();
    }

    @RequiredUIAccess
    @Override
    public void setValue(@Nullable String value, boolean fireListeners) {
        myFireListeners = fireListeners;
        try {
            myTextField.setText(value);
        }
        finally {
            myFireListeners = true;
        }
    }
}
