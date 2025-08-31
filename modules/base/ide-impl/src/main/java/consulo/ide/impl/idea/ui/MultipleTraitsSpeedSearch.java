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

import consulo.ide.impl.idea.util.PairConvertor;
import consulo.ui.ex.awt.speedSearch.SpeedSearchBase;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * @author Irina.Chernushina
 * @since 2012-12-13
 */
public abstract class MultipleTraitsSpeedSearch<Comp extends JComponent> extends SpeedSearchBase<Comp> {
  protected final List<PairConvertor<Object, String, Boolean>> myOrderedConvertors;

  public MultipleTraitsSpeedSearch(Comp component, @Nonnull List<PairConvertor<Object, String, Boolean>> convertors) {
    super(component);
    myOrderedConvertors = convertors;
  }

  @Override
  protected boolean isMatchingElement(Object element, String pattern) {
    for (PairConvertor<Object, String, Boolean> convertor : myOrderedConvertors) {
      Boolean matched = convertor.convert(element, pattern);
      if (Boolean.TRUE.equals(matched)) return true;
    }
    return false;
  }

  @Nullable
  @Override
  protected final String getElementText(Object element) {
    throw new IllegalStateException();
  }
}
