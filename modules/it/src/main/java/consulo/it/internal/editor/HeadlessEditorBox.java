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
package consulo.it.internal.editor;

import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.it.internal.ui.HeadlessComponentBase;
import consulo.language.editor.ui.EditorBox;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.undoRedo.CommandProcessor;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 */
public class HeadlessEditorBox extends HeadlessComponentBase implements EditorBox {
    private final @Nullable Project myProject;
    private Document myDocument;
    private FileType myFileType;
    private LocalizeValue myPlaceholder;
    private @Nullable Component mySuffixComponent;

    public HeadlessEditorBox(@Nullable Project project, Document document, FileType fileType, LocalizeValue placeholder) {
        myProject = project;
        myDocument = document;
        myFileType = fileType;
        myPlaceholder = placeholder;
    }

    public FileType getFileType() {
        return myFileType;
    }

    public LocalizeValue getPlaceholder() {
        return myPlaceholder;
    }

    @Override
    public Document getDocument() {
        return myDocument;
    }

    @RequiredUIAccess
    @Override
    public void setDocument(Document document, FileType fileType) {
        myDocument = document;
        myFileType = fileType;
    }

    @Override
    public @Nullable Editor getEditor() {
        return null;
    }

    @RequiredUIAccess
    @Override
    public void selectAll() {
    }

    @Override
    public String getValue() {
        return myDocument.getText();
    }

    @RequiredUIAccess
    @Override
    @SuppressWarnings("unchecked")
    public void setValue(@Nullable String value, boolean fireListeners) {
        Document document = myDocument;
        String text = value == null ? "" : value;
        if (text.equals(document.getText())) {
            return;
        }
        CommandProcessor.getInstance().newCommand()
            .project(myProject)
            .document(document)
            .inWriteAction()
            .run(() -> document.replaceString(0, document.getTextLength(), text));
        if (fireListeners) {
            getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent<>(this, getValue()));
        }
    }

    @Override
    public void setPlaceholder(LocalizeValue text) {
        myPlaceholder = text;
    }

    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        mySuffixComponent = suffixComponent;
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }

    @Override
    public boolean hasFocus() {
        return false;
    }

    @Override
    public void focus() {
    }

    @Override
    public void setFocusable(boolean focusable) {
    }

    @Override
    public boolean isFocusable() {
        return false;
    }
}
