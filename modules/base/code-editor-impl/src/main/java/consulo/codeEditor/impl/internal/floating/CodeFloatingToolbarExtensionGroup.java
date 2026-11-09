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
package consulo.codeEditor.impl.internal.floating;

import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.toolbar.floating.CodeFloatingToolbarActions;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * Each customized toolbar group must include this group.
 *
 * @author VISTALL
 * @since 2026-11-09
 */
@ActionImpl(id = CodeFloatingToolbarActions.EXTENSIONS_GROUP)
public class CodeFloatingToolbarExtensionGroup extends DefaultActionGroup {
}
