// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.openapi.diff.impl.combined.search;

import consulo.application.localize.ApplicationLocalize;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.event.FocusChangeListener;
import consulo.codeEditor.internal.EditorEventMulticasterEx;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.ui.console.ConsoleViewUtil;
import consulo.fileEditor.impl.internal.search.SearchSession;
import consulo.fileEditor.internal.SearchReplaceComponent;
import consulo.find.FindManager;
import consulo.find.FindModel;
import consulo.ide.impl.idea.find.FindUtil;
import consulo.ide.impl.idea.find.EditorSearchSession;
import consulo.ide.impl.idea.find.editorHeaderActions.NextOccurrenceAction;
import consulo.ide.impl.idea.find.editorHeaderActions.PrevOccurrenceAction;
import consulo.ide.impl.idea.find.editorHeaderActions.RestorePreviousSettingsAction;
import consulo.ide.impl.idea.find.editorHeaderActions.ToggleMatchCase;
import consulo.ide.impl.idea.find.editorHeaderActions.TogglePreserveCaseAction;
import consulo.ide.impl.idea.find.editorHeaderActions.ToggleRegex;
import consulo.ide.impl.idea.find.editorHeaderActions.ToggleWholeWordsOnlyAction;
import consulo.ide.impl.idea.find.impl.livePreview.LivePreviewController;
import consulo.ide.impl.idea.find.impl.livePreview.SearchResults;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class CombinedEditorSearchSession implements SearchSession, UiDataProvider {
    private final Project myProject;
    private Editor myCurrentEditor;
    private final Runnable myCloseAction;

    private final Disposable myDisposable = Disposable.newDisposable("CombinedEditorSearchSession");

    private FindModel myFindModel;
    private final FindModelObserver myModelObserver = new FindModelObserver();
    private final SearchReplaceComponent mySearchComponent;

    private int myCurrentSessionIndex;
    private Position myCurrentSessionPosition = Position.FIRST;
    private List<EditorSearchSessionHolder> myHolders = List.of();
    private EditorSearchSession myCurrentSession;

    private final List<CombinedEditorSearchSessionListener> myListeners = new CopyOnWriteArrayList<>();

    private enum Position {
        FIRST(0),
        SECOND(1),
        THIRD(2);

        private final int myIndex;

        Position(int index) {
            myIndex = index;
        }
    }

    private final class EditorSearchSessionHolder {
        private final List<EditorSearchSessionEx> mySessions;

        @RequiredUIAccess
        private EditorSearchSessionHolder(Project project, List<? extends Editor> editors) {
            List<EditorSearchSessionEx> sessions = new ArrayList<>();
            for (Editor editor : editors) {
                sessions.add(new EditorSearchSessionEx(editor, project, myFindModel, CombinedEditorSearchSession.this::close));
            }
            mySessions = sessions;
        }

        private boolean isSearchInProgress() {
            for (EditorSearchSessionEx session : mySessions) {
                if (session.isSearchInProgress()) {
                    return true;
                }
            }
            return false;
        }

        private boolean hasMatches() {
            for (EditorSearchSessionEx session : mySessions) {
                if (session.hasMatches()) {
                    return true;
                }
            }
            return false;
        }

        private int getMatchesCount() {
            int count = 0;
            for (EditorSearchSessionEx session : mySessions) {
                count += session.getSearchResults().getMatchesCount();
            }
            return count;
        }

        private void setMatchesLimit(int limit) {
            for (EditorSearchSessionEx session : mySessions) {
                session.getSearchResults().setMatchesLimit(limit);
            }
        }

        private void clearResults() {
            for (EditorSearchSessionEx session : mySessions) {
                session.getSearchResults().clear();
            }
        }

        private void addResultListener(SearchResults.SearchResultsListener listener) {
            for (EditorSearchSessionEx session : mySessions) {
                session.getSearchResults().addListener(listener);
            }
        }

        private void initLivePreview() {
            for (EditorSearchSessionEx session : mySessions) {
                session.initLivePreview();
            }
        }

        private void disableLivePreview() {
            for (EditorSearchSessionEx session : mySessions) {
                session.disableLivePreview();
            }
        }

        private void disposeLivePreview() {
            for (EditorSearchSessionEx session : mySessions) {
                session.disposeLivePreview();
            }
        }

        private void close() {
            for (EditorSearchSessionEx session : mySessions) {
                session.closeSession();
            }
        }

        private @Nullable EditorSearchSession getOrNull(Position position) {
            return position.myIndex < mySessions.size() ? mySessions.get(position.myIndex) : null;
        }

        private @Nullable Position indexOf(Editor editor) {
            for (int i = 0; i < mySessions.size(); i++) {
                if (mySessions.get(i).getEditor() == editor) {
                    for (Position position : Position.values()) {
                        if (position.myIndex == i) {
                            return position;
                        }
                    }
                }
            }
            return null;
        }
    }

    @RequiredUIAccess
    public CombinedEditorSearchSession(
        Project project,
        Editor currentEditor,
        Runnable closeAction,
        Component parentComponent,
        Disposable disposableParent
    ) {
        myProject = project;
        myCurrentEditor = currentEditor;
        myCloseAction = closeAction;

        Disposer.register(myDisposable, this::close);
        Disposer.register(disposableParent, myDisposable);

        myFindModel = EditorSearchSession.createDefaultFindModel(project, currentEditor);
        myCurrentSession = new EditorSearchSessionEx(currentEditor, project, myFindModel, this::close);

        mySearchComponent = SearchReplaceComponent.buildFor(project, parentComponent)
            .addPrimarySearchActions(EditorSearchSession.createPrimarySearchActions())
            .addExtraSearchActions(
                new ToggleMatchCase(),
                new ToggleWholeWordsOnlyAction(),
                new ToggleRegex())
            .addSearchFieldActions(new RestorePreviousSettingsAction())
            .addExtraReplaceAction(new TogglePreserveCaseAction())
            .addReplaceFieldActions(new PrevOccurrenceAction(false),
                new NextOccurrenceAction(false))
            .withDataProvider(this)
            .withCloseAction(this::close)
            .build();

        mySearchComponent.addListener(new MySearchComponentListener());
        registerEditorsFocusListener();
    }

    public void addListener(CombinedEditorSearchSessionListener listener) {
        myListeners.add(listener);
        Disposer.register(myDisposable, () -> myListeners.remove(listener));
    }

    @RequiredUIAccess
    public <H> void update(List<H> items, Function<H, List<? extends Editor>> mapper) {
        update(items, mapper, myCurrentEditor);
    }

    @RequiredUIAccess
    public <H> void update(List<H> items, Function<H, List<? extends Editor>> mapper, Editor currentEditor) {
        boolean emptyUpdate = items.isEmpty();
        boolean update = !myHolders.isEmpty() || !emptyUpdate;

        for (EditorSearchSessionHolder holder : myHolders) {
            holder.disposeLivePreview();
        }

        FindModel findModel = new FindModel();
        findModel.copyFrom(myFindModel);
        findModel.addObserver(myModelObserver);
        myFindModel = findModel;

        List<EditorSearchSessionHolder> holders = new ArrayList<>();
        for (H item : items) {
            List<? extends Editor> editors = mapper.apply(item);
            if (editors.isEmpty()) {
                continue;
            }
            EditorSearchSessionHolder holder = new EditorSearchSessionHolder(myProject, editors);
            holder.addResultListener(new MySearchResultsListener());
            holders.add(holder);
        }
        myHolders = holders;

        if (emptyUpdate) {
            updateUIWithResults(0, false);
        }
        else {
            updateCurrentState(currentEditor);
        }

        if (update) {
            for (EditorSearchSessionHolder holder : myHolders) {
                holder.initLivePreview();
            }
            EditorSearchSession.updateEmptyText(getComponent(), myFindModel, null);
        }

        EditorSearchSession.updateUIWithFindModel(getComponent(), myFindModel, !emptyUpdate ? myCurrentEditor : null);
    }

    private final class FindModelObserver implements FindModel.FindModelObserver {
        private boolean myUpdating;

        @Override
        @RequiredUIAccess
        public void findModelChanged(FindModel model) {
            if (myUpdating) {
                return;
            }
            try {
                myUpdating = true;
                EditorSearchSession.updateUIWithFindModel(getComponent(), model, myCurrentEditor);
                updateResults();
                FindUtil.updateFindInFileModel(myProject, model, !ConsoleViewUtil.isConsoleViewEditor(myCurrentEditor));
            }
            finally {
                myUpdating = false;
            }
        }
    }

    private void registerEditorsFocusListener() {
        if (EditorFactory.getInstance().getEventMulticaster() instanceof EditorEventMulticasterEx multicaster) {
            multicaster.addFocusChangeListener(new MyEditorFocusListener(), myDisposable);
        }
    }

    private final class MyEditorFocusListener implements FocusChangeListener {
        @Override
        public void focusGained(Editor editor) {
            if (editor != myCurrentEditor) {
                updateCurrentState(editor);
            }
        }
    }

    private void updateCurrentState(Editor newCurrentEditor) {
        for (int sessionIndex = 0; sessionIndex < myHolders.size(); sessionIndex++) {
            Position newPosition = myHolders.get(sessionIndex).indexOf(newCurrentEditor);
            if (newPosition == null) {
                continue;
            }

            updateCurrentState(sessionIndex, newPosition);
            break;
        }
    }

    private void updateCurrentState(int sessionIndex, Position sessionPosition) {
        EditorSearchSession newCurSession = myHolders.get(sessionIndex).getOrNull(sessionPosition);
        if (newCurSession == null) {
            return;
        }

        myCurrentSessionPosition = sessionPosition;
        myCurrentSessionIndex = sessionIndex;
        myCurrentSession = newCurSession;
        myCurrentEditor = newCurSession.getEditor();
    }

    @RequiredUIAccess
    private void updateResults() {
        String text = myFindModel.getStringToFind();
        if (text.isEmpty()) {
            nothingToSearchFor();
            return;
        }

        if (myFindModel.isRegularExpressions()) {
            try {
                Pattern.compile(text);
            }
            catch (PatternSyntaxException e) {
                getComponent().setNotFoundBackground();
                for (EditorSearchSessionHolder holder : myHolders) {
                    holder.clearResults();
                }
                getComponent().setStatusText(INCORRECT_REGEX_MESSAGE);
                return;
            }

            if (text.matches("\\|+")) {
                nothingToSearchFor();
                getComponent().setStatusText(ApplicationLocalize.editorsearchEmptyStringMatches().get());
                return;
            }
        }

        FindManager findManager = FindManager.getInstance(myProject);
        findManager.setFindWasPerformed();
        FindModel copy = new FindModel();
        copy.copyFrom(myFindModel);
        copy.setReplaceState(false);
        findManager.setFindNextModel(copy);
    }

    @RequiredUIAccess
    private void nothingToSearchFor() {
        updateUIWithResults(0, true);
        for (EditorSearchSessionHolder holder : myHolders) {
            holder.clearResults();
        }
    }

    @RequiredUIAccess
    private void updateUIWithResults(int matches, boolean regularBackground) {
        if (regularBackground) {
            getComponent().setRegularBackground();
        }
        else {
            getComponent().setNotFoundBackground();
        }
        for (CombinedEditorSearchSessionListener listener : myListeners) {
            listener.statusTextChanged(matches, myHolders.size());
        }
    }

    public void setStatusText(String status) {
        getComponent().setStatusText(status);
    }

    @Override
    public FindModel getFindModel() {
        return myFindModel;
    }

    @Override
    public SearchReplaceComponent getComponent() {
        return mySearchComponent;
    }

    @Override
    public boolean hasMatches() {
        for (EditorSearchSessionHolder holder : myHolders) {
            if (holder.hasMatches()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isSearchInProgress() {
        for (EditorSearchSessionHolder holder : myHolders) {
            if (holder.isSearchInProgress()) {
                return true;
            }
        }
        return false;
    }

    @Override
    @RequiredUIAccess
    public void searchForward() {
        search(true, myCurrentSessionIndex, myCurrentSessionPosition);
    }

    @Override
    @RequiredUIAccess
    public void searchBackward() {
        search(false, myCurrentSessionIndex, myCurrentSessionPosition);
    }

    @RequiredUIAccess
    private void search(boolean forward, int curSessionIdx, Position curSessionPosition) {
        if (curSessionIdx < 0 || curSessionIdx >= myHolders.size()) {
            return;
        }

        EditorSearchSession curSession = myHolders.get(curSessionIdx).getOrNull(curSessionPosition);

        if (curSession != null && canSearch(curSession, forward)) {
            search(curSession, forward);
            return;
        }

        int furtherIdx = curSessionIdx + (forward ? 1 : -1);

        if (furtherIdx < 0 || furtherIdx >= myHolders.size()) {
            return;
        }

        curSession = myHolders.get(furtherIdx).getOrNull(curSessionPosition);

        if (curSession != null && curSession.hasMatches()) {
            for (CombinedEditorSearchSessionListener listener : myListeners) {
                listener.onSearch(forward, curSession.getEditor());
            }
            // these ensure the right position of SearchResults.myCursor
            if (forward) {
                moveCaretToStart(curSession);
            }
            else {
                moveCaretToEnd(curSession);
            }
            updateCurrentState(furtherIdx, curSessionPosition);
            if (canSearch(curSession, forward)) {
                search(curSession, forward);
            }
        }
        else {
            search(forward, furtherIdx, curSessionPosition);
        }
    }

    private static void search(EditorSearchSession session, boolean forward) {
        if (forward) {
            session.searchForward();
        }
        else {
            session.searchBackward();
        }
    }

    private static void moveCaretToStart(EditorSearchSession session) {
        if (session.hasMatches()) {
            session.getEditor().getCaretModel().moveToOffset(0);
        }
    }

    private static void moveCaretToEnd(EditorSearchSession session) {
        if (session.hasMatches()) {
            session.getEditor().getCaretModel().moveToOffset(session.getEditor().getDocument().getTextLength());
        }
    }

    private static boolean hasSingleOccurence(EditorSearchSession session) {
        return session.getSearchResults().getOccurrences().size() == 1;
    }

    private static boolean canSearch(EditorSearchSession session, boolean forward) {
        if (!session.hasMatches()) {
            return false;
        }

        SearchResults searchResults = session.getSearchResults();
        if (hasSingleOccurence(session) && searchResults.getOccurrenceAtCaret() != null) {
            return false;
        }

        return (forward ? searchResults.firstOccurrenceAfterCaret() : searchResults.firstOccurrenceBeforeCaret()) != null;
    }

    @Override
    @RequiredUIAccess
    public void close() {
        myCloseAction.run();
        for (EditorSearchSessionHolder holder : myHolders) {
            holder.close();
        }
    }

    private final class MySearchComponentListener implements SearchReplaceComponent.Listener {
        @RequiredUIAccess
        @Override
        public void searchFieldDocumentChanged() {
            for (EditorSearchSessionHolder holder : myHolders) {
                holder.setMatchesLimit(LivePreviewController.MATCHES_LIMIT);
            }
            String text = getComponent().getSearchText();
            myFindModel.setStringToFind(text);
            updateResults();
            myFindModel.setMultiline(getComponent().getSearchText().contains("\n") || getComponent().getReplaceText().contains("\n"));
        }

        @RequiredUIAccess
        @Override
        public void replaceFieldDocumentChanged() {
            for (EditorSearchSessionHolder holder : myHolders) {
                holder.setMatchesLimit(LivePreviewController.MATCHES_LIMIT);
            }
            myFindModel.setStringToReplace(getComponent().getReplaceText());
            myFindModel.setMultiline(getComponent().getSearchText().contains("\n") || getComponent().getReplaceText().contains("\n"));
        }

        @RequiredUIAccess
        @Override
        public void multilineStateChanged() {
            myFindModel.setMultiline(getComponent().isMultiline());
        }

        @RequiredUIAccess
        @Override
        public void componentShown() {
            for (EditorSearchSessionHolder holder : myHolders) {
                holder.initLivePreview();
            }
        }

        @RequiredUIAccess
        @Override
        public void componentHidden() {
            for (EditorSearchSessionHolder holder : myHolders) {
                holder.disableLivePreview();
            }
        }
    }

    private final class MySearchResultsListener implements SearchResults.SearchResultsListener {
        @Override
        @RequiredUIAccess
        public void searchResultsUpdated(SearchResults sr) {
            if (sr.getFindModel() == null) {
                return;
            }

            if (getComponent().getSearchText().isEmpty()) {
                updateUIWithResults(0, true);
                return;
            }

            int matches = 0;
            for (EditorSearchSessionHolder holder : myHolders) {
                matches += holder.getMatchesCount();
            }

            updateUIWithResults(matches, matches > 0);

            getComponent().updateActions();
        }

        @Override
        public void cursorMoved() {
            getComponent().updateActions();
        }
    }

    @Override
    public void uiDataSnapshot(DataSink sink) {
        sink.set(SearchSession.KEY, this);
        myCurrentSession.uiDataSnapshot(sink);
    }

    private static final class EditorSearchSessionEx extends EditorSearchSession {
        private final Runnable myCombinedSessionClose;

        @RequiredUIAccess
        private EditorSearchSessionEx(Editor editor, Project project, FindModel findModel, Runnable combinedSessionClose) {
            super(editor, project, findModel);
            myCombinedSessionClose = combinedSessionClose;
        }

        @Override
        @RequiredUIAccess
        public void close() {
            myCombinedSessionClose.run();
        }

        private void closeSession() {
            disposeLivePreview();
        }

        @Override
        public void disposeLivePreview() {
            super.disposeLivePreview();
        }
    }
}
