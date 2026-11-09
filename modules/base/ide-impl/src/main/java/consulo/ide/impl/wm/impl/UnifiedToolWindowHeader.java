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
package consulo.ide.impl.wm.impl;

import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ui.impl.internal.wm.ToolWindowBase;
import consulo.project.ui.impl.internal.wm.UnifiedToolWindowContentUI;
import consulo.project.ui.impl.internal.wm.UnifiedToolWindowImpl;
import consulo.project.ui.impl.internal.wm.action.CloseContentTabAction;
import consulo.project.ui.impl.internal.wm.action.TabbedContentAction;
import consulo.project.ui.internal.ToolWindowContentUI;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.internal.ToolWindowEx;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.PopupHandler;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.ui.ex.impl.internal.action.MenuItemPresentationFactory;
import consulo.ui.ex.impl.internal.action.UnifiedActionRow;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowContentUiType;
import consulo.ui.ex.toolWindow.ToolWindowInternalDecorator;
import consulo.ui.ex.toolWindow.WindowInfo;
import consulo.ui.ex.toolWindow.action.ToolWindowActions;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.TabbedLayoutStyle;
import consulo.ui.impl.style.IslandsState;
import consulo.ui.style.ComponentColors;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Header of a tool window for the frontends which render the unified {@link consulo.ui} components rather than
 * swing. Analog of {@code consulo.desktop.awt.wm.impl.DesktopToolWindowHeader} - the tool window title, the tabs
 * of the content manager, the tab and title actions, the gear group and the hide button, plus the context menu
 * of the tool window.
 *
 * @author VISTALL
 * @since 2026-07-31
 */
public class UnifiedToolWindowHeader implements Disposable {
    public static final int HEIGHT = 26;

    private final ToolWindowBase myToolWindow;
    private final ToolWindowInternalDecorator myDecorator;
    private final Supplier<ActionGroup> myGearProducer;
    private final Supplier<DataContext> myTabDataContextProvider;
    private final Supplier<DataContext> myTitleDataContextProvider;

    private final DockLayout myLayout = DockLayout.create(Space.NONE);
    private final HorizontalLayout myWestLayout = HorizontalLayout.create(Space.SMALL);
    private final HorizontalLayout myTabsLayout = HorizontalLayout.create(Space.X_SMALL);
    private final Label myTitleLabel;

    private final TabbedLayout myTabbedLayout = TabbedLayout.create();
    private final Label myTabbedTitleLabel;
    private final Map<Content, Tab> myTabs = new LinkedHashMap<>();
    private boolean myTabbed;
    private boolean myUpdatingTabs;

    private final DefaultActionGroup myTitleActions = new DefaultActionGroup();
    private final DefaultActionGroup myTabActions = new DefaultActionGroup();

    private final PresentationFactory myPresentationFactory = new MenuItemPresentationFactory();

    private @Nullable ToolWindowContentUiType myRenderedContentUiType;

    private final Map<Content, ActionToolbar> myCloseToolbars = new HashMap<>();

    private final PropertyChangeListener myContentPropertyListener = event -> {
        String propertyName = event.getPropertyName();
        if (Content.PROP_ICON.equals(propertyName)
            || Content.PROP_DISPLAY_NAME.equals(propertyName)
            || Content.IS_CLOSABLE.equals(propertyName)) {
            UIAccess uiAccess = myLayout.getUIAccess();
            if (uiAccess != null) {
                uiAccess.giveIfNeed(this::rebuildTabs);
            }
        }
        else if (Content.PROP_PINNED.equals(propertyName) && event.getSource() instanceof Content content) {
            UIAccess uiAccess = myLayout.getUIAccess();
            if (uiAccess != null) {
                uiAccess.giveIfNeed(() -> updateCloseToolbar(content));
            }
        }
    };

    private final PropertyChangeListener myToolWindowPropertyListener = this::toolWindowPropertyChanged;

    private final UnifiedActionRow myTabActionRow = createActionRow(() -> myTabActions, this::createTabDataContext);
    private final UnifiedActionRow myTitleActionRow = createTitleActionRow();
    private final UnifiedActionRow myTabbedTabActionRow = createActionRow(() -> myTabActions, this::createTabDataContext);
    private final UnifiedActionRow myTabbedTitleActionRow = createTitleActionRow();

    @RequiredUIAccess
    public UnifiedToolWindowHeader(
        ToolWindowBase toolWindow,
        ToolWindowInternalDecorator decorator,
        Supplier<ActionGroup> gearProducer
    ) {
        myToolWindow = toolWindow;
        myDecorator = decorator;
        myGearProducer = gearProducer;

        ContentManager contentManager = toolWindow.getContentManagerIfCreated();

        myTabDataContextProvider = ActionToolbarContexts.forTargetComponent(toolWindow.getUIComponent());
        myTitleDataContextProvider = contentManager == null
            ? myTabDataContextProvider
            : ActionToolbarContexts.forSelectedContent(contentManager, toolWindow.getUIComponent());

        myLayout.setSize(new Size2D(-1, HEIGHT));

        myTitleLabel = Label.create(toolWindow.getDisplayName());
        toolWindow.addPropertyChangeListener(myToolWindowPropertyListener);
        myWestLayout.add(myTitleLabel);
        myWestLayout.add(myTabsLayout);
        myWestLayout.add(myTabActionRow.getComponent());

        myLayout.left(myWestLayout);
        myLayout.right(myTitleActionRow.getComponent());

        myTabbedTitleLabel = Label.create(toolWindow.getDisplayName());
        myTabbedTitleLabel.paddingBuilder().leftSet(Space.MEDIUM).rightSet(Space.SMALL).apply();

        DockLayout tabbedSuffix = DockLayout.create(Space.NONE);
        tabbedSuffix.left(myTabbedTabActionRow.getComponent());
        tabbedSuffix.right(myTabbedTitleActionRow.getComponent());

        myTabbedLayout.addStyle(TabbedLayoutStyle.NO_PADDING);
        if (IslandsState.isEnabled()) {
            myTabbedLayout.setBackgroundColor(ComponentColors.TOOL_WINDOW_BACKGROUND);
        }
        myTabbedLayout.setPrefixComponent(myTabbedTitleLabel);
        myTabbedLayout.setSuffixComponent(tabbedSuffix);
        myTabbedLayout.addSelectListener(event -> tabSelected(event.getTab()));

        if (contentManager != null) {
            contentManager.addContentManagerListener(new ContentManagerListener() {
                @Override
                @RequiredUIAccess
                public void contentAdded(ContentManagerEvent event) {
                    // a tab is drawn from what the content says of itself, and a content says it later too -
                    // the icon of a build turns green only once the build is over
                    event.getContent().addPropertyChangeListener(myContentPropertyListener);

                    rebuildTabs();
                }

                @Override
                @RequiredUIAccess
                public void contentRemoved(ContentManagerEvent event) {
                    event.getContent().removePropertyChangeListener(myContentPropertyListener);

                    rebuildTabs();
                }

                @Override
                @RequiredUIAccess
                public void selectionChanged(ContentManagerEvent event) {
                    rebuildTabs();

                    updateActionsAsync();
                }
            });
        }

        // a frontend without an awt hierarchy hands over a stand in which installs its own popup instead,
        // see consulo.ui.ex.awt.ActionPopupMenuInstaller
        PopupHandler.installPopupHandler(
            (JComponent) TargetAWT.to(myLayout),
            new HeaderPopupGroup(),
            ToolWindowContentUI.POPUP_PLACE
        );

        rebuildTabs();

        updateActionsAsync();
    }

    public DockLayout getComponent() {
        return myLayout;
    }

    private void toolWindowPropertyChanged(PropertyChangeEvent event) {
        if (!ToolWindowEx.PROP_STRIPE_TITLE.equals(event.getPropertyName())) {
            return;
        }

        if (UIAccess.isUIThread()) {
            updateTitle();
            return;
        }

        UIAccess uiAccess = myLayout.getUIAccess();
        if (uiAccess != null) {
            uiAccess.give(this::updateTitle);
        }
    }

    @RequiredUIAccess
    public void setAdditionalTitleActions(AnAction[] actions) {
        myTitleActions.removeAll();
        myTitleActions.addAll(actions);

        myTitleActionRow.updateAsync();
        myTabbedTitleActionRow.updateAsync();
    }

    @RequiredUIAccess
    public void setTabActions(AnAction[] actions) {
        myTabActions.removeAll();
        myTabActions.addAll(actions);

        myTabActionRow.updateAsync();
        myTabbedTabActionRow.updateAsync();
    }

    @RequiredUIAccess
    public void updateActionsAsync() {
        myTabActionRow.updateAsync();
        myTitleActionRow.updateAsync();
        myTabbedTabActionRow.updateAsync();
        myTabbedTitleActionRow.updateAsync();
    }

    @RequiredUIAccess
    private void updateTitle() {
        boolean visible = !Boolean.TRUE.equals(myToolWindow.getUserData(ToolWindow.HIDE_ID_LABEL));
        for (Label label : List.of(myTitleLabel, myTabbedTitleLabel)) {
            label.setText(myToolWindow.getDisplayName());
            label.setVisible(visible);
        }
    }

    @RequiredUIAccess
    private void rebuildTabs() {
        myTabsLayout.removeAll();

        myRenderedContentUiType = getContentUiType();

        updateTitle();

        ContentManager contentManager = myToolWindow.getContentManagerIfCreated();
        boolean tabsToShow = contentManager != null && hasTabsToShow(contentManager);

        if (tabsToShow && myRenderedContentUiType != ToolWindowContentUiType.COMBO) {
            enterTabbed();
            syncTabs(contentManager);
            return;
        }

        leaveTabbed();

        Content selected = contentManager == null ? null : contentManager.getSelectedContent();
        if (tabsToShow && selected != null) {
            myTabsLayout.add(createCombo(contentManager, selected));
        }
    }

    @RequiredUIAccess
    private void enterTabbed() {
        if (myTabbed) {
            return;
        }

        UnifiedToolWindowContentUI contentUI = getContentUI();
        if (contentUI == null) {
            return;
        }

        myTabbed = true;
        myLayout.setVisible(false);
        contentUI.setTabbedLayout(myTabbedLayout);
    }

    @RequiredUIAccess
    private void leaveTabbed() {
        if (!myTabbed) {
            return;
        }

        myTabbed = false;

        myUpdatingTabs = true;
        try {
            for (Tab tab : myTabs.values()) {
                myTabbedLayout.removeTab(tab);
            }
        }
        finally {
            myUpdatingTabs = false;
        }
        myTabs.clear();
        myCloseToolbars.clear();

        UnifiedToolWindowContentUI contentUI = getContentUI();
        if (contentUI != null) {
            contentUI.setTabbedLayout(null);
        }
        myLayout.setVisible(true);
    }

    @RequiredUIAccess
    private void syncTabs(ContentManager contentManager) {
        myUpdatingTabs = true;
        try {
            for (Iterator<Map.Entry<Content, Tab>> iterator = myTabs.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<Content, Tab> entry = iterator.next();
                if (contentManager.getIndexOfContent(entry.getKey()) < 0) {
                    myTabbedLayout.removeTab(entry.getValue());
                    myCloseToolbars.remove(entry.getKey());
                    iterator.remove();
                }
            }

            Content[] contents = contentManager.getContents();
            for (int i = 0; i < contents.length; i++) {
                Content content = contents[i];
                Tab tab = myTabs.get(content);
                if (tab == null) {
                    tab = createTab(content);
                    myTabs.put(content, tab);
                    myTabbedLayout.addTab(tab, content.getUIComponent(), i);
                }

                updateCloseToolbar(contentManager, content, tab);
                tab.update();
            }

            Content selected = contentManager.getSelectedContent();
            Tab selectedTab = selected == null ? null : myTabs.get(selected);
            if (selectedTab != null && myTabbedLayout.getSelectedTab() != selectedTab) {
                selectedTab.select();
            }
        }
        finally {
            myUpdatingTabs = false;
        }
    }

    private Tab createTab(Content content) {
        Tab tab = myTabbedLayout.createTab();
        tab.setRenderer((t, presentation) -> {
            if (Boolean.TRUE.equals(content.getUserData(ToolWindow.SHOW_CONTENT_ICON))) {
                presentation.withIcon(content.getIcon());
            }
            presentation.append(content.getTabName());
        });
        return tab;
    }

    @RequiredUIAccess
    private void updateCloseToolbar(ContentManager contentManager, Content content, Tab tab) {
        boolean closeable = isCloseable(contentManager, content);
        ActionToolbar toolbar = myCloseToolbars.get(content);
        if (closeable && toolbar == null) {
            toolbar = ActionToolbarFactory.getInstance().createActionToolbar(
                ActionPlaces.TOOLWINDOW_TITLE,
                ActionGroup.newImmutableBuilder().add(new CloseContentTabAction(contentManager, content)).build(),
                ActionToolbar.Style.INPLACE
            );
            toolbar.setTargetUIComponent(content.getUIComponent());
            toolbar.updateActionsAsync();
            myCloseToolbars.put(content, toolbar);
            tab.setSuffixComponent(toolbar.getUIComponent());
        }
        else if (!closeable && toolbar != null) {
            myCloseToolbars.remove(content);
            tab.setSuffixComponent(null);
        }
    }

    @RequiredUIAccess
    private void tabSelected(Tab tab) {
        if (myUpdatingTabs) {
            return;
        }

        ContentManager contentManager = myToolWindow.getContentManagerIfCreated();
        if (contentManager == null) {
            return;
        }

        for (Map.Entry<Content, Tab> entry : myTabs.entrySet()) {
            if (entry.getValue() == tab) {
                if (entry.getKey() != contentManager.getSelectedContent()) {
                    contentManager.setSelectedContent(entry.getKey(), true);

                    myToolWindow.fireActivated();
                }
                return;
            }
        }
    }

    private @Nullable UnifiedToolWindowContentUI getContentUI() {
        return myToolWindow instanceof UnifiedToolWindowImpl toolWindow ? toolWindow.getContentUI() : null;
    }

    @RequiredUIAccess
    public void updateContentUiType() {
        if (myRenderedContentUiType != getContentUiType()) {
            rebuildTabs();
        }
    }

    private @Nullable ToolWindowContentUiType getContentUiType() {
        WindowInfo windowInfo = myDecorator.getWindowInfo();
        return windowInfo == null ? null : windowInfo.getContentUiType();
    }

    @RequiredUIAccess
    private Component createCombo(ContentManager contentManager, Content selected) {
        ComboBox<Content> comboBox = ComboBox.create(contentManager.getContents());
        comboBox.setRenderer((presentation, item) -> {
            Content content = item.getValue();
            if (content == null) {
                return;
            }
            if (Boolean.TRUE.equals(content.getUserData(ToolWindow.SHOW_CONTENT_ICON))) {
                presentation.withIcon(content.getIcon());
            }
            presentation.append(content.getTabName());
        });
        comboBox.setValue(selected);
        comboBox.addStyle(ComboBoxStyle.INPLACE);
        comboBox.addStyle(ComboBoxStyle.TRANSPARENT_BACKGROUND);
        comboBox.addValueListener(event -> {
            Content content = event.getValue();
            if (content != null && content != contentManager.getSelectedContent()) {
                contentManager.setSelectedContent(content, true);

                myToolWindow.fireActivated();
            }
        });
        return comboBox;
    }

    /**
     * A lone content is the tool window itself and needs no tab, but one which can be closed has to offer the
     * way to close it.
     */
    private static boolean hasTabsToShow(ContentManager contentManager) {
        int count = contentManager.getContentCount();
        if (count >= 2) {
            return true;
        }
        return count == 1 && isCloseable(contentManager, contentManager.getContents()[0]);
    }

    /**
     * A content answers that it is closeable by default, so a tool window holding a single content - the project
     * view - would look closeable on its own. What decides is the tool window, which is what the close actions
     * of the platform ask as well.
     */
    private static boolean isCloseable(ContentManager contentManager, Content content) {
        return contentManager.canCloseContents() && content.isCloseable();
    }

    @RequiredUIAccess
    private void updateCloseToolbar(Content content) {
        ActionToolbar toolbar = myCloseToolbars.get(content);
        if (toolbar != null) {
            toolbar.updateActionsAsync();
        }
    }

    private UnifiedActionRow createTitleActionRow() {
        return createActionRow(
            () -> ActionGroup.newImmutableBuilder().addAll(myTitleActions, new GearActionGroup(), new HideAction()).build(),
            this::createTitleDataContext
        );
    }

    private UnifiedActionRow createActionRow(Supplier<ActionGroup> groupProducer, Supplier<DataContext> contextProducer) {
        return new UnifiedActionRow(
            groupProducer,
            contextProducer,
            ActionPlaces.TOOLWINDOW_TITLE,
            ToolWindowContentUI.POPUP_PLACE,
            myPresentationFactory,
            ActionToolbar.Style.HORIZONTAL
        );
    }

    private DataContext createTabDataContext() {
        return myTabDataContextProvider.get();
    }

    private DataContext createTitleDataContext() {
        return myTitleDataContextProvider.get();
    }

    @Override
    public void dispose() {
        myToolWindow.removePropertyChangeListener(myToolWindowPropertyListener);
        myTabActionRow.cancel();
        myTitleActionRow.cancel();
        myTabbedTabActionRow.cancel();
        myTabbedTitleActionRow.cancel();
    }

    /**
     * Options of the tool window, shown as a drop down of the header. The children are asked from the decorator
     * on every expansion - which of them apply depends on the window info, which changes while the header lives.
     */
    private class GearActionGroup extends ActionGroup {
        private GearActionGroup() {
            super(LocalizeValue.localizeTODO("Options"), true);

            getTemplatePresentation().setIcon(PlatformIconGroup.actionsMorevertical());
        }

        @Override
        public AnAction[] getChildren(@Nullable AnActionEvent e) {
            ActionGroup group = myGearProducer.get();
            return group == null ? EMPTY_ARRAY : group.getChildren(e);
        }

        @Override
        public boolean isDumbAware() {
            return true;
        }
    }

    private class HideAction extends LegacyDumbAwareAction {
        private HideAction() {
            super(UILocalize.toolWindowHideActionName(), LocalizeValue.empty(), PlatformIconGroup.generalHidetoolwindow());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            myDecorator.fireHidden();
        }

        @Override
        public void update(AnActionEvent event) {
            event.getPresentation().setEnabled(myDecorator.getWindowInfo().isVisible());
        }
    }

    /**
     * Context menu of the header - the actions of the selected content followed by the popup group of the
     * decorator, the same pair the awt header shows.
     */
    private class HeaderPopupGroup extends ActionGroup {
        @Override
        public AnAction[] getChildren(@Nullable AnActionEvent e) {
            DefaultActionGroup group = new DefaultActionGroup();

            ContentManager contentManager = myToolWindow.getContentManagerIfCreated();
            Content selected = contentManager == null ? null : contentManager.getSelectedContent();
            if (selected != null) {
                group.add(new TabbedContentAction.CloseAction(selected));
                group.add(new TabbedContentAction.CloseAllAction(contentManager));
                group.add(new TabbedContentAction.CloseAllButThisAction(selected));
                group.addSeparator();

                if (selected.isPinnable()) {
                    group.add(ToolWindowActions.getPinAction());
                    group.addSeparator();
                }

                group.add(new TabbedContentAction.MyNextTabAction(contentManager));
                group.add(new TabbedContentAction.MyPreviousTabAction(contentManager));
                group.addSeparator();
            }

            group.addAll(myDecorator.createPopupGroup());

            return group.getChildren(e);
        }

        @Override
        public boolean isDumbAware() {
            return true;
        }
    }
}
