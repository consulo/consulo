/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ui.internal;

import consulo.ui.style.Style;
import consulo.ui.style.StyleManager;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
public class WGwtStyleManagerImpl implements StyleManager {
  public static final WGwtStyleManagerImpl ourInstance = new WGwtStyleManagerImpl();

  private Style myCurrentStyle = new WGwtStyleImpl("Default");

  @NotNull
  @Override
  public List<Style> getStyles() {
    return Arrays.asList(myCurrentStyle);
  }

  @NotNull
  @Override
  public Style getCurrentStyle() {
    return myCurrentStyle;
  }

  @Override
  public void setCurrentStyle(@NotNull Style style) {
    throw new UnsupportedOperationException();
  }
}
