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
package consulo.ide.impl.idea.ide.ui;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.ComponentManager;
import consulo.ui.ex.action.BooleanOptionDescription;
import consulo.ide.localize.IdeLocalize;
import consulo.ui.ex.action.OptionsTopHitProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;

/**
* @author VISTALL
* @since 12-Jun-24
*/
@ExtensionImpl
public class AppearanceOptionsTopHitProviderEx extends OptionsTopHitProvider implements OptionsTopHitProvider.CoveredByToggleActions {
  private static final Collection<BooleanOptionDescription> ourOptions = List.of(
    AppearanceOptionsTopHitProvider.appearance(IdeLocalize.labelOptionWindow(IdeLocalize.optionHideToolWindowBars()), "HIDE_TOOL_STRIPES"),
    AppearanceOptionsTopHitProvider.appearance(IdeLocalize.labelOptionView(IdeLocalize.showMainToolbar()), "SHOW_MAIN_TOOLBAR"),
    AppearanceOptionsTopHitProvider.appearance(IdeLocalize.labelOptionView(IdeLocalize.showStatusBar()), "SHOW_STATUS_BAR"),
    AppearanceOptionsTopHitProvider.appearance(IdeLocalize.labelOptionView(IdeLocalize.showNavigationBar()), "SHOW_NAVIGATION_BAR")
  );

  @Nonnull
  @Override
  public Collection<BooleanOptionDescription> getOptions(@Nullable ComponentManager project) {
    return ourOptions;
  }

  @Override
  public String getId() {
    return AppearanceOptionsTopHitProvider.ID;
  }
}
