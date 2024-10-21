/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import consulo.codeEditor.CaretModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.document.Document;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.language.plain.PlainTextFileType;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.TextComponentAccessor;
import consulo.ui.ex.awt.util.MacUIUtil;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.Lists;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author max
 */
public class EditorComboBox extends ComboBox implements DocumentListener {
    public static TextComponentAccessor<EditorComboBox> COMPONENT_ACCESSOR = new TextComponentAccessor<>() {
        @Override
        public String getText(EditorComboBox component) {
            return component.getText();
        }

        @Override
        @RequiredUIAccess
        public void setText(EditorComboBox component, @Nonnull String text) {
            component.setText(text);
        }
    };

    private Document myDocument;
    private final Project myProject;
    private EditorTextField myEditorField = null;
    private final List<DocumentListener> myDocumentListeners = Lists.newLockFreeCopyOnWriteList();
    private boolean myIsListenerInstalled = false;
    private boolean myInheritSwingFont = true;
    private final FileType myFileType;
    private final boolean myIsViewer;

    public EditorComboBox(String text) {
        this(EditorFactory.getInstance().createDocument(text), null, PlainTextFileType.INSTANCE);
    }

    public EditorComboBox(String text, Project project, FileType fileType) {
        this(EditorFactory.getInstance().createDocument(text), project, fileType, false);
    }

    public EditorComboBox(Document document, Project project, FileType fileType) {
        this(document, project, fileType, false);
    }

    public EditorComboBox(Document document, Project project, FileType fileType, boolean isViewer) {
        myFileType = fileType;
        myIsViewer = isViewer;
        setDocument(document);
        myProject = project;
        enableEvents(AWTEvent.KEY_EVENT_MASK);

        addActionListener(e -> {
            final Editor editor = myEditorField != null ? myEditorField.getEditor() : null;
            if (editor != null) {
                editor.getSelectionModel().removeSelection();
            }
        });
        setHistory(new String[]{""});
        setEditable(true);
    }

    public void setFontInheritedFromLAF(boolean b) {
        myInheritSwingFont = b;
        setDocument(myDocument); // reinit editor.
    }

    public String getText() {
        return myDocument.getText();
    }

    public void addDocumentListener(DocumentListener listener) {
        myDocumentListeners.add(listener);
        installDocumentListener();
    }

    public void removeDocumentListener(DocumentListener listener) {
        myDocumentListeners.remove(listener);
        uninstallDocumentListener(false);
    }

    @Override
    public void beforeDocumentChange(DocumentEvent event) {
        for (DocumentListener documentListener : myDocumentListeners) {
            documentListener.beforeDocumentChange(event);
        }
    }

    @Override
    public void documentChanged(DocumentEvent event) {
        for (DocumentListener documentListener : myDocumentListeners) {
            documentListener.documentChanged(event);
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        MacUIUtil.drawComboboxFocusRing(this, g);
    }

    public Project getProject() {
        return myProject;
    }

    public Document getDocument() {
        return myDocument;
    }

    public void setDocument(Document document) {
        if (myDocument != null) {
            uninstallDocumentListener(true);
        }

        myDocument = document;
        installDocumentListener();
        if (myEditorField == null) {
            return;
        }

        myEditorField.setDocument(document);
    }

    private void installDocumentListener() {
        if (myDocument != null && !myDocumentListeners.isEmpty() && !myIsListenerInstalled) {
            myIsListenerInstalled = true;
            myDocument.addDocumentListener(this);
        }
    }

    private void uninstallDocumentListener(boolean force) {
        if (myDocument != null && myIsListenerInstalled && (force || myDocumentListeners.isEmpty())) {
            myIsListenerInstalled = false;
            myDocument.removeDocumentListener(this);
        }
    }

    @RequiredUIAccess
    public void setText(final String text) {
        CommandProcessor.getInstance().newCommand(() -> {
                myDocument.replaceString(0, myDocument.getTextLength(), text);
                if (myEditorField != null && myEditorField.getEditor() != null) {
                    myEditorField.getCaretModel().moveToOffset(myDocument.getTextLength());
                }
            })
            .withProject(getProject())
            .withDocument(myDocument)
            .executeInWriteAction();
    }

    public void removeSelection() {
        if (myEditorField != null) {
            final Editor editor = myEditorField.getEditor();
            if (editor != null) {
                editor.getSelectionModel().removeSelection();
            }
        }
    }

    public CaretModel getCaretModel() {
        return myEditorField.getCaretModel();
    }

    public void setHistory(final String[] history) {
        setModel(new DefaultComboBoxModel(history));
    }

    public void prependItem(String item) {
        ArrayList<Object> objects = new ArrayList<>();
        objects.add(item);
        int count = getItemCount();
        for (int i = 0; i < count; i++) {
            final Object itemAt = getItemAt(i);
            if (!item.equals(itemAt)) {
                objects.add(itemAt);
            }
        }
        setModel(new DefaultComboBoxModel(ArrayUtil.toObjectArray(objects)));
    }

    public void appendItem(String item) {
        ArrayList<Object> objects = new ArrayList<>();

        int count = getItemCount();
        for (int i = 0; i < count; i++) {
            objects.add(getItemAt(i));
        }

        if (!objects.contains(item)) {
            objects.add(item);
        }
        setModel(new DefaultComboBoxModel(ArrayUtil.toObjectArray(objects)));
    }

    private class MyEditor implements ComboBoxEditor {
        @Override
        public void addActionListener(ActionListener l) {
        }

        @Override
        public Component getEditorComponent() {
            return myEditorField;
        }

        @Override
        public Object getItem() {
            return myDocument.getText();
        }

        @Override
        public void removeActionListener(ActionListener l) {
        }

        @Override
        public void selectAll() {
            if (myEditorField != null) {
                final Editor editor = myEditorField.getEditor();
                if (editor != null) {
                    editor.getSelectionModel().setSelection(0, myDocument.getTextLength());
                }
            }
        }

        @Override
        @RequiredUIAccess
        public void setItem(Object anObject) {
            EditorComboBox.this.setText(anObject != null ? anObject.toString() : "");
        }
    }

    @Override
    public void addNotify() {
        releaseEditor();
        setEditor();

        super.addNotify();

        myEditorField.getFocusTarget().addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                EditorComboBox.this.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                EditorComboBox.this.repaint();
            }
        });
    }

    private void setEditor() {
        myEditorField = createEditorTextField(myDocument, myProject, myFileType, myIsViewer);
        final ComboBoxEditor editor = new MyEditor();
        setEditor(editor);
        setRenderer(new EditorComboBoxRenderer(editor));
    }

    protected ComboboxEditorTextField createEditorTextField(Document document, Project project, FileType fileType, boolean isViewer) {
        return new ComboboxEditorTextField(document, project, fileType, isViewer);
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        releaseEditor();
        myEditorField = null;
    }

    private void releaseEditor() {
        if (myEditorField != null) {
            myEditorField.releaseEditorLater();
        }
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        if (myEditorField != null && myEditorField.getEditor() != null) {
            setupEditorFont((EditorEx)myEditorField.getEditor());
        }
    }

    private void setupEditorFont(final EditorEx editor) {
        if (myInheritSwingFont) {
            editor.getColorsScheme().setEditorFontName(getFont().getFontName());
            editor.getColorsScheme().setEditorFontSize(getFont().getSize());
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (myEditorField == null) {
            return;
        }
        myEditorField.setEnabled(enabled);
    }

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        return ((EditorEx)myEditorField.getEditor()).processKeyTyped(e) || super.processKeyBinding(ks, e, condition, pressed);
    }

    public EditorEx getEditorEx() {
        return myEditorField != null ? (EditorEx)myEditorField.getEditor() : null;
    }
}
