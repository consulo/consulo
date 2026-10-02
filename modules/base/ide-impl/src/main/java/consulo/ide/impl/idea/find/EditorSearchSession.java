// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.find;

import consulo.application.HelpManager;
import consulo.application.localize.ApplicationLocalize;
import consulo.codeEditor.*;
import consulo.codeEditor.event.EditorFactoryEvent;
import consulo.codeEditor.event.EditorFactoryListener;
import consulo.codeEditor.event.SelectionEvent;
import consulo.codeEditor.event.SelectionListener;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.execution.ui.console.ConsoleViewUtil;
import consulo.fileEditor.internal.SearchReplaceComponent;
import consulo.fileEditor.impl.internal.search.SearchSession;
import consulo.fileEditor.impl.internal.search.StatusTextAction;
import consulo.find.*;
import consulo.find.localize.FindLocalize;
import consulo.ide.impl.idea.find.editorHeaderActions.*;
import consulo.ide.impl.idea.find.impl.HelpID;
import consulo.ide.impl.idea.find.impl.livePreview.LivePreviewController;
import consulo.ide.impl.idea.find.impl.livePreview.SearchResults;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.util.collection.ArrayUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author max, andrey.zaytsev
 */
public class EditorSearchSession implements SearchSession, UiDataProvider, SelectionListener, SearchResults.SearchResultsListener, SearchReplaceComponent.Listener {
    private static final String FIND_TYPE = "FindInFile";
    public static final Key<EditorSearchSession> SESSION_KEY = Key.create("EditorSearchSession");

    private static final Key<EditorSearchSession> EDITOR_SESSION_KEY = Key.create("EditorSearchSession.editor");

    private final Editor myEditor;
    private final LivePreviewController myLivePreviewController;
    private final SearchResults mySearchResults;
    private final FindModel myFindModel;
    private final SearchReplaceComponent myComponent;
    private RangeMarker myStartSessionSelectionMarker;
    private RangeMarker myStartSessionCaretMarker;
    private String myStartSelectedText;
    private boolean mySelectionUpdatedFromSearchResults;

    private boolean myClickToHighlightVisible;

    private final Disposable myDisposable = Disposable.newDisposable(EditorSearchSession.class.getName());

    public EditorSearchSession(Editor editor, Project project) {
        this(editor, project, createDefaultFindModel(project, editor));
    }

    @RequiredUIAccess
    public EditorSearchSession(final Editor editor, Project project, FindModel findModel) {
        assert !editor.isDisposed();

        myFindModel = findModel;

        myEditor = editor;
        saveInitialSelection();

        mySearchResults = new SearchResults(myEditor, project);
        myLivePreviewController = new LivePreviewController(mySearchResults, this, myDisposable);

        myComponent =
            SearchReplaceComponent.buildFor(project, myEditor.getContentUIComponent()).addPrimarySearchActions(createPrimarySearchActions()).addSecondarySearchActions(createSecondarySearchActions())
                .addPrimarySearchActions(new ToggleSelectionOnlyAction())
                .addExtraSearchActions(new ToggleMatchCase(),
                    new ToggleWholeWordsOnlyAction(),
                    new ToggleRegex(),
                    new ClickToHighlightAction())
                .addSearchFieldActions(new RestorePreviousSettingsAction())
                .addPrimaryReplaceActions(new ReplaceAction(),
                    new ReplaceAllAction(),
                    new ExcludeAction())
                .addExtraReplaceAction(new TogglePreserveCaseAction())
                .addReplaceFieldActions(new PrevOccurrenceAction(false), new NextOccurrenceAction(false)).withDataProvider(this)
                .withCloseAction(this::close).withReplaceAction(this::replaceCurrent)
                .withSecondarySearchActionsIsModifiedGetter(() -> myFindModel.getSearchContext() != FindSearchContext.ANY).build();

        myComponent.addListener(this);

        new SwitchToFind(getComponent().getUIComponent());
        new SwitchToReplace(getComponent().getUIComponent());

        myFindModel.addObserver(new FindModel.FindModelObserver() {
            boolean myReentrantLock = false;
            boolean myIsGlobal = myFindModel.isGlobal();
            boolean myIsReplace = myFindModel.isReplaceState();

            @Override
            public void findModelChanged(FindModel findModel1) {
                if (myReentrantLock) {
                    return;
                }
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

        updateMultiLineStateIfNeeded();

        EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryListener() {
            @Override
            public void editorReleased(EditorFactoryEvent event) {
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

    protected AnAction[] createPrimarySearchActions() {
        return new AnAction[]{new StatusTextAction(), new PrevOccurrenceAction(), new NextOccurrenceAction(), new FindAllAction(), AnSeparator.create(), new AddOccurrenceAction(),
            new RemoveOccurrenceAction(), new SelectAllAction(), AnSeparator.create()};
    }

    protected AnAction[] createSecondarySearchActions() {
        return new AnAction[]{new ToggleAnywhereAction(), new ToggleInCommentsAction(), new ToggleInLiteralsOnlyAction(), new ToggleExceptCommentsAction(), new ToggleExceptLiteralsAction(),
            new ToggleExceptCommentsAndLiteralsAction()};
    }

    private void saveInitialSelection() {
        if (mySelectionUpdatedFromSearchResults) {
            return;
        }
        SelectionModel selectionModel = myEditor.getSelectionModel();
        Document document = myEditor.getDocument();
        myStartSessionSelectionMarker = document.createRangeMarker(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd());
        myStartSessionCaretMarker = document.createRangeMarker(myEditor.getCaretModel().getOffset(), myEditor.getCaretModel().getOffset());
        myStartSelectedText = selectionModel.getSelectedText();
    }

    public Editor getEditor() {
        return myEditor;
    }

    public static @Nullable EditorSearchSession get(@Nullable Editor editor) {
        if (editor == null) {
            return null;
        }

        EditorSearchSession session = editor.getUserData(EDITOR_SESSION_KEY);
        if (session == null) {
            return null;
        }

        Component headerComponent = editor.getHeaderComponent();
        return headerComponent != null && headerComponent == session.getComponent().getUIComponent() ? session : null;
    }

    @RequiredUIAccess
    public static CompletableFuture<EditorSearchSession> start(Editor editor, @Nullable Project project) {
        return show(new EditorSearchSession(editor, project));
    }

    @RequiredUIAccess
    public static CompletableFuture<EditorSearchSession> start(Editor editor, FindModel findModel, @Nullable Project project) {
        return show(new EditorSearchSession(editor, project, findModel));
    }

    @RequiredUIAccess
    private static CompletableFuture<EditorSearchSession> show(EditorSearchSession session) {
        UIAccess uiAccess = UIAccess.current();
        return session.prepareAsync().handleAsync((o, throwable) -> {
            Editor editor = session.getEditor();
            editor.putUserData(EDITOR_SESSION_KEY, session);
            editor.setHeaderComponent(session.getComponent().getUIComponent());
            return session;
        }, uiAccess);
    }

    @Override
    public SearchReplaceComponent getComponent() {
        return myComponent;
    }

    public Project getProject() {
        return myComponent.getProject();
    }

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
    public void uiDataSnapshot(DataSink sink) {
        sink.set(SearchSession.KEY, this);
        sink.set(SESSION_KEY, this);
        sink.set(EditorKeys.EDITOR_EVEN_IF_INACTIVE, myEditor);
        sink.set(HelpManager.HELP_ID, myFindModel.isReplaceState() ? HelpID.REPLACE_IN_EDITOR : HelpID.FIND_IN_EDITOR);
    }

    @Override
    public void searchResultsUpdated(SearchResults sr) {
        if (sr.getFindModel() == null) {
            return;
        }
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
            myClickToHighlightVisible = tooManyMatches;
        }
        myComponent.updateActions();
    }

    @Override
    public void cursorMoved() {
        myComponent.updateActions();
    }

    @Override
    public void searchFieldDocumentChanged() {
        if (myEditor.isDisposed()) {
            return;
        }
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

    @Override
    public void componentShown() {
        initLivePreview();
    }

    @Override
    public void componentHidden() {
        myLivePreviewController.off();
        mySearchResults.removeListener(this);
    }

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
        myComponent.updateEmptyText(() -> getEmptyText().get());
    }

    private LocalizeValue getEmptyText() {
        if (myFindModel.isGlobal() || !myFindModel.getStringToFind().isEmpty()) {
            return LocalizeValue.empty();
        }
        String text = getEditor().getSelectionModel().getSelectedText();
        if (text != null && text.contains("\n")) {
            boolean replaceState = myFindModel.isReplaceState();
            AnAction action = ActionManager.getInstance()
                .getAction(replaceState ? IdeActions.ACTION_REPLACE : IdeActions.ACTION_TOGGLE_FIND_IN_SELECTION_ONLY);
            Shortcut shortcut = ArrayUtil.getFirstElement(action.getShortcutSet().getShortcuts());
            if (shortcut != null) {
                return ApplicationLocalize.editorsearchInSelectionWithHint(KeymapUtil.getShortcutText(shortcut));
            }
        }
        return ApplicationLocalize.editorsearchInSelection();
    }

    private static boolean wholeWordsApplicable(String stringToFind) {
        return !stringToFind.startsWith(" ") && !stringToFind.startsWith("\t") && !stringToFind.endsWith(" ") && !stringToFind.endsWith("\t");
    }

    private void setMatchesLimit(int value) {
        mySearchResults.setMatchesLimit(value);
    }

    @RequiredUIAccess
    private void replaceCurrent() {
        if (mySearchResults.getCursor() != null) {
            try {
                myLivePreviewController.performReplace();
            }
            catch (FindManager.MalformedReplacementStringException e) {
                MessageBoxes.okError(LocalizeValue.ofNullable(e.getMessage()))
                    .title(FindLocalize.findReplaceInvalidReplacementStringTitle())
                    .showAsync();
            }
        }
    }

    public void addTextToRecent(String text, boolean search) {
        myComponent.addTextToRecent(text, search);
    }

    @Override
    public void selectionChanged(SelectionEvent e) {
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
    @RequiredUIAccess
    public void close() {
        ProjectIdeFocusManager.getInstance(getProject()).requestFocus(myEditor.getContentUIComponent(), false);

        myLivePreviewController.dispose();
        myEditor.setHeaderComponent(null);

        if (myEditor.getUserData(EDITOR_SESSION_KEY) == this) {
            myEditor.putUserData(EDITOR_SESSION_KEY, null);
        }
    }

    private void initLivePreview() {
        if (myEditor.isDisposed()) {
            return;
        }

        myLivePreviewController.on();

        myLivePreviewController.setUserActivityDelay(0);
        updateResults(false);
        myLivePreviewController.setUserActivityDelay(LivePreviewController.USER_ACTIVITY_TRIGGERING_DELAY);

        mySearchResults.addListener(this);
    }

    private void updateResults(boolean allowedToChangedEditorSelection) {
        String text = myFindModel.getStringToFind();
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
                    myClickToHighlightVisible = false;
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

            FindManager findManager = FindManager.getInstance(getProject());
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
        myClickToHighlightVisible = false;
    }

    public String getTextInField() {
        return myComponent.getSearchText();
    }

    public void setTextInField(String text) {
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

    @RequiredUIAccess
    public CompletableFuture<?> prepareAsync() {
        return myComponent.prepareAsync();
    }

    private class ClickToHighlightAction extends LegacyDumbAwareAction implements CustomUIComponentAction {
        @Override
        @RequiredUIAccess
        public Component createCustomComponent(Presentation presentation, String place) {
            Hyperlink hyperlink = Hyperlink.create(FindLocalize.linkClickToHighlight(), e -> {
                setMatchesLimit(Integer.MAX_VALUE);
                updateResults(true);
            });
            hyperlink.setVisible(false);
            return hyperlink;
        }

        @Override
        @RequiredUIAccess
        public void update(AnActionEvent e) {
            if (e.getPresentation().getClientProperty(COMPONENT_KEY) instanceof Hyperlink hyperlink) {
                hyperlink.setVisible(myClickToHighlightVisible);
            }
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
        }
    }

    private abstract static class ButtonAction extends LegacyDumbAwareAction implements CustomUIComponentAction {
        private final LocalizeValue myTitle;

        ButtonAction(LocalizeValue title) {
            myTitle = title;
        }

        @Override
        @RequiredUIAccess
        public Component createCustomComponent(Presentation presentation, String place) {
            Button button = Button.create(myTitle, e -> onClick());
            button.addStyle(ButtonStyle.TOOLBAR);
            button.setFocusable(false);
            return button;
        }

        @RequiredUIAccess
        @Override
        public final void update(AnActionEvent e) {
            if (e.getPresentation().getClientProperty(COMPONENT_KEY) instanceof Button button) {
                update(button);
            }
        }

        @RequiredUIAccess
        @Override
        public final void actionPerformed(AnActionEvent e) {
            onClick();
        }

        @RequiredUIAccess
        protected abstract void update(Button button);

        @RequiredUIAccess
        protected abstract void onClick();
    }

    private class ReplaceAction extends ButtonAction {
        ReplaceAction() {
            super(FindLocalize.buttonReplace());
        }

        @Override
        @RequiredUIAccess
        protected void update(Button button) {
            button.setEnabled(mySearchResults.hasMatches());
        }

        @Override
        @RequiredUIAccess
        protected void onClick() {
            replaceCurrent();
        }
    }

    private class ReplaceAllAction extends ButtonAction {
        ReplaceAllAction() {
            super(FindLocalize.findReplaceAllAction());
        }

        @Override
        @RequiredUIAccess
        protected void update(Button button) {
            button.setEnabled(mySearchResults.hasMatches());
        }

        @Override
        @RequiredUIAccess
        protected void onClick() {
            myLivePreviewController.performReplaceAll();
        }
    }

    private class ExcludeAction extends ButtonAction {
        ExcludeAction() {
            super(FindLocalize.buttonExclude());
        }

        @Override
        @RequiredUIAccess
        protected void update(Button button) {
            FindResult cursor = mySearchResults.getCursor();
            button.setEnabled(cursor != null);
            button.setText(
                cursor != null && mySearchResults.isExcluded(cursor)
                    ? FindLocalize.buttonInclude()
                    : FindLocalize.buttonExclude()
            );
        }

        @Override
        @RequiredUIAccess
        protected void onClick() {
            myLivePreviewController.exclude();
            moveCursor(SearchResults.Direction.DOWN);
        }
    }
}
