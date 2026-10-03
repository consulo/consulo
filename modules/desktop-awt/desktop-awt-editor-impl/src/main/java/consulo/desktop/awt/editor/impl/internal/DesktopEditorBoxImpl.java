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
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.awt.AWTLanguageEditorUtil;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.language.editor.ui.internal.EditorBoxOptions;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.util.function.Consumer;

class DesktopEditorBoxImpl extends SwingComponentDelegate<DesktopEditorBoxImpl.MyEditorTextField> implements EditorBox {
    class MyEditorTextField extends EditorTextField implements FromSwingComponentWrapper {
        private MyEditorTextField() {
            super(myOptions.document(), myOptions.project(), myOptions.fileType(), myOptions.viewer(), myOptions.oneLine());
        }

        @Override
        protected EditorEx createEditor() {
            EditorEx editor = super.createEditor();
            editor.setHorizontalScrollbarVisible(!myOptions.oneLine());
            editor.setVerticalScrollbarVisible(!myOptions.oneLine());

            for (Consumer<EditorEx> customization : myOptions.customizations()) {
                customization.accept(editor);
            }
            return editor;
        }

        @Override
        public void uiDataSnapshot(DataSink sink) {
            super.uiDataSnapshot(sink);

            UiDataProvider provider = DesktopEditorBoxImpl.this.getUserData(UiDataProvider.KEY);
            if (provider != null) {
                provider.uiDataSnapshot(sink);
            }
        }

        @Override
        public Component toUIComponent() {
            return DesktopEditorBoxImpl.this;
        }
    }

    private final EditorBoxOptions myOptions;

    private @Nullable Component mySuffixComponent;

    private boolean myFireListeners = true;

    DesktopEditorBoxImpl(EditorBoxOptions options) {
        myOptions = options;
    }

    @Override
    protected MyEditorTextField createComponent() {
        MyEditorTextField field = new MyEditorTextField();
        if (myOptions.editorFont()) {
            field.setFontInheritedFromLAF(false);
            field.setFont(AWTLanguageEditorUtil.getEditorFont());
        }

        field.setPlaceholder(myOptions.placeholder().getNullIfEmpty());

        field.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(DocumentEvent event) {
                if (myFireListeners) {
                    fireValueChanged();
                }
            }
        });
        return field;
    }

    @SuppressWarnings("unchecked")
    @RequiredUIAccess
    private void fireValueChanged() {
        getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, getValue()));
    }

    @Override
    public Document getDocument() {
        return toAWTComponent().getDocument();
    }

    @RequiredUIAccess
    @Override
    public void setDocument(Document document, FileType fileType) {
        toAWTComponent().setNewDocumentAndFileType(fileType, document);
    }

    @Override
    public @Nullable Editor getEditor() {
        return toAWTComponent().getEditor();
    }

    @RequiredUIAccess
    @Override
    public void selectAll() {
        toAWTComponent().selectAll();
    }

    @Override
    public void setPlaceholder(LocalizeValue text) {
        toAWTComponent().setPlaceholder(text.getNullIfEmpty());
    }

    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        mySuffixComponent = suffixComponent;

        toAWTComponent().setSuffixComponent(suffixComponent == null ? null : (JComponent) TargetAWT.to(suffixComponent));
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }

    @Override
    public String getValue() {
        return toAWTComponent().getText();
    }

    @RequiredUIAccess
    @Override
    public void setValue(@Nullable String value, boolean fireListeners) {
        myFireListeners = fireListeners;
        try {
            toAWTComponent().setText(value);
        }
        finally {
            myFireListeners = true;
        }
    }
}
