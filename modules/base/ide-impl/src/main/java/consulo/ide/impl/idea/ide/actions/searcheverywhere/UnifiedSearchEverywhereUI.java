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
package consulo.ide.impl.idea.ide.actions.searcheverywhere;

import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.matcher.MinusculeMatcher;
import consulo.component.messagebus.MessageBusConnection;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.ide.actions.bigPopup.ShowFilterAction;
import consulo.ide.impl.idea.ide.util.gotoByName.SearchEverywhereConfiguration;
import consulo.ide.impl.idea.ui.IdeUICustomization;
import consulo.ide.impl.searchEverywhere.UnifiedSearchEverywhereManager;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.event.DumbModeListener;
import consulo.searchEverywhere.*;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.details.ModifiedInputDetails;
import consulo.ui.event.details.MouseInputDetails;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.ui.ex.awt.action.CustomComponentAction;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.TabbedLayoutStyle;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.Couple;
import org.jspecify.annotations.Nullable;

import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
public class UnifiedSearchEverywhereUI implements Disposable, UiDataProvider {
    public static final int SINGLE_CONTRIBUTOR_ELEMENTS_LIMIT = 30;
    public static final int MULTIPLE_CONTRIBUTORS_ELEMENTS_LIMIT = 15;
    public static final int THROTTLING_TIMEOUT = 100;

    private static final float WIDTH = 46;
    private static final float HEIGHT = 30;

    private static final Key<AdvancedLabel> TEXT_KEY = Key.create("UnifiedSearchEverywhereUI.text");
    private static final Key<Label> SUFFIX_KEY = Key.create("UnifiedSearchEverywhereUI.suffix");
    private static final Key<ToggleSwitch> SWITCH_KEY = Key.create("UnifiedSearchEverywhereUI.switch");

    private final Project myProject;
    private final UnifiedSearchEverywhereManager myManager;
    private final UIAccess myUIAccess;
    private final List<? extends SearchEverywhereContributor<?>> myShownContributors;
    private final PersistentSearchEverywhereContributorFilter<String> myContributorsFilter;

    private final UnifiedSearchListModel myListModel = new UnifiedSearchListModel();
    private final List<SETab> myTabs = new ArrayList<>();
    private SETab mySelectedTab;

    private boolean myEverywhereAutoSet = true;
    private @Nullable String myNotFoundString;

    private final MultiThreadSearcher mySearcher;
    private @Nullable ProgressIndicator mySearchProgressIndicator;

    private final List<SearchEverywhereFoundElementInfo> myPendingAdded = new ArrayList<>();
    private final List<SearchEverywhereFoundElementInfo> myPendingRemoved = new ArrayList<>();
    private @Nullable ScheduledFuture<?> myFlushFuture;

    private final DockLayout myRootLayout;
    private final TabbedLayout myTabbedLayout;
    private final TextBox mySearchField;
    private final ListBox<SearchEverywhereFoundElementInfo> myResultsList;
    private final DockLayout myResultsLayout;
    private final ActionToolbar myToolbar;
    private @Nullable ActionToolbar myRightActionsToolbar;

    private boolean mySwitchingTab;
    private boolean myApplyingResults;
    private boolean myFullView = true;
    private boolean myFullViewRequested = true;
    private boolean myShown;
    private boolean myDisposed;

    private @Nullable Object mySelectedElement;
    private boolean myMoreSelected;

    private Runnable mySearchFinishedHandler = () -> {
    };
    private Runnable myViewTypeListener = () -> {
    };

    @RequiredUIAccess
    public UnifiedSearchEverywhereUI(
        Project project,
        List<? extends SearchEverywhereContributor<?>> contributors,
        UnifiedSearchEverywhereManager manager
    ) {
        myProject = project;
        myManager = manager;
        myUIAccess = UIAccess.current();
        myShownContributors = contributors;

        mySearcher = new MultiThreadSearcher(mySearchListener, myUIAccess, SEResultsEqualityProvider.getProviders(), true);

        Map<String, String> namesMap = ContainerUtil.map2Map(contributors, c -> Couple.of(c.getSearchProviderId(), c.getFullGroupName()));
        myContributorsFilter = new PersistentSearchEverywhereContributorFilter<>(
            ContainerUtil.map(contributors, SearchEverywhereContributor::getSearchProviderId),
            SearchEverywhereConfiguration.getInstance(project),
            namesMap::get,
            c -> null
        );

        myRootLayout = DockLayout.create(Space.NONE);
        myRootLayout.putUserData(UiDataProvider.KEY, this);

        myTabbedLayout = TabbedLayout.create();
        myTabbedLayout.addStyle(TabbedLayoutStyle.NO_PADDING);

        mySearchField = TextBox.create();
        mySearchField.addStyle(TextBoxStyle.INPLACE);
        mySearchField.setPrefixComponent(ImageBox.create(PlatformIconGroup.actionsSearch()));

        myResultsList = ListBox.create(myListModel.getDataModel());
        myResultsList.setRenderer(ComponentItemRenderer.reusable(this::createRow, this::bindRow));
        myResultsList.addValueListener(event -> {
            SearchEverywhereFoundElementInfo info = myResultsList.getValue();
            if (info != null && UnifiedSearchListModel.isTitle(info)) {
                SearchEverywhereFoundElementInfo first = myListModel.firstItemOfGroup(info.getContributor());
                if (first != null) {
                    myResultsList.setValue(first);
                }
                return;
            }
            if (!myApplyingResults) {
                saveSelection();
            }
        });
        myResultsList.addClickListener(event -> {
            InputDetails details = event.getInputDetails();
            if (details instanceof MouseInputDetails mouseDetails) {
                SearchEverywhereFoundElementInfo info = myResultsList.getValue();
                if (info != null && !UnifiedSearchListModel.isTitle(info)) {
                    elementsSelected(info, toModifiers(mouseDetails));
                }
            }
        });

        ActionGroup.Builder actionGroup = ActionGroup.newImmutableBuilder();
        actionGroup.add(new DumbAwareActionGroup() {
            @Override
            public AnAction[] getChildren(@Nullable AnActionEvent e) {
                if (mySelectedTab == null) {
                    return EMPTY_ARRAY;
                }
                return mySelectedTab.actions.stream().filter(UnifiedSearchEverywhereUI::isShownInToolbar).toArray(AnAction[]::new);
            }
        });
        myToolbar = ActionToolbarFactory.getInstance()
            .createActionToolbar("search.everywhere.toolbar", actionGroup.build(), ActionToolbar.Style.HORIZONTAL);
        myToolbar.setTargetUIComponent(myRootLayout);
        myToolbar.getUIComponent().paddingBuilder().topReset().bottomReset().apply();

        createTabs();
        myTabbedLayout.setSuffixComponent(myToolbar.getUIComponent());

        DockLayout searchRow = DockLayout.create(Space.NONE);
        searchRow.center(mySearchField);
        searchRow.borderBuilder().bottomSet().apply();

        DockLayout tabsRow = DockLayout.create(Space.NONE);
        tabsRow.center(myTabbedLayout);

        VerticalLayout topLayout = VerticalLayout.create(Space.NONE);
        topLayout.add(tabsRow);
        topLayout.add(searchRow);

        myResultsLayout = DockLayout.create(Space.NONE);
        myResultsLayout.center(myResultsList);

        myRootLayout.top(topLayout);
        myRootLayout.center(myResultsLayout);
        myRootLayout.setMinWidth(Length.ofFont(WIDTH));
        myResultsLayout.setMinHeight(Length.ofFont(HEIGHT));

        initSearchActions();
    }

    public Component getComponent() {
        return myRootLayout;
    }

    public TextBox getSearchField() {
        return mySearchField;
    }

    public void setSearchFinishedHandler(Runnable searchFinishedHandler) {
        mySearchFinishedHandler = searchFinishedHandler;
    }

    public void setViewTypeListener(Runnable viewTypeListener) {
        myViewTypeListener = viewTypeListener;
    }

    public CompletableFuture<?> updateToolbarFuture() {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        futures.add(myToolbar.updateActionsAsync());
        if (myRightActionsToolbar != null) {
            futures.add(myRightActionsToolbar.updateActionsAsync());
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @RequiredUIAccess
    public void focusSearchField() {
        mySearchField.focus();
        mySearchField.selectAll();
    }

    @RequiredUIAccess
    public void onShown() {
        myShown = true;
        applyViewType(myFullViewRequested);
    }

    @RequiredUIAccess
    public void toggleEverywhereFilter() {
        myEverywhereAutoSet = false;
        if (mySelectedTab.everywhereAction == null) {
            return;
        }
        if (!mySelectedTab.everywhereAction.canToggleEverywhere()) {
            return;
        }
        mySelectedTab.everywhereAction.setEverywhere(!mySelectedTab.everywhereAction.isEverywhere());
        myToolbar.updateActionsAsync();
    }

    @RequiredUIAccess
    private void setEverywhereAuto(boolean everywhere) {
        myEverywhereAutoSet = true;
        if (mySelectedTab.everywhereAction == null) {
            return;
        }
        if (!mySelectedTab.everywhereAction.canToggleEverywhere()) {
            return;
        }
        mySelectedTab.everywhereAction.setEverywhere(everywhere);
        myToolbar.updateActionsAsync();
    }

    private boolean isEverywhere() {
        return mySelectedTab.everywhereAction == null || mySelectedTab.everywhereAction.isEverywhere();
    }

    private boolean canToggleEverywhere() {
        return mySelectedTab.everywhereAction != null && mySelectedTab.everywhereAction.canToggleEverywhere();
    }

    @RequiredUIAccess
    public void switchToContributor(String contributorID) {
        SETab selectedTab = myTabs.stream()
            .filter(tab -> tab.getID().equals(contributorID))
            .findAny()
            .orElseThrow(() -> new IllegalArgumentException(String.format("Contributor %s is not supported", contributorID)));
        switchToTab(selectedTab);
    }

    @RequiredUIAccess
    private void switchToNextTab() {
        int currentIndex = myTabs.indexOf(mySelectedTab);
        SETab nextTab = currentIndex == myTabs.size() - 1 ? myTabs.get(0) : myTabs.get(currentIndex + 1);
        switchToTab(nextTab);
    }

    @RequiredUIAccess
    private void switchToPrevTab() {
        int currentIndex = myTabs.indexOf(mySelectedTab);
        SETab prevTab = currentIndex == 0 ? myTabs.get(myTabs.size() - 1) : myTabs.get(currentIndex - 1);
        switchToTab(prevTab);
    }

    @RequiredUIAccess
    private void switchToTab(SETab tab) {
        mySelectedTab = tab;

        if (myTabbedLayout.getSelectedTab() != tab.tab) {
            mySwitchingTab = true;
            try {
                tab.tab.select();
            }
            finally {
                mySwitchingTab = false;
            }
        }

        if (myEverywhereAutoSet && isEverywhere() && canToggleEverywhere()) {
            setEverywhereAuto(false);
        }

        myListModel.setShowGroupTitles(isAllTabSelected());

        myToolbar.updateActionsAsync();
        updateRightActions();
        rebuildList();
    }

    @RequiredUIAccess
    private void updateRightActions() {
        if (myRightActionsToolbar != null) {
            myRightActionsToolbar = null;
            mySearchField.setSuffixComponent(null);
        }

        SearchEverywhereContributor<?> contributor = mySelectedTab.contributor;
        if (contributor instanceof SearchFieldActionsContributor actionsContributor) {
            Runnable onChanged = this::rebuildList;
            Consumer<AnAction> registerShortcut = action -> action.registerCustomShortcutSet(action.getShortcutSet(), mySearchField, this);

            List<AnAction> rightActions = actionsContributor.createRightActions(registerShortcut, onChanged);
            if (!rightActions.isEmpty()) {
                ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar(
                    "search.everywhere.right.actions",
                    ActionGroup.newImmutableBuilder().addAll(rightActions).build(),
                    ActionToolbar.Style.INPLACE
                );
                toolbar.setTargetUIComponent(mySearchField);
                myRightActionsToolbar = toolbar;
                mySearchField.setSuffixComponent(toolbar.getUIComponent());
                toolbar.updateActionsAsync();
            }
        }
    }

    public String getSelectedContributorID() {
        return mySelectedTab.getID();
    }

    public @Nullable Object getSelectionIdentity() {
        SearchEverywhereFoundElementInfo info = myResultsList.getValue();
        return info == null || UnifiedSearchListModel.isTitle(info) ? null : Objects.hashCode(info.getElement());
    }

    @Override
    @RequiredUIAccess
    public void dispose() {
        myDisposed = true;
        stopSearching();
        myListModel.clear();
    }

    @Override
    public void uiDataSnapshot(DataSink sink) {
        sink.set(PlatformDataKeys.PREDEFINED_TEXT, getSearchPattern());
        sink.set(Project.KEY, myProject);

        SearchEverywhereFoundElementInfo info = myResultsList.getValue();
        if (info != null && info.getElement() != UnifiedSearchListModel.MORE_ELEMENT && !UnifiedSearchListModel.isTitle(info)) {
            sink.lazy(PsiElement.KEY_OF_ARRAY, () -> {
                @SuppressWarnings("unchecked")
                SearchEverywhereContributor<Object> contributor = (SearchEverywhereContributor<Object>) info.getContributor();
                Object element = contributor.getDataForItem(info.getElement(), PsiElement.KEY);
                return element instanceof PsiElement psiElement ? PsiUtilCore.toPsiElementArray(List.of(psiElement)) : null;
            });
        }
    }

    private boolean isAllTabSelected() {
        return SearchEverywhereManagerImpl.ALL_CONTRIBUTORS_GROUP_ID.equals(getSelectedContributorID());
    }

    private String getSearchPattern() {
        String value = mySearchField.getValue();
        return value == null ? "" : value;
    }

    @RequiredUIAccess
    private void updateViewType(boolean full) {
        myFullViewRequested = full;
        if (myShown) {
            applyViewType(full);
        }
    }

    @RequiredUIAccess
    private void applyViewType(boolean full) {
        if (myFullView == full) {
            return;
        }

        myFullView = full;
        myResultsLayout.setVisible(full);
        myViewTypeListener.run();
    }

    @RequiredUIAccess
    private void createTabs() {
        SETab allTab = new SETab(null);
        myTabs.add(allTab);

        myShownContributors.stream()
            .filter(SearchEverywhereContributor::isShownInSeparateTab)
            .forEach(contributor -> myTabs.add(new SETab(contributor)));

        for (SETab tab : myTabs) {
            tab.tab = myTabbedLayout.addTab(LocalizeValue.of(tab.tabName), VerticalLayout.create(Space.NONE));
        }

        myTabbedLayout.addSelectListener(event -> {
            if (mySwitchingTab) {
                return;
            }
            for (SETab tab : myTabs) {
                if (tab.tab == event.getTab() && tab != mySelectedTab) {
                    switchToTab(tab);
                    mySearchField.focus();
                    return;
                }
            }
        });
    }

    private static boolean isShownInToolbar(AnAction action) {
        return !(action instanceof ShowFilterAction) && !(action instanceof CustomComponentAction);
    }

    private class SETab {
        final @Nullable SearchEverywhereContributor<?> contributor;
        final List<AnAction> actions;
        final @Nullable SearchEverywhereToggleAction everywhereAction;
        final String tabName;
        @Nullable Tab tab;

        @RequiredUIAccess
        SETab(@Nullable SearchEverywhereContributor<?> contributor) {
            this.contributor = contributor;
            this.tabName = contributor == null ? IdeLocalize.searcheverywhereAllelementsTabName().get() : contributor.getGroupName();

            @RequiredUIAccess
            Runnable onChanged = () -> {
                myToolbar.updateActionsAsync();
                rebuildList();
            };

            if (contributor == null) {
                LocalizeValue text = IdeLocalize.checkboxIncludeNonProjectItems(IdeUICustomization.getInstance().getProjectConceptName());
                CheckBoxSearchEverywhereToggleAction checkBox = new CheckBoxSearchEverywhereToggleAction(text) {
                    @Override
                    public boolean isEverywhere() {
                        return myManager.isEverywhere();
                    }

                    @Override
                    @RequiredUIAccess
                    public void setEverywhere(boolean state) {
                        myManager.setEverywhere(state);
                        myTabs.stream()
                            .filter(tab -> tab != SETab.this && tab.everywhereAction != null)
                            .forEach(tab -> tab.everywhereAction.setEverywhere(state));
                        onChanged.run();
                    }
                };
                actions = List.of(checkBox);
            }
            else {
                actions = new ArrayList<>(contributor.getActions(onChanged));
            }
            everywhereAction = (SearchEverywhereToggleAction) ContainerUtil.find(actions, o -> o instanceof SearchEverywhereToggleAction);
        }

        String getID() {
            return contributor == null ? SearchEverywhereManagerImpl.ALL_CONTRIBUTORS_GROUP_ID : contributor.getSearchProviderId();
        }
    }

    @RequiredUIAccess
    private void rebuildList() {
        UIAccess.assertIsUIThread();

        if (myDisposed) {
            return;
        }

        stopSearching();

        myResultsList.setPlaceholder(IdeLocalize.labelChoosebynameSearching());
        String rawPattern = getSearchPattern();
        updateViewType(!rawPattern.isEmpty());

        Map<SearchEverywhereContributor<?>, Integer> contributorsMap = new HashMap<>();
        if (mySelectedTab.contributor != null) {
            contributorsMap.put(mySelectedTab.contributor, SINGLE_CONTRIBUTOR_ELEMENTS_LIMIT);
        }
        else {
            getAllTabContributors().forEach(c -> contributorsMap.put(c, MULTIPLE_CONTRIBUTORS_ELEMENTS_LIMIT));
        }

        List<SearchEverywhereContributor<?>> contributors = DumbService.getInstance(myProject).filterByDumbAwareness(contributorsMap.keySet());
        if (contributors.isEmpty() && DumbService.isDumb(myProject)) {
            myResultsList.setPlaceholder(
                IdeLocalize.searcheverywhereIndexingModeNotSupported(mySelectedTab.tabName, Application.get().getName())
            );
            myListModel.clear();
            return;
        }
        if (contributors.size() != contributorsMap.size()) {
            myResultsList.setPlaceholder(
                IdeLocalize.searcheverywhereIndexingIncompleteResults(mySelectedTab.tabName, Application.get().getName())
            );
        }

        myListModel.expireResults();
        contributors.forEach(contributor -> myListModel.setHasMore(contributor, false));
        String commandPrefix = SearchTopHitProvider.getTopHitAccelerator();
        if (rawPattern.startsWith(commandPrefix)) {
            String typedCommand = rawPattern.split(" ")[0].substring(commandPrefix.length());
            List<SearchEverywhereCommandInfo> commands = getCommandsForCompletion(contributors, typedCommand);

            if (!commands.isEmpty()) {
                if (rawPattern.contains(" ")) {
                    contributorsMap.keySet().retainAll(
                        commands.stream().map(SearchEverywhereCommandInfo::getContributor).collect(Collectors.toSet())
                    );
                }
                else {
                    myListModel.clear();
                    List<SearchEverywhereFoundElementInfo> lst = ContainerUtil.map(
                        commands,
                        command -> new SearchEverywhereFoundElementInfo(
                            command,
                            0,
                            myStubCommandContributor,
                            myStubCommandContributor.getItemPresentation(command, null)
                        )
                    );
                    applyResults(() -> myListModel.addElements(lst));
                    ensureSelectionExists();
                }
            }
        }
        mySearchProgressIndicator = mySearcher.search(contributorsMap, rawPattern);
    }

    @RequiredUIAccess
    private void initSearchActions() {
        Consumer<AnActionEvent> nextTabAction = e -> switchToNextTab();
        Consumer<AnActionEvent> prevTabAction = e -> switchToPrevTab();

        registerAction(SearchEverywhereActions.AUTOCOMPLETE_COMMAND, CompleteCommandAction::new);
        registerAction(SearchEverywhereActions.SWITCH_TO_NEXT_TAB, nextTabAction);
        registerAction(SearchEverywhereActions.SWITCH_TO_PREV_TAB, prevTabAction);
        registerAction(IdeActions.ACTION_NEXT_TAB, nextTabAction);
        registerAction(IdeActions.ACTION_PREVIOUS_TAB, prevTabAction);
        registerAction(IdeActions.ACTION_SWITCHER, e -> {
            if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
                switchToPrevTab();
            }
            else {
                switchToNextTab();
            }
        });
        registerAction(SearchEverywhereActions.NAVIGATE_TO_NEXT_GROUP, e -> fetchGroups(true));
        registerAction(SearchEverywhereActions.NAVIGATE_TO_PREV_GROUP, e -> fetchGroups(false));
        registerSelectItemAction();

        DumbAwareAction.create(e -> moveSelection(-1))
            .registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)), mySearchField, this);
        DumbAwareAction.create(e -> moveSelection(1))
            .registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)), mySearchField, this);

        mySearchField.addValueListener(event -> {
            String newSearchString = getSearchPattern();
            myManager.setCurrentSearchText(newSearchString);
            if (myNotFoundString != null) {
                boolean newPatternContainsPrevious = myNotFoundString.length() > 1 && newSearchString.contains(myNotFoundString);
                if (myEverywhereAutoSet && isEverywhere() && canToggleEverywhere() && !newPatternContainsPrevious) {
                    myNotFoundString = null;
                    setEverywhereAuto(false);
                    return;
                }
            }

            rebuildList();
        });

        MessageBusConnection projectBusConnection = myProject.getMessageBus().connect(this);
        projectBusConnection.subscribe(DumbModeListener.class, new DumbModeListener() {
            @Override
            public void exitDumbMode() {
                myUIAccess.give(() -> rebuildList());
            }
        });
        projectBusConnection.subscribe(AnActionListener.class, new AnActionListener() {
            @Override
            public void afterActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
                if (mySelectedTab != null && action == mySelectedTab.everywhereAction) {
                    myEverywhereAutoSet = false;
                }
            }
        });
    }

    private void registerAction(String actionID, Supplier<? extends AnAction> actionSupplier) {
        Optional.ofNullable(ActionManager.getInstance().getAction(actionID))
            .map(AnAction::getShortcutSet)
            .ifPresent(shortcuts -> actionSupplier.get().registerCustomShortcutSet(shortcuts, myRootLayout, this));
    }

    private void registerAction(String actionID, Consumer<? super AnActionEvent> action) {
        registerAction(actionID, () -> DumbAwareAction.create(action));
    }

    private void registerSelectItemAction() {
        int[] allowedModifiers = new int[]{
            0,
            InputEvent.SHIFT_MASK,
            InputEvent.CTRL_MASK,
            InputEvent.META_MASK,
            InputEvent.ALT_MASK
        };

        AnAction selectItemAction = ActionManager.getInstance().getAction(SearchEverywhereActions.SELECT_ITEM);
        ShortcutSet selectShortcuts = selectItemAction != null
            ? selectItemAction.getShortcutSet()
            : new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
        List<KeyboardShortcut> keyboardShortcuts = Arrays.stream(selectShortcuts.getShortcuts())
            .filter(shortcut -> shortcut instanceof KeyboardShortcut)
            .map(shortcut -> (KeyboardShortcut) shortcut)
            .toList();

        for (int modifiers : allowedModifiers) {
            List<Shortcut> newShortcuts = new ArrayList<>();
            for (KeyboardShortcut shortcut : keyboardShortcuts) {
                boolean hasSecondStroke = shortcut.getSecondKeyStroke() != null;
                KeyStroke originalStroke = hasSecondStroke ? shortcut.getSecondKeyStroke() : shortcut.getFirstKeyStroke();

                if ((originalStroke.getModifiers() & modifiers) != 0) {
                    continue;
                }

                KeyStroke newStroke = KeyStroke.getKeyStroke(originalStroke.getKeyCode(), originalStroke.getModifiers() | modifiers);
                newShortcuts.add(
                    hasSecondStroke
                        ? new KeyboardShortcut(shortcut.getFirstKeyStroke(), newStroke)
                        : new KeyboardShortcut(newStroke, null)
                );
            }
            if (newShortcuts.isEmpty()) {
                continue;
            }

            ShortcutSet newShortcutSet = new CustomShortcutSet(newShortcuts.toArray(Shortcut.EMPTY_ARRAY));
            DumbAwareAction.create(event -> {
                SearchEverywhereFoundElementInfo info = myResultsList.getValue();
                if (info != null && !UnifiedSearchListModel.isTitle(info)) {
                    elementsSelected(info, modifiers);
                }
            }).registerCustomShortcutSet(newShortcutSet, myRootLayout, this);
        }
    }

    private static int toModifiers(ModifiedInputDetails details) {
        int modifiers = 0;
        for (ModifiedInputDetails.Modifier modifier : details.getModifiers()) {
            modifiers |= switch (modifier) {
                case ALT -> InputEvent.ALT_MASK;
                case CTRL -> InputEvent.CTRL_MASK;
                case SHIFT -> InputEvent.SHIFT_MASK;
                case META -> InputEvent.META_MASK;
            };
        }
        return modifiers;
    }

    @RequiredUIAccess
    private void moveSelection(int delta) {
        int size = myListModel.getSize();
        if (size == 0) {
            return;
        }

        SearchEverywhereFoundElementInfo value = myResultsList.getValue();
        int index = value == null ? -1 : myListModel.indexOf(value);
        int newIndex = index + delta;
        if (newIndex < 0) {
            newIndex = size - 1;
        }
        else if (newIndex >= size) {
            newIndex = 0;
        }
        myResultsList.setValue(myListModel.getInfo(newIndex));
    }

    @RequiredUIAccess
    private void fetchGroups(boolean down) {
        SearchEverywhereFoundElementInfo value = myResultsList.getValue();
        int index = value == null ? -1 : myListModel.indexOf(value);
        do {
            index += down ? 1 : -1;
        }
        while (index >= 0 && index < myListModel.getSize() && !myListModel.isGroupFirstItem(index) && !myListModel.isMoreElement(index));
        if (index >= 0 && index < myListModel.getSize()) {
            myResultsList.setValue(myListModel.getInfo(index));
        }
    }

    private Optional<SearchEverywhereCommandInfo> getSelectedCommand(String typedCommand) {
        SearchEverywhereFoundElementInfo info = myResultsList.getValue();
        if (info == null || info.getContributor() != myStubCommandContributor) {
            return Optional.empty();
        }

        SearchEverywhereCommandInfo selectedCommand = (SearchEverywhereCommandInfo) info.getElement();
        return selectedCommand.getCommand().contains(typedCommand) ? Optional.of(selectedCommand) : Optional.empty();
    }

    private static List<SearchEverywhereCommandInfo> getCommandsForCompletion(
        Collection<? extends SearchEverywhereContributor<?>> contributors,
        String enteredCommandPart
    ) {
        Comparator<SearchEverywhereCommandInfo> cmdComparator = (cmd1, cmd2) -> {
            String cmdName1 = cmd1.getCommand();
            String cmdName2 = cmd2.getCommand();
            if (!enteredCommandPart.isEmpty()) {
                if (cmdName1.startsWith(enteredCommandPart) && !cmdName2.startsWith(enteredCommandPart)) {
                    return -1;
                }
                if (!cmdName1.startsWith(enteredCommandPart) && cmdName2.startsWith(enteredCommandPart)) {
                    return 1;
                }
            }

            return String.CASE_INSENSITIVE_ORDER.compare(cmdName1, cmd2.getCommand());
        };

        return contributors.stream()
            .flatMap(contributor -> contributor.getSupportedCommands().stream())
            .filter(command -> command.getCommand().contains(enteredCommandPart))
            .sorted(cmdComparator)
            .collect(Collectors.toList());
    }

    @RequiredUIAccess
    private void elementsSelected(SearchEverywhereFoundElementInfo info, int modifiers) {
        if (info.getElement() == UnifiedSearchListModel.MORE_ELEMENT) {
            showMoreElements(info.getContributor());
            return;
        }

        @SuppressWarnings("unchecked")
        SearchEverywhereContributor<Object> contributor = (SearchEverywhereContributor<Object>) info.getContributor();

        SearchEverywhereItemPresentation presentation = info.getPresentation();
        boolean closePopup = contributor.processSelectedItem(info.getElement(), modifiers, getSearchPattern());

        if (closePopup) {
            closePopup();
        }
        else if (presentation != null && presentation.getSwitchState() != null && !myDisposed) {
            info.setPresentation(presentation.withSwitchState(!presentation.getSwitchState()));
            myListModel.refresh(info);
        }
    }

    @RequiredUIAccess
    private void showMoreElements(SearchEverywhereContributor<?> contributor) {
        Map<SearchEverywhereContributor<?>, Collection<SearchEverywhereFoundElementInfo>> found = myListModel.getFoundElementsMap();
        int limit = myListModel.getItemsForContributor(contributor)
            + (mySelectedTab.contributor != null ? SINGLE_CONTRIBUTOR_ELEMENTS_LIMIT : MULTIPLE_CONTRIBUTORS_ELEMENTS_LIMIT);
        mySearchProgressIndicator = mySearcher.findMoreItems(found, getSearchPattern(), contributor, limit);
    }

    @RequiredUIAccess
    private void stopSearching() {
        if (mySearchProgressIndicator != null && !mySearchProgressIndicator.isCanceled()) {
            mySearchProgressIndicator.cancel();
        }
        clearBuffer();
    }

    @RequiredUIAccess
    private void closePopup() {
        stopSearching();
        mySearchFinishedHandler.run();
    }

    private List<SearchEverywhereContributor<?>> getAllTabContributors() {
        return ContainerUtil.filter(myShownContributors, contributor -> myContributorsFilter.isSelected(contributor.getSearchProviderId()));
    }

    @RequiredUIAccess
    private DockLayout createRow() {
        AdvancedLabel text = AdvancedLabel.create();

        Label suffix = Label.create(LocalizeValue.empty());
        suffix.setForegroundColor(ComponentColors.DISABLED_TEXT);

        ToggleSwitch toggleSwitch = ToggleSwitch.create();

        HorizontalLayout rightLayout = HorizontalLayout.create(Space.SMALL);
        rightLayout.add(suffix);
        rightLayout.add(toggleSwitch);

        DockLayout row = DockLayout.create(Space.LARGE);
        row.center(text);
        row.right(rightLayout);
        row.paddingBuilder().horizontalSet(Space.LARGE).verticalSet(Space.SMALL).apply();

        row.putUserData(TEXT_KEY, text);
        row.putUserData(SUFFIX_KEY, suffix);
        row.putUserData(SWITCH_KEY, toggleSwitch);
        return row;
    }

    @RequiredUIAccess
    private void bindRow(DockLayout row, RenderItem<SearchEverywhereFoundElementInfo> item) {
        AdvancedLabel text = Objects.requireNonNull(row.getUserData(TEXT_KEY));
        Label suffix = Objects.requireNonNull(row.getUserData(SUFFIX_KEY));
        ToggleSwitch toggleSwitch = Objects.requireNonNull(row.getUserData(SWITCH_KEY));

        SearchEverywhereFoundElementInfo info = item.getValue();
        if (info == null) {
            text.updatePresentation(TextItemPresentation::clearText);
            suffix.setVisible(false);
            toggleSwitch.setVisible(false);
            return;
        }

        if (UnifiedSearchListModel.isTitle(info)) {
            text.updatePresentation(presentation -> {
                presentation.clearText();
                presentation.withIcon(null);
                presentation.append(LocalizeValue.of(info.getContributor().getFullGroupName()), TextAttribute.GRAYED);
            });
            suffix.setVisible(false);
            toggleSwitch.setVisible(false);
            row.setBackgroundColor(null);
            row.setToolTipText(LocalizeValue.empty());
            return;
        }

        if (info.getElement() == UnifiedSearchListModel.MORE_ELEMENT) {
            text.updatePresentation(presentation -> {
                presentation.clearText();
                presentation.withIcon(Image.empty(Image.DEFAULT_ICON_SIZE));
                presentation.append(IdeLocalize.searcheverywhereMoreElements(), TextAttribute.GRAYED);
            });
            suffix.setVisible(false);
            toggleSwitch.setVisible(false);
            row.setBackgroundColor(null);
            row.setToolTipText(LocalizeValue.empty());
            return;
        }

        SearchEverywhereItemPresentation presentation = info.getPresentation();
        if (presentation == null) {
            presentation = SearchEverywhereItemPresentation.of(String.valueOf(info.getElement()));
        }

        text.updatePresentation(presentation::render);

        LocalizeValue suffixText = presentation.getSuffixText();
        suffix.setText(suffixText);
        suffix.setImage(presentation.getSuffixIcon());
        suffix.setVisible(suffixText.isNotEmpty() || presentation.getSuffixIcon() != null);

        Boolean switchState = presentation.getSwitchState();
        toggleSwitch.setVisible(switchState != null);
        if (switchState != null) {
            toggleSwitch.setValue(switchState, false);
        }

        row.setBackgroundColor(item.isSelected() ? null : presentation.getBackgroundColor());
        row.setToolTipText(presentation.getTooltip());
    }

    @RequiredUIAccess
    private void applyResults(Runnable change) {
        myApplyingResults = true;
        try {
            change.run();
        }
        finally {
            myApplyingResults = false;
        }
        restoreSelection();
    }

    @RequiredUIAccess
    private void saveSelection() {
        SearchEverywhereFoundElementInfo info = myResultsList.getValue();
        if (info == null) {
            mySelectedElement = null;
            myMoreSelected = false;
            return;
        }

        int index = myListModel.indexOf(info);
        if (info.getElement() == UnifiedSearchListModel.MORE_ELEMENT && index > 0) {
            myMoreSelected = true;
            mySelectedElement = myListModel.getInfo(index - 1).getElement();
        }
        else {
            myMoreSelected = false;
            mySelectedElement = info.getElement();
        }
    }

    @RequiredUIAccess
    private void restoreSelection() {
        if (myListModel.isEmpty()) {
            return;
        }

        int index = selectedElementIndex();
        if (index >= 0 && myMoreSelected && index + 1 < myListModel.getSize()) {
            index += 1;
        }
        if (index < 0) {
            index = 0;
        }

        SearchEverywhereFoundElementInfo info = myListModel.getInfo(index);
        if (myResultsList.getValue() != info) {
            myApplyingResults = true;
            try {
                myResultsList.setValue(info, false);
            }
            finally {
                myApplyingResults = false;
            }
        }
    }

    @RequiredUIAccess
    private void ensureSelectionExists() {
        if (myResultsList.getValue() == null && !myListModel.isEmpty()) {
            myResultsList.setValue(myListModel.getInfo(0), false);
        }
    }

    private int selectedElementIndex() {
        Object selected = mySelectedElement;
        if (selected == null) {
            return -1;
        }
        List<SearchEverywhereFoundElementInfo> infos = myListModel.getInfos();
        for (int i = 0; i < infos.size(); i++) {
            Object element = infos.get(i).getElement();
            if (element != UnifiedSearchListModel.MORE_ELEMENT && Objects.equals(element, selected)) {
                return i;
            }
        }
        return -1;
    }

    private class CompleteCommandAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            Optional<SearchEverywhereCommandInfo> suggestedCommand = getCompleteCommand();
            suggestedCommand.ifPresent(command -> mySearchField.setValue(command.getCommandWithPrefix() + " "));
        }

        @Override
        @RequiredUIAccess
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(getCompleteCommand().isPresent());
        }

        private Optional<SearchEverywhereCommandInfo> getCompleteCommand() {
            String pattern = getSearchPattern();
            String commandPrefix = SearchTopHitProvider.getTopHitAccelerator();
            if (pattern.startsWith(commandPrefix) && !pattern.contains(" ")) {
                String typedCommand = pattern.substring(commandPrefix.length());
                SearchEverywhereCommandInfo command = getSelectedCommand(typedCommand).orElseGet(() -> {
                    Collection<SearchEverywhereContributor<?>> contributors =
                        mySelectedTab.contributor == null ? getAllTabContributors() : List.of(mySelectedTab.contributor);
                    List<SearchEverywhereCommandInfo> completions = getCommandsForCompletion(contributors, typedCommand);
                    return completions.isEmpty() ? null : completions.get(0);
                });

                return Optional.ofNullable(command);
            }

            return Optional.empty();
        }
    }

    private LocalizeValue getNotFoundText() {
        SearchEverywhereContributor<?> contributor = mySelectedTab.contributor;
        return contributor != null
            ? IdeLocalize.searcheverywhereNothingFoundForContributorAnywhere(contributor.getFullGroupName())
            : IdeLocalize.searcheverywhereNothingFoundForAllAnywhere();
    }

    @RequiredUIAccess
    private void scheduleFlush() {
        if (myFlushFuture == null) {
            myFlushFuture = myUIAccess.getScheduler().schedule(this::flushBuffer, ModalityState.any(), THROTTLING_TIMEOUT, TimeUnit.MILLISECONDS);
        }
    }

    @RequiredUIAccess
    private void flushBuffer() {
        myFlushFuture = null;
        if (myDisposed) {
            return;
        }

        List<SearchEverywhereFoundElementInfo> added = new ArrayList<>(myPendingAdded);
        List<SearchEverywhereFoundElementInfo> removed = new ArrayList<>(myPendingRemoved);
        myPendingAdded.clear();
        myPendingRemoved.clear();

        if (!added.isEmpty()) {
            onElementsAdded(added);
        }
        if (!removed.isEmpty()) {
            onElementsRemoved(removed);
        }
    }

    @RequiredUIAccess
    private void clearBuffer() {
        myPendingAdded.clear();
        myPendingRemoved.clear();
        ScheduledFuture<?> future = myFlushFuture;
        myFlushFuture = null;
        if (future != null) {
            future.cancel(false);
        }
    }

    @RequiredUIAccess
    private void onElementsAdded(List<SearchEverywhereFoundElementInfo> list) {
        boolean wasEmpty = myListModel.isEmpty();

        applyResults(() -> myListModel.addElements(list));

        if (wasEmpty && !myListModel.isEmpty()) {
            Object prevSelection = myManager.getPrevSelection(getSelectedContributorID());
            if (prevSelection instanceof Integer intPrevSelection) {
                for (SearchEverywhereFoundElementInfo info : myListModel.getInfos()) {
                    if (Objects.hashCode(info.getElement()) == intPrevSelection) {
                        myResultsList.setValue(info);
                        break;
                    }
                }
            }
        }

        updateViewType(!myListModel.isEmpty() || !getSearchPattern().isEmpty());
    }

    @RequiredUIAccess
    private void onElementsRemoved(List<SearchEverywhereFoundElementInfo> list) {
        applyResults(() -> list.forEach(info -> myListModel.removeElement(info.getElement(), info.getContributor())));
        updateViewType(!myListModel.isEmpty() || !getSearchPattern().isEmpty());
    }

    @RequiredUIAccess
    private void onSearchFinished(Map<SearchEverywhereContributor<?>, Boolean> hasMoreContributors) {
        if (myDisposed) {
            return;
        }

        if (myListModel.isEmpty() || myListModel.isResultsExpired()) {
            if (myEverywhereAutoSet && !isEverywhere() && canToggleEverywhere() && !getSearchPattern().isEmpty()) {
                setEverywhereAuto(true);
                myNotFoundString = getSearchPattern();
                return;
            }

            if (myListModel.isResultsExpired()) {
                myListModel.clear();
            }
        }

        myResultsList.setPlaceholder(getSearchPattern().isEmpty() ? LocalizeValue.empty() : getNotFoundText());
        applyResults(() -> hasMoreContributors.forEach(myListModel::setHasMore));

        if (selectedElementIndex() < 0) {
            mySelectedElement = null;
        }
        updateViewType(!myListModel.isEmpty() || !getSearchPattern().isEmpty());
    }

    private final SESearcher.Listener mySearchListener = new SESearcher.Listener() {
        @Override
        @RequiredUIAccess
        public void elementsAdded(List<? extends SearchEverywhereFoundElementInfo> list) {
            myPendingAdded.addAll(list);
            scheduleFlush();
        }

        @Override
        @RequiredUIAccess
        public void elementsRemoved(List<? extends SearchEverywhereFoundElementInfo> list) {
            for (SearchEverywhereFoundElementInfo removing : list) {
                boolean pending = myPendingAdded.removeIf(
                    existing -> existing.getContributor() == removing.getContributor() && existing.getElement() == removing.getElement()
                );
                if (!pending) {
                    myPendingRemoved.add(removing);
                }
            }
            scheduleFlush();
        }

        @Override
        @RequiredUIAccess
        public void searchFinished(Map<SearchEverywhereContributor<?>, Boolean> hasMoreContributors) {
            flushBuffer();
            clearBuffer();
            onSearchFinished(hasMoreContributors);
        }
    };

    private final SearchEverywhereContributor<Object> myStubCommandContributor = new SearchEverywhereContributor<>() {
        @Override
        public String getSearchProviderId() {
            return "CommandsContributor";
        }

        @Override
        public String getGroupName() {
            return IdeLocalize.searcheverywhereCommandsTabName().get();
        }

        @Override
        public int getSortWeight() {
            return 10;
        }

        @Override
        public boolean showInFindResults() {
            return false;
        }

        @Override
        public void fetchElements(String pattern, ProgressIndicator progressIndicator, Predicate<? super Object> predicate) {
        }

        @Override
        @RequiredUIAccess
        public boolean processSelectedItem(Object selected, int modifiers, String searchText) {
            mySearchField.setValue(((SearchEverywhereCommandInfo) selected).getCommandWithPrefix() + " ");
            return false;
        }

        @Override
        public javax.swing.ListCellRenderer<? super Object> getElementsRenderer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public SearchEverywhereItemPresentation getItemPresentation(Object item, @Nullable MinusculeMatcher matcher) {
            SearchEverywhereCommandInfo command = (SearchEverywhereCommandInfo) item;
            return SearchEverywhereItemPresentation.builder()
                .icon(Image.empty(Image.DEFAULT_ICON_SIZE))
                .append(command.getCommandWithPrefix() + " ")
                .append(command.getDefinition(), TextAttribute.GRAYED)
                .build();
        }

        @Override
        public @Nullable Object getDataForItem(Object element, Key dataId) {
            return null;
        }
    };
}
