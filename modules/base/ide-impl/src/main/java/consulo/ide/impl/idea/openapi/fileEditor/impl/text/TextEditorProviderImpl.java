/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.fileEditor.impl.text;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.*;
import consulo.document.FileDocumentManager;
import consulo.fileEditor.*;
import consulo.fileEditor.highlight.BackgroundEditorHighlighter;
import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.fileEditor.structureView.StructureViewBuilderProvider;
import consulo.fileEditor.text.TextEditorProvider;
import consulo.fileEditor.text.TextEditorState;
import consulo.ide.impl.fileEditor.text.TextEditorComponentContainerFactory;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.language.impl.file.SingleRootFileViewProvider;
import consulo.logging.Logger;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.util.ShowNotifier;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import kava.beans.PropertyChangeListener;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public class TextEditorProviderImpl extends TextEditorProvider {
  private static final Logger LOG = Logger.getInstance(TextEditorProviderImpl.class);

  protected final TextEditorComponentContainerFactory myTextEditorComponentContainerFactory;

  @Inject
  public TextEditorProviderImpl(TextEditorComponentContainerFactory textEditorComponentContainerFactory) {
    myTextEditorComponentContainerFactory = textEditorComponentContainerFactory;
  }

  @Override
  public boolean accept(@Nonnull Project project, @Nonnull VirtualFile file) {
    return isTextFile(file) && !SingleRootFileViewProvider.isTooLargeForContentLoading(file);
  }

  @RequiredUIAccess
  @Override
  @Nonnull
  public FileEditor createEditor(@Nonnull Project project, @Nonnull VirtualFile file) {
    LOG.assertTrue(accept(project, file));
    return new TextEditorImpl(project, file, this);
  }

  @Override
  @Nonnull
  public TextEditor getTextEditor(@Nonnull Editor editor) {
    TextEditor textEditor = editor.getUserData(TEXT_EDITOR_KEY);
    if (textEditor == null) {
      textEditor = createWrapperForEditor(editor);
      putTextEditor(editor, textEditor);
    }

    return textEditor;
  }

  @Nonnull
  protected EditorWrapper createWrapperForEditor(@Nonnull Editor editor) {
    return new EditorWrapper(editor);
  }

  public void setStateImpl(Project project, Editor editor, TextEditorState state){
    if (state.CARETS != null && state.CARETS.length > 0) {
      if (editor.getCaretModel().supportsMultipleCarets()) {
        CaretModel caretModel = editor.getCaretModel();
        List<CaretState> states = new ArrayList<>(state.CARETS.length);
        for (TextEditorState.CaretState caretState : state.CARETS) {
          states.add(new CaretState(new LogicalPosition(caretState.LINE, caretState.COLUMN, caretState.LEAN_FORWARD),
                                    new LogicalPosition(caretState.SELECTION_START_LINE, caretState.SELECTION_START_COLUMN),
                                    new LogicalPosition(caretState.SELECTION_END_LINE, caretState.SELECTION_END_COLUMN)));
        }
        caretModel.setCaretsAndSelections(states, false);
      }
      else {
        TextEditorState.CaretState caretState = state.CARETS[0];
        LogicalPosition pos = new LogicalPosition(caretState.LINE, caretState.COLUMN);
        editor.getCaretModel().moveToLogicalPosition(pos);
        int startOffset = editor.logicalPositionToOffset(new LogicalPosition(caretState.SELECTION_START_LINE,
                                                                             caretState.SELECTION_START_COLUMN));
        int endOffset = editor.logicalPositionToOffset(new LogicalPosition(caretState.SELECTION_END_LINE,
                                                                           caretState.SELECTION_END_COLUMN));
        if (startOffset == endOffset) {
          editor.getSelectionModel().removeSelection();
        }
        else {
          editor.getSelectionModel().setSelection(startOffset, endOffset);
        }
      }
    }

    int relativeCaretPosition = state.RELATIVE_CARET_POSITION;
    Runnable scrollingRunnable = () -> {
      if (!editor.isDisposed()) {
        editor.getScrollingModel().disableAnimation();
        if (relativeCaretPosition != Integer.MAX_VALUE) {
          EditorUtil.setRelativeCaretPosition(editor, relativeCaretPosition);
        }
        editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        editor.getScrollingModel().enableAnimation();
      }
    };
    //noinspection TestOnlyProblems
    if (Boolean.TRUE.equals(editor.getUserData(TREAT_AS_SHOWN))) scrollingRunnable.run();
    else {
      ShowNotifier.once(editor.getContentUIComponent(), scrollingRunnable);
    }
  }

  public class EditorWrapper extends UserDataHolderBase implements TextEditor {
    private final Editor myEditor;

    public EditorWrapper(@Nonnull Editor editor) {
      myEditor = editor;
    }

    @Override
    @Nonnull
    public Editor getEditor() {
      return myEditor;
    }

    @Override
    @Nonnull
    public JComponent getComponent() {
      return myEditor.getComponent();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
      return myEditor.getContentComponent();
    }

    @Nullable
    @Override
    public Component getUIComponent() {
      return myEditor.getUIComponent();
    }

    @Nullable
    @Override
    public Component getPreferredFocusedUIComponent() {
      return myEditor.getUIComponent();
    }

    @Override
    @Nonnull
    public String getName() {
      return "Text";
    }

    @Override
    @RequiredReadAction
    public StructureViewBuilder getStructureViewBuilder() {
      VirtualFile file = FileDocumentManager.getInstance().getFile(myEditor.getDocument());
      if (file == null) return null;

      Project project = myEditor.getProject();
      LOG.assertTrue(project != null);
      for (StructureViewBuilderProvider provider : project.getApplication().getExtensionList(StructureViewBuilderProvider.class)) {
        StructureViewBuilder builder = provider.getStructureViewBuilder(file.getFileType(), file, project);
        if (builder != null) {
          return builder;
        }
      }
      return null;
    }

    @Nullable
    @Override
    public VirtualFile getFile() {
      return FileDocumentManager.getInstance().getFile(myEditor.getDocument());
    }

    @Override
    @Nonnull
    public FileEditorState getState(@Nonnull FileEditorStateLevel level) {
      return getStateImpl(null, myEditor, level);
    }

    @Override
    public void setState(@Nonnull FileEditorState state) {
      setStateImpl(null, myEditor, (TextEditorState)state);
    }

    @Override
    public boolean isModified() {
      return false;
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public void dispose() { }

    @Override
    public void selectNotify() { }

    @Override
    public void deselectNotify() { }

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener) { }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener) { }

    @Override
    public BackgroundEditorHighlighter getBackgroundHighlighter() {
      return null;
    }

    @Override
    public FileEditorLocation getCurrentLocation() {
      return null;
    }

    @Override
    public boolean canNavigateTo(@Nonnull Navigatable navigatable) {
      return false;
    }

    @Override
    public void navigateTo(@Nonnull Navigatable navigatable) {
    }
  }
}
