/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.editor.impl.internal.generation;

import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionImpl;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;

/**
 * @author VISTALL
 * @since 17-Jul-22
 */
@ActionImpl(id = IdeActions.GROUP_GENERATE, children = {
        @ActionRef(type = OverrideMethodsAction.class),
        @ActionRef(type = ImplementMethodsAction.class),
        @ActionRef(type = DelegateMethodsAction.class),
        @ActionRef(type = GenerateByPatternAction.class)
})
public class GenerateGroup extends DefaultActionGroup {
}
