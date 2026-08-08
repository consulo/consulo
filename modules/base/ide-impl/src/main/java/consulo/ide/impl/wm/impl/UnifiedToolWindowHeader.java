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
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ui.impl.internal.wm.ToolWindowBase;
import consulo.project.ui.impl.internal.wm.action.TabbedContentAction;
import consulo.project.ui.internal.ToolWindowContentUI;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
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
import consulo.ui.ex.toolWindow.ToolWindowInternalDecorator;
import consulo.ui.ex.toolWindow.action.ToolWindowActions;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.Style;
import consulo.ui.style.StyleManager;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
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

    private final DockLayout myLayout = DockLayout.create(0);
    private final HorizontalLayout myWestLayout = HorizontalLayout.create(4);
    private final HorizontalLayout myTabsLayout = HorizontalLayout.create(2);

    private final DefaultActionGroup myTitleActions = new DefaultActionGroup();
    private final DefaultActionGroup myTabActions = new DefaultActionGroup();

    private final PresentationFactory myPresentationFactory = new MenuItemPresentationFactory();

    private final UnifiedActionRow myTabActionRow = createActionRow(() -> myTabActions);
    private final UnifiedActionRow myTitleActionRow = createActionRow(
        () -> ActionGroup.newImmutableBuilder().addAll(myTitleActions, new GearActionGroup(), new HideAction()).build()
    );

    @RequiredUIAccess
    public UnifiedToolWindowHeader(
        ToolWindowBase toolWindow,
        ToolWindowInternalDecorator decorator,
        Supplier<ActionGroup> gearProducer
    ) {
        myToolWindow = toolWindow;
        myDecorator = decorator;
        myGearProducer = gearProducer;

        myLayout.setSize(new Size2D(-1, HEIGHT));

        myWestLayout.add(Label.create(toolWindow.getDisplayName()));
        myWestLayout.add(myTabsLayout);
        myWestLayout.add(myTabActionRow.getComponent());

        myLayout.left(myWestLayout);
        myLayout.right(myTitleActionRow.getComponent());

        ContentManager contentManager = toolWindow.getContentManagerIfCreated();
        if (contentManager != null) {
            contentManager.addContentManagerListener(new ContentManagerListener() {
                @Override
                @RequiredUIAccess
                public void contentAdded(ContentManagerEvent event) {
                    rebuildTabs();
                }

                @Override
                @RequiredUIAccess
                public void contentRemoved(ContentManagerEvent event) {
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

    public Component getComponent() {
        return myLayout;
    }

    @RequiredUIAccess
    public void setAdditionalTitleActions(AnAction[] actions) {
        myTitleActions.removeAll();
        myTitleActions.addAll(actions);

        myTitleActionRow.updateAsync();
    }

    @RequiredUIAccess
    public void setTabActions(AnAction[] actions) {
        myTabActions.removeAll();
        myTabActions.addAll(actions);

        myTabActionRow.updateAsync();
    }

    @RequiredUIAccess
    public void updateActionsAsync() {
        myTabActionRow.updateAsync();
        myTitleActionRow.updateAsync();
    }

    @RequiredUIAccess
    private void rebuildTabs() {
        myTabsLayout.removeAll();

        ContentManager contentManager = myToolWindow.getContentManagerIfCreated();
        if (contentManager == null || !hasTabsToShow(contentManager)) {
            return;
        }

        Content selected = contentManager.getSelectedContent();

        for (Content content : contentManager.getContents()) {
            myTabsLayout.add(createTab(contentManager, content, content == selected));
        }
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
    private Component createTab(ContentManager contentManager, Content content, boolean selected) {
        Style style = StyleManager.get().getCurrentStyle();

        Button tab = Button.create(LocalizeValue.of(content.getTabName()));
        tab.setIcon(content.getIcon());
        // never the primary style - that paints the accent color of the toolkit, which is not a color of the theme.
        // the borderless text follows the theme already, the foreground of a component is not settable on every
        // frontend
        tab.addStyle(ButtonStyle.BORDERLESS);
        tab.addClickListener(event -> {
            contentManager.setSelectedContent(content, true);

            myToolWindow.fireActivated();
        });

        // the label and the cross are one tab, so the underline of the selection has to be drawn under both of
        // them rather than under the label alone
        HorizontalLayout tabLayout = HorizontalLayout.create(0);
        tabLayout.add(tab);

        if (isCloseable(contentManager, content)) {
            Button close = Button.create(LocalizeValue.of(""));
            close.setIcon(PlatformIconGroup.actionsClose());
            // the inplace style is a lumo variant, and a theme which is not lumo leaves it looking like a button
            close.addStyle(ButtonStyle.BORDERLESS);
            close.addClickListener(event -> contentManager.removeContent(content, true));

            tabLayout.add(close);
        }

        if (selected) {
            tabLayout.addBorder(
                BorderPosition.BOTTOM,
                BorderStyle.LINE,
                style.getColorValue(ComponentColors.TABBED_PANE_UNDERLINE),
                2
            );
        }

        return tabLayout;
    }

    private UnifiedActionRow createActionRow(Supplier<ActionGroup> groupProducer) {
        return new UnifiedActionRow(
            groupProducer,
            this::createDataContext,
            ActionPlaces.TOOLWINDOW_TITLE,
            ToolWindowContentUI.POPUP_PLACE,
            myPresentationFactory,
            ActionToolbar.Style.HORIZONTAL
        );
    }

    private DataContext createDataContext() {
        DataManager dataManager = DataManager.getInstance();

        // the groups are expanded off the ui thread, the providers have to be snapshotted before that
        return dataManager.createAsyncDataContext(dataManager.getDataContext(myToolWindow.getUIComponent()));
    }

    @Override
    public void dispose() {
        myTabActionRow.cancel();
        myTitleActionRow.cancel();
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
