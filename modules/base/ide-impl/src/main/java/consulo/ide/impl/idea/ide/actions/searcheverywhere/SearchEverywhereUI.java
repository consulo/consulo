// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.searcheverywhere;

import com.google.common.collect.Lists;
import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.impl.internal.progress.ProgressIndicatorBase;
import consulo.application.internal.TooManyUsagesStatus;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.util.diff.Diff;
import consulo.application.util.diff.FilesTooBigForDiffException;
import consulo.application.util.function.Processor;
import consulo.application.util.matcher.MatcherHolder;
import consulo.application.util.matcher.MinusculeMatcher;
import consulo.application.util.matcher.NameUtil;
import consulo.component.ProcessCanceledException;
import consulo.component.messagebus.MessageBusConnection;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposer;
import consulo.ide.IdeBundle;
import consulo.ide.impl.find.PsiElement2UsageTargetAdapter;
import consulo.ide.impl.idea.ide.SearchTopHitProvider;
import consulo.ide.impl.idea.ide.actions.BigPopupUI;
import consulo.ide.impl.idea.ide.actions.SearchEverywhereClassifier;
import consulo.ide.impl.idea.ide.actions.bigPopup.ShowFilterAction;
import consulo.ide.impl.idea.ide.util.gotoByName.QuickSearchComponent;
import consulo.ide.impl.idea.ide.util.gotoByName.SearchEverywhereConfiguration;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionMenuUtil;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.openapi.progress.util.ProgressWindowListener;
import consulo.ide.impl.idea.ui.IdeUICustomization;
import consulo.ide.impl.idea.ui.SeparatorComponent;
import consulo.ide.impl.idea.ui.popup.PopupUpdateProcessor;
import consulo.ide.impl.idea.usages.UsageLimitUtil;
import consulo.ide.impl.idea.usages.impl.UsageViewManagerImpl;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.event.DumbModeListener;
import consulo.project.ui.internal.ToolWindowManagerEx;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.TextBoxWithExtensions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.internal.GuiUtils;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.image.Image;
import consulo.usage.*;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.Couple;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Konstantin Bulenkov
 * @author Mikhail.Sokolov
 */
public class SearchEverywhereUI extends BigPopupUI implements DataProvider, QuickSearchComponent {
    private static final Logger LOG = Logger.getInstance(SearchEverywhereUI.class);

    public static final String SEARCH_EVERYWHERE_SEARCH_FILED_KEY = "search-everywhere-textfield"; //only for testing purposes

    public static final int SINGLE_CONTRIBUTOR_ELEMENTS_LIMIT = 30;
    public static final int MULTIPLE_CONTRIBUTORS_ELEMENTS_LIMIT = 15;
    public static final int THROTTLING_TIMEOUT = 100;

    private static final SimpleTextAttributes SMALL_LABEL_ATTRS =
        new SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER, JBCurrentTheme.BigPopup.listTitleLabelForeground());

    private final List<? extends SearchEverywhereContributor<?>> myShownContributors;

    private SearchListModel myListModel;

    private SETab mySelectedTab;
    private final List<SETab> myTabs = new ArrayList<>();

    private boolean myEverywhereAutoSet = true;
    private String myNotFoundString;

    private JBPopup myHint;

    private final SESearcher mySearcher;
    private final ThrottlingListenerWrapper myBufferedListener;
    private ProgressIndicator mySearchProgressIndicator;

    private final SEListSelectionTracker mySelectionTracker;
    private final PersistentSearchEverywhereContributorFilter<String> myContributorsFilter;
    private ActionToolbar myToolbar;

    public SearchEverywhereUI(Project project, List<? extends SearchEverywhereContributor<?>> contributors) {
        super(project);
        List<SEResultsEqualityProvider> equalityProviders = SEResultsEqualityProvider.getProviders();
        myBufferedListener = new ThrottlingListenerWrapper(THROTTLING_TIMEOUT, mySearchListener, Runnable::run);
        mySearcher = new MultiThreadSearcher(
            myBufferedListener,
            run -> project.getApplication().invokeLater(run),
            equalityProviders
        );
        myShownContributors = contributors;
        Map<String, String> namesMap = ContainerUtil.map2Map(
            contributors,
            c -> Couple.of(c.getSearchProviderId(), c.getFullGroupName())
        );
        myContributorsFilter = new PersistentSearchEverywhereContributorFilter<>(
            ContainerUtil.map(contributors, SearchEverywhereContributor::getSearchProviderId),
            SearchEverywhereConfiguration.getInstance(project),
            namesMap::get,
            c -> null
        );

        init();

        initSearchActions();

        myResultsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        myResultsList.addListSelectionListener(e -> {
            int[] selectedIndices = myResultsList.getSelectedIndices();
            if (selectedIndices.length > 1) {
                boolean multiSelection = Arrays.stream(selectedIndices)
                    .allMatch(i -> myListModel.getContributorForIndex(i).isMultiSelectionSupported());
                if (!multiSelection) {
                    int index = myResultsList.getLeadSelectionIndex();
                    myResultsList.setSelectedIndex(index);
                }
            }
        });

        mySelectionTracker = new SEListSelectionTracker(myResultsList, myListModel);
        myResultsList.addListSelectionListener(mySelectionTracker);
    }

    @Override
    @Nonnull
    protected CompositeCellRenderer createCellRenderer() {
        return new CompositeCellRenderer();
    }

    @Nonnull
    @Override
    public JBList<Object> createList() {
        myListModel = new SearchListModel();
        addListDataListener(myListModel);

        return new JBList<>(myListModel);
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
        myToolbar.updateActionsImmediately();
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
        myToolbar.updateActionsImmediately();
    }

    private boolean isEverywhere() {
        return mySelectedTab.everywhereAction == null || mySelectedTab.everywhereAction.isEverywhere();
    }

    private boolean canToggleEverywhere() {
        return mySelectedTab.everywhereAction != null && mySelectedTab.everywhereAction.canToggleEverywhere();
    }

    public void switchToContributor(@Nonnull String contributorID) {
        SETab selectedTab = myTabs.stream()
            .filter(tab -> tab.getID().equals(contributorID))
            .findAny()
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("Contributor %s is not supported", contributorID)
            ));
        switchToTab(selectedTab);
    }

    private void switchToNextTab() {
        int currentIndex = myTabs.indexOf(mySelectedTab);
        SETab nextTab = currentIndex == myTabs.size() - 1 ? myTabs.get(0) : myTabs.get(currentIndex + 1);
        switchToTab(nextTab);
    }

    private void switchToPrevTab() {
        int currentIndex = myTabs.indexOf(mySelectedTab);
        SETab prevTab = currentIndex == 0 ? myTabs.get(myTabs.size() - 1) : myTabs.get(currentIndex - 1);
        switchToTab(prevTab);
    }

    @RequiredUIAccess
    private void switchToTab(SETab tab) {
        boolean prevTabIsAll = mySelectedTab != null && isAllTabSelected();
        mySelectedTab = tab;
        boolean nextTabIsAll = isAllTabSelected();

        if (myEverywhereAutoSet && isEverywhere() && canToggleEverywhere()) {
            setEverywhereAuto(false);
        }

        if (prevTabIsAll != nextTabIsAll) {
            //reset cell renderer to show/hide group titles in "All" tab
            ListCellRenderer cellRenderer = myResultsList.getCellRenderer();
            if (cellRenderer instanceof ExpandedItemListCellRendererWrapper expandedItemListCellRendererWrapper) {
                cellRenderer = expandedItemListCellRendererWrapper.getWrappee();
            }

            myResultsList.setCellRenderer(cellRenderer);
        }
        if (myToolbar != null) {
            myToolbar.updateActionsImmediately();
        }
        repaint();
        rebuildList();
    }

    public String getSelectedContributorID() {
        return mySelectedTab.getID();
    }

    @Nullable
    public Object getSelectionIdentity() {
        Object value = myResultsList.getSelectedValue();
        return value == null ? null : Objects.hashCode(value);
    }

    @Override
    public void dispose() {
        stopSearching();
        myListModel.clear();
    }

    @Nullable
    @Override
    public Object getData(@Nonnull Key dataId) {
        IntStream indicesStream = Arrays.stream(myResultsList.getSelectedIndices())
            .filter(i -> !myListModel.isMoreElement(i));

        //common data section---------------------
        if (PlatformDataKeys.PREDEFINED_TEXT == dataId) {
            return getSearchPattern();
        }
        if (Project.KEY == dataId) {
            return myProject;
        }

        if (PsiElement.KEY_OF_ARRAY == dataId) {
            List<PsiElement> elements = indicesStream.mapToObj(i -> {
                SearchEverywhereContributor<Object> contributor = myListModel.getContributorForIndex(i);
                Object item = myListModel.getElementAt(i);
                return (PsiElement) contributor.getDataForItem(item, PsiElement.KEY);
            }).filter(Objects::nonNull).collect(Collectors.toList());
            return PsiUtilCore.toPsiElementArray(elements);
        }

        //item-specific data section--------------
        return indicesStream.mapToObj(i -> {
            SearchEverywhereContributor<Object> contributor = myListModel.getContributorForIndex(i);
            Object item = myListModel.getElementAt(i);
            return contributor.getDataForItem(item, dataId);
        }).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Override
    public void registerHint(@Nonnull JBPopup h) {
        if (myHint != null && myHint.isVisible() && myHint != h) {
            myHint.cancel();
        }
        myHint = h;
    }

    @Override
    public void unregisterHint() {
        myHint = null;
    }

    private void hideHint() {
        if (myHint != null && myHint.isVisible()) {
            myHint.cancel();
        }
    }

    private void updateHint(Object element) {
        if (myHint == null || !myHint.isVisible()) {
            return;
        }
        final PopupUpdateProcessor updateProcessor = myHint.getUserData(PopupUpdateProcessor.class);
        if (updateProcessor != null) {
            updateProcessor.updatePopup(element);
        }
    }

    private boolean isAllTabSelected() {
        return SearchEverywhereManagerImpl.ALL_CONTRIBUTORS_GROUP_ID.equals(getSelectedContributorID());
    }

    @Override
    @Nonnull
    @RequiredUIAccess
    protected JComponent createSettingsPanel() {
        ActionGroup.Builder actionGroup = ActionGroup.newImmutableBuilder();
        actionGroup.add(new ActionGroup() {
            @Nonnull
            @Override
            public AnAction[] getChildren(@Nullable AnActionEvent e) {
                if (e == null || mySelectedTab == null) {
                    return EMPTY_ARRAY;
                }
                return mySelectedTab.actions.toArray(EMPTY_ARRAY);
            }
        });
        actionGroup.add(new ShowInFindToolWindowAction());

        myToolbar = ActionManager.getInstance().createActionToolbar("search.everywhere.toolbar", actionGroup.build(), true);
        myToolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
        myToolbar.setTargetComponent(null);
        myToolbar.updateActionsImmediately();
        JComponent toolbarComponent = myToolbar.getComponent();
        toolbarComponent.setOpaque(false);
        toolbarComponent.setBorder(JBUI.Borders.empty(2, 18, 2, 9));
        return toolbarComponent;
    }

    @Nonnull
    @Override
    protected String getInitialHint() {
        return IdeLocalize.searcheverywhereHistoryShortcutsHint(
            KeymapUtil.getKeystrokeText(SearchTextField.ALT_SHOW_HISTORY_KEYSTROKE),
            KeymapUtil.getKeystrokeText(SearchTextField.SHOW_HISTORY_KEYSTROKE)
        ).get();
    }

    @Nonnull
    @Override
    protected TextBoxWithExtensions createSearchField() {
        TextBoxWithExtensions field = super.createSearchField();
        field.setExtensions(new TextBoxWithExtensions.Extension(true, AllIcons.Actions.Search, null));
        field.addUserDataProvider(PlatformDataKeys.PREDEFINED_TEXT, () -> field.getValue());
        return field;
    }

    @Override
    protected void installScrollingActions() {
        ScrollingUtil.installMoveUpAction(myResultsList, (JComponent) TargetAWT.to(getSearchField()));
        ScrollingUtil.installMoveDownAction(myResultsList, (JComponent) TargetAWT.to(getSearchField()));
    }

    @Override
    @Nonnull
    protected JPanel createTopLeftPanel() {
        JPanel contributorsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        contributorsPanel.setBorder(JBUI.Borders.empty(0, 12, 0, 0));
        contributorsPanel.setOpaque(false);

        SETab allTab = new SETab(null);
        contributorsPanel.add(allTab);
        myTabs.add(allTab);

        myShownContributors.stream().filter(SearchEverywhereContributor::isShownInSeparateTab).forEach(contributor -> {
            SETab tab = new SETab(contributor);
            contributorsPanel.add(tab);
            myTabs.add(tab);
        });

        return contributorsPanel;
    }

    private class SETab extends JLabel {
        final SearchEverywhereContributor<?> contributor;
        final List<AnAction> actions;
        final SearchEverywhereToggleAction everywhereAction;

        @RequiredUIAccess
        SETab(@Nullable SearchEverywhereContributor<?> contributor) {
            super(contributor == null ? IdeLocalize.searcheverywhereAllelementsTabName().get() : contributor.getGroupName());
            this.contributor = contributor;
            Runnable onChanged = () -> {
                myToolbar.updateActionsImmediately();
                rebuildList();
            };
            if (contributor == null) {
                LocalizeValue text = IdeLocalize.checkboxIncludeNonProjectItems(IdeUICustomization.getInstance().getProjectConceptName());
                CheckBoxSearchEverywhereToggleAction checkBox = new CheckBoxSearchEverywhereToggleAction(text) {
                    final SearchEverywhereManagerImpl seManager = (SearchEverywhereManagerImpl) SearchEverywhereManager.getInstance(myProject);

                    @Override
                    public boolean isEverywhere() {
                        return seManager.isEverywhere();
                    }

                    @Override
                    public void setEverywhere(boolean state) {
                        seManager.setEverywhere(state);
                        myTabs.stream().filter(tab -> tab != SETab.this).forEach(tab -> tab.everywhereAction.setEverywhere(state));
                        onChanged.run();
                    }
                };
                actions = Arrays.asList(checkBox, new FiltersAction(myContributorsFilter, onChanged));
            }
            else {
                actions = new ArrayList<>(contributor.getActions(onChanged));
            }
            everywhereAction = (SearchEverywhereToggleAction) ContainerUtil.find(actions, o -> o instanceof SearchEverywhereToggleAction);
            Insets insets = JBCurrentTheme.BigPopup.tabInsets();
            setBorder(JBUI.Borders.empty(insets.top, insets.left, insets.bottom, insets.right));
            addMouseListener(new MouseAdapter() {
                @Override
                @RequiredUIAccess
                public void mousePressed(MouseEvent e) {
                    switchToTab(SETab.this);
                }
            });
        }

        public String getID() {
            return getContributor()
                .map(SearchEverywhereContributor::getSearchProviderId)
                .orElse(SearchEverywhereManagerImpl.ALL_CONTRIBUTORS_GROUP_ID);
        }

        public Optional<SearchEverywhereContributor<?>> getContributor() {
            return Optional.ofNullable(contributor);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            size.height = JBUIScale.scale(29);
            return size;
        }

        @Override
        public boolean isOpaque() {
            return mySelectedTab == this;
        }

        @Override
        public Color getBackground() {
            return mySelectedTab == this ? UIUtil.getPanelBackground() : super.getBackground();
        }
    }

    @RequiredUIAccess
    private void rebuildList() {
        myProject.getApplication().assertIsDispatchThread();

        stopSearching();

        myResultsList.setEmptyText(IdeLocalize.labelChoosebynameSearching().get());
        String rawPattern = getSearchPattern();
        updateViewType(rawPattern.isEmpty() ? ViewType.SHORT : ViewType.FULL);
        String namePattern = mySelectedTab.getContributor()
            .map(contributor -> contributor.filterControlSymbols(rawPattern))
            .orElse(rawPattern);

        MinusculeMatcher matcher = NameUtil.buildMatcherWithFallback(
            "*" + rawPattern,
            "*" + namePattern,
            NameUtil.MatchingCaseSensitivity.NONE
        );
        MatcherHolder.associateMatcher(myResultsList, matcher);

        Map<SearchEverywhereContributor<?>, Integer> contributorsMap = new HashMap<>();
        Optional<SearchEverywhereContributor<?>> selectedContributor = mySelectedTab.getContributor();
        if (selectedContributor.isPresent()) {
            contributorsMap.put(selectedContributor.get(), SINGLE_CONTRIBUTOR_ELEMENTS_LIMIT);
        }
        else {
            contributorsMap.putAll(
                getAllTabContributors().stream()
                    .collect(Collectors.toMap(c -> c, c -> MULTIPLE_CONTRIBUTORS_ELEMENTS_LIMIT))
            );
        }

        List<SearchEverywhereContributor<?>> contributors =
            DumbService.getInstance(myProject).filterByDumbAwareness(contributorsMap.keySet());
        if (contributors.isEmpty() && DumbService.isDumb(myProject)) {
            myResultsList.setEmptyText(
                IdeLocalize.searcheverywhereIndexingModeNotSupported(mySelectedTab.getText(), Application.get().getName()).get()
            );
            myListModel.clear();
            return;
        }
        if (contributors.size() != contributorsMap.size()) {
            myResultsList.setEmptyText(
                IdeLocalize.searcheverywhereIndexingIncompleteResults(
                    mySelectedTab.getText(),
                    Application.get().getName()
                ).get()
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
                        commands.stream()
                            .map(SearchEverywhereCommandInfo::getContributor)
                            .collect(Collectors.toSet())
                    );
                }
                else {
                    myListModel.clear();
                    List<SearchEverywhereFoundElementInfo> lst = ContainerUtil.map(
                        commands,
                        command -> new SearchEverywhereFoundElementInfo(command, 0, myStubCommandContributor)
                    );
                    myListModel.addElements(lst);
                    ScrollingUtil.ensureSelectionExists(myResultsList);
                }
            }
        }
        mySearchProgressIndicator = mySearcher.search(contributorsMap, rawPattern);
    }

    private void initSearchActions() {
        MouseAdapter listMouseListener = new MouseAdapter() {
            private int currentDescriptionIndex = -1;

            @Override
            public void mouseClicked(MouseEvent e) {
                onMouseClicked(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int index = myResultsList.locationToIndex(e.getPoint());
                indexChanged(index);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                int index = myResultsList.getSelectedIndex();
                indexChanged(index);
            }

            private void indexChanged(int index) {
                if (index != currentDescriptionIndex) {
                    currentDescriptionIndex = index;
                    showDescriptionForIndex(index);
                }
            }
        };
        myResultsList.addMouseMotionListener(listMouseListener);
        myResultsList.addMouseListener(listMouseListener);

        ScrollingUtil.redirectExpandSelection(myResultsList, (JComponent) TargetAWT.to(mySearchField));

        Consumer<AnActionEvent> nextTabAction = e -> {
            switchToNextTab();
            triggerTabSwitched(e);
        };
        Consumer<AnActionEvent> prevTabAction = e -> {
            switchToPrevTab();
            triggerTabSwitched(e);
        };

        registerAction(SearchEverywhereActions.AUTOCOMPLETE_COMMAND, CompleteCommandAction::new);
        registerAction(SearchEverywhereActions.SWITCH_TO_NEXT_TAB, nextTabAction);
        registerAction(SearchEverywhereActions.SWITCH_TO_PREV_TAB, prevTabAction);
        registerAction(IdeActions.ACTION_NEXT_TAB, nextTabAction);
        registerAction(IdeActions.ACTION_PREVIOUS_TAB, prevTabAction);
        registerAction(IdeActions.ACTION_SWITCHER, e -> {
            if (e.getInputEvent().isShiftDown()) {
                switchToPrevTab();
            }
            else {
                switchToNextTab();
            }
            triggerTabSwitched(e);
        });
        registerAction(SearchEverywhereActions.NAVIGATE_TO_NEXT_GROUP, e -> fetchGroups(true));
        registerAction(SearchEverywhereActions.NAVIGATE_TO_PREV_GROUP, e -> fetchGroups(false));
        registerSelectItemAction();

        AnAction escape = ActionManager.getInstance().getAction("EditorEscape");
        DumbAwareAction.create(__ -> closePopup())
            .registerCustomShortcutSet(escape == null ? CommonShortcuts.ESCAPE : escape.getShortcutSet(), this);

        mySearchField.addValueListener((e) -> {
            String newSearchString = getSearchPattern();
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

        myResultsList.addListSelectionListener(e -> {
            Object selectedValue = myResultsList.getSelectedValue();
            if (selectedValue != null && myHint != null && myHint.isVisible()) {
                updateHint(selectedValue);
            }

            showDescriptionForIndex(myResultsList.getSelectedIndex());
        });

        MessageBusConnection projectBusConnection = myProject.getMessageBus().connect(this);
        projectBusConnection.subscribe(DumbModeListener.class, new DumbModeListener() {
            @Override
            public void exitDumbMode() {
                myProject.getApplication().invokeLater(() -> rebuildList());
            }
        });
        projectBusConnection.subscribe(AnActionListener.class, new AnActionListener() {
            @Override
            public void afterActionPerformed(
                @Nonnull AnAction action,
                @Nonnull DataContext dataContext,
                @Nonnull AnActionEvent event
            ) {
                if (action == mySelectedTab.everywhereAction && event.getInputEvent() != null) {
                    myEverywhereAutoSet = false;
                }
            }
        });

        myProject.getApplication().getMessageBus()
            .connect(this)
            .subscribe(ProgressWindowListener.class, pw -> Disposer.register(pw, () -> myResultsList.repaint()));

        TargetAWT.to(mySearchField).addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                Component oppositeComponent = e.getOppositeComponent();
                if (!isHintComponent(oppositeComponent) && !UIUtil.haveCommonOwner(SearchEverywhereUI.this, oppositeComponent)) {
                    closePopup();
                }
            }
        });
    }

    private void showDescriptionForIndex(int index) {
        if (index >= 0 && !myListModel.isMoreElement(index)) {
            SearchEverywhereContributor<Object> contributor = myListModel.getContributorForIndex(index);
            Object data = contributor.getDataForItem(myListModel.getElementAt(index), SearchEverywhereDataKeys.ITEM_STRING_DESCRIPTION);
            if (data instanceof String) {
                ActionMenuUtil.showDescriptionInStatusBar(true, myResultsList, (String) data);
            }
        }
    }

    private void registerAction(String actionID, Supplier<? extends AnAction> actionSupplier) {
        Optional.ofNullable(ActionManager.getInstance().getAction(actionID))
            .map(AnAction::getShortcutSet)
            .ifPresent(shortcuts -> actionSupplier.get().registerCustomShortcutSet(shortcuts, this, this));
    }

    private void registerAction(String actionID, Consumer<? super AnActionEvent> action) {
        registerAction(actionID, () -> DumbAwareAction.create(action));
    }

    // when user adds shortcut for "select item" we should add shortcuts
    // with all possible modifiers (Ctrl, Shift, Alt, etc.)
    private void registerSelectItemAction() {
        int[] allowedModifiers = new int[]{
            0,
            InputEvent.SHIFT_MASK,
            InputEvent.CTRL_MASK,
            InputEvent.META_MASK,
            InputEvent.ALT_MASK
        };

        ShortcutSet selectShortcuts = ActionManager.getInstance()
            .getAction(SearchEverywhereActions.SELECT_ITEM)
            .getShortcutSet();
        Collection<KeyboardShortcut> keyboardShortcuts = Arrays.stream(selectShortcuts.getShortcuts())
            .filter(shortcut -> shortcut instanceof KeyboardShortcut)
            .map(shortcut -> (KeyboardShortcut) shortcut)
            .collect(Collectors.toList());

        for (int modifiers : allowedModifiers) {
            Collection<Shortcut> newShortcuts = new ArrayList<>();
            for (KeyboardShortcut shortcut : keyboardShortcuts) {
                boolean hasSecondStroke = shortcut.getSecondKeyStroke() != null;
                KeyStroke originalStroke = hasSecondStroke ? shortcut.getSecondKeyStroke() : shortcut.getFirstKeyStroke();

                if ((originalStroke.getModifiers() & modifiers) != 0) {
                    continue;
                }

                KeyStroke newStroke = KeyStroke.getKeyStroke(
                    originalStroke.getKeyCode(),
                    originalStroke.getModifiers() | modifiers
                );
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
            DumbAwareAction.create(event -> elementsSelected(myResultsList.getSelectedIndices(), modifiers))
                .registerCustomShortcutSet(newShortcutSet, this, this);
        }
    }

    private void triggerTabSwitched(AnActionEvent e) {
    }

    private void fetchGroups(boolean down) {
        int index = myResultsList.getSelectedIndex();
        do {
            index += down ? 1 : -1;
        }
        while (index >= 0 && index < myListModel.getSize() && !myListModel.isGroupFirstItem(index) && !myListModel.isMoreElement(index));
        if (index >= 0 && index < myListModel.getSize()) {
            myResultsList.setSelectedIndex(index);
            ScrollingUtil.ensureIndexIsVisible(myResultsList, index, 0);
        }
    }

    private Optional<SearchEverywhereCommandInfo> getSelectedCommand(String typedCommand) {
        int index = myResultsList.getSelectedIndex();
        if (index < 0) {
            return Optional.empty();
        }

        SearchEverywhereContributor contributor = myListModel.getContributorForIndex(index);
        if (contributor != myStubCommandContributor) {
            return Optional.empty();
        }

        SearchEverywhereCommandInfo selectedCommand = (SearchEverywhereCommandInfo) myListModel.getElementAt(index);
        return selectedCommand.getCommand().contains(typedCommand) ? Optional.of(selectedCommand) : Optional.empty();
    }

    @Nonnull
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

    private void onMouseClicked(@Nonnull MouseEvent e) {
        boolean multiSelectMode = e.isShiftDown() || UIUtil.isControlKeyDown(e);
        if (e.getButton() == MouseEvent.BUTTON1 && !multiSelectMode) {
            e.consume();
            final int i = myResultsList.locationToIndex(e.getPoint());
            if (i > -1) {
                myResultsList.setSelectedIndex(i);
                elementsSelected(new int[]{i}, e.getModifiers());
            }
        }
    }

    private boolean isHintComponent(Component component) {
        return myHint != null && !myHint.isDisposed() && component != null
            && SwingUtilities.isDescendingFrom(component, myHint.getContent());
    }

    private void elementsSelected(int[] indexes, int modifiers) {
        if (indexes.length == 1 && myListModel.isMoreElement(indexes[0])) {
            SearchEverywhereContributor contributor = myListModel.getContributorForIndex(indexes[0]);
            showMoreElements(contributor);
            return;
        }

        indexes = Arrays.stream(indexes).filter(i -> !myListModel.isMoreElement(i)).toArray();

        String searchText = getSearchPattern();
        if (searchText.startsWith(SearchTopHitProvider.getTopHitAccelerator()) && searchText.contains(" ")) {
            //featureTriggered(SearchEverywhereUsageTriggerCollector.COMMAND_USED, null);
        }

        boolean closePopup = false;
        boolean isAllTab = isAllTabSelected();
        for (int i : indexes) {
            SearchEverywhereContributor<Object> contributor = myListModel.getContributorForIndex(i);
            Object value = myListModel.getElementAt(i);
            if (isAllTab) {
                //String reportableContributorID = SearchEverywhereUsageTriggerCollector.getReportableContributorID(contributor);
                //FeatureUsageData data = SearchEverywhereUsageTriggerCollector.createData(reportableContributorID);
                //featureTriggered(SearchEverywhereUsageTriggerCollector.CONTRIBUTOR_ITEM_SELECTED, data);
            }
            closePopup |= contributor.processSelectedItem(value, modifiers, searchText);
        }

        if (closePopup) {
            closePopup();
        }
        else {
            myProject.getApplication().invokeLater(() -> myResultsList.repaint());
        }
    }

    private void showMoreElements(SearchEverywhereContributor contributor) {
        //featureTriggered(SearchEverywhereUsageTriggerCollector.MORE_ITEM_SELECTED, null);
        Map<SearchEverywhereContributor<?>, Collection<SearchEverywhereFoundElementInfo>> found = myListModel.getFoundElementsMap();
        int limit = myListModel.getItemsForContributor(contributor) + (
            mySelectedTab.getContributor().isPresent() ? SINGLE_CONTRIBUTOR_ELEMENTS_LIMIT : MULTIPLE_CONTRIBUTORS_ELEMENTS_LIMIT
        );
        mySearchProgressIndicator = mySearcher.findMoreItems(found, getSearchPattern(), contributor, limit);
    }

    private void stopSearching() {
        if (mySearchProgressIndicator != null && !mySearchProgressIndicator.isCanceled()) {
            mySearchProgressIndicator.cancel();
        }
        if (myBufferedListener != null) {
            myBufferedListener.clearBuffer();
        }
    }

    private void closePopup() {
        ActionMenuUtil.showDescriptionInStatusBar(true, myResultsList, null);
        stopSearching();
        searchFinishedHandler.run();
    }

    @Nonnull
    private List<SearchEverywhereContributor<?>> getAllTabContributors() {
        return ContainerUtil.filter(
            myShownContributors,
            contributor -> myContributorsFilter.isSelected(contributor.getSearchProviderId())
        );
    }

    @Nonnull
    private Collection<SearchEverywhereContributor<?>> getContributorsForCurrentTab() {
        return isAllTabSelected() ? getAllTabContributors() : Collections.singleton(mySelectedTab.getContributor().get());
    }

    private class CompositeCellRenderer implements ListCellRenderer<Object> {

        @Override
        public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
        ) {
            if (value == SearchListModel.MORE_ELEMENT) {
                Component component = myMoreRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                ((JComponent) component).setBorder(JBCurrentTheme.listCellBorder());
                return component;
            }

            SearchEverywhereContributor<Object> contributor = myListModel.getContributorForIndex(index);
            JComponent component = (JComponent) SearchEverywhereClassifier.EP_Manager
                .getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (component == null) {
                component = (JComponent) contributor.getElementsRenderer().getListCellRendererComponent(list, value, index, isSelected, true);
            }

            GuiUtils.targetToDevice(component, list);
            component.setBorder(JBCurrentTheme.listCellBorder());
            if (isAllTabSelected() && myListModel.isGroupFirstItem(index)) {
                component = myGroupTitleRenderer.withDisplayedData(contributor.getFullGroupName(), component);
            }

            return component;
        }
    }

    private final ListCellRenderer<Object> myCommandRenderer = new ColoredListCellRenderer<>() {
        @Override
        protected void customizeCellRenderer(
            @Nonnull JList<?> list,
            Object value,
            int index,
            boolean selected,
            boolean hasFocus
        ) {
            setIcon(Image.empty(Image.DEFAULT_ICON_SIZE));
            setFont(list.getFont());

            SearchEverywhereCommandInfo command = (SearchEverywhereCommandInfo) value;
            append(
                command.getCommandWithPrefix() + " ",
                new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, list.getForeground())
            );
            append(command.getDefinition(), new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GRAY));
            setBackground(UIUtil.getListBackground(selected));
        }
    };

    private final ListCellRenderer<Object> myMoreRenderer = new ColoredListCellRenderer<>() {
        // todo
        //@Override
        //protected int getMinHeight() {
        //  return -1;
        //}

        @Override
        protected void customizeCellRenderer(
            @Nonnull JList<?> list,
            Object value,
            int index,
            boolean selected,
            boolean hasFocus
        ) {
            if (value != SearchListModel.MORE_ELEMENT) {
                throw new AssertionError(value);
            }
            setFont(UIUtil.getLabelFont().deriveFont(UIUtil.getFontSize(UIUtil.FontSize.SMALL)));
            append("... more", SMALL_LABEL_ATTRS);
            setIpad(JBInsets.create(1, 7));
        }
    };

    private final GroupTitleRenderer myGroupTitleRenderer = new GroupTitleRenderer();

    private static class GroupTitleRenderer extends CellRendererPanel {

        final SimpleColoredComponent titleLabel = new SimpleColoredComponent();

        GroupTitleRenderer() {
            setLayout(new BorderLayout());
            SeparatorComponent separatorComponent = new SeparatorComponent(
                titleLabel.getPreferredSize().height / 2,
                JBColor.border(),
                null
            );

            JPanel topPanel = JBUI.Panels.simplePanel(5, 0)
                .addToCenter(separatorComponent)
                .addToLeft(titleLabel)
                .withBorder(JBUI.Borders.empty(1, 7))
                .withBackground(UIUtil.getListBackground());
            add(topPanel, BorderLayout.NORTH);
        }

        public GroupTitleRenderer withDisplayedData(String title, Component itemContent) {
            titleLabel.clear();
            titleLabel.append(title, SMALL_LABEL_ATTRS);
            Component prevContent = ((BorderLayout) getLayout()).getLayoutComponent(BorderLayout.CENTER);
            if (prevContent != null) {
                remove(prevContent);
            }
            add(itemContent, BorderLayout.CENTER);
            return this;
        }
    }

    public static class SearchListModel extends AbstractListModel<Object> {

        static final Object MORE_ELEMENT = new Object();

        private final List<SearchEverywhereFoundElementInfo> listElements = new ArrayList<>();

        private boolean resultsExpired = false;

        public boolean isResultsExpired() {
            return resultsExpired;
        }

        public void expireResults() {
            resultsExpired = true;
        }

        @Override
        public int getSize() {
            return listElements.size();
        }

        @Override
        public Object getElementAt(int index) {
            return listElements.get(index).getElement();
        }

        public List<Object> getItems() {
            return new ArrayList<>(values());
        }

        public Collection<Object> getFoundItems(SearchEverywhereContributor contributor) {
            return listElements.stream()
                .filter(info -> info.getContributor() == contributor && info.getElement() != MORE_ELEMENT)
                .map(SearchEverywhereFoundElementInfo::getElement)
                .collect(Collectors.toList());
        }

        public boolean hasMoreElements(SearchEverywhereContributor contributor) {
            return listElements.stream().anyMatch(info -> info.getElement() == MORE_ELEMENT && info.getContributor() == contributor);
        }

        public void addElements(List<? extends SearchEverywhereFoundElementInfo> items) {
            if (items.isEmpty()) {
                return;
            }
            Map<SearchEverywhereContributor<?>, List<SearchEverywhereFoundElementInfo>> itemsMap = new HashMap<>();
            items.forEach(info -> {
                List<SearchEverywhereFoundElementInfo> list = itemsMap.computeIfAbsent(info.getContributor(), contributor -> new ArrayList<>());
                list.add(info);
            });
            itemsMap.forEach(
                (contributor, list) -> Collections.sort(list, Comparator.comparingInt(SearchEverywhereFoundElementInfo::getPriority).reversed())
            );

            if (resultsExpired) {
                retainContributors(itemsMap.keySet());
                clearMoreItems();

                itemsMap.forEach((contributor, list) -> {
                    Object[] oldItems = ArrayUtil.toObjectArray(getFoundItems(contributor));
                    Object[] newItems = list.stream().map(SearchEverywhereFoundElementInfo::getElement).toArray();
                    try {
                        Diff.Change change = Diff.buildChanges(oldItems, newItems);
                        applyChange(change, contributor, list);
                    }
                    catch (FilesTooBigForDiffException e) {
                        LOG.error("Cannot calculate diff for updated search results");
                    }
                });
                resultsExpired = false;
            }
            else {
                itemsMap.forEach((contributor, list) -> {
                    int startIndex = contributors().indexOf(contributor);
                    int insertionIndex = getInsertionPoint(contributor);
                    int endIndex = insertionIndex + list.size() - 1;
                    listElements.addAll(insertionIndex, list);
                    fireIntervalAdded(this, insertionIndex, endIndex);

                    // there were items for this contributor before update
                    if (startIndex >= 0) {
                        listElements.subList(startIndex, endIndex + 1)
                            .sort(Comparator.comparingInt(SearchEverywhereFoundElementInfo::getPriority).reversed());
                        fireContentsChanged(this, startIndex, endIndex);
                    }
                });
            }
        }

        private void retainContributors(Collection<SearchEverywhereContributor<?>> retainContributors) {
            Iterator<SearchEverywhereFoundElementInfo> iterator = listElements.iterator();
            int startInterval = 0;
            int endInterval = -1;
            while (iterator.hasNext()) {
                SearchEverywhereFoundElementInfo item = iterator.next();
                if (retainContributors.contains(item.getContributor())) {
                    if (startInterval <= endInterval) {
                        fireIntervalRemoved(this, startInterval, endInterval);
                        startInterval = endInterval + 2;
                    }
                    else {
                        startInterval++;
                    }
                }
                else {
                    iterator.remove();
                }
                endInterval++;
            }

            if (startInterval <= endInterval) {
                fireIntervalRemoved(this, startInterval, endInterval);
            }
        }

        private void clearMoreItems() {
            ListIterator<SearchEverywhereFoundElementInfo> iterator = listElements.listIterator();
            while (iterator.hasNext()) {
                int index = iterator.nextIndex();
                if (iterator.next().getElement() == MORE_ELEMENT) {
                    iterator.remove();
                    fireContentsChanged(this, index, index);
                }
            }
        }

        private void applyChange(Diff.Change change, SearchEverywhereContributor<?> contributor, List<SearchEverywhereFoundElementInfo> newItems) {
            int firstItemIndex = contributors().indexOf(contributor);
            if (firstItemIndex < 0) {
                firstItemIndex = getInsertionPoint(contributor);
            }

            for (Diff.Change ch : toRevertedList(change)) {
                if (ch.deleted > 0) {
                    for (int i = ch.deleted - 1; i >= 0; i--) {
                        int index = firstItemIndex + ch.line0 + i;
                        listElements.remove(index);
                    }
                    fireIntervalRemoved(this, firstItemIndex + ch.line0, firstItemIndex + ch.line0 + ch.deleted - 1);
                }

                if (ch.inserted > 0) {
                    List<SearchEverywhereFoundElementInfo> addedItems = newItems.subList(ch.line1, ch.line1 + ch.inserted);
                    listElements.addAll(firstItemIndex + ch.line0, addedItems);
                    fireIntervalAdded(this, firstItemIndex + ch.line0, firstItemIndex + ch.line0 + ch.inserted - 1);
                }
            }
        }

        private static List<Diff.Change> toRevertedList(Diff.Change change) {
            List<Diff.Change> res = new ArrayList<>();
            while (change != null) {
                res.add(0, change);
                change = change.link;
            }
            return res;
        }

        public void removeElement(@Nonnull Object item, SearchEverywhereContributor contributor) {
            int index = contributors().indexOf(contributor);
            if (index < 0) {
                return;
            }

            while (index < listElements.size() && listElements.get(index).getContributor() == contributor) {
                if (item.equals(listElements.get(index).getElement())) {
                    listElements.remove(index);
                    fireIntervalRemoved(this, index, index);
                    return;
                }
                index++;
            }
        }

        public void setHasMore(SearchEverywhereContributor<?> contributor, boolean newVal) {
            int index = contributors().lastIndexOf(contributor);
            if (index < 0) {
                return;
            }

            boolean alreadyHas = isMoreElement(index);
            if (alreadyHas && !newVal) {
                listElements.remove(index);
                fireIntervalRemoved(this, index, index);
            }

            if (!alreadyHas && newVal) {
                index += 1;
                listElements.add(index, new SearchEverywhereFoundElementInfo(MORE_ELEMENT, 0, contributor));
                fireIntervalAdded(this, index, index);
            }
        }

        public void clear() {
            int index = listElements.size() - 1;
            listElements.clear();
            if (index >= 0) {
                fireIntervalRemoved(this, 0, index);
            }
        }

        public boolean contains(Object val) {
            return values().contains(val);
        }

        public boolean isMoreElement(int index) {
            return listElements.get(index).getElement() == MORE_ELEMENT;
        }

        public <Item> SearchEverywhereContributor<Item> getContributorForIndex(int index) {
            //noinspection unchecked
            return (SearchEverywhereContributor<Item>) listElements.get(index).getContributor();
        }

        public boolean isGroupFirstItem(int index) {
            return index == 0 || listElements.get(index).getContributor() != listElements.get(index - 1).getContributor();
        }

        public int getItemsForContributor(SearchEverywhereContributor<?> contributor) {
            List<SearchEverywhereContributor> contributorsList = contributors();
            int first = contributorsList.indexOf(contributor);
            int last = contributorsList.lastIndexOf(contributor);
            if (isMoreElement(last)) {
                last -= 1;
            }
            return last - first + 1;
        }

        public Map<SearchEverywhereContributor<?>, Collection<SearchEverywhereFoundElementInfo>> getFoundElementsMap() {
            return listElements.stream()
                .filter(info -> info.element != MORE_ELEMENT)
                .collect(Collectors.groupingBy(SearchEverywhereFoundElementInfo::getContributor, Collectors.toCollection(ArrayList::new)));
        }

        @Nonnull
        private List<SearchEverywhereContributor> contributors() {
            return Lists.transform(listElements, SearchEverywhereFoundElementInfo::getContributor);
        }

        @Nonnull
        private List<Object> values() {
            return Lists.transform(listElements, SearchEverywhereFoundElementInfo::getElement);
        }

        private int getInsertionPoint(SearchEverywhereContributor contributor) {
            if (listElements.isEmpty()) {
                return 0;
            }

            List<SearchEverywhereContributor> list = contributors();
            int index = list.lastIndexOf(contributor);
            if (index >= 0) {
                return isMoreElement(index) ? index : index + 1;
            }

            index = Collections.binarySearch(
                list,
                contributor,
                Comparator.comparingInt(SearchEverywhereContributor::getSortWeight)
            );
            return -index - 1;
        }
    }

    private class ShowInFindToolWindowAction extends DumbAwareAction {

        ShowInFindToolWindowAction() {
            super(
                IdeLocalize.showInFindWindowButtonName(),
                IdeLocalize.showInFindWindowButtonDescription(),
                AllIcons.General.Pin_tab
            );
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            stopSearching();

            Collection<SearchEverywhereContributor<?>> contributors = getContributorsForCurrentTab();
            contributors = ContainerUtil.filter(contributors, SearchEverywhereContributor::showInFindResults);

            if (contributors.isEmpty()) {
                return;
            }

            String searchText = getSearchPattern();
            String contributorsString = contributors.stream()
                .map(SearchEverywhereContributor::getGroupName)
                .collect(Collectors.joining(", "));

            UsageViewPresentation presentation = new UsageViewPresentation();
            String tabCaptionText = IdeLocalize.searcheverywhereFoundMatchesTitle(searchText, contributorsString).get();
            presentation.setCodeUsagesString(tabCaptionText);
            presentation.setUsagesInGeneratedCodeString(
                IdeLocalize.searcheverywhereFoundMatchesGeneratedCodeTitle(searchText, contributorsString).get()
            );
            presentation.setTargetsNodeText(IdeBundle.message("searcheverywhere.found.targets.title", searchText, contributorsString));
            presentation.setTabName(tabCaptionText);
            presentation.setTabText(tabCaptionText);

            Collection<Usage> usages = new LinkedHashSet<>();
            Collection<PsiElement> targets = new LinkedHashSet<>();

            Collection<Object> cached = contributors.stream()
                .flatMap(contributor -> myListModel.getFoundItems(contributor).stream())
                .collect(Collectors.toSet());
            fillUsages(cached, usages, targets);

            Collection<SearchEverywhereContributor<?>> contributorsForAdditionalSearch;
            contributorsForAdditionalSearch = ContainerUtil.filter(contributors, contributor -> myListModel.hasMoreElements(contributor));

            closePopup();
            if (!contributorsForAdditionalSearch.isEmpty()) {
                ProgressManager.getInstance().run(new Task.Modal(myProject, tabCaptionText, true) {
                    private final ProgressIndicator progressIndicator = new ProgressIndicatorBase();

                    @Override
                    public void run(@Nonnull ProgressIndicator indicator) {
                        progressIndicator.start();
                        TooManyUsagesStatus tooManyUsagesStatus = TooManyUsagesStatus.createFor(progressIndicator);

                        Collection<Object> foundElements = new ArrayList<>();
                        int alreadyFoundCount = cached.size();
                        for (SearchEverywhereContributor<?> contributor : contributorsForAdditionalSearch) {
                            if (progressIndicator.isCanceled()) {
                                break;
                            }
                            try {
                                fetch(contributor, foundElements, alreadyFoundCount, tooManyUsagesStatus);
                            }
                            catch (ProcessCanceledException ignore) {
                            }
                        }
                        fillUsages(foundElements, usages, targets);
                    }

                    <Item> void fetch(
                        SearchEverywhereContributor<Item> contributor,
                        Collection<Object> foundElements,
                        int alreadyFoundCount,
                        TooManyUsagesStatus tooManyUsagesStatus
                    ) {
                        contributor.fetchElements(searchText, progressIndicator, o -> {
                            if (progressIndicator.isCanceled()) {
                                return false;
                            }

                            if (cached.contains(o)) {
                                return true;
                            }

                            foundElements.add(o);
                            tooManyUsagesStatus.pauseProcessingIfTooManyUsages();
                            if (
                                foundElements.size() + alreadyFoundCount >= UsageLimitUtil.USAGES_LIMIT
                                    && tooManyUsagesStatus.switchTooManyUsagesStatus()
                            ) {
                                int usageCount = foundElements.size() + alreadyFoundCount;
                                UsageViewManagerImpl.showTooManyUsagesWarningLater(
                                    (Project) getProject(),
                                    tooManyUsagesStatus,
                                    progressIndicator,
                                    presentation,
                                    usageCount,
                                    null
                                );
                                return !progressIndicator.isCanceled();
                            }
                            return true;
                        });
                    }

                    @Override
                    public void onCancel() {
                        progressIndicator.cancel();
                    }

                    @Override
                    public void onSuccess() {
                        showInFindWindow(targets, usages, presentation);
                    }

                    @Override
                    public void onThrowable(@Nonnull Throwable error) {
                        progressIndicator.cancel();
                    }
                });
            }
            else {
                showInFindWindow(targets, usages, presentation);
            }
        }

        private void fillUsages(
            Collection<Object> foundElements,
            Collection<? super Usage> usages,
            Collection<? super PsiElement> targets
        ) {
            ReadAction.run(
                () -> foundElements.stream()
                    .filter(o -> o instanceof PsiElement)
                    .forEach(
                        o -> {
                            PsiElement element = (PsiElement) o;
                            if (element.getTextRange() != null) {
                                UsageInfo usageInfo = new UsageInfo(element);
                                usages.add(new UsageInfo2UsageAdapter(usageInfo));
                            }
                            else {
                                targets.add(element);
                            }
                        }
                    )
            );
        }

        private void showInFindWindow(
            Collection<? extends PsiElement> targets,
            Collection<Usage> usages,
            UsageViewPresentation presentation
        ) {
            UsageTarget[] targetsArray = targets.isEmpty()
                ? UsageTarget.EMPTY_ARRAY
                : PsiElement2UsageTargetAdapter.convert(PsiUtilCore.toPsiElementArray(targets));
            Usage[] usagesArray = usages.toArray(Usage.EMPTY_ARRAY);
            UsageViewManager.getInstance(myProject).showUsages(targetsArray, usagesArray, presentation);
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            SearchEverywhereContributor<?> contributor = mySelectedTab == null ? null : mySelectedTab.contributor;
            e.getPresentation().setEnabled(contributor == null || contributor.showInFindResults());
            e.getPresentation().setIcon(
                ToolWindowManagerEx.getInstanceEx(myProject)
                    .getLocationIcon(ToolWindowId.FIND, AllIcons.General.Pin_tab)
            );
        }
    }

    static class FiltersAction extends ShowFilterAction {
        final PersistentSearchEverywhereContributorFilter<?> filter;
        final Runnable rebuildRunnable;

        FiltersAction(@Nonnull PersistentSearchEverywhereContributorFilter<?> filter, @Nonnull Runnable rebuildRunnable) {
            this.filter = filter;
            this.rebuildRunnable = rebuildRunnable;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        protected boolean isActive() {
            return filter.getAllElements().size() != filter.getSelectedElements().size();
        }

        @Override
        protected ElementsChooser<?> createChooser() {
            return createChooser(filter, rebuildRunnable);
        }

        private static <T> ElementsChooser<T> createChooser(
            @Nonnull PersistentSearchEverywhereContributorFilter<T> filter,
            @Nonnull Runnable rebuildRunnable
        ) {
            ElementsChooser<T> res = new ElementsChooser<>(filter.getAllElements(), false) {
                @Override
                protected String getItemText(@Nonnull T value) {
                    return filter.getElementText(value);
                }

                @Nullable
                @Override
                protected Image getItemIcon(@Nonnull T value) {
                    return filter.getElementIcon(value);
                }
            };
            res.markElements(filter.getSelectedElements());
            ElementsChooser.ElementsMarkListener<T> listener = (element, isMarked) -> {
                filter.setSelected(element, isMarked);
                rebuildRunnable.run();
            };
            res.addElementsMarkListener(listener);
            return res;
        }
    }

    private class CompleteCommandAction extends DumbAwareAction {
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            if (completeCommand()) {
                //FeatureUsageData data = SearchEverywhereUsageTriggerCollector.createData(null).addInputEvent(e);
                //featureTriggered(SearchEverywhereUsageTriggerCollector.COMMAND_COMPLETED, data);
            }
        }

        @Override
        @RequiredUIAccess
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setEnabled(getCompleteCommand().isPresent());
        }

        @RequiredUIAccess
        private boolean completeCommand() {
            Optional<SearchEverywhereCommandInfo> suggestedCommand = getCompleteCommand();
            if (suggestedCommand.isPresent()) {
                mySearchField.setValue(suggestedCommand.get().getCommandWithPrefix() + " ");
                return true;
            }

            return false;
        }

        private Optional<SearchEverywhereCommandInfo> getCompleteCommand() {
            String pattern = getSearchPattern();
            String commandPrefix = SearchTopHitProvider.getTopHitAccelerator();
            if (pattern.startsWith(commandPrefix) && !pattern.contains(" ")) {
                String typedCommand = pattern.substring(commandPrefix.length());
                SearchEverywhereCommandInfo command = getSelectedCommand(typedCommand).orElseGet(() -> {
                    List<SearchEverywhereCommandInfo> completions = getCommandsForCompletion(getContributorsForCurrentTab(), typedCommand);
                    return completions.isEmpty() ? null : completions.get(0);
                });

                return Optional.ofNullable(command);
            }

            return Optional.empty();
        }
    }

    private String getNotFoundText() {
        return mySelectedTab.getContributor()
            .map(c -> IdeLocalize.searcheverywhereNothingFoundForContributorAnywhere(c.getFullGroupName()).get())
            .orElse(IdeLocalize.searcheverywhereNothingFoundForAllAnywhere().get());
    }

    private final SearchListener mySearchListener = new SearchListener();

    private class SearchListener implements SESearcher.Listener {
        @Override
        public void elementsAdded(@Nonnull List<? extends SearchEverywhereFoundElementInfo> list) {
            boolean wasEmpty = myListModel.listElements.isEmpty();

            mySelectionTracker.lock();
            myListModel.addElements(list);
            mySelectionTracker.unlock();

            mySelectionTracker.restoreSelection();

            if (wasEmpty && !myListModel.listElements.isEmpty()) {
                Object prevSelection = ((SearchEverywhereManagerImpl) SearchEverywhereManager.getInstance(myProject))
                    .getPrevSelection(getSelectedContributorID());
                if (prevSelection instanceof Integer) {
                    for (SearchEverywhereFoundElementInfo info : myListModel.listElements) {
                        if (Objects.hashCode(info.element) == (Integer) prevSelection) {
                            myResultsList.setSelectedValue(info.element, true);
                            break;
                        }
                    }
                }
            }
        }

        @Override
        public void elementsRemoved(@Nonnull List<? extends SearchEverywhereFoundElementInfo> list) {
            list.forEach(info -> myListModel.removeElement(info.getElement(), info.getContributor()));
        }

        @Override
        public void searchFinished(@Nonnull Map<SearchEverywhereContributor<?>, Boolean> hasMoreContributors) {
            if (myResultsList.isEmpty() || myListModel.isResultsExpired()) {
                if (myEverywhereAutoSet && !isEverywhere() && canToggleEverywhere() && !getSearchPattern().isEmpty()) {
                    setEverywhereAuto(true);
                    myNotFoundString = getSearchPattern();
                    return;
                }

                hideHint();
                if (myListModel.isResultsExpired()) {
                    myListModel.clear();
                }
            }

            myResultsList.setEmptyText(getSearchPattern().isEmpty() ? "" : getNotFoundText());
            hasMoreContributors.forEach(myListModel::setHasMore);

            mySelectionTracker.resetSelectionIfNeeded();
        }
    }

    private final SearchEverywhereContributor<Object> myStubCommandContributor = new SearchEverywhereContributor<>() {
        @Nonnull
        @Override
        public String getSearchProviderId() {
            return "CommandsContributor";
        }

        @Nonnull
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
        public void fetchElements(
            @Nonnull String pattern,
            @Nonnull ProgressIndicator progressIndicator,
            @Nonnull Processor<? super Object> consumer
        ) {
        }

        @Override
        @RequiredUIAccess
        public boolean processSelectedItem(@Nonnull Object selected, int modifiers, @Nonnull String searchText) {
            mySearchField.setValue(((SearchEverywhereCommandInfo) selected).getCommandWithPrefix() + " ");
            //featureTriggered(SearchEverywhereUsageTriggerCollector.COMMAND_COMPLETED, null);
            return false;
        }

        @Nonnull
        @Override
        public ListCellRenderer<? super Object> getElementsRenderer() {
            return myCommandRenderer;
        }

        @Nullable
        @Override
        public Object getDataForItem(@Nonnull Object element, @Nonnull Key dataId) {
            return null;
        }
    };
}
