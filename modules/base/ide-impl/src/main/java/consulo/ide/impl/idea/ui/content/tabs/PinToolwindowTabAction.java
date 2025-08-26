/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui.content.tabs;

import consulo.annotation.component.ActionImpl;
import consulo.ide.impl.idea.ide.actions.PinActiveTabAction;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.Toggleable;
import consulo.ui.ex.toolWindow.action.ToolWindowActions;

/**
 * @author spleaner
 * @deprecated use {@link PinActiveTabAction}
 */
@ActionImpl(id = ToolWindowActions.ACTION_NAME)
@Deprecated
public class PinToolwindowTabAction extends PinActiveTabAction.TW implements Toggleable {
    public static AnAction getPinAction() {
        return ToolWindowActions.getPinAction();
    }
}
