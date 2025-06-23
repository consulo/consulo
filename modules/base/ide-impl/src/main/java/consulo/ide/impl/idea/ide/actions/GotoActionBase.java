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

package consulo.ide.impl.idea.ide.actions;

import consulo.application.impl.internal.IdeaModalityState;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.ide.impl.idea.ide.actions.searcheverywhere.SearchEverywhereManager;
import consulo.ide.impl.idea.ide.util.gotoByName.*;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.ui.IdeEventQueueProxy;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author msk
 */
public abstract class GotoActionBase extends AnAction {
    private static final Logger LOG = Logger.getInstance(GotoActionBase.class);

    protected static Class myInAction = null;
    private static final Map<Class, Pair<String, Integer>> ourLastStrings = new HashMap<>();
    private static final Map<Class, List<String>> ourHistory = new HashMap<>();
    private int myHistoryIndex = 0;

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        LOG.assertTrue(!getClass().equals(myInAction));
        try {
            myInAction = getClass();
            List<String> strings = ourHistory.get(myInAction);
            myHistoryIndex = strings == null || strings.size() <= 1 || !ourLastStrings.containsKey(myInAction) ? 0 : 1;
            gotoActionPerformed(e);
        }
        catch (Throwable t) {
            LOG.error(t);
            myInAction = null;
        }
    }

    protected abstract void gotoActionPerformed(AnActionEvent e);

    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        DataContext dataContext = event.getDataContext();
        Project project = dataContext.getData(Project.KEY);
        presentation.setEnabled(
            !getClass().equals(myInAction)
                && (!requiresProject() || project != null)
                && hasContributors(dataContext)
        );
        presentation.setVisible(hasContributors(dataContext));
    }

    protected boolean hasContributors(DataContext dataContext) {
        return true;
    }

    protected boolean requiresProject() {
        return true;
    }

    @Nullable
    public static PsiElement getPsiContext(AnActionEvent e) {
        PsiFile file = e.getData(PsiFile.KEY);
        if (file != null) {
            return file;
        }
        Project project = e.getData(Project.KEY);
        return getPsiContext(project);
    }

    @Nullable
    public static PsiElement getPsiContext(Project project) {
        if (project == null) {
            return null;
        }
        Editor selectedEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (selectedEditor == null) {
            return null;
        }
        Document document = selectedEditor.getDocument();
        return PsiDocumentManager.getInstance(project).getPsiFile(document);
    }

    protected abstract static class GotoActionCallback<T> {
        @Nullable
        protected ChooseByNameFilter<T> createFilter(@Nonnull ChooseByNamePopup popup) {
            return null;
        }

        public abstract void elementChosen(ChooseByNamePopup popup, Object element);
    }

    protected static Pair<String, Integer> getInitialText(boolean useEditorSelection, AnActionEvent e) {
        String predefined = e.getData(PlatformDataKeys.PREDEFINED_TEXT);
        if (!StringUtil.isEmpty(predefined)) {
            return Pair.create(predefined, 0);
        }
        if (useEditorSelection) {
            Editor editor = e.getData(Editor.KEY);
            if (editor != null) {
                String selectedText = editor.getSelectionModel().getSelectedText();
                if (selectedText != null && !selectedText.contains("\n")) {
                    return Pair.create(selectedText, 0);
                }
            }
        }

        String query = e.getData(SpeedSearchSupply.SPEED_SEARCH_CURRENT_QUERY);
        if (!StringUtil.isEmpty(query)) {
            return Pair.create(query, 0);
        }

        Project project = e == null ? null : e.getData(Project.KEY);
        Component focusOwner = ProjectIdeFocusManager.getInstance(project).getFocusOwner();
        if (focusOwner instanceof JComponent jComponent) {
            SpeedSearchSupply supply = SpeedSearchSupply.getSupply(jComponent);
            if (supply != null) {
                return Pair.create(supply.getEnteredPrefix(), 0);
            }
        }

        if (myInAction != null) {
            Pair<String, Integer> lastString = ourLastStrings.get(myInAction);
            if (lastString != null) {
                return lastString;
            }
        }

        return Pair.create("", 0);
    }

    @Nullable
    public static String getInitialTextForNavigation(@Nonnull AnActionEvent e) {
        Editor editor = e.getData(Editor.KEY);
        String selectedText = editor != null ? editor.getSelectionModel().getSelectedText() : null;
        if (selectedText == null) {
            //selectedText = e.getData(JBTerminalWidget.SELECTED_TEXT_DATA_KEY);
        }
        return selectedText != null && !selectedText.contains("\n") ? selectedText : null;
    }

    protected <T> void showNavigationPopup(AnActionEvent e, ChooseByNameModel model, GotoActionCallback<T> callback) {
        showNavigationPopup(e, model, callback, true);
    }

    protected <T> void showNavigationPopup(
        AnActionEvent e,
        ChooseByNameModel model,
        GotoActionCallback<T> callback,
        boolean allowMultipleSelection
    ) {
        showNavigationPopup(e, model, callback, null, true, allowMultipleSelection);
    }

    protected <T> void showNavigationPopup(
        AnActionEvent e,
        ChooseByNameModel model,
        GotoActionCallback<T> callback,
        @Nullable String findUsagesTitle,
        boolean useSelectionFromEditor
    ) {
        showNavigationPopup(e, model, callback, findUsagesTitle, useSelectionFromEditor, true);
    }

    protected <T> void showNavigationPopup(
        AnActionEvent e,
        ChooseByNameModel model,
        GotoActionCallback<T> callback,
        @Nullable String findUsagesTitle,
        boolean useSelectionFromEditor,
        boolean allowMultipleSelection
    ) {
        showNavigationPopup(
            e,
            model,
            callback,
            findUsagesTitle,
            useSelectionFromEditor,
            allowMultipleSelection,
            new DefaultChooseByNameItemProvider(getPsiContext(e))
        );
    }

    protected <T> void showNavigationPopup(
        AnActionEvent e,
        ChooseByNameModel model,
        GotoActionCallback<T> callback,
        @Nullable String findUsagesTitle,
        boolean useSelectionFromEditor,
        boolean allowMultipleSelection,
        DefaultChooseByNameItemProvider itemProvider
    ) {
        Project project = e.getData(Project.KEY);
        boolean mayRequestOpenInCurrentWindow =
            model.willOpenEditor() && FileEditorManagerEx.getInstanceEx(project).hasSplitOrUndockedWindows();
        Pair<String, Integer> start = getInitialText(useSelectionFromEditor, e);
        showNavigationPopup(
            callback,
            findUsagesTitle,
            ChooseByNamePopup.createPopup(
                project,
                model,
                itemProvider,
                start.first,
                mayRequestOpenInCurrentWindow,
                start.second
            ),
            allowMultipleSelection
        );
    }

    protected <T> void showNavigationPopup(GotoActionCallback<T> callback, @Nullable String findUsagesTitle, ChooseByNamePopup popup) {
        showNavigationPopup(callback, findUsagesTitle, popup, true);
    }

    protected <T> void showNavigationPopup(
        GotoActionCallback<T> callback,
        @Nullable String findUsagesTitle,
        ChooseByNamePopup popup,
        boolean allowMultipleSelection
    ) {
        Class startedAction = myInAction;
        LOG.assertTrue(startedAction != null);

        popup.setCheckBoxShortcut(getShortcutSet());
        popup.setFindUsagesTitle(findUsagesTitle);
        ChooseByNameFilter<T> filter = callback.createFilter(popup);

        if (historyEnabled() && popup.getAdText() == null) {
            popup.setAdText(
                "Press " + KeymapUtil.getKeystrokeText(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_MASK)) +
                    " or " + KeymapUtil.getKeystrokeText(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_MASK)) +
                    " to navigate through the history"
            );
        }

        popup.invoke(
            new ChooseByNamePopupComponent.Callback() {
                @Override
                public void onClose() {
                    //noinspection ConstantConditions
                    if (startedAction != null && startedAction.equals(myInAction)) {
                        String text = popup.getEnteredText();
                        ourLastStrings.put(myInAction, Pair.create(text, popup.getSelectedIndex()));
                        updateHistory(text);
                        myInAction = null;
                    }
                    if (filter != null) {
                        filter.close();
                    }
                }

                private void updateHistory(@Nullable String text) {
                    if (!StringUtil.isEmptyOrSpaces(text)) {
                        List<String> history = ourHistory.get(myInAction);
                        if (history == null) {
                            history = new ArrayList<>();
                        }
                        if (!text.equals(ContainerUtil.getFirstItem(history))) {
                            history.add(0, text);
                        }
                        ourHistory.put(myInAction, history);
                    }
                }

                @Override
                public void elementChosen(Object element) {
                    callback.elementChosen(popup, element);
                }
            },
            IdeaModalityState.current(),
            allowMultipleSelection
        );

        JTextField editor = popup.getTextField();

        DocumentAdapter historyResetListener = new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                myHistoryIndex = 0;
            }
        };

        abstract class HistoryAction extends DumbAwareAction {
            @Override
            @RequiredUIAccess
            public void update(@Nonnull AnActionEvent e) {
                e.getPresentation().setEnabled(historyEnabled());
            }

            void setText(@Nonnull List<String> strings) {
                javax.swing.text.Document document = editor.getDocument();
                document.removeDocumentListener(historyResetListener);
                editor.setText(strings.get(myHistoryIndex));
                document.addDocumentListener(historyResetListener);
                editor.selectAll();
            }
        }

        editor.getDocument().addDocumentListener(historyResetListener);

        new HistoryAction() {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                List<String> strings = ourHistory.get(myInAction);
                setText(strings);
                myHistoryIndex = myHistoryIndex >= strings.size() - 1 ? 0 : myHistoryIndex + 1;
            }

        }.registerCustomShortcutSet(CustomShortcutSet.fromString("ctrl UP"), editor);

        new HistoryAction() {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                List<String> strings = ourHistory.get(myInAction);
                setText(strings);
                myHistoryIndex = myHistoryIndex <= 0 ? strings.size() - 1 : myHistoryIndex - 1;
            }
        }.registerCustomShortcutSet(CustomShortcutSet.fromString("ctrl DOWN"), editor);
    }

    protected void showInSearchEverywherePopup(String searchProviderID, AnActionEvent event, boolean useEditorSelection) {
        showInSearchEverywherePopup(searchProviderID, event, useEditorSelection, false);
    }

    protected void showInSearchEverywherePopup(
        @Nonnull String searchProviderID,
        @Nonnull AnActionEvent event,
        boolean useEditorSelection,
        boolean sendStatistics
    ) {
        Project project = event.getData(Project.KEY);
        if (project == null) {
            return;
        }
        SearchEverywhereManager seManager = SearchEverywhereManager.getInstance(project);
        FeatureUsageTracker.getInstance().triggerFeatureUsed(IdeActions.ACTION_SEARCH_EVERYWHERE);

        if (seManager.isShown()) {
            if (searchProviderID.equals(seManager.getSelectedContributorID())) {
                seManager.toggleEverywhereFilter();
            }
            else {
                seManager.setSelectedContributor(searchProviderID);
            }
            return;
        }

        IdeEventQueueProxy.getInstance().closeAllPopups(false);
        String searchText = StringUtil.nullize(getInitialText(useEditorSelection, event).first);
        seManager.show(searchProviderID, searchText, event);
    }

    private static boolean historyEnabled() {
        return !ContainerUtil.isEmpty(ourHistory.get(myInAction));
    }
}
