/*
 * Copyright 2013-2022 consulo.io
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
package consulo.usage.impl;

import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.EditorColorsUtil;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.usage.UsageTreeColorsScheme;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 28-Mar-22
 */
@Singleton
public class UsageTreeColorsSchemeImpl implements UsageTreeColorsScheme {
  @Nonnull
  @Override
  public EditorColorsScheme getScheme() {
    return EditorColorsUtil.getColorSchemeForBackground(TargetAWT.from(UIUtil.getTreeTextBackground()));
  }
}
