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
package consulo.test.impl.ui;

import com.intellij.openapi.util.EmptyRunnable;
import consulo.ui.color.ColorValue;
import consulo.ui.style.*;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 2020-08-24
 */
public class TestStyleManagerImpl implements StyleManager {
  public static final TestStyleManagerImpl INSTANCE = new TestStyleManagerImpl();

  private Style myDummy = new Style() {
    @Nonnull
    @Override
    public String getName() {
      return "Dummy";
    }

    @Nonnull
    @Override
    public ColorValue getColorValue(@Nonnull StyleColorValue colorKey) {
      return StandardColors.BLACK;
    }
  };

  @Nonnull
  @Override
  public List<Style> getStyles() {
    return Collections.singletonList(myDummy);
  }

  @Nonnull
  @Override
  public Style getCurrentStyle() {
    return myDummy;
  }

  @Override
  public void setCurrentStyle(@Nonnull Style newStyle) {

  }

  @Nonnull
  @Override
  public Runnable addChangeListener(@Nonnull StyleChangeListener listener) {
    return EmptyRunnable.getInstance();
  }
}
