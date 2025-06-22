/*
 * Copyright 2013-2016 consulo.io
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
package consulo.desktop.awt.welcomeScreen;

import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.externalService.statistic.UsageTrigger;
import consulo.ide.impl.welcomeScreen.WelcomeScreenSlider;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ui.internal.NotificationIconBuilder;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.touchBar.TouchBarController;
import consulo.ui.ex.awt.JBCardLayout;
import consulo.ui.ex.awt.TitlelessDecorator;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.layout.VerticalLayout;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 14-Sep-16
 */
public class FlatWelcomeScreen extends JPanel implements WelcomeScreenSlider {
    public static final String MAIN = "main";

    private final FlatWelcomePanel myMainWelcomePanel;
    private final FlatWelcomeFrame myWelcomeFrame;
    private final TitlelessDecorator myTitlelessDecorator;

    private Consumer<List<NotificationType>> myEventListener;
    private Supplier<Point> myEventLocation;

    private WelcomeDesktopBalloonLayoutImpl myWelcomeDesktopBalloonLayout;

    @RequiredUIAccess
    public FlatWelcomeScreen(FlatWelcomeFrame welcomeFrame, TitlelessDecorator titlelessDecorator) {
        super(new JBCardLayout());
        myTitlelessDecorator = titlelessDecorator;
        DataManager.registerDataProvider(this, dataId -> {
            if (KEY == dataId) {
                return this;
            }
            return null;
        });

        myWelcomeFrame = welcomeFrame;
        myMainWelcomePanel = new FlatWelcomePanel(welcomeFrame, titlelessDecorator) {
            @Override
            @RequiredUIAccess
            public JComponent createActionPanel() {
                return FlatWelcomeScreen.this.createActionPanel(this);
            }
        };
        add(myMainWelcomePanel, MAIN);

        registerKeyboardAction(e -> {
            for (Component component : getComponents()) {
                if (component.isVisible() && component != myMainWelcomePanel) {
                    removeSlide((JComponent) component);
                    break;
                }
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    @RequiredUIAccess
    private JComponent createActionPanel(FlatWelcomePanel welcomePanel) {
        VerticalLayout layout = VerticalLayout.create();

        ActionManager actionManager = ActionManager.getInstance();
        ActionGroup quickStart = (ActionGroup) actionManager.getAction(IdeActions.GROUP_WELCOME_SCREEN_QUICKSTART);
        List<AnAction> group = new ArrayList<>();
        collectAllActions(group, quickStart);

        DataManager manager = DataManager.getInstance();

        for (AnAction action : group) {
            AnActionEvent e = AnActionEvent.createFromAnAction(action,
                null,
                ActionPlaces.WELCOME_SCREEN,
                manager.getDataContext(welcomePanel)
            );

            action.update(e);

            Button button = Button.create(e.getPresentation().getTextValue());
            button.setIcon(e.getPresentation().getIcon());
            button.addStyle(ButtonStyle.BORDERLESS);
            button.addClickListener(event -> {
                AnActionEvent in = AnActionEvent.createFromAnAction(action,
                    null,
                    ActionPlaces.WELCOME_SCREEN,
                    manager.getDataContext(event.getComponent()),
                    event.getInputDetails()
                );
                
                action.actionPerformed(in);
            });

            layout.add(button);
        }

        layout.add(createActionComponent(LocalizeValue.localizeTODO("Configure"),
            IdeActions.GROUP_WELCOME_SCREEN_CONFIGURE,
            PlatformIconGroup.welcomePreferences())
        );

        layout.add(createEventsLink());

        layout.add(createActionComponent(LocalizeValue.localizeTODO("Get Help"),
            IdeActions.GROUP_WELCOME_SCREEN_DOC,
            PlatformIconGroup.welcomeHelp())
        );

        JComponent component = (JComponent) TargetAWT.to(layout);
        TouchBarController.getInstance().setActions(component, quickStart);
        return component;
    }

    public Consumer<List<NotificationType>> getEventListener() {
        return myEventListener;
    }

    public Supplier<Point> getEventLocation() {
        return myEventLocation;
    }

    public void setWelcomeDesktopBalloonLayout(WelcomeDesktopBalloonLayoutImpl welcomeDesktopBalloonLayout) {
        myWelcomeDesktopBalloonLayout = welcomeDesktopBalloonLayout;
    }

    @RequiredUIAccess
    private consulo.ui.Component createEventsLink() {
        Button eventsButton = createActionComponent(LocalizeValue.localizeTODO("Notifications"),
            PlatformIconGroup.toolwindowsNotifications(),
            (e) -> myWelcomeDesktopBalloonLayout.showPopup()
        );

        eventsButton.setVisible(false);

        myEventListener = types -> {
            eventsButton.setIcon(NotificationIconBuilder.getIcon(types));
            eventsButton.setVisible(true);
        };

        myEventLocation = () -> {
            Point location = SwingUtilities.convertPoint(TargetAWT.to(eventsButton), 0, 0, getRootPane().getLayeredPane());
            return new Point(location.x, location.y + 5);
        };

        return eventsButton;
    }

    public static void collectAllActions(List<AnAction> group, ActionGroup actionGroup) {
        for (AnAction action : actionGroup.getChildren(null)) {
            if (action instanceof ActionGroup && !((ActionGroup) action).isPopup()) {
                collectAllActions(group, (ActionGroup) action);
            }
            else {
                group.add(action);
            }
        }
    }

    @RequiredUIAccess
    private consulo.ui.Component createActionComponent(final LocalizeValue text, final String groupId, Image icon) {
        Consumer<ClickEvent> runnable = (e) -> {
            consulo.ui.Component component = e.getComponent();
            InputDetails inputDetails = Objects.requireNonNull(e.getInputDetails());

            ActionGroup configureGroup = (ActionGroup) ActionManager.getInstance().getAction(groupId);
            ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu("WelcomeActions", configureGroup);
            menu.show(component, inputDetails.getX(), inputDetails.getY());
            UsageTrigger.trigger("welcome.screen." + groupId);
        };

        return createActionComponent(text, icon, runnable);
    }

    @RequiredUIAccess
    public static Button createActionComponent(LocalizeValue text, consulo.ui.image.Image icon, Consumer<ClickEvent> runnable) {
        Button button = Button.create(text, runnable::accept);
        button.addStyle(ButtonStyle.BORDERLESS);
        button.setIcon(icon);
        return button;
    }

    @Override
    public void setTitle(@Nonnull String title) {
        myWelcomeFrame.setTitle(title);
    }

    @Override
    public void removeSlide(@Nonnull JComponent target) {
        JBCardLayout layout = (JBCardLayout) getLayout();

        layout.swipe(this, MAIN, JBCardLayout.SwipeDirection.BACKWARD, () -> remove(target));

        if (target instanceof Disposable disposable) {
            disposable.disposeWithTree();
        }

        myWelcomeFrame.setDefaultTitle();
    }

    @Override
    public Disposable getDisposable() {
        return myWelcomeFrame;
    }

    @Override
    public TitlelessDecorator getTitlelessDecorator() {
        return myTitlelessDecorator;
    }
}
