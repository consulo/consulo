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
package consulo.language.editor.ui.awt;

import consulo.codeEditor.*;
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
import consulo.project.Project;
import consulo.ui.ex.UIBundle;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author zajac
 * @since 6.05.2012
 */
public class DetailViewImpl extends JPanel implements DetailView, UserDataHolder {
    private final Project myProject;
    private final UserDataHolderBase myDataHolderBase = new UserDataHolderBase();
    private final JLabel myLabel = new JLabel("", SwingConstants.CENTER);

    private Editor myEditor;
    private ItemWrapper myWrapper;
    private JPanel myDetailPanel;
    private JPanel myDetailPanelWrapper;
    private RangeHighlighter myHighlighter;
    private PreviewEditorState myEditorState = PreviewEditorState.EMPTY;
    private String myEmptyLabel = UIBundle.message("message.nothingToShow");

    public DetailViewImpl(Project project) {
        super(new BorderLayout());
        myProject = project;

        setPreferredSize(JBUI.size(600, 300));
        myLabel.setVerticalAlignment(SwingConstants.CENTER);
    }

    @Override
    public void clearEditor() {
        if (getEditor() != null) {
            clearHighlighting();
            remove(getEditor().getComponent());
            EditorFactory.getInstance().releaseEditor(getEditor());
            myEditorState = PreviewEditorState.EMPTY;
            setEditor(null);
            repaint();
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
    public ItemWrapper getCurrentItem() {
        return myWrapper;
    }

    @Override
    public boolean hasEditorOnly() {
        return false;
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (ScreenUtil.isStandardAddRemoveNotify(this)) {
            clearEditor();
        }
    }

    @Override
    public Editor getEditor() {
        return myEditor;
    }

    public void setEditor(Editor editor) {
        myEditor = editor;
    }

    @Override
    public void navigateInPreviewEditor(PreviewEditorState editorState) {
        final VirtualFile file = editorState.getFile();
        final LogicalPosition positionToNavigate = editorState.getNavigate();
        final TextAttributes lineAttributes = editorState.getAttributes();
        Document document = FileDocumentManager.getInstance().getDocument(file);

        clearEditor();
        myEditorState = editorState;
        remove(myLabel);
        if (document != null) {
            if (getEditor() == null || getEditor().getDocument() != document) {
                setEditor(createEditor(myProject, document, file));
                add(getEditor().getComponent(), BorderLayout.CENTER);
            }

            if (positionToNavigate != null) {
                getEditor().getCaretModel().moveToLogicalPosition(positionToNavigate);
                validate();
                getEditor().getScrollingModel().scrollToCaret(ScrollType.CENTER);
            }
            else {
                revalidate();
                repaint();
            }

            clearHighlighting();
            if (lineAttributes != null && positionToNavigate != null && positionToNavigate.line < getEditor().getDocument().getLineCount()) {
                myHighlighter = getEditor().getMarkupModel().addLineHighlighter(positionToNavigate.line, HighlighterLayer.SELECTION - 1,
                    lineAttributes);
            }
        }
        else {
            myLabel.setText("Navigate to selected " + (file.isDirectory() ? "directory " : "file ") + "in Project View");
            add(myLabel, BorderLayout.CENTER);
            validate();
        }
    }

    @Nonnull
    protected Editor createEditor(@Nullable Project project, Document document, VirtualFile file) {
        EditorEx editor = (EditorEx) EditorFactory.getInstance().createViewer(document, project);

        final EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        EditorHighlighter highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(file, scheme, project);

        editor.setFile(file);
        editor.setHighlighter(highlighter);

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
        if (myHighlighter != null) {
            getEditor().getMarkupModel().removeHighlighter(myHighlighter);
            myHighlighter = null;
        }
    }

    @Override
    public JPanel getPropertiesPanel() {
        return myDetailPanel;
    }

    @Override
    public void setPropertiesPanel(@Nullable final JPanel panel) {
        if (panel == null) {
            if (myDetailPanelWrapper != null) {
                myDetailPanelWrapper.removeAll();
            }
            myLabel.setText(myEmptyLabel);
            add(myLabel, BorderLayout.CENTER);
        }
        else if (panel != myDetailPanel) {
            remove(myLabel);
            if (myDetailPanelWrapper == null) {
                myDetailPanelWrapper = new JPanel(new GridLayout(1, 1));
                myDetailPanelWrapper.setBorder(IdeBorderFactory.createEmptyBorder(5, 5, 5, 5));
                myDetailPanelWrapper.add(panel);

                add(myDetailPanelWrapper, BorderLayout.NORTH);
            }
            else {
                myDetailPanelWrapper.removeAll();
                myDetailPanelWrapper.add(panel);
            }
        }
        myDetailPanel = panel;
        revalidate();
        repaint();
    }

    public void setEmptyLabel(String text) {
        myEmptyLabel = text;
    }

    @Override
    public <T> T getUserData(@Nonnull Key<T> key) {
        return myDataHolderBase.getUserData(key);
    }

    @Override
    public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
        myDataHolderBase.putUserData(key, value);
    }
}
