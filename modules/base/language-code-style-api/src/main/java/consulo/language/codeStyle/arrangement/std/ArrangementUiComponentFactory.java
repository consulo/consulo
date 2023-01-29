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
package consulo.language.codeStyle.arrangement.std;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.codeStyle.arrangement.ArrangementColorsProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface ArrangementUiComponentFactory {
  ExtensionPointName<ArrangementUiComponentFactory> EP_NAME = ExtensionPointName.create(ArrangementUiComponentFactory.class);

  @Nullable
  ArrangementUiComponent build(@Nonnull StdArrangementTokenUiRole role,
                               @Nonnull List<ArrangementSettingsToken> tokens,
                               @Nonnull ArrangementColorsProvider colorsProvider,
                               @Nonnull ArrangementStandardSettingsManager settingsManager);
}
