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
package consulo.welcomeScreen.impl.internal;

import consulo.welcomeScreen.WelcomeSlide;
import consulo.module.creation.impl.internal.UnifiedNewProjectPanel;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.Layout;

/**
 * The new project wizard as a slide of the welcome screen.
 *
 * @author VISTALL
 * @since 2026-11-08
 */
public class NewProjectWelcomeSlide implements WelcomeSlide {
    private final UnifiedNewProjectPanel myPanel;

    public NewProjectWelcomeSlide(UnifiedNewProjectPanel panel) {
        myPanel = panel;
    }

    @Override
    @RequiredUIAccess
    public Layout getLayout() {
        return myPanel.getLayout();
    }

    @Override
    public void setCloseAction(@RequiredUIAccess Runnable closeAction) {
        myPanel.setCloseAction(closeAction);
    }

    @Override
    public void dispose() {
        myPanel.dispose();
    }
}
