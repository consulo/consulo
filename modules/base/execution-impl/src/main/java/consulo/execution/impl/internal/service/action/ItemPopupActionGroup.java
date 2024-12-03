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
package consulo.execution.impl.internal.service.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.execution.impl.internal.service.ServiceViewActionProvider;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
* @author VISTALL
* @since 17.05.2024
*/
@ActionImpl(id = "ServiceViewItemPopupGroup")
public final class ItemPopupActionGroup extends ActionGroup implements DumbAware {
  @Override
  @Nonnull
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    return ServiceViewActionProvider.doGetActions(e, false);
  }
}
