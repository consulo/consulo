/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui;

import consulo.ui.ex.SimpleColoredText;
import consulo.ui.ex.awt.ColoredListCellRenderer;

import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * Typed version of {@link ColoredListCellRenderer}.
 *
 * @deprecated useless, go for {@link ColoredListCellRenderer} directly
 */
public abstract class ColoredListCellRendererWrapper<T> extends ColoredListCellRenderer<T> {
  @Override
  protected final void customizeCellRenderer(@Nonnull JList list, Object value, int index, boolean selected, boolean hasFocus) {
    @SuppressWarnings("unchecked") T t = (T)value;
    doCustomize(list, t, index, selected, hasFocus);
  }

  protected abstract void doCustomize(JList list, T value, int index, boolean selected, boolean hasFocus);

  public void append(@Nonnull SimpleColoredText text) {
    text.appendToComponent(this);
  }
}