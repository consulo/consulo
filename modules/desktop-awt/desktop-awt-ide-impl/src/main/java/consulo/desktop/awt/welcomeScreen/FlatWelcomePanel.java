/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import consulo.application.ApplicationProperties;
import consulo.desktop.awt.startup.splash.AnimatedLogoLabel;
import consulo.disposer.Disposable;
import consulo.ide.impl.welcomeScreen.BaseWelcomeScreenPanel;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.TitlelessDecorator;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.MorphColor;
import consulo.ui.ex.awt.AWTTitlelessDecorator;
import consulo.ui.ex.awt.VerticalFlowLayout;
import consulo.ui.style.StyleManager;

import javax.swing.*;
import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public abstract class FlatWelcomePanel extends BaseWelcomeScreenPanel {
    @RequiredUIAccess
    public FlatWelcomePanel(FlatWelcomeFrame flatWelcomeFrame, TitlelessDecorator titlelessDecorator) {
        super(flatWelcomeFrame, titlelessDecorator);
    }

    @RequiredUIAccess
    public abstract JComponent createActionPanel();

    
    @Override
    protected JComponent createLeftComponent(Disposable parentDisposable) {
        return new NewRecentProjectPanel(parentDisposable, true).getRootPanel();
    }

    @RequiredUIAccess
    @Override
    
    protected JComponent createRightComponent() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setBorder(JBUI.Borders.empty(53, 22, 45, 0));
        Color foreground = MorphColor.ofWithoutCache(() -> ApplicationProperties.isInSandbox()
            ? StyleManager.get().getCurrentStyle().isDark() ? JBColor.LIGHT_GRAY : JBColor.WHITE
            : JBColor.GRAY
        );
        AnimatedLogoLabel animatedLogoLabel = new AnimatedLogoLabel(8, foreground, false, true);
        logoPanel.add(animatedLogoLabel, BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new VerticalFlowLayout(false, false));
        topPanel.add(logoPanel);
        topPanel.add(createActionPanel());

        panel.add(topPanel, BorderLayout.NORTH);
        return panel;
    }
}
