/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors.
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
package consulo.versionControlSystem.log.impl.internal.ui.action;

import consulo.annotation.component.ActionImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.versionControlSystem.log.impl.internal.VcsLogTabLocation;

@ActionImpl(id = "Vcs.Log.OpenAnotherTab")
public class OpenAnotherLogTabInToolWindowAction extends OpenAnotherLogTabAction {
    public OpenAnotherLogTabInToolWindowAction() {
        super(PlatformIconGroup.generalAdd());
    }

    @Override
    protected VcsLogTabLocation getLocation(AnActionEvent e) {
        return VcsLogTabLocation.TOOL_WINDOW;
    }
}
