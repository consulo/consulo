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

import com.vaadin.flow.dom.Element;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.document.Document;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.internal.EditorBoxOptions;
import consulo.language.editor.ui.internal.EditorBoxSupport;
import consulo.localize.LocalizeValue;
import consulo.project.ProjectManager;
import consulo.ui.HasFocus;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.ToVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

abstract class WebEditorBoxBase<V extends com.vaadin.flow.component.Component & FromVaadinComponentWrapper> extends VaadinComponentDelegate<V> implements EditorBox {
    protected final EditorBoxSupport mySupport;

    private @Nullable Element myEditorElement;

    private boolean myFireListeners = true;
    private boolean myViewerHeightListenerAdded;

    private final DocumentListener myViewerHeightListener = new DocumentListener() {
        @Override
        public void documentChanged(DocumentEvent event) {
            EditorEx editor = mySupport.getEditor();
            if (editor != null) {
                fitContentHeight(editor);
            }
        }
    };

    @RequiredUIAccess
    protected WebEditorBoxBase(EditorBoxOptions options, EditorFactory editorFactory, ProjectManager projectManager) {
        mySupport = new EditorBoxSupport(options, editorFactory, projectManager, this::valueChanged, editor -> {
        });

        V vaadin = toVaadinComponent();
        vaadin.addAttachListener(event -> attachEditor());
        vaadin.addDetachListener(event -> {
            if (myViewerHeightListenerAdded) {
                mySupport.getDocument().removeDocumentListener(myViewerHeightListener);
                myViewerHeightListenerAdded = false;
            }
            mySupport.releaseEditor();
        });
    }

    protected abstract void insertEditorElement(Element container, Element editorElement);

    @RequiredUIAccess
    private void attachEditor() {
        Element container = toVaadinComponent().getElement();

        Element staleElement = myEditorElement;
        if (staleElement != null) {
            container.removeChild(staleElement);
            myEditorElement = null;
        }

        EditorEx editor = mySupport.createEditor();

        com.vaadin.flow.component.Component editorComponent = ((ToVaadinComponentWrapper) editor.getContentUIComponent()).toVaadinComponent();
        if (editorComponent instanceof ArquillEditorElement arquillEditor) {
            arquillEditor.setOneLine(mySupport.isOneLine());
            if (!mySupport.isOneLine() && mySupport.isViewer()) {
                arquillEditor.addMetricsListener(event -> fitContentHeight(editor.getDocument().getLineCount(), event.getLineHeight()));
            }
        }

        Element editorElement = editorComponent.getElement();
        insertEditorElement(container, editorElement);
        myEditorElement = editorElement;

        if (!mySupport.isOneLine() && mySupport.isViewer()) {
            fitContentHeight(editor);
            Document document = mySupport.getDocument();
            if (!myViewerHeightListenerAdded) {
                document.addDocumentListener(myViewerHeightListener);
                myViewerHeightListenerAdded = true;
            }
        }
    }

    /**
     * A viewer of several lines shows them all, with room for the horizontal scroll bar under them.
     */
    private void fitContentHeight(EditorEx editor) {
        fitContentHeight(editor.getDocument().getLineCount(), editor.getLineHeight());
    }

    private void fitContentHeight(int lineCount, int lineHeight) {
        int lines = Math.max(1, lineCount);
        toVaadinComponent().getElement().getStyle().set("height", "calc(" + (lines * lineHeight) + "px + 1.5em)");
    }

    @RequiredUIAccess
    private void recreateEditor() {
        if (mySupport.getEditor() != null) {
            attachEditor();
        }
    }

    @SuppressWarnings("unchecked")
    @RequiredUIAccess
    protected void valueChanged() {
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
