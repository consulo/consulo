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

import consulo.disposer.Disposable;
import consulo.ide.impl.welcomeScreen.WelcomeScreenSlider;
import consulo.ide.localize.IdeLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.JBCardLayout;
import consulo.ui.ex.awt.TitlelessDecorator;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2024-11-24
 */
public class WelcomeNewProjectAction extends NewProjectAction {
    @Override
    public boolean displayTextInToolbar() {
        return true;
    }

    @Nonnull
    @RequiredUIAccess
    public JComponent createSlide(@Nonnull Disposable parentDisposable,
                                  @Nonnull WelcomeScreenSlider owner,
                                  @Nonnull TitlelessDecorator titlelessDecorator) {
        owner.setTitle(IdeLocalize.titleNewProject().get());

        return new SlideNewProjectPanel(parentDisposable, owner, null, null, titlelessDecorator);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        WelcomeScreenSlider slider = e.getRequiredData(WelcomeScreenSlider.KEY);
        TitlelessDecorator titlelessDecorator = e.getRequiredData(TitlelessDecorator.KEY);

        JPanel sliderPanel = (JPanel) slider;

        JComponent panel = createSlide(slider.getDisposable(), slider, titlelessDecorator);

        JBCardLayout layout = (JBCardLayout) sliderPanel.getLayout();
        
        String id = getClass().getName();

        sliderPanel.add(panel, id);

        layout.swipe(sliderPanel, id, JBCardLayout.SwipeDirection.FORWARD);
    }

    @Nullable
    @Override
    protected Image getTemplateIcon() {
        return PlatformIconGroup.welcomeCreatenewproject();
    }
}
