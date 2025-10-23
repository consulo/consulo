/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.tipOfDay;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-06-23
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface TipOfDayManager {
  /**
   * Schedule show tips dialog. Will open one time, per app start (even if multiple projects open)
   */
  void scheduleShow(@Nonnull UIAccess uiAccess, @Nonnull Project project);

  /**
   * Force show tip dialog without any checks
   */
  @RequiredUIAccess
  void showAsync();
}
