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
package consulo.ui.ex.awt;

import consulo.ui.ex.awt.speedSearch.SpeedSearchBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;

/**
 * @author Anna.Kozlova
 * @since 11-Jul-2006
 */
public class ComboboxSpeedSearch extends SpeedSearchBase<JComboBox> {
  public ComboboxSpeedSearch(@Nonnull final JComboBox comboBox) {
    super(comboBox);
    removeKeyStroke(comboBox.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT), KeyStroke.getKeyStroke(' ', 0));
  }

  private static void removeKeyStroke(@Nullable InputMap map, KeyStroke ks) {
    while (map != null) {
      map.remove(ks);
      map = map.getParent();
    }
  }

  @Override
  protected void selectElement(Object element, String selectedText) {
    myComponent.setSelectedItem(element);
    myComponent.repaint();
  }

  @Override
  protected int getSelectedIndex() {
    return myComponent.getSelectedIndex();
  }

  @Override
  protected Object[] getAllElements() {
    ListModel model = myComponent.getModel();
    Object[] elements = new Object[model.getSize()];
    for (int i = 0; i < elements.length; i++) {
      elements[i] = model.getElementAt(i);
    }
    return elements;
  }

  @Override
  protected String getElementText(Object element) {
    return element.toString();
  }
}