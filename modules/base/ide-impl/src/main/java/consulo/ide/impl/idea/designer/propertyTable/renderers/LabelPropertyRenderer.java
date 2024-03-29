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
package consulo.ide.impl.idea.designer.propertyTable.renderers;

import consulo.ide.impl.idea.designer.model.PropertiesContainer;
import consulo.ide.impl.idea.designer.model.PropertyContext;
import consulo.ide.impl.idea.designer.propertyTable.PropertyRenderer;
import consulo.ide.impl.idea.designer.propertyTable.PropertyTable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * This is convenient class for implementing property renderers which
 * are based on JLabel.
 *
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public class LabelPropertyRenderer extends JLabel implements PropertyRenderer {
  @Nullable private final String myStaticText;

  public LabelPropertyRenderer(@Nullable String staticText) {
    myStaticText = staticText;
    setOpaque(true);
    putClientProperty("html.disable", true);
    setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
  }

  @Nonnull
  public JComponent getComponent(@Nullable PropertiesContainer container,
                                 PropertyContext context,
                                 @Nullable Object value,
                                 boolean selected,
                                 boolean hasFocus) {
    // Reset text and icon
    setText(null);
    setIcon(null);

    // Background and foreground
    PropertyTable.updateRenderer(this, selected);

    if (value != null) {
      customize(value);
    }

    return this;
  }

  /**
   * Here all subclasses should customize their text, icon and other
   * attributes. Note, that background and foreground colors are already
   * set.
   */
  protected void customize(@Nonnull Object value) {
    setText(myStaticText != null ? myStaticText : value.toString());
  }
}