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

import com.intellij.openapi.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-06-23
 */
@Singleton
public class UnifiedTipOfDayManager implements TipOfDayManager {
  @Override
  public void scheduleShow(@Nonnull UIAccess uiAccess, @Nonnull Project project) {

  }

  @RequiredUIAccess
  @Override
  public void showAsync() {
    
  }
}
