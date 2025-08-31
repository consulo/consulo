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
package consulo.ide.impl.idea.designer.propertyTable.editors;

import consulo.ide.impl.idea.designer.model.PropertiesContainer;
import consulo.ide.impl.idea.designer.model.PropertyContext;
import consulo.ide.impl.idea.designer.propertyTable.InplaceContext;
import consulo.ide.impl.idea.designer.propertyTable.PropertyEditor;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public class BooleanEditor extends PropertyEditor {
  protected final JCheckBox myCheckBox;
  private boolean myInsideChange;

  public BooleanEditor() {
    myCheckBox = new JCheckBox();
    myCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (!myInsideChange) {
          fireValueCommitted(true, false);
        }
      }
    });
  }

  public void updateUI() {
    SwingUtilities.updateComponentTreeUI(myCheckBox);
  }

  public Object getValue() throws Exception {
    return Boolean.valueOf(myCheckBox.isSelected());
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myCheckBox;
  }

  @Nonnull
  public JComponent getComponent(@Nullable PropertiesContainer container,
                                 @Nullable PropertyContext context, Object value,
                                 @Nullable InplaceContext inplaceContext) {
    try {
      myInsideChange = true;
      myCheckBox.setBackground(UIUtil.getTableBackground());
      myCheckBox.setSelected(value != null && (Boolean)value);
      return myCheckBox;
    }
    finally {
      myInsideChange = false;
    }
  }
}