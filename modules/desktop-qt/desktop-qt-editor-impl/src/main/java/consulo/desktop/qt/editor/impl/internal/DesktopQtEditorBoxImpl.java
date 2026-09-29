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

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.document.Document;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.internal.EditorBoxOptions;
import consulo.language.editor.ui.internal.EditorBoxSupport;
import consulo.localize.LocalizeValue;
import consulo.project.ProjectManager;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.virtualFileSystem.fileType.FileType;
import io.qt.core.Qt;
import io.qt.widgets.QFrame;
import io.qt.widgets.QHBoxLayout;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

class DesktopQtEditorBoxImpl extends QtComponentDelegate<QFrame> implements EditorBox {
    private final EditorBoxSupport mySupport;

    private @Nullable Component mySuffixComponent;

    private boolean myFireListeners = true;

    DesktopQtEditorBoxImpl(EditorBoxOptions options, EditorFactory editorFactory, ProjectManager projectManager) {
        mySupport = new EditorBoxSupport(options, editorFactory, projectManager, this::valueChanged, this::editorReleasing);
    }

    @Override
    protected QFrame createQt(QWidget parent) {
        QFrame frame = new QFrame(parent);
        frame.setFrameShape(QFrame.Shape.StyledPanel);
        frame.setFrameShadow(QFrame.Shadow.Sunken);

        QHBoxLayout layout = new QHBoxLayout(frame);
        layout.setContentsMargins(frame.frameWidth(), frame.frameWidth(), frame.frameWidth(), frame.frameWidth());
        layout.setSpacing(0);
        return frame;
    }

    @RequiredUIAccess
    @Override
    protected void initialize(QFrame component) {
        super.initialize(component);

        attachEditor(component);

        Component suffixComponent = mySuffixComponent;
        if (suffixComponent != null) {
            attachSuffix(component, suffixComponent);
        }

        component.destroyed.connect(() -> mySupport.releaseEditor());
    }

    @RequiredUIAccess
    private void attachEditor(QFrame frame) {
        EditorEx editor = mySupport.createEditor();

        QtComponentDelegate<?> editorComponent = (QtComponentDelegate<?>) editor.getUIComponent();
        editorComponent.setParent(this);
        editorComponent.bind(frame, null);

        QWidget editorWidget = editorComponent.toQtComponent();
        ((QHBoxLayout) frame.layout()).insertWidget(0, editorWidget, 1);
        frame.setFocusProxy(editorWidget);

        if (mySupport.isOneLine() && editor instanceof DesktopQtEditorImpl qtEditor) {
            DesktopQtEditorWidget surface = qtEditor.getSurface();
            if (surface != null) {
                surface.setVerticalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAlwaysOff);
                surface.setHorizontalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAlwaysOff);
                surface.setFixedHeight(qtEditor.getLineHeight() + surface.frameWidth() * 2);
            }
        }
    }

    private void attachSuffix(QFrame frame, Component suffixComponent) {
        QtComponentDelegate<?> suffix = (QtComponentDelegate<?>) suffixComponent;
        suffix.setParent(this);
        suffix.bind(frame, null);

        frame.layout().addWidget(suffix.toQtComponent());
    }

    private void editorReleasing(EditorEx editor) {
        if (editor.getUIComponent() instanceof QtComponentDelegate<?> editorComponent) {
            editorComponent.disposeQt();
        }
    }

    @RequiredUIAccess
    private void recreateEditor() {
        QFrame frame = myComponent;
        if (frame != null && !frame.isDisposed() && mySupport.getEditor() != null) {
            attachEditor(frame);
        }
    }

    @SuppressWarnings("unchecked")
    @RequiredUIAccess
    private void valueChanged() {
        if (myFireListeners) {
            getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent(this, getValue()));
        }
    }

    @RequiredUIAccess
    @Override
    public void disposeQt() {
        mySupport.releaseEditor();

        Component suffixComponent = mySuffixComponent;
        if (suffixComponent instanceof QtComponentDelegate<?> suffix) {
            suffix.disposeQt();
        }

        super.disposeQt();
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
    public void setPlaceholder(LocalizeValue text) {
        mySupport.setPlaceholder(text);
    }

    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        Component oldSuffix = mySuffixComponent;
        if (oldSuffix instanceof QtComponentDelegate<?> oldDelegate) {
            oldDelegate.setParent(null);
        }

        mySuffixComponent = suffixComponent;

        QFrame frame = myComponent;
        if (suffixComponent != null && frame != null && !frame.isDisposed()) {
            attachSuffix(frame, suffixComponent);
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
