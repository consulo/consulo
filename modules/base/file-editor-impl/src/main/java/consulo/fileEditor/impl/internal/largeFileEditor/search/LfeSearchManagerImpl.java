// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileEditor.impl.internal.largeFileEditor.search;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.util.HtmlBuilder;
import consulo.application.util.HtmlChunk;
import consulo.codeEditor.Editor;
import consulo.codeEditor.event.CaretEvent;
import consulo.fileEditor.EditorNotifications;
import consulo.fileEditor.LargeFileEditor;
import consulo.fileEditor.impl.internal.largeFileEditor.Utils;
import consulo.fileEditor.impl.internal.largeFileEditor.search.action.*;
import consulo.fileEditor.impl.internal.ui.RegExHelpPopup;
import consulo.fileEditor.internal.SearchReplaceComponent;
import consulo.fileEditor.internal.largeFileEditor.*;
import consulo.fileEditor.localize.FileEditorLocalize;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.language.editor.ui.internal.HintManagerEx;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.CollectionListModel;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.action.DefaultCustomComponentAction;
import consulo.ui.ex.awt.hint.LightweightHint;
import consulo.ui.ex.awt.hint.LightweightHintFactory;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.io.IOException;
import java.util.List;

public final class LfeSearchManagerImpl implements LfeSearchManager, CloseSearchTask.Callback {
    private static final int CONTEXT_ONE_SIDE_LENGTH = 100;
    private static final long STATUS_TEXT_LIFE_TIME = 3000;

    private static final Logger LOG = Logger.getInstance(LfeSearchManagerImpl.class);
    private static final long PROGRESS_STATUS_UPDATE_PERIOD = 150;

    private final LargeFileEditor largeFileEditor;
    private final FileDataProviderForSearch fileDataProviderForSearch;
    private final RangeSearchCreator rangeSearchCreator;

    // TODO: 2019-05-21 need to implement using this for optimization of "close" searching
    private final JBList<SearchResult> myCloseSearchResultsList;

    private CloseSearchTask lastExecutedCloseSearchTask;
    private boolean notFoundState;
    private long lastProgressStatusUpdateTime = System.currentTimeMillis();

    private SearchReplaceComponent mySearchReplaceComponent;
    private LargeFileFindAllAction myFindAllAction;
    private FindForwardBackwardAction myFindForwardAction;
    private FindForwardBackwardAction myFindBackwardAction;
    private LargeFilePrevNextOccurrenceAction myNextOccurrenceAction;
    private LargeFilePrevNextOccurrenceAction myPrevOccurrenceAction;
    private LargeFileToggleAction myToggleCaseSensitiveAction;
    private LargeFileToggleAction myToggleWholeWordsAction;
    private LargeFileToggleAction myToggleRegularExpression;
    private LargeFileStatusTextAction myStatusTextAction;

    private String myStatusText;
    private boolean myIsStatusTextHidden;
    private long myLastTimeStatusTextWasChanged;

    public LfeSearchManagerImpl(@Nonnull LargeFileEditor largeFileEditor,
                                FileDataProviderForSearch fileDataProviderForSearch,
                                @Nonnull RangeSearchCreator rangeSearchCreator) {
        this.largeFileEditor = largeFileEditor;
        this.fileDataProviderForSearch = fileDataProviderForSearch;
        this.rangeSearchCreator = rangeSearchCreator;

        createActions();
        createSearchReplaceComponent();
        attachListenersToSearchReplaceComponent();

        myCloseSearchResultsList = createCloseSearchResultsList();

        lastExecutedCloseSearchTask = null;
        notFoundState = false;

        myStatusText = "";
        myIsStatusTextHidden = true;
        myLastTimeStatusTextWasChanged = System.currentTimeMillis();
    }

    @Override
    public SearchReplaceComponent getSearchReplaceComponent() {
        return mySearchReplaceComponent;
    }

    @Override
    public CloseSearchTask getLastExecutedCloseSearchTask() {
        return lastExecutedCloseSearchTask;
    }

    @Override
    public void onSearchActionHandlerExecuted() {
        largeFileEditor.getEditor().setHeaderComponent(mySearchReplaceComponent.getComponent());
        mySearchReplaceComponent.requestFocusInTheSearchFieldAndSelectContent(largeFileEditor.getProject());
        mySearchReplaceComponent.getSearchTextComponent().selectAll();
    }

    @Override
    public @Nonnull LargeFileEditor getLargeFileEditor() {
        return largeFileEditor;
    }

    @Override
    public void launchNewRangeSearch(long fromPageNumber, long toPageNumber, boolean forwardDirection) {
        SearchTaskOptions options = new SearchTaskOptions()
            .setStringToFind(mySearchReplaceComponent.getSearchTextComponent().getText())
            .setSearchDirectionForward(forwardDirection)
            .setSearchBounds(fromPageNumber, SearchTaskOptions.NO_LIMIT,
                toPageNumber, SearchTaskOptions.NO_LIMIT)
            .setCaseSensitive(myToggleCaseSensitiveAction.isSelected())
            .setWholeWords(myToggleWholeWordsAction.isSelected())
            .setRegularExpression(myToggleRegularExpression.isSelected())
            .setContextOneSideLength(CONTEXT_ONE_SIDE_LENGTH);

        launchNewRangeSearch(options);
    }

    private void launchNewRangeSearch(SearchTaskOptions searchTaskOptions) {
        showRegexSearchWarningIfNeed();

        RangeSearch rangeSearch = rangeSearchCreator.createContent(
            largeFileEditor.getProject(), largeFileEditor.getFile(),
            largeFileEditor.getFile().getName());
        rangeSearch.runNewSearch(searchTaskOptions, fileDataProviderForSearch);
    }

    @Override
    public void gotoNextOccurrence(boolean directionForward) {
        int gotoSearchResultIndex = getNextOccurrenceIndexIfCan(directionForward,
            largeFileEditor.getCaretPageNumber(),
            largeFileEditor.getCaretPageOffset(),
            myCloseSearchResultsList);

        if (gotoSearchResultIndex == -1) {

            boolean launchedLoopedCloseSearch = false;

            SearchTaskOptions normalCloseSearchOptions = generateOptionsForNormalCloseSearch(directionForward);

            if (notFoundState) {
                notFoundState = false;
                launchedLoopedCloseSearch = launchLoopedCloseSearchTaskIfNeeded(normalCloseSearchOptions);
            }

            if (!launchedLoopedCloseSearch) {
                launchCloseSearch(normalCloseSearchOptions);
            }
        }
        else {
            myCloseSearchResultsList.setSelectedIndex(gotoSearchResultIndex);
            setNewStatusText(LocalizeValue.of());
        }
    }

    @RequiredUIAccess
    private void launchCloseSearch(SearchTaskOptions options) {
        if (StringUtil.isEmpty(options.stringToFind)) {
            return;
        }

        stopSearchTaskIfItExists();

        showRegexSearchWarningIfNeed();

        lastExecutedCloseSearchTask = new CloseSearchTask(
            options, largeFileEditor.getProject(), fileDataProviderForSearch, this);
        ApplicationManager.getApplication().executeOnPooledThread(lastExecutedCloseSearchTask);
    }

    private void showRegexSearchWarningIfNeed() {
        EditorNotifications.getInstance(largeFileEditor.getProject()).updateNotifications(largeFileEditor.getFile());
    }

    private boolean launchLoopedCloseSearchTaskIfNeeded(SearchTaskOptions normalCloseSearchOptions) {
        if (lastExecutedCloseSearchTask == null || !lastExecutedCloseSearchTask.isFinished()) {
            return false;
        }

        SearchTaskOptions oldOptions = lastExecutedCloseSearchTask.getOptions();
        if (oldOptions.loopedPhase) {
            return false;
        }
        if (!normalCloseSearchOptions.stringToFind.equals(oldOptions.stringToFind)
            || normalCloseSearchOptions.wholeWords != oldOptions.wholeWords
            || normalCloseSearchOptions.caseSensitive != oldOptions.caseSensitive
            || normalCloseSearchOptions.regularExpression != oldOptions.regularExpression
            || normalCloseSearchOptions.searchForwardDirection != oldOptions.searchForwardDirection
            || normalCloseSearchOptions.leftBoundPageNumber != oldOptions.leftBoundPageNumber
            || normalCloseSearchOptions.leftBoundCaretPageOffset != oldOptions.leftBoundCaretPageOffset
            || normalCloseSearchOptions.rightBoundPageNumber != oldOptions.rightBoundPageNumber
            || normalCloseSearchOptions.rightBoundCaretPageOffset != oldOptions.rightBoundCaretPageOffset) {
            return false;
        }

        SearchTaskOptions loopedOptions;
        try {
            loopedOptions = normalCloseSearchOptions.clone();
        }
        catch (CloneNotSupportedException e) {
            LOG.warn(e);
            Messages.showWarningDialog(FileEditorLocalize.largeFileEditorMessageErrorWhileSearching().get(), FileEditorLocalize.largeFileEditorTitleSearchError().get());
            return false;
        }
        loopedOptions.loopedPhase = true;
        if (loopedOptions.searchForwardDirection) {
            loopedOptions.setSearchBounds(
                0, SearchTaskOptions.NO_LIMIT,
                normalCloseSearchOptions.leftBoundPageNumber, normalCloseSearchOptions.leftBoundCaretPageOffset);
        }
        else {
            loopedOptions.setSearchBounds(
                normalCloseSearchOptions.rightBoundPageNumber, normalCloseSearchOptions.rightBoundCaretPageOffset,
                SearchTaskOptions.NO_LIMIT, SearchTaskOptions.NO_LIMIT);
        }

        launchCloseSearch(loopedOptions);

        return true;
    }

    private SearchTaskOptions generateOptionsForNormalCloseSearch(boolean directionForward) {
        SearchTaskOptions options = new SearchTaskOptions()
            .setSearchDirectionForward(directionForward)
            .setStringToFind(mySearchReplaceComponent.getSearchTextComponent().getText())
            .setCaseSensitive(myToggleCaseSensitiveAction.isSelected())
            .setWholeWords(myToggleWholeWordsAction.isSelected())
            .setRegularExpression(myToggleRegularExpression.isSelected())
            .setContextOneSideLength(CONTEXT_ONE_SIDE_LENGTH);

        if (!myCloseSearchResultsList.isEmpty() && myCloseSearchResultsList.getSelectedIndex() != -1) {
            Position position = myCloseSearchResultsList.getSelectedValue().startPosition;
            if (directionForward) {
                options.setSearchBounds(
                    //position.pageNumber, position.symbolOffsetInPage + 1,
                    position.pageNumber + 1, 0,
                    SearchTaskOptions.NO_LIMIT, SearchTaskOptions.NO_LIMIT);
            }
            else {
                options.setSearchBounds(SearchTaskOptions.NO_LIMIT, SearchTaskOptions.NO_LIMIT,
                    position.pageNumber, position.symbolOffsetInPage);
            }
        }
        else {
            long caretPageNumber = largeFileEditor.getCaretPageNumber();
            int caretPageOffset = largeFileEditor.getCaretPageOffset();
            if (directionForward) {
                options.setSearchBounds(caretPageNumber, caretPageOffset,
                    SearchTaskOptions.NO_LIMIT, SearchTaskOptions.NO_LIMIT);
            }
            else {
                options.setSearchBounds(SearchTaskOptions.NO_LIMIT, SearchTaskOptions.NO_LIMIT,
                    caretPageNumber, caretPageOffset);
            }
        }

        return options;
    }

    @Override
    public void tellSearchProgress(CloseSearchTask caller, long curPageNumber, long pagesAmount) {
        long time = System.currentTimeMillis();
        if (time - lastProgressStatusUpdateTime > PROGRESS_STATUS_UPDATE_PERIOD
            || curPageNumber == 0
            || curPageNumber == pagesAmount - 1) {
            lastProgressStatusUpdateTime = time;
            ApplicationManager.getApplication().invokeLater(() -> {
                if (!caller.isShouldStop()) {
                    setNewStatusText(FileEditorLocalize.largeFileEditorMessageSearchingAtSomePercentOfFile(Utils.calculatePagePositionPercent(curPageNumber, pagesAmount)));
                }
            });
        }
    }

    @Override
    public void tellClosestResultFound(CloseSearchTask caller, List<? extends SearchResult> allMatchesAtFrame,
                                       int indexOfClosestResult) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!caller.isShouldStop()) {
                setNewStatusText(LocalizeValue.of());
                SearchResult closestResult = allMatchesAtFrame.get(indexOfClosestResult);
                largeFileEditor.getEditorModel().showSearchResult(closestResult);
                largeFileEditor.getEditorModel().setHighlightingCloseSearchResultsEnabled(true);
            }
        });
    }

    @Override
    public void tellSearchIsFinished(CloseSearchTask caller, long lastScannedPageNumber) {
        ApplicationManager.getApplication().invokeLater(() -> {

            SearchTaskOptions options = caller.getOptions();
            if (!caller.isShouldStop()) {
                if (options.loopedPhase) {
                    setNewStatusText(FileEditorLocalize.largeFileEditorMessageSearchIsCompletedAndNoMoreMatches());
                    mySearchReplaceComponent.setNotFoundBackground();
                    if (!(largeFileEditor.getEditor().getHeaderComponent() instanceof SearchReplaceComponent)) {
                        LocalizeValue message = FileEditorLocalize.largeFileEditorMessageSomeStringNotFound(options.stringToFind);
                        showSimpleHintInEditor(message, largeFileEditor.getEditor());
                    }
                }
                else {
                    notFoundState = true;
                    AnAction action = ActionManager.getInstance().getAction(
                        options.searchForwardDirection ? IdeActions.ACTION_FIND_NEXT : IdeActions.ACTION_FIND_PREVIOUS);
                    String shortcutsText = KeymapUtil.getFirstKeyboardShortcutText(action);
                    LocalizeValue message;
                    setNewStatusText(LocalizeValue.of());
                    message = !shortcutsText.isEmpty()
                        ? options.searchForwardDirection
                        ? FileEditorLocalize.largeFileEditorSomeStringNotFoundPressSomeShortcutToSearchFromTheStart(options.stringToFind, shortcutsText)
                        : FileEditorLocalize.largeFileEditorSomeStringNotFoundPressSomeShortcutToSearchFromTheEnd(options.stringToFind, shortcutsText)
                        : options.searchForwardDirection
                        ? FileEditorLocalize.largeFileEditorSomeStringNotFoundPerformSomeActionAgainToSearchFromStart(options.stringToFind, action.getTemplatePresentation().getText())
                        : FileEditorLocalize.largeFileEditorSomeStringNotFoundPerformSomeActionAgainToSearchFromEnd(options.stringToFind, action.getTemplatePresentation().getText());
                    showSimpleHintInEditor(message, largeFileEditor.getEditor());
                }
            }
        });
    }

    private static void showSimpleHintInEditor(LocalizeValue message, Editor editor) {
        JComponent hintComponent = HintUtil.createInformationLabel(message.toString());
        LightweightHintFactory hintFactory = Application.get().getInstance(LightweightHintFactory.class);
        LightweightHint hint = hintFactory.create(hintComponent);

        ((HintManagerEx) HintManager.getInstance()).showEditorHint(hint,
            editor,
            HintManager.UNDER,
            HintManager.HIDE_BY_ANY_KEY |
                HintManager.HIDE_BY_TEXT_CHANGE |
                HintManager.HIDE_BY_SCROLLING,
            0, false);
    }

    @Override
    public void tellSearchWasStopped(CloseSearchTask caller, long curPageNumber) {
    }

    @Override
    public void tellSearchWasCatchedException(CloseSearchTask caller, IOException e) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!caller.isShouldStop()) {
                setNewStatusText(FileEditorLocalize.largeFileEditorMessageSearchStoppedBecauseSomethingWentWrong());
            }
        });
    }

    @Override
    public void onEscapePressed() {
        if (lastExecutedCloseSearchTask != null
            && !lastExecutedCloseSearchTask.isShouldStop()
            && !lastExecutedCloseSearchTask.isFinished()) {
            stopSearchTaskIfItExists();
            if (lastExecutedCloseSearchTask != null) {
                setNewStatusText(FileEditorLocalize.largeFileEditorMessageStoppedByUser());
            }
        }
        else {
            stopSearchTaskIfItExists();
            ProjectIdeFocusManager
                .getInstance(largeFileEditor.getProject())
                .requestFocus(largeFileEditor.getEditor().getContentComponent(), false);
            largeFileEditor.getEditorModel().setHighlightingCloseSearchResultsEnabled(false);
            if (largeFileEditor.getEditor().getHeaderComponent() instanceof SearchReplaceComponent) {
                largeFileEditor.getEditor().setHeaderComponent(null);
            }
        }
    }

    @Override
    public String getStatusText() {
        return myStatusText;
    }

    @Override
    public void updateStatusText() {
        if (myIsStatusTextHidden) {
            return;
        }

        if (System.currentTimeMillis() - myLastTimeStatusTextWasChanged > STATUS_TEXT_LIFE_TIME) {
            myStatusText = "";
            myIsStatusTextHidden = true;
        }
    }

    @Override
    public void updateSearchReplaceComponentActions() {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            mySearchReplaceComponent.updateActions();
        }
        else {
            ApplicationManager.getApplication().invokeLater(() -> mySearchReplaceComponent.updateActions());
        }
    }


    @RequiredUIAccess
    @Override
    public void onSearchParametersChanged() {
        if (lastExecutedCloseSearchTask != null) {
            lastExecutedCloseSearchTask.shouldStop();
            setNewStatusText(LocalizeValue.of());
        }
        mySearchReplaceComponent.setRegularBackground();
        largeFileEditor.getEditorModel().setHighlightingCloseSearchResultsEnabled(false);

        String stringToFind = mySearchReplaceComponent.getSearchTextComponent().getText();
        boolean isMultiline = stringToFind.contains("\n");
        mySearchReplaceComponent.update(stringToFind, "", false, isMultiline);
    }

    @Override
    public void onCaretPositionChanged(CaretEvent e) {
        if (myCloseSearchResultsList.getSelectedIndex() != -1
            && e.getEditor().getCaretModel().getOffset() != myCloseSearchResultsList.getSelectedValue().startPosition.symbolOffsetInPage
            && e.getEditor().getCaretModel().getOffset() != 0) {
            myCloseSearchResultsList.clearSelection();
        }
    }

    @Override
    public void dispose() {
        stopSearchTaskIfItExists();
    }

    @Override
    public List<SearchResult> getSearchResultsInPage(Page page) {
        SearchTaskOptions options = new SearchTaskOptions()
            .setStringToFind(mySearchReplaceComponent.getSearchTextComponent().getText())
            .setStringToFind(mySearchReplaceComponent.getSearchTextComponent().getText())
            .setCaseSensitive(myToggleCaseSensitiveAction.isSelected())
            .setWholeWords(myToggleWholeWordsAction.isSelected())
            .setRegularExpression(myToggleRegularExpression.isSelected())
            .setSearchDirectionForward(true)
            .setSearchBounds(page.getPageNumber(), SearchTaskOptions.NO_LIMIT,
                page.getPageNumber(), SearchTaskOptions.NO_LIMIT)
            .setContextOneSideLength(0);

        if (StringUtil.isEmpty(options.stringToFind)) {
            return null;
        }

        RangeSearch rangeSearch = new RangeSearch(
            getLargeFileEditor().getFile(), getLargeFileEditor().getProject(),
            new RangeSearchCallback() {
                @Override
                public FileDataProviderForSearch getFileDataProviderForSearch(boolean createIfNotExists, Project project, VirtualFile virtualFile) {
                    return fileDataProviderForSearch;
                }

                @Override
                public void showResultInEditor(SearchResult searchResult, Project project, VirtualFile virtualFile) {
                    // ignore
                }
            });

        rangeSearch.runNewSearch(options, fileDataProviderForSearch, false);
        return rangeSearch.getSearchResultsList();
    }

    @Override
    public boolean isSearchWorkingNow() {
        return lastExecutedCloseSearchTask != null && !lastExecutedCloseSearchTask.isFinished();
    }

    @Override
    public boolean canShowRegexSearchWarning() {
        if (!myToggleRegularExpression.isSelected()) {
            return false;
        }

        String stringToFind = mySearchReplaceComponent.getSearchTextComponent().getText();

        // "pageSize / 10", because it's strictly shorter then even full page consisted of only 4-byte symbols and much longer then simple stringsToFind
        return stringToFind.length() > largeFileEditor.getPageSize() / 10 ||
            stringToFind.contains("*") ||
            stringToFind.contains("+") ||
            stringToFind.contains("{");
    }

    private void createActions() {
        myNextOccurrenceAction = new LargeFilePrevNextOccurrenceAction(this, true);
        myPrevOccurrenceAction = new LargeFilePrevNextOccurrenceAction(this, false);
        myFindAllAction = new LargeFileFindAllAction(this);
        myFindForwardAction = new FindForwardBackwardAction(true, this);
        myFindBackwardAction = new FindForwardBackwardAction(false, this);
        myToggleCaseSensitiveAction = new LargeFileToggleAction(this, FileEditorLocalize.largeFileEditorMatchCaseActionMnemonicText());
        myToggleWholeWordsAction = new LargeFileToggleAction(this, FileEditorLocalize.largeFileEditorWordsActionMnemonicText()) {
            @Override
            public void update(@Nonnull AnActionEvent e) {
                boolean enabled = myToggleRegularExpression != null && !myToggleRegularExpression.isSelected(e);
                boolean visible = mySearchReplaceComponent == null || !mySearchReplaceComponent.isMultiline();
                e.getPresentation().setEnabled(enabled);
                e.getPresentation().setVisible(visible);
                setSelected(e, isSelected(e) && enabled && visible);
                super.update(e);
            }
        };
        myToggleRegularExpression = new LargeFileToggleAction(this, FileEditorLocalize.largeFileEditorRegexActionMnemonicText()) {
            @Override
            public void setSelected(boolean state) {
                super.setSelected(state);
                if (state && myToggleWholeWordsAction != null) {
                    myToggleWholeWordsAction.setSelected(false);
                }
            }
        };
        myStatusTextAction = new LargeFileStatusTextAction(this);
    }

    private void createSearchReplaceComponent() {
        mySearchReplaceComponent = SearchReplaceComponent
            .buildFor(largeFileEditor.getProject(),
                largeFileEditor.getEditor().getContentComponent())
            .addPrimarySearchActions(myPrevOccurrenceAction,
                myNextOccurrenceAction,
                new AnSeparator(),
                myFindAllAction,
                myFindBackwardAction,
                myFindForwardAction)
            .addExtraSearchActions(myToggleCaseSensitiveAction,
                myToggleWholeWordsAction,
                myToggleRegularExpression,
                new DefaultCustomComponentAction(
                    () -> RegExHelpPopup.createRegExLink(
                        new HtmlBuilder().append(HtmlChunk.text("?").bold()).wrapWithHtmlBody().toString(),
                        null,
                        null)),
                myStatusTextAction)
            //.addSearchFieldActions(new RestorePreviousSettingsAction())
            .withCloseAction(this::onEscapePressed)
            .build();
    }

    private void attachListenersToSearchReplaceComponent() {
        mySearchReplaceComponent.addListener(new SearchReplaceComponent.Listener() {
            @Override
            public void searchFieldDocumentChanged() {
                onSearchParametersChanged();
            }
        });
    }

    private JBList<SearchResult> createCloseSearchResultsList() {
        CollectionListModel<SearchResult> model = new CollectionListModel<>();
        JBList<SearchResult> list = new JBList<>(model);
        list.addListSelectionListener(new CloseSearchResultsListSelectionListener(list));
        return list;
    }

    private void stopSearchTaskIfItExists() {
        if (lastExecutedCloseSearchTask != null) {
            lastExecutedCloseSearchTask.shouldStop();
        }
    }

    private void setNewStatusText(@Nonnull LocalizeValue newStatusText) {
        myStatusText = newStatusText.get();
        myLastTimeStatusTextWasChanged = System.currentTimeMillis();

        myIsStatusTextHidden = StringUtil.isEmpty(newStatusText.get());

        updateSearchReplaceComponentActions();
    }

    private static int getNextOccurrenceIndexIfCan(boolean directionForward,
                                                   long currentPageNumber,
                                                   int caretPageOffset,
                                                   JBList<SearchResult> listResult) {
        ListModel<SearchResult> model = listResult.getModel();
        int index;
        SearchResult searchResult;

        if (model.getSize() == -1) {
            return -1;
        }

        if (listResult.getSelectedIndex() != -1) {

            index = listResult.getSelectedIndex();
            if (directionForward) {
                index++;
            }
            else {
                index--;
            }
        }
        else {

            index = 0;
            while (true) {
                if (index >= model.getSize()) {
                    if (directionForward) {
                        return -1;
                    }
                    else {
                        return model.getSize() - 1;
                    }
                }
                else {
                    searchResult = model.getElementAt(index);
                    if (currentPageNumber > searchResult.startPosition.pageNumber
                        || currentPageNumber == searchResult.startPosition.pageNumber
                        && caretPageOffset >= searchResult.startPosition.symbolOffsetInPage) {
                        index++;
                    }
                    else {
                        break;
                    }
                }
            }

            if (!directionForward) {
                index--;
            }
        }

        if (index < 0 || index >= model.getSize()) {
            return -1;
        }
        else {
            return index;
        }
    }


    private final class CloseSearchResultsListSelectionListener implements ListSelectionListener {
        private final JBList<SearchResult> list;

        CloseSearchResultsListSelectionListener(JBList<SearchResult> list) {
            this.list = list;
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) { // it happens when the selecting process is over and the selected position is set finaly
                SearchResult selectedSearchResult = list.getSelectedValue();
                if (selectedSearchResult != null) {
                    largeFileEditor.showSearchResult(selectedSearchResult);
                }
            }
        }
    }
}
