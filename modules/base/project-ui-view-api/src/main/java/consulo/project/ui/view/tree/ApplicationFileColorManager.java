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
package consulo.project.ui.view.tree;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.util.lang.function.BooleanConsumer;
import jakarta.annotation.Nonnull;

import java.util.function.BooleanSupplier;

/**
 * @author VISTALL
 * @since 21-Jul-24
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ApplicationFileColorManager {
  boolean isEnabled();

  void setEnabled(boolean enabled);

  boolean isEnabledForTabs();

  void setEnabledForTabs(boolean enabled);

  boolean isEnabledForProjectView();

  void setEnabledForProjectView(boolean enabled);

  default boolean isFileColorsEnabledFor(BooleanSupplier opaqueGet,
                                         BooleanConsumer opaqueSet) {
    boolean enabled = isEnabled() && isEnabledForProjectView();
    boolean opaque = opaqueGet.getAsBoolean();
    if (enabled && opaque) {
      opaqueSet.accept(false);
    }
    else if (!enabled && !opaque) {
      opaqueSet.accept(true);
    }
    return enabled;
  }

  @Nonnull
  static ApplicationFileColorManager getInstance() {
    return Application.get().getInstance(ApplicationFileColorManager.class);
  }
}
