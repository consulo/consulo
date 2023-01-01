/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.diff.actions.impl;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil;
import consulo.application.dumb.DumbAware;

public abstract class NextDifferenceAction extends AnAction implements DumbAware {
  public NextDifferenceAction() {
    ActionUtil.copyFrom(this, IdeActions.ACTION_NEXT_DIFF);
  }
}
