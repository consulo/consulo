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
package consulo.ide.impl.idea.diagnostic;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
@ActionImpl(
    id = "Internal.Errors",
    children = {
        @ActionRef(type = DropAnErrorAction.class),
        @ActionRef(type = DropAnErrorWithAttachmentsAction.class),
        @ActionRef(type = DropAnOutOfMemoryErrorAction.class),
        @ActionRef(type = DropAnOutOfPermGenMemoryErrorAction.class)
    },
    parents = @ActionParentRef(@ActionRef(id = "Internal"))
)
public class InternalErrorsActionGroup extends DefaultActionGroup {
    public InternalErrorsActionGroup() {
        super(LocalizeValue.localizeTODO("Exceptions"), true);
    }
}
