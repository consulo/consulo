/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.language.editor.ui;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.EditorSettings;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.SoftWrapAppliancePlaces;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.codeEditor.util.popup.DetailView;
import consulo.codeEditor.util.popup.ItemWrapper;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.language.editor.highlight.EditorHighlighterFactory;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.layout.DockLayout;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * @author zajac
 * @since 2012-05-06
 */
public class DetailViewImpl implements DetailView, UserDataHolder {
    private final Project myProject;
    private final UserDataHolderBase myDataHolderBase = new UserDataHolderBase();
    private final DockLayout myRoot = DockLayout.create(Space.NONE);
    private final Label myLabel = Label.create();

    private @Nullable Editor myEditor;
    private @Nullable ItemWrapper myWrapper;
    private @Nullable Component myDetailPanel;
    private @Nullable DockLayout myDetailPanelWrapper;
    private @Nullable Component myCenter;
    private @Nullable RangeHighlighter myHighlighter;
    private PreviewEditorState myEditorState = PreviewEditorState.EMPTY;
    private LocalizeValue myEmptyLabel = UILocalize.messageNothingtoshow();

    @RequiredUIAccess
    public DetailViewImpl(Project project) {
        myProject = project;
        myRoot.paddingBuilder().allSet(Space.SMALL).apply();
    }

    @Override
    public Component getUIComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    private void setCenter(@Nullable Component component) {
        Component old = myCenter;
        if (old == component) {
            return;
        }
        if (old != null) {
            myRoot.remove(old);
        }
        myCenter = component;
        if (component != null) {
            myRoot.center(component);
        }
    }

    @RequiredUIAccess
    @Override
    public void clearEditor() {
        Editor editor = myEditor;
        if (editor != null) {
            clearHighlighting();
            if (myCenter != myLabel) {
                setCenter(null);
            }
            EditorFactory.getInstance().releaseEditor(editor);
            myEditorState = PreviewEditorState.EMPTY;
            myEditor = null;
        }
    }

    @Override
    public void setCurrentItem(@Nullable ItemWrapper wrapper) {
        myWrapper = wrapper;
    }

    @Override
    public PreviewEditorState getEditorState() {
        return myEditorState;
    }

    @Override
    public @Nullable ItemWrapper getCurrentItem() {
        return myWrapper;
    }

    @Override
    public boolean hasEditorOnly() {
        return false;
    }

    @Override
    public @Nullable Editor getEditor() {
        return myEditor;
    }

    @RequiredUIAccess
    @Override
    public void navigateInPreviewEditor(PreviewEditorState editorState) {
        VirtualFile file = editorState.getFile();
        LogicalPosition positionToNavigate = editorState.getNavigate();
        TextAttributes lineAttributes = editorState.getAttributes();
        Document document = file == null ? null : FileDocumentManager.getInstance().getDocument(file);

        clearEditor();
        myEditorState = editorState;
        if (document != null) {
            Editor editor = createEditor(myProject, document, file);
            editor.setBorder(null);
            myEditor = editor;

            DockLayout editorWrapper = DockLayout.create(Space.NONE);
            editorWrapper.borderBuilder().allSet().apply();
            editorWrapper.center(editor.getUIComponent());
            setCenter(editorWrapper);

            if (positionToNavigate != null) {
                editor.getCaretModel().moveToLogicalPosition(positionToNavigate);
                UIAccess.current().give(() -> {
                    if (myEditor == editor && !editor.isDisposed()) {
                        editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
                    }
                });
            }

            if (lineAttributes != null && positionToNavigate != null && positionToNavigate.line < editor.getDocument().getLineCount()) {
                myHighlighter = editor.getMarkupModel().addLineHighlighter(positionToNavigate.line, HighlighterLayer.SELECTION - 1, lineAttributes);
            }
        }
        else if (file != null) {
            myLabel.setText(LocalizeValue.of("Navigate to selected " + (file.isDirectory() ? "directory " : "file ") + "in Project View"));
            setCenter(myLabel);
        }
    }

    protected Editor createEditor(@Nullable Project project, Document document, VirtualFile file) {
        EditorEx editor = (EditorEx) EditorFactory.getInstance().createViewer(document, project);

        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        editor.setFile(file);
        editor.setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(file, scheme, project));

        EditorSettings settings = editor.getSettings();
        settings.setAnimatedScrolling(false);
        settings.setRefrainFromScrolling(false);
        settings.setLineNumbersShown(true);
        settings.setFoldingOutlineShown(false);
        settings.setSoftWrapAppliancePlace(SoftWrapAppliancePlaces.PREVIEW);
        editor.getFoldingModel().setFoldingEnabled(false);

        return editor;
    }

    private void clearHighlighting() {
        RangeHighlighter highlighter = myHighlighter;
        Editor editor = myEditor;
        if (highlighter != null && editor != null) {
            editor.getMarkupModel().removeHighlighter(highlighter);
        }
        myHighlighter = null;
    }

    @Override
    public @Nullable Component getPropertiesPanel() {
        return myDetailPanel;
    }

    @RequiredUIAccess
    @Override
    public void setPropertiesPanel(@Nullable Component panel) {
        DockLayout wrapper = myDetailPanelWrapper;
        if (panel == null) {
            if (wrapper != null) {
                wrapper.removeAll();
            }
            myLabel.setText(myEmptyLabel);
            setCenter(myLabel);
        }
        else if (panel != myDetailPanel) {
            if (myCenter == myLabel) {
                setCenter(null);
            }
            if (wrapper == null) {
                wrapper = DockLayout.create(Space.NONE);
                wrapper.paddingBuilder().bottomSet(Space.SMALL).apply();
                myDetailPanelWrapper = wrapper;
                myRoot.top(wrapper);
            }
            else {
                wrapper.removeAll();
            }
            wrapper.center(panel);
        }
        myDetailPanel = panel;
    }

    public void setEmptyLabel(LocalizeValue text) {
        myEmptyLabel = text;
    }

    @Override
    public <T> @Nullable T getUserData(Key<T> key) {
        return myDataHolderBase.getUserData(key);
    }

    @Override
    public <T> void putUserData(Key<T> key, @Nullable T value) {
        myDataHolderBase.putUserData(key, value);
    }
}
