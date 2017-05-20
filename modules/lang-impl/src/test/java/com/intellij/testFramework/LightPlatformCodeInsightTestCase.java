/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.testFramework;

import com.intellij.codeInsight.generation.CommentByLineCommentHandler;
import com.intellij.ide.DataManager;
import com.intellij.injected.editor.DocumentWindow;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import consulo.testFramework.util.TestPathUtil;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.editor.impl.TrailingSpacesStripper;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileTypes.InternalStdFileTypes;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.encoding.EncodingProjectManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.impl.source.PostprocessReformattingAspect;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.util.FileComparisonFailure;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public abstract class LightPlatformCodeInsightTestCase extends LightPlatformTestCase {
  private static final Logger LOG = Logger.getInstance("#com.intellij.testFramework.LightCodeInsightTestCase");

  protected static Editor myEditor;
  protected static PsiFile myFile;
  protected static VirtualFile myVFile;

  private static final String CARET_MARKER = "<caret>";
  @NonNls private static final String SELECTION_START_MARKER = "<selection>";
  @NonNls private static final String SELECTION_END_MARKER = "</selection>";

  @Override
  protected void runTest() throws Throwable {
    final Throwable[] throwable = {null};
    Runnable action = new Runnable() {
      @Override
      public void run() {
        CommandProcessor.getInstance().executeCommand(getProject(), new Runnable() {
          @Override
          public void run() {

            try {
              doRunTest();
            }
            catch (Throwable t) {
              throwable[0] = t;
            }
          }
        }, "", null);
      }
    };
    if (isRunInWriteAction()) {
      ApplicationManager.getApplication().runWriteAction(action);
    }
    else {
      action.run();
    }

    if (throwable[0] != null) {
      throw throwable[0];
    }
  }

  protected void doRunTest() throws Throwable {
    LightPlatformCodeInsightTestCase.super.runTest();
  }

  protected boolean isRunInWriteAction() {
    return true;
  }

  /**
   * Configure test from data file. Data file is usual java, xml or whatever file that needs to be tested except it
   * has &lt;caret&gt; marker where caret should be placed when file is loaded in editor and &lt;selection&gt;&lt;/selection&gt;
   * denoting selection bounds.
   * @param filePath - relative path from %IDEA_INSTALLATION_HOME%/testData/
   * @throws Exception
   */
  protected void configureByFile(@TestDataFile @NonNls @NotNull String filePath) {
    try {
      String fullPath = getTestDataPath() + filePath;

      final File ioFile = new File(fullPath);
      String fileText = FileUtil.loadFile(ioFile, CharsetToolkit.UTF8);
      fileText = StringUtil.convertLineSeparators(fileText);

      configureFromFileText(ioFile.getName(), fileText);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @NonNls
  @NotNull
  protected String getTestDataPath() {
    return TestPathUtil.getTestDataPath();
  }

  protected VirtualFile getVirtualFile(@NonNls String filePath) {
    String fullPath = getTestDataPath() + filePath;

    final VirtualFile vFile = LocalFileSystem.getInstance().findFileByPath(fullPath.replace(File.separatorChar, '/'));
    assertNotNull("file " + fullPath + " not found", vFile);
    return vFile;
  }

  /**
   * Same as configureByFile but text is provided directly.
   * @param fileName - name of the file.
   * @param fileText - data file text.
   * @throws java.io.IOException
   */
  @NotNull
  protected static Document configureFromFileText(@NonNls @NotNull final String fileName, @NonNls @NotNull final String fileText) throws IOException {
    return ApplicationManager.getApplication().runWriteAction(new Computable<Document>() {
      @Override
      public Document compute() {
        if (myVFile != null) {
          // avoid messing with invalid files, in case someone calls configureXXX() several times
          PsiDocumentManager.getInstance(ourProject).commitAllDocuments();
          FileEditorManager.getInstance(ourProject).closeFile(myVFile);
          try {
            myVFile.delete(this);
          }
          catch (IOException e) {
            LOG.error(e);
          }
          myVFile = null;
        }
        final Document fakeDocument = new DocumentImpl(fileText);

        int caretIndex = fileText.indexOf(CARET_MARKER);
        int selStartIndex = fileText.indexOf(SELECTION_START_MARKER);
        int selEndIndex = fileText.indexOf(SELECTION_END_MARKER);

        final RangeMarker caretMarker = caretIndex >= 0 ? fakeDocument.createRangeMarker(caretIndex, caretIndex) : null;
        final RangeMarker selStartMarker = selStartIndex >= 0 ? fakeDocument.createRangeMarker(selStartIndex, selStartIndex) : null;
        final RangeMarker selEndMarker = selEndIndex >= 0 ? fakeDocument.createRangeMarker(selEndIndex, selEndIndex) : null;

        if (caretMarker != null) {
          fakeDocument.deleteString(caretMarker.getStartOffset(), caretMarker.getStartOffset() + CARET_MARKER.length());
        }
        if (selStartMarker != null) {
          fakeDocument.deleteString(selStartMarker.getStartOffset(),
                                    selStartMarker.getStartOffset() + SELECTION_START_MARKER.length());
        }
        if (selEndMarker != null) {
          fakeDocument.deleteString(selEndMarker.getStartOffset(),
                                    selEndMarker.getStartOffset() + SELECTION_END_MARKER.length());
        }

        String newFileText = fakeDocument.getText();
        Document document;
        try {
          document = setupFileEditorAndDocument(fileName, newFileText);
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
        setupCaret(caretMarker, newFileText);
        setupSelection(selStartMarker, selEndMarker);
        setupEditorForInjectedLanguage();
        return document;
      }
    });
  }

  private static void setupSelection(final RangeMarker selStartMarker, final RangeMarker selEndMarker) {
    if (selStartMarker != null) {
      myEditor.getSelectionModel().setSelection(selStartMarker.getStartOffset(), selEndMarker.getStartOffset());
    }
  }

  private static void setupCaret(final RangeMarker caretMarker, String fileText) {
    if (caretMarker != null) {
      int caretLine = StringUtil.offsetToLineNumber(fileText, caretMarker.getStartOffset());
      int caretCol = EditorUtil.calcColumnNumber(null, myEditor.getDocument().getText(),
                                                 myEditor.getDocument().getLineStartOffset(caretLine), caretMarker.getStartOffset(),
                                                 CodeStyleSettingsManager.getSettings(getProject()).getIndentOptions(InternalStdFileTypes.JAVA).TAB_SIZE);
      LogicalPosition pos = new LogicalPosition(caretLine, caretCol);
      myEditor.getCaretModel().moveToLogicalPosition(pos);
    }
  }

  protected static Editor createEditor(@NotNull VirtualFile file) {
    Editor editor = FileEditorManager.getInstance(getProject()).openTextEditor(new OpenFileDescriptor(getProject(), file, 0), false);
    ((EditorImpl)editor).setCaretActive();
    return editor;
  }

  @NotNull
  private static Document setupFileEditorAndDocument(@NotNull String fileName, @NotNull String fileText) throws IOException {
    EncodingProjectManager.getInstance(getProject()).setEncoding(null, CharsetToolkit.UTF8_CHARSET);
    EncodingProjectManager.getInstance(ProjectManager.getInstance().getDefaultProject()).setEncoding(null, CharsetToolkit.UTF8_CHARSET);
    PostprocessReformattingAspect.getInstance(ourProject).doPostponedFormatting();
    deleteVFile();
    myVFile = getSourceRoot().createChildData(null, fileName);
    VfsUtil.saveText(myVFile, fileText);
    final FileDocumentManager manager = FileDocumentManager.getInstance();
    final Document document = manager.getDocument(myVFile);
    assertNotNull("Can't create document for '" + fileName + "'", document);
    manager.reloadFromDisk(document);
    document.insertString(0, " ");
    document.deleteString(0, 1);
    myFile = getPsiManager().findFile(myVFile);
    assertNotNull("Can't create PsiFile for '" + fileName + "'. Unknown file type most probably.", myFile);
    assertTrue(myFile.isPhysical());
    myEditor = createEditor(myVFile);
    myVFile.setCharset(CharsetToolkit.UTF8_CHARSET);

    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    return document;
  }

  private static void setupEditorForInjectedLanguage() {
    Editor editor = InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(myEditor, myFile);
    if (editor instanceof EditorWindow) {
      myFile = ((EditorWindow)editor).getInjectedFile();
      myEditor = editor;
    }
  }

  private static void deleteVFile() {
    if (myVFile != null) {
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        @Override
        public void run() {
          try {
            myVFile.delete(this);
          } catch (IOException e) {
            LOG.error(e);
          }
        }
      });
    }
  }

  @Override
  protected void tearDown() throws Exception {
    FileEditorManager editorManager = FileEditorManager.getInstance(getProject());
    VirtualFile[] openFiles = editorManager.getOpenFiles();
    for (VirtualFile openFile : openFiles) {
      editorManager.closeFile(openFile);
    }
    deleteVFile();
    myEditor = null;
    myFile = null;
    myVFile = null;
    super.tearDown();
  }

  /**
   * Validates that content of the editor as well as caret and selection matches one specified in data file that
   * should be formed with the same format as one used in configureByFile
   * @param filePath - relative path from %IDEA_INSTALLATION_HOME%/testData/
   * @throws Exception
   */
  protected void checkResultByFile(@NonNls @NotNull String filePath) {
    checkResultByFile(null, filePath, false);
  }

  /**
   * Validates that content of the editor as well as caret and selection matches one specified in data file that
   * should be formed with the same format as one used in configureByFile
   * @param message - this check specific message. Added to text, caret position, selection checking. May be null
   * @param filePath - relative path from %IDEA_INSTALLATION_HOME%/testData/
   * @param ignoreTrailingSpaces - whether trailing spaces in editor in data file should be stripped prior to comparing.
   * @throws Exception
   */
  protected void checkResultByFile(@Nullable String message, @NotNull String filePath, final boolean ignoreTrailingSpaces) {
    bringRealEditorBack();

    getProject().getComponent(PostprocessReformattingAspect.class).doPostponedFormatting();
    if (ignoreTrailingSpaces) {
      final Editor editor = myEditor;
      TrailingSpacesStripper.strip(editor.getDocument(), false, true);
      EditorUtil.fillVirtualSpaceUntilCaret(editor);
    }

    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();

    String fullPath = getTestDataPath() + filePath;

    File ioFile = new File(fullPath);

    assertTrue(getMessage("Cannot find file " + fullPath, message), ioFile.exists());
    String fileText = null;
    try {
      fileText = FileUtil.loadFile(ioFile, CharsetToolkit.UTF8);
    } catch (IOException e) {
      LOG.error(e);
    }
    checkResultByText(message, StringUtil.convertLineSeparators(fileText), ignoreTrailingSpaces, getTestDataPath() + "/" + filePath);
  }

  /**
   * Same as checkResultByFile but text is provided directly.
   * @param fileText
   */
  protected void checkResultByText(@NonNls @NotNull String fileText) {
    checkResultByText(null, fileText, false, null);
  }

  /**
     * Same as checkResultByFile but text is provided directly.
     * @param message - this check specific message. Added to text, caret position, selection checking. May be null
     * @param fileText
     * @param ignoreTrailingSpaces - whether trailing spaces in editor in data file should be stripped prior to comparing.
     */
  protected void checkResultByText(final String message, @NotNull String fileText, final boolean ignoreTrailingSpaces) {
    checkResultByText(message, fileText, ignoreTrailingSpaces, null);  
  }
  
  /**
   * Same as checkResultByFile but text is provided directly.
   * @param message - this check specific message. Added to text, caret position, selection checking. May be null
   * @param fileText
   * @param ignoreTrailingSpaces - whether trailing spaces in editor in data file should be stripped prior to comparing.
   */
  protected void checkResultByText(final String message, @NotNull final String fileText, final boolean ignoreTrailingSpaces, final String filePath) {
    bringRealEditorBack();
    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        final Document document = EditorFactory.getInstance().createDocument(fileText);

        int caretIndex = fileText.indexOf(CARET_MARKER);
        int selStartIndex = fileText.indexOf(SELECTION_START_MARKER);
        int selEndIndex = fileText.indexOf(SELECTION_END_MARKER);

        final RangeMarker caretMarker = caretIndex >= 0 ? document.createRangeMarker(caretIndex, caretIndex) : null;
        final RangeMarker selStartMarker = selStartIndex >= 0
                                           ? document.createRangeMarker(selStartIndex, selStartIndex)
                                           : null;
        final RangeMarker selEndMarker = selEndIndex >= 0
                                         ? document.createRangeMarker(selEndIndex, selEndIndex)
                                         : null;

        if (ignoreTrailingSpaces) {
          ((DocumentImpl)document).stripTrailingSpaces(ourProject);
        }

        if (caretMarker != null) {
          document.deleteString(caretMarker.getStartOffset(), caretMarker.getStartOffset() + CARET_MARKER.length());
        }
        if (selStartMarker != null) {
          document.deleteString(selStartMarker.getStartOffset(),
                                selStartMarker.getStartOffset() + SELECTION_START_MARKER.length());
        }
        if (selEndMarker != null) {
          document.deleteString(selEndMarker.getStartOffset(),
                                selEndMarker.getStartOffset() + SELECTION_END_MARKER.length());
        }


        PostprocessReformattingAspect.getInstance(getProject()).doPostponedFormatting();
        String newFileText = document.getText();

        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        String fileText = myFile.getText();
        String failMessage = getMessage("Text mismatch", message);
        if (filePath != null && !newFileText.equals(fileText)) {
          throw new FileComparisonFailure(failMessage, newFileText, fileText, filePath);
        }
        assertEquals(failMessage, newFileText, fileText);

        checkCaretPosition(caretMarker, newFileText, message);
        checkSelection(selStartMarker, selEndMarker, newFileText, message);
      }
    });
  }

  private static String getMessage(@NonNls String engineMessage, String userMessage) {
    if (userMessage == null) return engineMessage;
    return userMessage + " [" + engineMessage + "]";
  }

  private static void checkSelection(final RangeMarker selStartMarker, final RangeMarker selEndMarker, String newFileText, String message) {
    if (selStartMarker != null && selEndMarker != null) {
      int selStartLine = StringUtil.offsetToLineNumber(newFileText, selStartMarker.getStartOffset());
      int selStartCol = selStartMarker.getStartOffset() - StringUtil.lineColToOffset(newFileText, selStartLine, 0);

      int selEndLine = StringUtil.offsetToLineNumber(newFileText, selEndMarker.getEndOffset());
      int selEndCol = selEndMarker.getEndOffset() - StringUtil.lineColToOffset(newFileText, selEndLine, 0);

      assertEquals(
          getMessage("selectionStartLine", message),
          selStartLine + 1,
          StringUtil.offsetToLineNumber(newFileText, myEditor.getSelectionModel().getSelectionStart()) + 1);

      assertEquals(
          getMessage("selectionStartCol", message),
          selStartCol + 1,
          myEditor.getSelectionModel().getSelectionStart() -
          StringUtil.lineColToOffset(newFileText, selStartLine, 0) +
                                                                   1);

      assertEquals(
          getMessage("selectionEndLine", message),
          selEndLine + 1,
          StringUtil.offsetToLineNumber(newFileText, myEditor.getSelectionModel().getSelectionEnd()) + 1);

      assertEquals(
          getMessage("selectionEndCol", message),
          selEndCol + 1,
          myEditor.getSelectionModel().getSelectionEnd() - StringUtil.lineColToOffset(newFileText, selEndLine, 0) +
          1);
    }
    else {
      assertTrue(getMessage("must not have selection", message), !myEditor.getSelectionModel().hasSelection());
    }
  }

  private static void checkCaretPosition(final RangeMarker caretMarker, String newFileText, String message) {
    if (caretMarker != null) {
      int caretLine = StringUtil.offsetToLineNumber(newFileText, caretMarker.getStartOffset());
      //int caretCol = caretMarker.getStartOffset() - StringUtil.lineColToOffset(newFileText, caretLine, 0);
      int caretCol = EditorUtil.calcColumnNumber(null, newFileText,
                                                 StringUtil.lineColToOffset(newFileText, caretLine, 0),
                                                 caretMarker.getStartOffset(),
                                                 CodeStyleSettingsManager.getSettings(getProject()).getIndentOptions(InternalStdFileTypes.JAVA).TAB_SIZE);

      assertEquals(getMessage("caretLine", message), caretLine, myEditor.getCaretModel().getLogicalPosition().line);
      assertEquals(getMessage("caretColumn", message), caretCol, myEditor.getCaretModel().getLogicalPosition().column);
    }
  }

  @Override
  public Object getData(String dataId) {
    if (PlatformDataKeys.EDITOR.is(dataId)) {
      return myEditor;
    }
    if (dataId.equals(AnActionEvent.injectedId(PlatformDataKeys.EDITOR.getName()))) {
      return InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(getEditor(), getFile());
    }
    if (LangDataKeys.PSI_FILE.is(dataId)) {
      return myFile;
    }
    if (dataId.equals(AnActionEvent.injectedId(LangDataKeys.PSI_FILE.getName()))) {
      Editor editor = InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(getEditor(), getFile());
      return editor instanceof EditorWindow ? ((EditorWindow)editor).getInjectedFile() : getFile();
    }
    return super.getData(dataId);
  }

  /**
   * @return Editor used in test.
   */
  protected static Editor getEditor() {
    return myEditor;
  }

  /**
   * @return PsiFile opened in editor used in test
   */
  protected static PsiFile getFile() {
    return myFile;
  }

  protected static VirtualFile getVFile() {
    return myVFile;
  }

  protected static void bringRealEditorBack() {
    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    if (myEditor instanceof EditorWindow) {
      Document document = ((DocumentWindow)myEditor.getDocument()).getDelegate();
      myFile = PsiDocumentManager.getInstance(getProject()).getPsiFile(document);
      myEditor = ((EditorWindow)myEditor).getDelegate();
      myVFile = myFile.getVirtualFile();
    }
  }

  protected void caretUp() {
    EditorActionManager actionManager = EditorActionManager.getInstance();
    EditorActionHandler action = actionManager.getActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_UP);
    action.execute(getEditor(), DataManager.getInstance().getDataContext());
  }
  protected void deleteLine() {
    EditorActionManager actionManager = EditorActionManager.getInstance();
    EditorActionHandler action = actionManager.getActionHandler(IdeActions.ACTION_EDITOR_DELETE_LINE);
    action.execute(getEditor(), DataManager.getInstance().getDataContext());
  }
  protected static void type(char c) {
    EditorActionManager actionManager = EditorActionManager.getInstance();
    final DataContext dataContext = DataManager.getInstance().getDataContext();
    if (c == '\n') {
      actionManager.getActionHandler(IdeActions.ACTION_EDITOR_ENTER).execute(getEditor(), dataContext);
    }
    else if (c == '\b') {
      actionManager.getActionHandler(IdeActions.ACTION_EDITOR_BACKSPACE).execute(getEditor(), dataContext);
    }
    else {
      actionManager.getTypedAction().actionPerformed(getEditor(), c, dataContext);
    }
  }

  protected static void type(@NonNls String s) {
    for (char c : s.toCharArray()) {
      type(c);
    }
  }
  protected static void backspace() {
    executeAction(IdeActions.ACTION_EDITOR_BACKSPACE);
  }
  protected static void delete() {
    executeAction(IdeActions.ACTION_EDITOR_DELETE);
  }

  protected static void home() {
    executeAction(IdeActions.ACTION_EDITOR_MOVE_LINE_START);
  }

  protected static void end() {
    executeAction(IdeActions.ACTION_EDITOR_MOVE_LINE_END);
  }

  protected static void copy() {
    executeAction(IdeActions.ACTION_EDITOR_COPY);
  }

  protected static void paste() {
    executeAction(IdeActions.ACTION_EDITOR_PASTE);
  }
  
  protected static void moveCaretToPreviousWordWithSelection() {
    executeAction("EditorPreviousWordWithSelection");
  }

  protected static void moveCaretToNextWordWithSelection() {
    executeAction("EditorNextWordWithSelection");
  }

  protected static void cutLineBackward() {
    executeAction("EditorCutLineBackward");
  }
  
  protected static void cutToLineEnd() {
    executeAction("EditorCutLineEnd");
  }

  protected static void killToWordStart() {
    executeAction("EditorKillToWordStart");
  }
  
  protected static void killToWordEnd() {
    executeAction("EditorKillToWordEnd");
  }

  protected static void killRegion() {
    executeAction("EditorKillRegion");
  }

  protected static void killRingSave() {
    executeAction("EditorKillRingSave");
  }

  protected static void unindent() {
    executeAction("EditorUnindentSelection");
  }
  
  protected static void lineComment() {
    new CommentByLineCommentHandler().invoke(getProject(), getEditor(), getEditor().getCaretModel().getPrimaryCaret(), getFile());
  }

  protected static void executeAction(@NonNls @NotNull final String actionId) {
    CommandProcessor.getInstance().executeCommand(getProject(), new Runnable() {
      @Override
      public void run() {
        EditorActionManager actionManager = EditorActionManager.getInstance();
        EditorActionHandler actionHandler = actionManager.getActionHandler(actionId);
        actionHandler.execute(getEditor(), null, DataManager.getInstance().getDataContext());
      }
    }, "", null);
  }

  protected static DataContext getCurrentEditorDataContext() {
    final DataContext defaultContext = DataManager.getInstance().getDataContext();
    return new DataContext() {
      @Override
      @Nullable
      public Object getData(@NonNls String dataId) {
        if (PlatformDataKeys.EDITOR.is(dataId)) {
          return getEditor();
        }
        if (CommonDataKeys.PROJECT.is(dataId)) {
          return getProject();
        }
        if (LangDataKeys.PSI_FILE.is(dataId)) {
          return getFile();
        }
        if (LangDataKeys.PSI_ELEMENT.is(dataId)) {
          PsiFile file = getFile();
          if (file == null) return null;
          Editor editor = getEditor();
          if (editor == null) return null;
          return file.findElementAt(editor.getCaretModel().getOffset());
        }
        return defaultContext.getData(dataId);
      }
    };
  }
}
