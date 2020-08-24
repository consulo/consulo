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
package consulo.ui.impl.style;

import com.intellij.util.EventDispatcher;
import consulo.ui.style.Style;
import consulo.ui.style.StyleChangeListener;
import consulo.ui.style.StyleManager;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 05-Nov-17
 */
public abstract class StyleManagerImpl implements StyleManager {
  private EventDispatcher<StyleChangeListener> myEventDispatcher = EventDispatcher.create(StyleChangeListener.class);

  protected void fireStyleChanged(@Nonnull Style oldStyle, @Nonnull Style newStyle) {
    myEventDispatcher.getMulticaster().styleChanged(oldStyle, newStyle);
  }

  @Nonnull
  @Override
  public Runnable addChangeListener(@Nonnull StyleChangeListener listener) {
    myEventDispatcher.addListener(listener);
    return () -> myEventDispatcher.removeListener(listener);
  }
}
