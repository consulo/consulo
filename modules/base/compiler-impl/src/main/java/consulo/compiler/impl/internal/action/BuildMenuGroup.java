/*
 * Copyright 2013-2023 consulo.io
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
package consulo.compiler.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;

/**
 * @author VISTALL
 * @since 2023-11-13
 */
@ActionImpl(id = "BuildMenu", children = {
  @ActionRef(type = CompileDirtyAction.class),
  @ActionRef(type = MakeModuleAction.class),
  @ActionRef(type = CompileAction.class),
  @ActionRef(type = AnSeparator.class),
  @ActionRef(type = CompileProjectAction.class),
  @ActionRef(type = AnSeparator.class),
  @ActionRef(id = "BuildArtifact")
}, parents = @ActionParentRef(value = @ActionRef(id = IdeActions.GROUP_MAIN_MENU), anchor = ActionRefAnchor.BEFORE, relatedToAction = @ActionRef(id = IdeActions.GROUP_RUN)))
public class BuildMenuGroup extends DefaultActionGroup {
  @Override
  public boolean isPopup() {
    return true;
  }
}
