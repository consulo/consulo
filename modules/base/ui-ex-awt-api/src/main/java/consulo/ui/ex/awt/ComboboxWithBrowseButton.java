/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import consulo.component.ComponentManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.localize.LocalizeValue;

import javax.swing.*;
import java.awt.*;

public class ComboboxWithBrowseButton extends ComponentWithBrowseButton<JComboBox> {
  public ComboboxWithBrowseButton() {
    super(new JComboBox(), null);
  }

  public ComboboxWithBrowseButton(JComboBox comboBox) {
    super(comboBox, null);
  }

  public JComboBox getComboBox() {
    return getChildComponent();
  }

  @Override
  public void setTextFieldPreferredWidth(final int charCount) {
    super.setTextFieldPreferredWidth(charCount);
    final Component comp = getChildComponent().getEditor().getEditorComponent();
    Dimension size = comp.getPreferredSize();
    FontMetrics fontMetrics = comp.getFontMetrics(comp.getFont());
    size.width = fontMetrics.charWidth('a') * charCount;
    comp.setPreferredSize(size);
  }

  public void addBrowseFolderListener(ComponentManager project, FileChooserDescriptor descriptor) {
    addBrowseFolderListener(
      LocalizeValue.empty(),
      LocalizeValue.empty(),
      project,
      descriptor,
      TextComponentAccessor.STRING_COMBOBOX_WHOLE_TEXT
    );
  }
}
