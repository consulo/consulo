/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ide.impl.searchEverywhere;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.util.registry.Registry;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ide.actions.searcheverywhere.SearchEverywhereManagerImpl;
import consulo.ide.impl.idea.ide.actions.searcheverywhere.UnifiedSearchEverywhereUI;
import consulo.project.Project;
import consulo.searchEverywhere.SearchEverywhereContributor;
import consulo.searchEverywhere.SearchEverywhereContributorFactory;
import consulo.searchEverywhere.SearchEverywhereManager;
import consulo.ui.HeavyPopup;
import consulo.ui.PopupOptions;
import consulo.ui.TextBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.SearchTextField;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * @author VISTALL
 * @since 2026-08-01
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedSearchEverywhereManager implements SearchEverywhereManager {
    private static final int HISTORY_LIMIT = 50;

    private record HistoryItem(String searchText, String contributorID) {
    }

    private final Project myProject;

    private @Nullable HeavyPopup myPopup;
    private @Nullable UnifiedSearchEverywhereUI mySearchEverywhereUI;

    private final List<HistoryItem> myHistory = new ArrayList<>();
    private final Map<String, Object> myPrevSelections = new HashMap<>();
    private List<String> myHistoryList = List.of();
    private String myHistoryContributorID = "";
    private int myHistoryIndex = -1;

    private volatile String myCurrentSearchText = "";
    private boolean myEverywhere;

    @Inject
    public UnifiedSearchEverywhereManager(Project project) {
        myProject = project;
    }

    @Override
    public boolean isShown() {
        return mySearchEverywhereUI != null && myPopup != null;
    }

    @Override
    @RequiredUIAccess
    public void show(String contributorID, @Nullable String searchText, AnActionEvent initEvent) {
        if (isShown()) {
            return;
        }

        List<SearchEverywhereContributor<?>> contributors = new ArrayList<>();
        myProject.getApplication().getExtensionPoint(SearchEverywhereContributorFactory.class)
            .collectMapped(contributors, factory -> factory.createContributor(initEvent));
        contributors.sort(Comparator.comparingInt(SearchEverywhereContributor::getSortWeight));

        UnifiedSearchEverywhereUI ui = new UnifiedSearchEverywhereUI(myProject, contributors, this);
        ui.switchToContributor(contributorID);

        resetHistory(contributorID);
        boolean suppressHistory = "true".equals(System.getProperty("idea.searchEverywhere.noHistory", "false"));
        suppressHistory = suppressHistory
            || (SearchEverywhereManagerImpl.ALL_CONTRIBUTORS_GROUP_ID.equals(contributorID) && Registry.is("search.everywhere.disable.history.for.all"));

        if (searchText == null && !suppressHistory) {
            searchText = prevHistoryItem();
        }

        if (searchText != null && !searchText.isEmpty()) {
            ui.getSearchField().setValue(searchText);
            ui.getSearchField().selectAll();
        }

        DumbAwareAction.create(e -> showHistoryItem(true))
            .registerCustomShortcutSet(SearchTextField.SHOW_HISTORY_SHORTCUT, ui.getComponent(), ui);
        DumbAwareAction.create(e -> showHistoryItem(false))
            .registerCustomShortcutSet(SearchTextField.ALT_SHOW_HISTORY_SHORTCUT, ui.getComponent(), ui);

        HeavyPopup popup = HeavyPopup.create(PopupOptions.builder().resizable().movable().build());
        popup.setContent(ui.getComponent());
        popup.addCloseListener(event -> onClosed(popup));

        ui.setSearchFinishedHandler(() -> {
            if (myPopup == popup && popup.isVisible()) {
                popup.close();
            }
        });
        ui.setViewTypeListener(() -> {
            if (myPopup == popup && popup.isVisible()) {
                popup.pack();
            }
        });

        mySearchEverywhereUI = ui;
        myPopup = popup;
        myCurrentSearchText = searchText == null ? "" : searchText;

        UIAccess uiAccess = UIAccess.current();
        ui.updateToolbarFuture().whenCompleteAsync((o, throwable) -> {
            if (myPopup != popup || myProject.isDisposed()) {
                return;
            }

            popup.showInCenterOf(myProject.getWindow());
            ui.onShown();
            ui.focusSearchField();
        }, uiAccess);
    }

    @RequiredUIAccess
    private void onClosed(HeavyPopup popup) {
        UnifiedSearchEverywhereUI ui = mySearchEverywhereUI;
        if (myPopup != popup || ui == null) {
            return;
        }

        saveSearchText(ui);

        myPopup = null;
        mySearchEverywhereUI = null;
        myCurrentSearchText = "";

        Disposer.dispose(ui);
    }

    @Override
    public String getSelectedContributorID() {
        UnifiedSearchEverywhereUI ui = checkIsShown();
        return ui.getSelectedContributorID();
    }

    @Override
    @RequiredUIAccess
    public void setSelectedContributor(String contributorID) {
        UnifiedSearchEverywhereUI ui = checkIsShown();
        if (!contributorID.equals(ui.getSelectedContributorID())) {
            ui.switchToContributor(contributorID);
        }
    }

    @Override
    @RequiredUIAccess
    public void toggleEverywhereFilter() {
        checkIsShown().toggleEverywhereFilter();
    }

    @Override
    public boolean isEverywhere() {
        return myEverywhere;
    }

    public void setEverywhere(boolean everywhere) {
        myEverywhere = everywhere;
    }

    @Override
    public String getCurrentSearchText() {
        return myCurrentSearchText;
    }

    public void setCurrentSearchText(String currentSearchText) {
        myCurrentSearchText = currentSearchText;
    }

    public @Nullable Object getPrevSelection(String contributorID) {
        return myPrevSelections.remove(contributorID);
    }

    private UnifiedSearchEverywhereUI checkIsShown() {
        UnifiedSearchEverywhereUI ui = mySearchEverywhereUI;
        if (ui == null || myPopup == null) {
            throw new IllegalStateException("Method should be called only when search popup is shown");
        }
        return ui;
    }

    @RequiredUIAccess
    private void saveSearchText(UnifiedSearchEverywhereUI ui) {
        String contributorID = ui.getSelectedContributorID();
        TextBox searchField = ui.getSearchField();
        String searchText = searchField.getValue();
        if (searchText != null && !searchText.isEmpty()) {
            myHistory.removeIf(item -> searchText.equals(item.searchText()) && contributorID.equals(item.contributorID()));
            myHistory.add(new HistoryItem(searchText, contributorID));

            List<HistoryItem> contributorHistory = myHistory.stream().filter(item -> item.contributorID().equals(contributorID)).toList();
            if (contributorHistory.size() > HISTORY_LIMIT) {
                myHistory.remove(contributorHistory.getFirst());
            }
        }
        myPrevSelections.put(contributorID, ui.getSelectionIdentity());
        resetHistory(contributorID);
    }

    @RequiredUIAccess
    private void showHistoryItem(boolean next) {
        UnifiedSearchEverywhereUI ui = mySearchEverywhereUI;
        if (ui == null) {
            return;
        }

        String contributorID = ui.getSelectedContributorID();
        if (!myHistoryContributorID.equals(contributorID)) {
            resetHistory(contributorID);
        }

        TextBox searchField = ui.getSearchField();
        searchField.setValue(next ? nextHistoryItem() : prevHistoryItem());
        searchField.selectAll();
    }

    private void resetHistory(String contributorID) {
        myHistoryContributorID = contributorID;
        myHistoryIndex = -1;

        List<String> texts = new ArrayList<>();
        for (HistoryItem item : myHistory) {
            if (SearchEverywhereManagerImpl.ALL_CONTRIBUTORS_GROUP_ID.equals(contributorID) || item.contributorID().equals(contributorID)) {
                texts.remove(item.searchText());
                texts.add(item.searchText());
            }
        }
        int size = texts.size();
        myHistoryList = size > HISTORY_LIMIT ? texts.subList(size - HISTORY_LIMIT, size) : texts;
    }

    private String nextHistoryItem() {
        if (myHistoryList.isEmpty()) {
            return "";
        }

        myHistoryIndex += 1;
        if (myHistoryIndex >= myHistoryList.size()) {
            myHistoryIndex = 0;
        }
        return myHistoryList.get(myHistoryIndex);
    }

    private String prevHistoryItem() {
        if (myHistoryList.isEmpty()) {
            return "";
        }

        myHistoryIndex -= 1;
        if (myHistoryIndex < 0) {
            myHistoryIndex = myHistoryList.size() - 1;
        }
        return myHistoryList.get(myHistoryIndex);
    }
}
