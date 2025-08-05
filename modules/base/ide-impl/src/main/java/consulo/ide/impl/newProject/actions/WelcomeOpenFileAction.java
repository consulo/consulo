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
import consulo.ide.impl.idea.ide.actions.OpenFileAction;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;

/**
 * @author VISTALL
 * @since 2024-11-24
 */
@ActionImpl(id = "WelcomeScreen.OpenProject")
public class WelcomeOpenFileAction extends OpenFileAction {
    public WelcomeOpenFileAction() {
        super(
            ActionLocalize.actionWelcomescreenOpenprojectText(),
            ActionLocalize.actionWelcomescreenOpenprojectDescription(),
            PlatformIconGroup.welcomeOpenproject()
        );
    }

    @Override
    public boolean displayTextInToolbar() {
        return true;
    }
}
