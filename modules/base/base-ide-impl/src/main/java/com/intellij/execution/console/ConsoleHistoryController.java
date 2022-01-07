/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.execution.console;

import com.intellij.AppTopics;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.ide.scratch.ScratchFileService;
import com.intellij.lang.Language;
import consulo.disposer.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.undo.UndoConstants;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.actions.ContentChooser;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerAdapter;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectEx;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringHash;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FactoryMap;
import consulo.container.boot.ContainerPathManager;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.util.dataholder.Key;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author gregsh
 */
public class ConsoleHistoryController {

  private static final Key<ConsoleHistoryController> CONTROLLER_KEY = Key.create("CONTROLLER_KEY");

  private static final Logger LOG = Logger.getInstance(ConsoleHistoryController.class);

  private final static Map<String, ConsoleHistoryModel> ourModels = FactoryMap.createMap(k -> new ConsoleHistoryModel(null), ContainerUtil::createConcurrentWeakValueMap);

  private final LanguageConsoleView myConsole;
  private final AnAction myHistoryNext = new MyAction(true, getKeystrokesUpDown(true));
  private final AnAction myHistoryPrev = new MyAction(false, getKeystrokesUpDown(false));
  private final AnAction myBrowseHistory = new MyBrowseAction();
  private boolean myMultiline;
  private final ModelHelper myHelper;
  private long myLastSaveStamp;

  @Deprecated
  public ConsoleHistoryController(@Nonnull String type, @Nullable String persistenceId, @Nonnull LanguageConsoleView console) {
    this(new ConsoleRootType(type, null) {
    }, persistenceId, console);
  }

  public ConsoleHistoryController(@Nonnull ConsoleRootType rootType, @Nullable String persistenceId, @Nonnull LanguageConsoleView console) {
    this(rootType, persistenceId, console, ourModels.get(getHistoryName(rootType, fixNullPersistenceId(persistenceId, console))));
  }

  private ConsoleHistoryController(@Nonnull ConsoleRootType rootType, @Nullable String persistenceId, @Nonnull LanguageConsoleView console, @Nonnull ConsoleHistoryModel model) {
    myHelper = new ModelHelper(rootType, fixNullPersistenceId(persistenceId, console), model.copy());
    myConsole = console;
  }

  public static ConsoleHistoryController getController(LanguageConsoleView console) {
    return console.getVirtualFile().getUserData(CONTROLLER_KEY);
  }

  public static void addToHistory(@Nonnull LanguageConsoleView consoleView, @Nullable String command) {
    ConsoleHistoryController controller = getController(consoleView);
    if (controller != null) {
      controller.addToHistory(command);
    }
  }

  public void addToHistory(@Nullable String command) {
    getModel().addToHistory(command);
  }

  public boolean hasHistory() {
    return !getModel().isEmpty();
  }

  @Nonnull
  private static String fixNullPersistenceId(@Nullable String persistenceId, @Nonnull LanguageConsoleView console) {
    if (StringUtil.isNotEmpty(persistenceId)) return persistenceId;
    String url = console.getProject().getPresentableUrl();
    return StringUtil.isNotEmpty(url) ? url : "default";
  }

  public boolean isMultiline() {
    return myMultiline;
  }

  public ConsoleHistoryController setMultiline(boolean multiline) {
    myMultiline = multiline;
    return this;
  }

  ConsoleHistoryModel getModel() {
    return myHelper.getModel();
  }

  public void install() {
    class Listener extends FileDocumentManagerAdapter implements ProjectEx.ProjectSaved {
      @Override
      public void beforeDocumentSaving(@Nonnull Document document) {
        if (document == myConsole.getEditorDocument()) {
          saveHistory();
        }
      }

      @Override
      public void saved(@Nonnull Project project) {
        saveHistory();
      }
    }
    Listener listener = new Listener();
    ApplicationManager.getApplication().getMessageBus().connect(myConsole).subscribe(ProjectEx.ProjectSaved.TOPIC, listener);
    myConsole.getProject().getMessageBus().connect(myConsole).subscribe(AppTopics.FILE_DOCUMENT_SYNC, listener);

    myConsole.getVirtualFile().putUserData(CONTROLLER_KEY, this);
    Disposer.register(myConsole, new Disposable() {
      @Override
      public void dispose() {
        myConsole.getVirtualFile().putUserData(CONTROLLER_KEY, null);
        saveHistory();
      }
    });
    if (myHelper.getModel().getHistorySize() == 0) {
      loadHistory(myHelper.getId());
    }
    configureActions();
    myLastSaveStamp = getCurrentTimeStamp();
  }

  private long getCurrentTimeStamp() {
    return getModel().getModificationCount() + myConsole.getEditorDocument().getModificationStamp();
  }

  private void configureActions() {
    EmptyAction.setupAction(myHistoryNext, "Console.History.Next", null);
    EmptyAction.setupAction(myHistoryPrev, "Console.History.Previous", null);
    EmptyAction.setupAction(myBrowseHistory, "Console.History.Browse", null);
    if (!myMultiline) {
      addShortcuts(myHistoryNext, getShortcutUpDown(true));
      addShortcuts(myHistoryPrev, getShortcutUpDown(false));
    }
    myHistoryNext.registerCustomShortcutSet(myHistoryNext.getShortcutSet(), myConsole.getCurrentEditor().getComponent());
    myHistoryPrev.registerCustomShortcutSet(myHistoryPrev.getShortcutSet(), myConsole.getCurrentEditor().getComponent());
    myBrowseHistory.registerCustomShortcutSet(myBrowseHistory.getShortcutSet(), myConsole.getCurrentEditor().getComponent());
  }

  /**
   * Use this method if you decided to change the id for your console but don't want your users to loose their current histories
   *
   * @param id previous id id
   * @return true if some text has been loaded; otherwise false
   */
  public boolean loadHistory(String id) {
    String prev = myHelper.getContent();
    boolean result = myHelper.loadHistory(id, myConsole.getVirtualFile());
    String userValue = myHelper.getContent();
    if (prev != userValue && userValue != null) {
      setConsoleText(userValue, false, false);
    }
    return result;
  }

  private void saveHistory() {
    if (myLastSaveStamp == getCurrentTimeStamp()) return;
    myHelper.setContent(myConsole.getEditorDocument().getText());
    myHelper.saveHistory();
    myLastSaveStamp = getCurrentTimeStamp();
  }

  public AnAction getHistoryNext() {
    return myHistoryNext;
  }

  public AnAction getHistoryPrev() {
    return myHistoryPrev;
  }

  public AnAction getBrowseHistory() {
    return myBrowseHistory;
  }

  protected void setConsoleText(final String command, final boolean storeUserText, final boolean regularMode) {
    if (regularMode && myMultiline && StringUtil.isEmptyOrSpaces(command)) return;
    final Editor editor = myConsole.getCurrentEditor();
    final Document document = editor.getDocument();
    new WriteCommandAction.Simple(myConsole.getProject()) {
      @Override
      public void run() {
        if (storeUserText) {
          String text = document.getText();
          if (Comparing.equal(command, text) && myHelper.getContent() != null) return;
          myHelper.setContent(text);
        }
        String text = StringUtil.notNullize(command);
        int offset;
        if (regularMode) {
          if (myMultiline) {
            offset = insertTextMultiline(text, editor, document);
          }
          else {
            document.setText(text);
            offset = document.getTextLength();
          }
        }
        else {
          offset = 0;
          try {
            document.putUserData(UndoConstants.DONT_RECORD_UNDO, Boolean.TRUE);
            document.setText(text);
          }
          finally {
            document.putUserData(UndoConstants.DONT_RECORD_UNDO, null);
          }
        }
        editor.getCaretModel().moveToOffset(offset);
        editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
      }
    }.execute();
  }

  protected int insertTextMultiline(String text, Editor editor, Document document) {
    TextRange selection = EditorUtil.getSelectionInAnyMode(editor);

    int start = document.getLineStartOffset(document.getLineNumber(selection.getStartOffset()));
    int end = document.getLineEndOffset(document.getLineNumber(selection.getEndOffset()));

    document.replaceString(start, end, text);
    editor.getSelectionModel().setSelection(start, start + text.length());
    return start;
  }

  private class MyAction extends DumbAwareAction {
    private final boolean myNext;

    @Nonnull
    private final Collection<KeyStroke> myUpDownKeystrokes;

    public MyAction(final boolean next, @Nonnull Collection<KeyStroke> upDownKeystrokes) {
      myNext = next;
      myUpDownKeystrokes = upDownKeystrokes;
      getTemplatePresentation().setVisible(false);
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
      String command;
      if (myNext) {
        command = getModel().getHistoryNext();
        if (!myMultiline && command == null) return;
      }
      else {
        command = ObjectUtils.chooseNotNull(getModel().getHistoryPrev(), myMultiline ? "" : StringUtil.notNullize(myHelper.getContent()));
      }
      setConsoleText(command, myNext && !getModel().hasHistory(false), true);
    }

    @Override
    public void update(final AnActionEvent e) {
      super.update(e);
      boolean enabled = myMultiline || !isUpDownKey(e) || canMoveInEditor(myNext);
      //enabled &= getModel().hasHistory(myNext);
      e.getPresentation().setEnabled(enabled);
    }

    private boolean isUpDownKey(AnActionEvent e) {
      final InputEvent event = e.getInputEvent();
      if (!(event instanceof KeyEvent)) {
        return false;
      }
      final KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent((KeyEvent)event);
      return myUpDownKeystrokes.contains(keyStroke);
    }
  }

  private boolean canMoveInEditor(final boolean next) {
    final Editor consoleEditor = myConsole.getCurrentEditor();
    final Document document = consoleEditor.getDocument();
    final CaretModel caretModel = consoleEditor.getCaretModel();

    if (LookupManager.getActiveLookup(consoleEditor) != null) return false;

    if (next) {
      return document.getLineNumber(caretModel.getOffset()) == 0;
    }
    else {
      final int lineCount = document.getLineCount();
      return (lineCount == 0 || document.getLineNumber(caretModel.getOffset()) == lineCount - 1) && StringUtil.isEmptyOrSpaces(document.getText().substring(caretModel.getOffset()));
    }
  }


  private class MyBrowseAction extends DumbAwareAction {

    @Override
    public void update(AnActionEvent e) {
      boolean enabled = hasHistory();
      e.getPresentation().setEnabled(enabled);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      String s1 = KeymapUtil.getFirstKeyboardShortcutText(myHistoryNext);
      String s2 = KeymapUtil.getFirstKeyboardShortcutText(myHistoryPrev);
      String title = myConsole.getTitle() + " History" + (StringUtil.isNotEmpty(s1) && StringUtil.isNotEmpty(s2) ? " (" + s1 + " and " + s2 + " while in editor)" : "");
      final ContentChooser<String> chooser = new ContentChooser<String>(myConsole.getProject(), title, true, true) {

        @Override
        protected void removeContentAt(String content) {
          getModel().removeFromHistory(content);
        }

        @Override
        protected String getStringRepresentationFor(String content) {
          return content;
        }

        @Override
        protected List<String> getContents() {
          List<String> entries = getModel().getEntries();
          Collections.reverse(entries);
          return entries;
        }

        @Override
        protected Editor createIdeaEditor(String text) {
          PsiFile consoleFile = myConsole.getFile();
          Language language = consoleFile.getLanguage();
          Project project = consoleFile.getProject();

          PsiFile psiFile = PsiFileFactory.getInstance(project)
                  .createFileFromText("a." + consoleFile.getFileType().getDefaultExtension(), language, StringUtil.convertLineSeparators(new String(text)), false, true);
          VirtualFile virtualFile = psiFile.getViewProvider().getVirtualFile();
          if (virtualFile instanceof LightVirtualFile) ((LightVirtualFile)virtualFile).setWritable(false);
          Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
          EditorFactory editorFactory = EditorFactory.getInstance();
          EditorEx editor = (EditorEx)editorFactory.createViewer(document, project);
          editor.getSettings().setFoldingOutlineShown(false);
          editor.getSettings().setLineMarkerAreaShown(false);
          editor.getSettings().setIndentGuidesShown(false);

          SyntaxHighlighter highlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(language, project, psiFile.getViewProvider().getVirtualFile());
          editor.setHighlighter(new LexerEditorHighlighter(highlighter, editor.getColorsScheme()));
          return editor;
        }
      };
      chooser.setContentIcon(null);
      chooser.setSplitterOrientation(false);
      chooser.setSelectedIndex(Math.max(0, getModel().getHistorySize() - getModel().getCurrentIndex() - 1));
      if (chooser.showAndGet() && myConsole.getCurrentEditor().getComponent().isShowing()) {
        setConsoleText(chooser.getSelectedText(), false, true);
      }
    }
  }

  public static class ModelHelper {
    private final ConsoleRootType myRootType;
    private final String myId;
    private final ConsoleHistoryModel myModel;
    private String myContent;

    public ModelHelper(ConsoleRootType rootType, String id, ConsoleHistoryModel model) {
      myRootType = rootType;
      myId = id;
      myModel = model;
    }

    public ConsoleHistoryModel getModel() {
      return myModel;
    }

    public void setContent(String userValue) {
      myContent = userValue;
    }

    public String getId() {
      return myId;
    }

    public String getContent() {
      return myContent;
    }

    @Nonnull
    private String getOldHistoryFilePath(final String id) {
      String pathName = myRootType.getConsoleTypeId() + Long.toHexString(StringHash.calc(id));
      return ContainerPathManager.get().getSystemPath() + File.separator + "userHistory" + File.separator + pathName + ".hist.xml";
    }

    public boolean loadHistory(String id, VirtualFile consoleFile) {
      try {
        VirtualFile file = myRootType.isHidden() ? null : HistoryRootType.getInstance().findFile(null, getHistoryName(myRootType, id), ScratchFileService.Option.existing_only);
        if (file == null) {
          if (loadHistoryOld(id)) {
            if (!myRootType.isHidden()) {
              // migrate content
              AccessToken token = ApplicationManager.getApplication().acquireWriteActionLock(getClass());
              try {
                VfsUtil.saveText(consoleFile, myContent);
              }
              finally {
                token.finish();
              }
            }
            return true;
          }
          return false;
        }
        String[] split = VfsUtilCore.loadText(file).split(myRootType.getEntrySeparator());
        getModel().resetEntries(Arrays.asList(split));
        return true;
      }
      catch (Exception ignored) {
        return false;
      }
    }

    public boolean loadHistoryOld(String id) {
      File file = new File(PathUtil.toSystemDependentName(getOldHistoryFilePath(id)));
      if (!file.exists()) return false;
      try {
        Element rootElement = JDOMUtil.load(file);
        String text = loadHistory(rootElement, id);
        if (text != null) {
          myContent = text;
          return true;
        }
      }
      catch (Exception ex) {
        //noinspection ThrowableResultOfMethodCallIgnored
        Throwable cause = ExceptionUtil.getRootCause(ex);
        if (cause instanceof EOFException) {
          LOG.warn("Failed to load " + myRootType.getId() + " history from: " + file.getPath(), ex);
          return false;
        }
        else {
          LOG.error(ex);
        }
      }
      return false;
    }

    private void saveHistoryOld() {
      File file = new File(PathUtil.toSystemDependentName(getOldHistoryFilePath(myId)));
      final File dir = file.getParentFile();
      if (!dir.exists() && !dir.mkdirs() || !dir.isDirectory()) {
        LOG.error("failed to create folder: " + dir.getAbsolutePath());
        return;
      }

      try {
        JDOMUtil.writeDocument(new org.jdom.Document(saveHistoryElement()), file, "\n");
      }
      catch (Exception ex) {
        LOG.error(ex);
      }
    }

    private void saveHistory() {
      try {
        if (getModel().isEmpty()) return;
        if (myRootType.isHidden()) {
          saveHistoryOld();
          return;
        }
        AccessToken token = ApplicationManager.getApplication().acquireWriteActionLock(getClass());
        try {
          VirtualFile file = HistoryRootType.getInstance().findFile(null, getHistoryName(myRootType, myId), ScratchFileService.Option.create_if_missing);
          VfsUtil.saveText(file, StringUtil.join(getModel().getEntries(), myRootType.getEntrySeparator()));
        }
        finally {
          token.finish();
        }
      }
      catch (Exception ex) {
        LOG.error(ex);
      }
    }

    @Nullable
    private String loadHistory(Element rootElement, String expectedId) {
      if (!rootElement.getName().equals("console-history")) return null;
      String id = rootElement.getAttributeValue("id");
      if (!expectedId.equals(id)) return null;
      List<String> entries = ContainerUtil.newArrayList();
      String consoleContent = null;
      for (Element childElement : rootElement.getChildren()) {
        String childName = childElement.getName();

        if ("history-entry".equals(childName)) {
          entries.add(StringUtil.notNullize(childElement.getText()));
        }
        else if ("console-content".equals(childName)) {
          consoleContent = StringUtil.notNullize(childElement.getText());
        }
      }
      getModel().resetEntries(entries);
      return consoleContent;
    }

    private Element saveHistoryElement() {
      Element rootElement = new Element("console-history");
      rootElement.setAttribute("version", "1");
      rootElement.setAttribute("id", myId);
      for (String s : getModel().getEntries()) {
        Element historyEntryElement = new Element("history-element");
        historyEntryElement.setText(s);
        rootElement.addContent(historyEntryElement);
      }

      String current = myContent;
      if (StringUtil.isNotEmpty(current)) {
        rootElement.addContent(new Element("console-content").setText(current));
      }
      return rootElement;
    }
  }

  @Nonnull
  private static String getHistoryName(@Nonnull ConsoleRootType rootType, @Nonnull String id) {
    return rootType.getConsoleTypeId() + "/" + PathUtil.makeFileName(rootType.getHistoryPathName(id), rootType.getDefaultFileExtension());
  }

  @Nullable
  public static VirtualFile getContentFile(@Nonnull final ConsoleRootType rootType, @Nonnull String id, ScratchFileService.Option option) {
    final String pathName = PathUtil.makeFileName(rootType.getContentPathName(id), rootType.getDefaultFileExtension());
    try {
      return rootType.findFile(null, pathName, option);
    }
    catch (final IOException e) {
      LOG.warn(e);
      ApplicationManager.getApplication().invokeLater(() -> {
        String message = String.format("Unable to open '%s/%s'\nReason: %s", rootType.getId(), pathName, e.getLocalizedMessage());
        Messages.showErrorDialog(message, "Unable to Open File");
      });
      return null;
    }
  }

  private static ShortcutSet getShortcutUpDown(boolean isUp) {
    AnAction action = ActionManager.getInstance().getActionOrStub(isUp ? IdeActions.ACTION_EDITOR_MOVE_CARET_UP : IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN);
    if (action != null) {
      return action.getShortcutSet();
    }
    return new CustomShortcutSet(KeyStroke.getKeyStroke(isUp ? KeyEvent.VK_UP : KeyEvent.VK_DOWN, 0));
  }

  private static void addShortcuts(@Nonnull AnAction action, @Nonnull ShortcutSet newShortcuts) {
    if (action.getShortcutSet().getShortcuts().length == 0) {
      action.registerCustomShortcutSet(newShortcuts, null);
    }
    else {
      action.registerCustomShortcutSet(new CompositeShortcutSet(action.getShortcutSet(), newShortcuts), null);
    }
  }

  private static Collection<KeyStroke> getKeystrokesUpDown(boolean isUp) {
    Collection<KeyStroke> result = new ArrayList<>();

    final ShortcutSet shortcutSet = getShortcutUpDown(isUp);
    for (Shortcut shortcut : shortcutSet.getShortcuts()) {
      if (shortcut.isKeyboard() && ((KeyboardShortcut)shortcut).getSecondKeyStroke() == null) {
        result.add(((KeyboardShortcut)shortcut).getFirstKeyStroke());
      }
    }

    return result;
  }

}
