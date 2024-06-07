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
package consulo.ui.ex.action;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 12.05.2024
 */
public class MoreActionGroup extends DefaultActionGroup {
  private final boolean myHorizontal;

  public MoreActionGroup() {
    this(true);
  }

  public MoreActionGroup(boolean horizontal) {
    myHorizontal = horizontal;
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    Presentation presentation = e.getPresentation();

    presentation.setIcon(myHorizontal ? PlatformIconGroup.actionsMorehorizontal() : PlatformIconGroup.actionsMorevertical());
    presentation.putClientProperty(ActionButton.HIDE_DROPDOWN_ICON, true);
    // TODO not supported presentation.setHideGroupIfEmpty(true);
  }
}
