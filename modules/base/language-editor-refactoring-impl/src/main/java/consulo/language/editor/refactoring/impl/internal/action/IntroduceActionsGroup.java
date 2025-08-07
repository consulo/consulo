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
package consulo.language.editor.refactoring.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author UNV
 * @since 2025-08-06
 */
@ActionImpl(
    id = "IntroduceActionsGroup",
    children = {
        @ActionRef(type = IntroduceVariableAction.class),
        @ActionRef(type = IntroduceConstantAction.class),
        @ActionRef(type = IntroduceFieldAction.class),
        @ActionRef(type = IntroduceParameterAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = ExtractMethodAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = ExtractClassAction.class),
        @ActionRef(type = ExtractIncludeAction.class),
        @ActionRef(type = ExtractInterfaceAction.class),
        @ActionRef(type = ExtractSuperclassAction.class),
        @ActionRef(type = ExtractModuleAction.class)
    }
)
public class IntroduceActionsGroup extends DefaultActionGroup implements DumbAware {
    public IntroduceActionsGroup() {
        super(ActionLocalize.groupIntroduceactionsgroupText(), true);
    }
}
