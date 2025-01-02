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
import consulo.ide.impl.welcomeScreen.WelcomeScreenSlider;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.JBCardLayout;
import consulo.ui.ex.awt.TitlelessDecorator;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.VerticalLayout;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 14-Sep-16
 */
public class FlatWelcomeScreen extends JPanel implements WelcomeScreenSlider {
    public static final String MAIN = "main";

    private final FlatWelcomePanel myMainWelcomePanel;
    private final FlatWelcomeFrame myWelcomeFrame;
    private final TitlelessDecorator myTitlelessDecorator;

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

    public FlatWelcomePanel getMainWelcomePanel() {
        return myMainWelcomePanel;
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
            AnActionEvent e = AnActionEvent.createFromAnAction(action, null, ActionPlaces.WELCOME_SCREEN, manager.getDataContext(welcomePanel));

            action.update(e);

            Button button = Button.create(e.getPresentation().getTextValue());
            button.setIcon(e.getPresentation().getIcon());
            button.addStyle(ButtonStyle.BORDERLESS);
            button.addClickListener(event -> action.actionPerformed(e));

            layout.add(button);
        }

        return (JComponent) TargetAWT.to(layout);
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

    @Override
    public void setTitle(@Nonnull String title) {
        myWelcomeFrame.setTitle(title);
    }

    @Override
    public void removeSlide(@Nonnull JComponent target) {
        JBCardLayout layout = (JBCardLayout) getLayout();

        layout.swipe(this, MAIN, JBCardLayout.SwipeDirection.BACKWARD, () -> remove(target));

        if (target instanceof Disposable) {
            ((Disposable) target).dispose();
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
