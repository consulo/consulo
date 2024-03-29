/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.codeStyle.arrangement;

import consulo.colorScheme.TextAttributes;
import consulo.language.codeStyle.arrangement.std.ArrangementSettingsToken;
import jakarta.annotation.Nonnull;

import java.awt.*;

/**
 * @author Denis Zhdanov
 * @since 10/23/12 11:46 PM
 */
public interface ArrangementColorsProvider {

  @Nonnull
  Color getBorderColor(boolean selected);

  @Nonnull
  TextAttributes getTextAttributes(@Nonnull ArrangementSettingsToken token, boolean selected);
}
