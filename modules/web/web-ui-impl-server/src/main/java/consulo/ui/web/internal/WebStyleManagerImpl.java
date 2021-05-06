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
package consulo.ui.web.internal;

import consulo.ui.impl.style.StyleManagerImpl;
import consulo.ui.style.Style;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
public class WebStyleManagerImpl extends StyleManagerImpl {
  public static final WebStyleManagerImpl ourInstance = new WebStyleManagerImpl();

  private Style myCurrentStyle = new WebStyleImpl("Default");

  @Nonnull
  @Override
  public List<Style> getStyles() {
    return Arrays.asList(myCurrentStyle);
  }

  @Nonnull
  @Override
  public Style getCurrentStyle() {
    return myCurrentStyle;
  }

  @Override
  public void setCurrentStyle(@Nonnull Style style) {
    Style oldStyle = myCurrentStyle;
    myCurrentStyle = style;
    fireStyleChanged(oldStyle, style);
  }
}
