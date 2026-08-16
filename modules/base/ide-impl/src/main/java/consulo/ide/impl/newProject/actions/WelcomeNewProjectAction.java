/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ide.impl.newProject.actions;

import consulo.annotation.component.ActionImpl;
import consulo.disposer.Disposer;
import consulo.ide.impl.module.creation.UnifiedNewProjectPanel;
import consulo.ide.impl.welcomeScreen.UnifiedWelcomeScreenSlider;
import consulo.ide.impl.welcomeScreen.WelcomeScreenSlider;
import consulo.ide.localize.IdeLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.JBCardLayout;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2024-11-24
 */
@ActionImpl(id = "WelcomeScreen.CreateNewProject")
public class WelcomeNewProjectAction extends NewProjectAction {
    public WelcomeNewProjectAction() {
        super(
            ActionLocalize.actionWelcomescreenCreatenewprojectText(),
            ActionLocalize.actionWelcomescreenCreatenewprojectDescription(),
            PlatformIconGroup.welcomeCreatenewproject()
        );
    }

    @Override
    public boolean displayTextInToolbar() {
        return true;
    }

    @RequiredUIAccess
    private void showUnifiedSlide(UnifiedWelcomeScreenSlider slider) {
        slider.setTitle(IdeLocalize.titleNewProject());

        UnifiedNewProjectPanel panel =
            new UnifiedNewProjectPanel(slider.getDisposable(), null, slider.getTitlelessDecorator());
        Disposer.register(slider.getDisposable(), panel);

        panel.setDefaultActions(
            () -> generateProject(null, panel),
            () -> {
                slider.removeSlide(panel.getLayout());

                Disposer.dispose(panel);
            }
        );

        slider.showSlide(UnifiedNewProjectPanel.class.getName(), panel::getLayout);
    }

    @RequiredUIAccess
    private void showSwingSlide(WelcomeScreenSlider slider) {
        slider.setTitle(IdeLocalize.titleNewProject().get());

        UnifiedNewProjectPanel panel =
            new UnifiedNewProjectPanel(slider.getDisposable(), null, slider.getTitlelessDecorator());
        Disposer.register(slider.getDisposable(), panel);

        JComponent slideComponent = (JComponent) TargetAWT.to(panel.getLayout());

        panel.setDefaultActions(
            () -> generateProject(null, panel),
            () -> {
                slider.removeSlide(slideComponent);

                Disposer.dispose(panel);
            }
        );

        JPanel sliderPanel = (JPanel) slider;

        JBCardLayout layout = (JBCardLayout) sliderPanel.getLayout();

        String id = UnifiedNewProjectPanel.class.getName();

        sliderPanel.add(slideComponent, id);

        layout.swipe(sliderPanel, id, JBCardLayout.SwipeDirection.FORWARD);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        UnifiedWelcomeScreenSlider unifiedSlider = e.getData(UnifiedWelcomeScreenSlider.KEY);
        if (unifiedSlider != null) {
            showUnifiedSlide(unifiedSlider);
            return;
        }

        showSwingSlide(e.getRequiredData(WelcomeScreenSlider.KEY));
    }
}
