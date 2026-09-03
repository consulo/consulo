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
package consulo.ide.impl.welcomeScreen;

import consulo.dataContext.DataManager;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.ImageBox;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.TitlelessDecorator;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.impl.internal.action.ActionRunnerAsync;
import consulo.ui.ex.internal.LogoImage;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.SwipeLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StandardColors;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Unified counterpart of {@link BaseWelcomeScreenPanel} - the same two pane structure built only from
 * unified ui, shared by every frontend.
 *
 * @author VISTALL
 * @since 2026-08-15
 */
public abstract class BaseUnifiedWelcomeScreenPanel implements UnifiedWelcomeScreenSlider {
    public static final String MAIN = "main";

    protected final Disposable myParentDisposable;
    protected final DataManager myDataManager;
    protected final ActionManager myActionManager;
    protected final TitlelessDecorator myTitlelessDecorator;

    private @Nullable SwipeLayout myRoot;

    protected BaseUnifiedWelcomeScreenPanel(
        Disposable parentDisposable,
        DataManager dataManager,
        ActionManager actionManager,
        TitlelessDecorator titlelessDecorator
    ) {
        myParentDisposable = parentDisposable;
        myDataManager = dataManager;
        myActionManager = actionManager;
        myTitlelessDecorator = titlelessDecorator;
    }

    @Override
    public TitlelessDecorator getTitlelessDecorator() {
        return myTitlelessDecorator;
    }

    @RequiredUIAccess
    public Component getComponent() {
        SwipeLayout root = myRoot;
        if (root == null) {
            myRoot = root = SwipeLayout.create();
            root.putUserData(UiDataProvider.KEY, sink -> sink.set(UnifiedWelcomeScreenSlider.KEY, this));
            root.register(MAIN, this::buildMainLayout);
        }
        return root;
    }

    @RequiredUIAccess
    private Layout buildMainLayout() {
        DockLayout layout = DockLayout.create();

        Component leftComponent = createLeftComponent(myParentDisposable);
        leftComponent.borderBuilder().rightSet().apply();
        myTitlelessDecorator.makeLeftComponentLower(leftComponent);
        layout.left(leftComponent);

        layout.center(myTitlelessDecorator.modifyRightComponent(layout, createRightComponent()));

        return layout;
    }

    @RequiredUIAccess
    protected abstract Component createLeftComponent(Disposable parentDisposable);

    @RequiredUIAccess
    protected Component createRightComponent() {
        // the awt screen stacks the logo and every entry in a single column and centres each of them on the
        // width of the widest, rather than stretching them
        VerticalLayout rightLayout = VerticalLayout.create(Space.NONE, HorizontalAlignment.CENTER);

        ImageBox logo = ImageBox.create(LogoImage.create(8, StandardColors.GRAY));
        // the insets the awt screen keeps around its logo
        logo.paddingBuilder().verticalSet(Space.XXX_LARGE).apply();
        rightLayout.add(logo);

        VerticalLayout actionLayout = VerticalLayout.create(Space.NONE, HorizontalAlignment.CENTER);
        rightLayout.add(actionLayout);

        // quick start is filled once the platform has answered which of its actions are visible, so it is a
        // layout of its own - otherwise it would land after the two group entries
        VerticalLayout quickStartLayout = VerticalLayout.create(Space.NONE, HorizontalAlignment.CENTER);
        actionLayout.add(quickStartLayout);

        addActionGroup(quickStartLayout, IdeActions.GROUP_WELCOME_SCREEN_QUICKSTART);

        // the awt screen keeps these two as groups behind a single entry rather than listing what is in them
        actionLayout.add(createGroupButton(
            UILocalize.welcomeScreenConfigureActionGroupName(),
            IdeActions.GROUP_WELCOME_SCREEN_CONFIGURE,
            PlatformIconGroup.welcomePreferences()
        ));

        addCustomActionEntries(actionLayout);

        actionLayout.add(createGroupButton(
            UILocalize.welcomeScreenGetHelpActionGroupName(),
            IdeActions.GROUP_WELCOME_SCREEN_DOC,
            PlatformIconGroup.welcomeHelp()
        ));

        DockLayout rightDock = DockLayout.create();
        rightDock.top(rightLayout);
        return rightDock;
    }

    @RequiredUIAccess
    protected void addCustomActionEntries(VerticalLayout actionLayout) {
    }

    /**
     * A group stays a group - the entry opens it as a popup, the way the awt welcome screen does.
     */
    @RequiredUIAccess
    protected Button createGroupButton(LocalizeValue text, String groupId, Image icon) {
        Button button = Button.create(text, event -> {
            ActionGroup group = (ActionGroup) myActionManager.getAction(groupId);
            if (group == null) {
                return;
            }

            InputDetails inputDetails = Objects.requireNonNull(event.getInputDetails());

            ActionPopupMenu menu = myActionManager.createActionPopupMenu(ActionPlaces.WELCOME_SCREEN, group);
            menu.show(event.getComponent(), inputDetails.getX(), inputDetails.getY());
        });
        button.addStyle(ButtonStyle.BORDERLESS);
        button.setIcon(icon);
        return button;
    }

    /**
     * Every action of the group as a link, once the platform has told which of them are visible.
     */
    @RequiredUIAccess
    protected void addActionGroup(VerticalLayout target, String groupId) {
        ActionGroup actionGroup = (ActionGroup) myActionManager.getAction(groupId);
        if (actionGroup == null) {
            return;
        }

        List<AnAction> group = new ArrayList<>();
        collectAllActions(group, actionGroup);

        Component rootComponent = getComponent();

        List<AnActionEvent> events = new ArrayList<>(group.size());
        List<CompletableFuture<?>> updates = new ArrayList<>(group.size());
        for (AnAction action : group) {
            AnActionEvent e =
                AnActionEvent.createFromAnAction(action, null, ActionPlaces.WELCOME_SCREEN, myDataManager.getDataContext(rootComponent));
            events.add(e);
            updates.add(ActionRunnerAsync.performDumbAwareUpdateAsync(action, e));
        }

        UIAccess uiAccess = UIAccess.current();
        CompletableFuture.allOf(updates.toArray(new CompletableFuture[0])).whenCompleteAsync((r, throwable) -> {
            for (int i = 0; i < group.size(); i++) {
                AnAction action = group.get(i);
                AnActionEvent e = events.get(i);

                Presentation presentation = e.getPresentation();
                if (presentation.isVisible()) {
                    // the event above answered whether the action is shown at all, it says nothing about the click
                    // which follows - an action opening a popup reads the position out of the event it is given,
                    // and it is only the click that has one
                    Button component = Button.create(presentation.getTextValue(), event -> {
                        AnActionEvent clickEvent = new AnActionEvent(
                            null,
                            myDataManager.getDataContext(event.getComponent()),
                            ActionPlaces.WELCOME_SCREEN,
                            presentation,
                            myActionManager,
                            0,
                            false,
                            false,
                            event.getInputDetails()
                        );
                        clickEvent.setInjectedContext(action.isInInjectedContext());

                        action.actionPerformed(clickEvent);
                    });
                    component.addStyle(ButtonStyle.BORDERLESS);
                    component.setIcon(presentation.getIcon());

                    target.add(component);
                }
            }
        }, uiAccess);
    }

    public static void collectAllActions(List<AnAction> group, ActionGroup actionGroup) {
        for (AnAction action : actionGroup.getChildren(null)) {
            if (action instanceof ActionGroup childGroup && !childGroup.isPopup()) {
                collectAllActions(group, childGroup);
            }
            else {
                group.add(action);
            }
        }
    }

    @Override
    @RequiredUIAccess
    public Layout showSlide(String id, @RequiredUIAccess Supplier<Layout> layoutSupplier) {
        SwipeLayout root = (SwipeLayout) getComponent();
        root.register(id, layoutSupplier);
        return root.swipeLeftTo(id);
    }

    @Override
    @RequiredUIAccess
    public void removeSlide(Layout target) {
        SwipeLayout root = (SwipeLayout) getComponent();
        root.swipeRightTo(MAIN);
        root.remove(target);

        if (target instanceof Disposable disposable) {
            disposable.disposeWithTree();
        }
    }

    @Override
    public Disposable getDisposable() {
        return myParentDisposable;
    }
}
