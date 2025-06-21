/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ide.impl.actionSystem;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author VISTALL
 * @since 21.06.2025.
 */
@ActionImpl(
        id = "TouchBarDefaultExecution",
        children = {
                @ActionRef(id = "RunConfiguration"),
                @ActionRef(id = "RunnerActionsTouchbar"),
                @ActionRef(id = "Stop")
        },
        parents = @ActionParentRef(value = @ActionRef(id = "TouchBarDefault"), anchor = ActionRefAnchor.FIRST)
)
public class TouchBarDefaultExecutionActionGroup extends DefaultActionGroup {
}
