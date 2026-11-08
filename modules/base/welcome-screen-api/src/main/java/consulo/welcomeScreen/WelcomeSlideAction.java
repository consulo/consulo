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
package consulo.welcomeScreen;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TitlelessDecorator;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-25
 */
public abstract class WelcomeSlideAction extends DumbAwareAction {
    protected WelcomeSlideAction(LocalizeValue text, LocalizeValue description, @Nullable Image icon) {
        super(text, description, icon);
    }

    protected abstract LocalizeValue getSlideTitle();

    @RequiredUIAccess
    protected abstract WelcomeSlide createSlide(Disposable parentDisposable, TitlelessDecorator titlelessDecorator);

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        WelcomeScreenSlider slider = e.getData(WelcomeScreenSlider.KEY);
        if (slider == null) {
            return;
        }

        slider.setTitle(getSlideTitle());

        WelcomeSlide slide = createSlide(slider.getDisposable(), slider.getTitlelessDecorator());
        Disposer.register(slider.getDisposable(), slide);

        slide.setCloseAction(() -> {
            slider.removeSlide(slide.getLayout());

            Disposer.dispose(slide);
        });

        slider.showSlide(slide.getClass().getName(), slide::getLayout);
    }
}
