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
package consulo.web.editor.impl.internal;

import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.dom.Element;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.document.Document;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.internal.EditorBoxOptions;
import consulo.language.editor.ui.internal.EditorBoxSupport;
import consulo.localize.LocalizeValue;
import consulo.project.ProjectManager;
import consulo.ui.Component;
import consulo.ui.HasFocus;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.ToVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

class WebEditorBoxImpl extends VaadinComponentDelegate<WebEditorBoxImpl.Vaadin> implements EditorBox {
    @Tag("vaadin-input-container")
    @StyleSheet("/editorBox/webEditorBox.css")
    public class Vaadin extends com.vaadin.flow.component.Component implements FromVaadinComponentWrapper, HasSize {
        @Override
        public Component toUIComponent() {
            return WebEditorBoxImpl.this;
        }
    }

    private final EditorBoxSupport mySupport;

    private @Nullable Component mySuffixComponent;

    private @Nullable Element myEditorElement;

    private boolean myFireListeners = true;

    @RequiredUIAccess
    WebEditorBoxImpl(EditorBoxOptions options, EditorFactory editorFactory, ProjectManager projectManager) {
        mySupport = new EditorBoxSupport(options, editorFactory, projectManager, this::valueChanged, editor -> {
        });

        Vaadin vaadin = toVaadinComponent();
        vaadin.getElement().getClassList().add("consulo-editor-box");
        vaadin.getElement().getClassList().set("consulo-editor-box-multiline", !options.oneLine());

        vaadin.addAttachListener(event -> attachEditor());
        vaadin.addDetachListener(event -> mySupport.releaseEditor());
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @RequiredUIAccess
    private void attachEditor() {
        Element container = toVaadinComponent().getElement();

        Element staleElement = myEditorElement;
        if (staleElement != null) {
            container.removeChild(staleElement);
            myEditorElement = null;
        }

        EditorEx editor = mySupport.createEditor();

        com.vaadin.flow.component.Component editorComponent = ((ToVaadinComponentWrapper) editor.getUIComponent()).toVaadinComponent();
        if (editorComponent instanceof ArquillEditorElement arquillEditor) {
            arquillEditor.setOneLine(mySupport.isOneLine());
        }

        Element editorElement = editorComponent.getElement();
        container.insertChild(0, editorElement);
        myEditorElement = editorElement;
    }

    @RequiredUIAccess
    private void recreateEditor() {
        if (mySupport.getEditor() != null) {
            attachEditor();
        }
    }

    @SuppressWarnings("unchecked")
    @RequiredUIAccess
    private void valueChanged() {
        if (myFireListeners) {
            getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, getValue()));
        }
    }

    @Override
    public Document getDocument() {
        return mySupport.getDocument();
    }

    @RequiredUIAccess
    @Override
    public void setDocument(Document document, FileType fileType) {
        mySupport.setDocument(document, fileType);

        recreateEditor();
    }

    @Override
    public @Nullable Editor getEditor() {
        return mySupport.getEditor();
    }

    @RequiredUIAccess
    @Override
    public void selectAll() {
        mySupport.selectAll();
    }

    @RequiredUIAccess
    @Override
    public void setEnabled(boolean value) {
        super.setEnabled(value);

        if (mySupport.isEnabled() != value) {
            mySupport.setEnabled(value);

            recreateEditor();
        }
    }

    @Override
    public void focus() {
        EditorEx editor = mySupport.getEditor();
        if (editor != null && editor.getContentUIComponent() instanceof HasFocus hasFocus) {
            hasFocus.focus();
        }
    }

    @Override
    public void setPlaceholder(LocalizeValue text) {
        mySupport.setPlaceholder(text);
    }

    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        Element container = toVaadinComponent().getElement();

        Component oldSuffix = mySuffixComponent;
        if (oldSuffix != null) {
            container.removeChild(((ToVaadinComponentWrapper) oldSuffix).toVaadinComponent().getElement());
        }

        mySuffixComponent = suffixComponent;

        if (suffixComponent != null) {
            Element suffixElement = ((ToVaadinComponentWrapper) suffixComponent).toVaadinComponent().getElement();
            suffixElement.setAttribute("slot", "suffix");
            container.appendChild(suffixElement);
        }
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }

    @Override
    public String getValue() {
        return mySupport.getText();
    }

    @RequiredUIAccess
    @Override
    public void setValue(@Nullable String value, boolean fireListeners) {
        myFireListeners = fireListeners;
        try {
            mySupport.setText(value);
        }
        finally {
            myFireListeners = true;
        }
    }
}
