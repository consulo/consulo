/*
 * Copyright 2013-2018 consulo.io
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
package consulo.fileEditor.impl.text;

import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProviderImpl;
import com.intellij.openapi.fileEditor.impl.text.TextEditorState;
import com.intellij.openapi.fileTypes.BinaryFileTypeDecompilers;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import consulo.util.dataholder.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.SingleRootFileViewProvider;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author VISTALL
 * @since 2018-05-10
 * <p>
 * Extracted part from {@link TextEditorProviderImpl}
 */
public abstract class TextEditorProvider implements FileEditorProvider, DumbAware {
  @Nonnull
  public static TextEditorProvider getInstance() {
    return EP_FILE_EDITOR_PROVIDER.findExtension(TextEditorProvider.class);
  }

  @TestOnly
  public static final Key<Boolean> TREAT_AS_SHOWN = Key.create("treat.editor.component.as.shown");

  protected static final Key<TextEditor> TEXT_EDITOR_KEY = Key.create("textEditor");

  @NonNls
  private static final String TYPE_ID = "text-editor";
  @NonNls
  private static final String LINE_ATTR = "line";
  @NonNls
  private static final String COLUMN_ATTR = "column";
  @NonNls
  private static final String LEAN_FORWARD_ATTR = "lean-forward";
  @NonNls
  private static final String SELECTION_START_LINE_ATTR = "selection-start-line";
  @NonNls
  private static final String SELECTION_START_COLUMN_ATTR = "selection-start-column";
  @NonNls
  private static final String SELECTION_END_LINE_ATTR = "selection-end-line";
  @NonNls
  private static final String SELECTION_END_COLUMN_ATTR = "selection-end-column";
  @NonNls
  private static final String RELATIVE_CARET_POSITION_ATTR = "relative-caret-position";
  @NonNls
  private static final String CARET_ELEMENT = "caret";

  @Nonnull
  public abstract TextEditor getTextEditor(@Nonnull Editor editor);


  @Override
  public boolean accept(@Nonnull Project project, @Nonnull VirtualFile file) {
    return isTextFile(file) && !SingleRootFileViewProvider.isTooLargeForContentLoading(file);
  }

  @Override
  @Nonnull
  public FileEditorState readState(@Nonnull Element element, @Nonnull Project project, @Nonnull VirtualFile file) {
    TextEditorState state = new TextEditorState();

    try {
      List<Element> caretElements = element.getChildren(CARET_ELEMENT);
      if (caretElements.isEmpty()) {
        state.CARETS = new TextEditorState.CaretState[]{readCaretInfo(element)};
      }
      else {
        state.CARETS = new TextEditorState.CaretState[caretElements.size()];
        for (int i = 0; i < caretElements.size(); i++) {
          state.CARETS[i] = readCaretInfo(caretElements.get(i));
        }
      }

      String verticalScrollProportion = element.getAttributeValue(RELATIVE_CARET_POSITION_ATTR);
      state.RELATIVE_CARET_POSITION = verticalScrollProportion == null ? 0 : Integer.parseInt(verticalScrollProportion);
    }
    catch (NumberFormatException ignored) {
    }

    return state;
  }

  private static TextEditorState.CaretState readCaretInfo(Element element) {
    TextEditorState.CaretState caretState = new TextEditorState.CaretState();
    caretState.LINE = parseWithDefault(element, LINE_ATTR);
    caretState.COLUMN = parseWithDefault(element, COLUMN_ATTR);
    caretState.LEAN_FORWARD = parseBooleanWithDefault(element, LEAN_FORWARD_ATTR);
    caretState.SELECTION_START_LINE = parseWithDefault(element, SELECTION_START_LINE_ATTR);
    caretState.SELECTION_START_COLUMN = parseWithDefault(element, SELECTION_START_COLUMN_ATTR);
    caretState.SELECTION_END_LINE = parseWithDefault(element, SELECTION_END_LINE_ATTR);
    caretState.SELECTION_END_COLUMN = parseWithDefault(element, SELECTION_END_COLUMN_ATTR);
    return caretState;
  }

  private static int parseWithDefault(Element element, String attributeName) {
    String value = element.getAttributeValue(attributeName);
    return value == null ? 0 : Integer.parseInt(value);
  }

  private static boolean parseBooleanWithDefault(Element element, String attributeName) {
    String value = element.getAttributeValue(attributeName);
    return value != null && Boolean.parseBoolean(value);
  }

  @Override
  public void writeState(@Nonnull FileEditorState _state, @Nonnull Project project, @Nonnull Element element) {
    TextEditorState state = (TextEditorState)_state;

    element.setAttribute(RELATIVE_CARET_POSITION_ATTR, Integer.toString(state.RELATIVE_CARET_POSITION));
    if (state.CARETS != null) {
      for (TextEditorState.CaretState caretState : state.CARETS) {
        Element e = new Element(CARET_ELEMENT);
        e.setAttribute(LINE_ATTR, Integer.toString(caretState.LINE));
        e.setAttribute(COLUMN_ATTR, Integer.toString(caretState.COLUMN));
        e.setAttribute(LEAN_FORWARD_ATTR, Boolean.toString(caretState.LEAN_FORWARD));
        e.setAttribute(SELECTION_START_LINE_ATTR, Integer.toString(caretState.SELECTION_START_LINE));
        e.setAttribute(SELECTION_START_COLUMN_ATTR, Integer.toString(caretState.SELECTION_START_COLUMN));
        e.setAttribute(SELECTION_END_LINE_ATTR, Integer.toString(caretState.SELECTION_END_LINE));
        e.setAttribute(SELECTION_END_COLUMN_ATTR, Integer.toString(caretState.SELECTION_END_COLUMN));
        element.addContent(e);
      }
    }
  }

  @Override
  @Nonnull
  public String getEditorTypeId() {
    return TYPE_ID;
  }

  @Override
  @Nonnull
  public FileEditorPolicy getPolicy() {
    return FileEditorPolicy.NONE;
  }

  @Nullable
  public static Document[] getDocuments(@Nonnull FileEditor editor) {
    if (editor instanceof DocumentsEditor) {
      DocumentsEditor documentsEditor = (DocumentsEditor)editor;
      Document[] documents = documentsEditor.getDocuments();
      return documents.length > 0 ? documents : null;
    }

    if (editor instanceof TextEditor) {
      Document document = ((TextEditor)editor).getEditor().getDocument();
      return new Document[]{document};
    }

    Project[] projects = ProjectManager.getInstance().getOpenProjects();
    for (int i = projects.length - 1; i >= 0; i--) {
      VirtualFile file = FileEditorManagerEx.getInstanceEx(projects[i]).getFile(editor);
      if (file != null) {
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document != null) {
          return new Document[]{document};
        }
      }
    }

    return null;
  }

  public static void putTextEditor(Editor editor, TextEditor textEditor) {
    editor.putUserData(TEXT_EDITOR_KEY, textEditor);
  }

  @Nonnull
  public TextEditorState getStateImpl(final Project project, @Nonnull Editor editor, @Nonnull FileEditorStateLevel level) {
    TextEditorState state = new TextEditorState();
    CaretModel caretModel = editor.getCaretModel();
    if (caretModel.supportsMultipleCarets()) {
      List<CaretState> caretsAndSelections = caretModel.getCaretsAndSelections();
      state.CARETS = new TextEditorState.CaretState[caretsAndSelections.size()];
      for (int i = 0; i < caretsAndSelections.size(); i++) {
        CaretState caretState = caretsAndSelections.get(i);
        LogicalPosition caretPosition = caretState.getCaretPosition();
        LogicalPosition selectionStartPosition = caretState.getSelectionStart();
        LogicalPosition selectionEndPosition = caretState.getSelectionEnd();
        state.CARETS[i] = createCaretState(caretPosition, selectionStartPosition, selectionEndPosition);
      }
    }
    else {
      LogicalPosition caretPosition = caretModel.getLogicalPosition();
      LogicalPosition selectionStartPosition = editor.offsetToLogicalPosition(editor.getSelectionModel().getSelectionStart());
      LogicalPosition selectionEndPosition = editor.offsetToLogicalPosition(editor.getSelectionModel().getSelectionEnd());
      state.CARETS = new TextEditorState.CaretState[1];
      state.CARETS[0] = createCaretState(caretPosition, selectionStartPosition, selectionEndPosition);
    }

    // Saving scrolling proportion on UNDO may cause undesirable results of undo action fails to perform since
    // scrolling proportion restored slightly differs from what have been saved.
    state.RELATIVE_CARET_POSITION = level == FileEditorStateLevel.UNDO ? Integer.MAX_VALUE : EditorUtil.calcRelativeCaretPosition(editor);

    return state;
  }

  public static boolean isTextFile(@Nonnull VirtualFile file) {
    if (file.isDirectory() || !file.isValid()) {
      return false;
    }

    final FileType ft = file.getFileType();
    return !ft.isBinary() || BinaryFileTypeDecompilers.INSTANCE.forFileType(ft) != null;
  }

  private static TextEditorState.CaretState createCaretState(LogicalPosition caretPosition, LogicalPosition selectionStartPosition, LogicalPosition selectionEndPosition) {
    TextEditorState.CaretState caretState = new TextEditorState.CaretState();
    caretState.LINE = getLine(caretPosition);
    caretState.COLUMN = getColumn(caretPosition);
    caretState.LEAN_FORWARD = caretPosition != null && caretPosition.leansForward;
    caretState.SELECTION_START_LINE = getLine(selectionStartPosition);
    caretState.SELECTION_START_COLUMN = getColumn(selectionStartPosition);
    caretState.SELECTION_END_LINE = getLine(selectionEndPosition);
    caretState.SELECTION_END_COLUMN = getColumn(selectionEndPosition);
    return caretState;
  }

  private static int getLine(@Nullable LogicalPosition pos) {
    return pos == null ? 0 : pos.line;
  }

  private static int getColumn(@Nullable LogicalPosition pos) {
    return pos == null ? 0 : pos.column;
  }
}
