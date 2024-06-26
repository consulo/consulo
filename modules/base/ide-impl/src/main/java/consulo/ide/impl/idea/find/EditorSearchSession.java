// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.find;

import consulo.application.Application;
import consulo.application.localize.ApplicationLocalize;import consulo.application.ui.UISettings;
import consulo.codeEditor.*;
import consulo.codeEditor.event.EditorFactoryEvent;
import consulo.codeEditor.event.EditorFactoryListener;
import consulo.codeEditor.event.SelectionEvent;
import consulo.codeEditor.event.SelectionListener;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.execution.ui.console.ConsoleViewUtil;
import consulo.find.FindBundle;
import consulo.find.FindManager;
import consulo.find.FindModel;
import consulo.find.FindResult;
import consulo.ide.impl.idea.find.editorHeaderActions.*;
import consulo.ide.impl.idea.find.impl.HelpID;
import consulo.ide.impl.idea.find.impl.livePreview.LivePreviewController;
import consulo.ide.impl.idea.find.impl.livePreview.SearchResults;
import consulo.ide.impl.idea.openapi.actionSystem.ex.DefaultCustomComponentAction;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.language.editor.PlatformDataKeys;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.LinkLabel;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.action.CustomComponentAction;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.update.Activatable;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author max, andrey.zaytsev
 */
public class EditorSearchSession implements SearchSession, DataProvider, SelectionListener, SearchResults.SearchResultsListener, SearchReplaceComponent.Listener {
  private static final String FIND_TYPE = "FindInFile";
  public static final Key<EditorSearchSession> SESSION_KEY = Key.create("EditorSearchSession");

  private final Editor myEditor;
  private final LivePreviewController myLivePreviewController;
  private final SearchResults mySearchResults;
  @Nonnull
  private final FindModel myFindModel;
  private final SearchReplaceComponent myComponent;
  private RangeMarker myStartSessionSelectionMarker;
  private RangeMarker myStartSessionCaretMarker;
  private String myStartSelectedText;
  private boolean mySelectionUpdatedFromSearchResults;

  private final LinkLabel<Object> myClickToHighlightLabel = new LinkLabel<>(
    FindBundle.message("link.click.to.highlight"),
    null,
    (__, ___) -> {
      setMatchesLimit(Integer.MAX_VALUE);
      updateResults(true);
    }
  );
  private final Disposable myDisposable = Disposable.newDisposable(EditorSearchSession.class.getName());

  public EditorSearchSession(@Nonnull Editor editor, Project project) {
    this(editor, project, createDefaultFindModel(project, editor));
  }

  public EditorSearchSession(@Nonnull final Editor editor, Project project, @Nonnull FindModel findModel) {
    assert !editor.isDisposed();

    myClickToHighlightLabel.setVisible(false);

    myFindModel = findModel;

    myEditor = editor;
    saveInitialSelection();

    mySearchResults = new SearchResults(myEditor, project);
    myLivePreviewController = new LivePreviewController(mySearchResults, this, myDisposable);

    myComponent =
            SearchReplaceComponent.buildFor(project, myEditor.getContentComponent()).addPrimarySearchActions(createPrimarySearchActions()).addSecondarySearchActions(createSecondarySearchActions())
                    .addPrimarySearchActions(new ToggleSelectionOnlyAction())
                    .addExtraSearchActions(new ToggleMatchCase(), new ToggleWholeWordsOnlyAction(), new ToggleRegex(), new DefaultCustomComponentAction(() -> myClickToHighlightLabel))
                    .addSearchFieldActions(new RestorePreviousSettingsAction()).addPrimaryReplaceActions(new ReplaceAction(), new ReplaceAllAction(), new ExcludeAction())
                    .addExtraReplaceAction(new TogglePreserveCaseAction()).addReplaceFieldActions(new PrevOccurrenceAction(false), new NextOccurrenceAction(false)).withDataProvider(this)
                    .withCloseAction(this::close).withReplaceAction(this::replaceCurrent)
                    .withSecondarySearchActionsIsModifiedGetter(() -> myFindModel.getSearchContext() != FindModel.SearchContext.ANY).build();

    myComponent.addListener(this);
    new UiNotifyConnector(myComponent.getComponent(), new Activatable() {
      @Override
      public void showNotify() {
        initLivePreview();
      }

      @Override
      public void hideNotify() {
        myLivePreviewController.off();
        mySearchResults.removeListener(EditorSearchSession.this);
      }
    });

    new SwitchToFind(getComponent().getComponent());
    new SwitchToReplace(getComponent().getComponent());

    myFindModel.addObserver(new FindModel.FindModelObserver() {
      boolean myReentrantLock = false;
      boolean myIsGlobal = myFindModel.isGlobal();
      boolean myIsReplace = myFindModel.isReplaceState();

      @Override
      public void findModelChanged(FindModel findModel1) {
        if (myReentrantLock) return;
        try {
          myReentrantLock = true;
          String stringToFind = myFindModel.getStringToFind();
          if (!wholeWordsApplicable(stringToFind)) {
            myFindModel.setWholeWordsOnly(false);
          }
          if (myIsGlobal != myFindModel.isGlobal() || myIsReplace != myFindModel.isReplaceState()) {
            if (myFindModel.getStringToFind().isEmpty() && myFindModel.isGlobal()) {
              myFindModel.setStringToFind(StringUtil.notNullize(myEditor.getSelectionModel().getSelectedText()));
            }
            if (!myFindModel.isGlobal()) {
              if (myFindModel.getStringToFind().equals(myStartSelectedText)) {
                myFindModel.setStringToFind("");
              }
              else {
                restoreInitialCaretPositionAndSelection();
              }
            }
            myIsGlobal = myFindModel.isGlobal();
            myIsReplace = myFindModel.isReplaceState();
          }
          EditorSearchSession.this.updateUIWithFindModel();
          mySearchResults.clear();
          EditorSearchSession.this.updateResults(true);
          FindUtil.updateFindInFileModel(EditorSearchSession.this.getProject(), myFindModel, !ConsoleViewUtil.isConsoleViewEditor(editor));
        }
        finally {
          myReentrantLock = false;
        }
      }
    });

    updateUIWithFindModel();

    if (Application.get().isUnitTestMode()) {
      initLivePreview();
    }
    updateMultiLineStateIfNeeded();

    EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryListener() {
      @Override
      public void editorReleased(@Nonnull EditorFactoryEvent event) {
        if (event.getEditor() == myEditor) {
          Disposer.dispose(myDisposable);
          myLivePreviewController.dispose();
          myStartSessionSelectionMarker.dispose();
          myStartSessionCaretMarker.dispose();
        }
      }
    }, myDisposable);

    myEditor.getSelectionModel().addSelectionListener(this, myDisposable);

    //FindUtil.triggerUsedOptionsStats(FIND_TYPE, findModel);
  }

  @Nonnull
  protected AnAction[] createPrimarySearchActions() {
    return new AnAction[]{new StatusTextAction(), new PrevOccurrenceAction(), new NextOccurrenceAction(), new FindAllAction(), new AnSeparator(), new AddOccurrenceAction(),
            new RemoveOccurrenceAction(), new SelectAllAction(), new AnSeparator()};
  }

  @Nonnull
  protected AnAction[] createSecondarySearchActions() {
    return new AnAction[]{new ToggleAnywhereAction(), new ToggleInCommentsAction(), new ToggleInLiteralsOnlyAction(), new ToggleExceptCommentsAction(), new ToggleExceptLiteralsAction(),
            new ToggleExceptCommentsAndLiteralsAction()};
  }

  private void saveInitialSelection() {
    if (mySelectionUpdatedFromSearchResults) return;
    SelectionModel selectionModel = myEditor.getSelectionModel();
    Document document = myEditor.getDocument();
    myStartSessionSelectionMarker = document.createRangeMarker(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd());
    myStartSessionCaretMarker = document.createRangeMarker(myEditor.getCaretModel().getOffset(), myEditor.getCaretModel().getOffset());
    myStartSelectedText = selectionModel.getSelectedText();
  }

  public Editor getEditor() {
    return myEditor;
  }

  @Nullable
  public static EditorSearchSession get(@Nullable Editor editor) {
    JComponent headerComponent = editor != null ? editor.getHeaderComponent() : null;
    SearchReplaceComponent searchReplaceComponent = ObjectUtil.tryCast(headerComponent, SearchReplaceComponent.class);
    return searchReplaceComponent != null ? searchReplaceComponent.getDataUnchecked(SESSION_KEY) : null;
  }

  @Nonnull
  public static EditorSearchSession start(@Nonnull Editor editor, @Nullable Project project) {
    EditorSearchSession session = new EditorSearchSession(editor, project);
    editor.setHeaderComponent(session.getComponent().getComponent());
    return session;
  }

  @Nonnull
  public static EditorSearchSession start(@Nonnull Editor editor, @Nonnull FindModel findModel, @Nullable Project project) {
    EditorSearchSession session = new EditorSearchSession(editor, project, findModel);
    editor.setHeaderComponent(session.getComponent().getComponent());
    return session;
  }

  @Nonnull
  @Override
  public SearchReplaceComponent getComponent() {
    return myComponent;
  }

  public Project getProject() {
    return myComponent.getProject();
  }

  @Nonnull
  private static FindModel createDefaultFindModel(Project project, Editor editor) {
    FindModel findModel = new FindModel();
    findModel.copyFrom(FindManager.getInstance(project).getFindInFileModel());
    if (editor.getSelectionModel().hasSelection()) {
      String selectedText = editor.getSelectionModel().getSelectedText();
      if (selectedText != null) {
        findModel.setStringToFind(selectedText);
      }
    }
    findModel.setPromptOnReplace(false);
    return findModel;
  }


  @Override
  @Nullable
  public Object getData(@Nonnull final Key dataId) {
    if (SearchSession.KEY == dataId) {
      return this;
    }
    if (SESSION_KEY == dataId) {
      return this;
    }
    if (EditorKeys.EDITOR_EVEN_IF_INACTIVE == dataId) {
      return myEditor;
    }
    if (PlatformDataKeys.HELP_ID == dataId) {
      return myFindModel.isReplaceState() ? HelpID.REPLACE_IN_EDITOR : HelpID.FIND_IN_EDITOR;
    }
    return null;
  }

  @Override
  public void searchResultsUpdated(@Nonnull SearchResults sr) {
    if (sr.getFindModel() == null) return;
    if (myComponent.getSearchText().isEmpty()) {
      updateUIWithEmptyResults();
    }
    else {
      int matches = sr.getMatchesCount();
      boolean tooManyMatches = matches > mySearchResults.getMatchesLimit();
      LocalizeValue status;
      if (matches == 0 && !sr.getFindModel().isGlobal() && !myEditor.getSelectionModel().hasSelection()) {
        status = ApplicationLocalize.editorsearchNoselection();
        myComponent.setRegularBackground();
      }
      else {
        int cursorIndex = sr.getCursorVisualIndex();
        status = tooManyMatches
          ? ApplicationLocalize.editorsearchToomuch(mySearchResults.getMatchesLimit())
          : cursorIndex != -1
          ? ApplicationLocalize.editorsearchCurrentCursorPosition(cursorIndex, matches)
          : ApplicationLocalize.editorsearchMatches(matches);
        if (!tooManyMatches && matches <= 0) {
          myComponent.setNotFoundBackground();
        }
        else {
          myComponent.setRegularBackground();
        }
      }
      myComponent.setStatusText(status.get());
      myClickToHighlightLabel.setVisible(tooManyMatches);
    }
    myComponent.updateActions();
  }

  @Override
  public void cursorMoved() {
    myComponent.updateActions();
  }

  @Override
  public void searchFieldDocumentChanged() {
    if (myEditor.isDisposed()) return;
    setMatchesLimit(LivePreviewController.MATCHES_LIMIT);
    String text = myComponent.getSearchText();
    myFindModel.setStringToFind(text);
    updateResults(true);
    updateMultiLineStateIfNeeded();
  }

  private void updateMultiLineStateIfNeeded() {
    myFindModel.setMultiline(myComponent.getSearchText().contains("\n") || myComponent.getReplaceText().contains("\n"));
  }

  @Override
  public void replaceFieldDocumentChanged() {
    setMatchesLimit(LivePreviewController.MATCHES_LIMIT);
    myFindModel.setStringToReplace(myComponent.getReplaceText());
    updateMultiLineStateIfNeeded();
  }

  @Override
  public void multilineStateChanged() {
    myFindModel.setMultiline(myComponent.isMultiline());
  }

  @Nonnull
  @Override
  public FindModel getFindModel() {
    return myFindModel;
  }

  @Override
  public boolean hasMatches() {
    return mySearchResults.hasMatches();
  }

  @Override
  public boolean isSearchInProgress() {
    return mySearchResults.isUpdating();
  }

  @Override
  public void searchForward() {
    moveCursor(SearchResults.Direction.DOWN);
    addTextToRecent(myComponent.getSearchText(), true);
  }

  @Override
  public void searchBackward() {
    moveCursor(SearchResults.Direction.UP);
    addTextToRecent(myComponent.getSearchText(), true);
  }

  private void updateUIWithFindModel() {
    myComponent.update(myFindModel.getStringToFind(), myFindModel.getStringToReplace(), myFindModel.isReplaceState(), myFindModel.isMultiline());
    updateEmptyText();
    myLivePreviewController.setTrackingSelection(!myFindModel.isGlobal());
  }

  private void updateEmptyText() {
    myComponent.updateEmptyText(this::getEmptyText);
  }

  @Nonnull
  private String getEmptyText() {
    if (myFindModel.isGlobal() || !myFindModel.getStringToFind().isEmpty()) return "";
    String text = getEditor().getSelectionModel().getSelectedText();
    if (text != null && text.contains("\n")) {
      boolean replaceState = myFindModel.isReplaceState();
      AnAction action = ActionManager.getInstance().getAction(replaceState ? IdeActions.ACTION_REPLACE : IdeActions.ACTION_TOGGLE_FIND_IN_SELECTION_ONLY);
      Shortcut shortcut = ArrayUtil.getFirstElement(action.getShortcutSet().getShortcuts());
      if (shortcut != null) {
        return ApplicationLocalize.editorsearchInSelectionWithHint(KeymapUtil.getShortcutText(shortcut)).get();
      }
    }
    return ApplicationLocalize.editorsearchInSelection().get();
  }

  private static boolean wholeWordsApplicable(String stringToFind) {
    return !stringToFind.startsWith(" ") && !stringToFind.startsWith("\t") && !stringToFind.endsWith(" ") && !stringToFind.endsWith("\t");
  }

  private void setMatchesLimit(int value) {
    mySearchResults.setMatchesLimit(value);
  }

  private void replaceCurrent() {
    if (mySearchResults.getCursor() != null) {
      try {
        myLivePreviewController.performReplace();
      }
      catch (FindManager.MalformedReplacementStringException e) {
        Messages.showErrorDialog(
          myComponent.getComponent(),
          e.getMessage(),
          FindBundle.message("find.replace.invalid.replacement.string.title")
        );
      }
    }
  }

  public void addTextToRecent(String text, boolean search) {
    myComponent.addTextToRecent(text, search);
  }

  @Override
  public void selectionChanged(@Nonnull SelectionEvent e) {
    saveInitialSelection();
    updateEmptyText();
  }

  @Override
  public void beforeSelectionUpdate() {
    mySelectionUpdatedFromSearchResults = true;
  }

  @Override
  public void afterSelectionUpdate() {
    mySelectionUpdatedFromSearchResults = false;
  }

  private void moveCursor(SearchResults.Direction direction) {
    myLivePreviewController.moveCursor(direction);
  }

  @Override
  public void close() {
    ProjectIdeFocusManager.getInstance(getProject()).requestFocus(myEditor.getContentComponent(), false);

    myLivePreviewController.dispose();
    myEditor.setHeaderComponent(null);
  }

  private void initLivePreview() {
    if (myEditor.isDisposed()) return;

    myLivePreviewController.on();

    myLivePreviewController.setUserActivityDelay(0);
    updateResults(false);
    myLivePreviewController.setUserActivityDelay(LivePreviewController.USER_ACTIVITY_TRIGGERING_DELAY);

    mySearchResults.addListener(this);
  }

  private void updateResults(final boolean allowedToChangedEditorSelection) {
    final String text = myFindModel.getStringToFind();
    if (text.isEmpty()) {
      nothingToSearchFor(allowedToChangedEditorSelection);
    }
    else {

      if (myFindModel.isRegularExpressions()) {
        try {
          Pattern.compile(text);
        }
        catch (PatternSyntaxException e) {
          myComponent.setNotFoundBackground();
          myClickToHighlightLabel.setVisible(false);
          mySearchResults.clear();
          myComponent.setStatusText(INCORRECT_REGEX_MESSAGE);
          return;
        }
        if (text.matches("\\|+")) {
          nothingToSearchFor(allowedToChangedEditorSelection);
          myComponent.setStatusText(ApplicationLocalize.editorsearchEmptyStringMatches().get());
          return;
        }
      }


      final FindManager findManager = FindManager.getInstance(getProject());
      if (allowedToChangedEditorSelection) {
        findManager.setFindWasPerformed();
        FindModel copy = new FindModel();
        copy.copyFrom(myFindModel);
        copy.setReplaceState(false);
        findManager.setFindNextModel(copy);
      }
      if (myLivePreviewController != null) {
        myLivePreviewController.updateInBackground(myFindModel, allowedToChangedEditorSelection);
      }
    }
  }

  private void nothingToSearchFor(boolean allowedToChangedEditorSelection) {
    updateUIWithEmptyResults();
    mySearchResults.clear();
    if (allowedToChangedEditorSelection && !myComponent.isJustClearedSearch()) {
      restoreInitialCaretPositionAndSelection();
    }
  }

  private void restoreInitialCaretPositionAndSelection() {
    int originalSelectionStart = Math.min(myStartSessionSelectionMarker.getStartOffset(), myEditor.getDocument().getTextLength());
    int originalSelectionEnd = Math.min(myStartSessionSelectionMarker.getEndOffset(), myEditor.getDocument().getTextLength());

    myEditor.getSelectionModel().setSelection(originalSelectionStart, originalSelectionEnd);
    myEditor.getCaretModel().moveToOffset(Math.min(myStartSessionCaretMarker.getEndOffset(), myEditor.getDocument().getTextLength()));
    myEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
  }

  private void updateUIWithEmptyResults() {
    myComponent.setRegularBackground();
    myComponent.setStatusText(ApplicationLocalize.editorsearchMatches(0).get());
    myClickToHighlightLabel.setVisible(false);
  }

  public String getTextInField() {
    return myComponent.getSearchText();
  }

  public void setTextInField(final String text) {
    myComponent.setSearchText(text);
    myFindModel.setStringToFind(text);
  }

  public void selectAllOccurrences() {
    FindUtil.selectSearchResultsInEditor(myEditor, mySearchResults.getOccurrences().iterator(), -1);
  }

  public void removeOccurrence() {
    mySearchResults.prevOccurrence(true);
  }

  public void addNextOccurrence() {
    mySearchResults.nextOccurrence(true);
  }

  public void clearUndoInTextFields() {
    myComponent.resetUndoRedoActions();
  }


  private abstract static class ButtonAction extends DumbAwareAction implements CustomComponentAction, ActionListener {
    private final String myTitle;
    private final char myMnemonic;

    ButtonAction(@Nonnull String title, char mnemonic) {
      myTitle = title;
      myMnemonic = mnemonic;
    }

    @Nonnull
    @Override
    public JComponent createCustomComponent(@Nonnull Presentation presentation, @Nonnull String place) {
      JButton button = new JButton(myTitle) {
        @Override
        public Dimension getPreferredSize() {
          return new Dimension(super.getPreferredSize().width, JBUIScale.scale(24));
        }
      };
      button.setFocusable(false);
      if (!UISettings.getInstance().getDisableMnemonicsInControls()) {
        button.setMnemonic(myMnemonic);
      }
      button.addActionListener(this);
      return button;
    }

    @RequiredUIAccess
    @Override
    public final void update(@Nonnull AnActionEvent e) {
      JButton button = (JButton)e.getPresentation().getClientProperty(COMPONENT_KEY);
      if (button != null) {
        update(button);
      }
    }

    @RequiredUIAccess
    @Override
    public final void actionPerformed(@Nonnull AnActionEvent e) {
      onClick();
    }

    @Override
    public final void actionPerformed(ActionEvent e) {
      onClick();
    }

    protected abstract void update(@Nonnull JButton button);

    protected abstract void onClick();
  }

  private class ReplaceAction extends ButtonAction {
    ReplaceAction() {
      super("Replace", 'p');
    }

    @Override
    protected void update(@Nonnull JButton button) {
      button.setEnabled(mySearchResults.hasMatches());
    }

    @Override
    protected void onClick() {
      replaceCurrent();
    }
  }

  private class ReplaceAllAction extends ButtonAction {
    ReplaceAllAction() {
      super("Replace all", 'a');
    }

    @Override
    protected void update(@Nonnull JButton button) {
      button.setEnabled(mySearchResults.hasMatches());
    }

    @Override
    protected void onClick() {
      myLivePreviewController.performReplaceAll();
    }
  }

  private class ExcludeAction extends ButtonAction {
    ExcludeAction() {
      super("", 'l');
    }

    @Override
    protected void update(@Nonnull JButton button) {
      FindResult cursor = mySearchResults.getCursor();
      button.setEnabled(cursor != null);
      button.setText(
        cursor != null && mySearchResults.isExcluded(cursor)
          ? FindBundle.message("button.include")
          : FindBundle.message("button.exclude")
      );
    }

    @Override
    protected void onClick() {
      myLivePreviewController.exclude();
      moveCursor(SearchResults.Direction.DOWN);
    }
  }
}
