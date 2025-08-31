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
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.JBTextField;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Alexander Lobas
 */
public class TextEditor extends PropertyEditor {
  public final JBTextField myTextField = new JBTextField(); // public modifier for accessing from descendants from different jars

  public TextEditor() {
    myTextField.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        fireValueCommitted(true, false);
      }
    });
    myTextField.getDocument().addDocumentListener(
      new DocumentAdapter() {
        protected void textChanged(DocumentEvent e) {
          preferredSizeChanged();
        }
      }
    );
  }

  @Nonnull
  @Override
  public JComponent getComponent(@Nullable PropertiesContainer container,
                                 @Nullable PropertyContext context, Object value,
                                 @Nullable InplaceContext inplaceContext) {
    setEditorValue(container, value);

    if (inplaceContext == null) {
      myTextField.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
    }
    else {
      myTextField.setBorder(UIUtil.getTextFieldBorder());
      if (inplaceContext.isStartChar()) {
        myTextField.setText(inplaceContext.getText(myTextField.getText()));
      }
    }
    return myTextField;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myTextField;
  }

  @Override
  public Object getValue() throws Exception {
    return myTextField.getText();
  }

  protected void setEditorValue(@Nullable PropertiesContainer container, @Nullable Object value) {
    myTextField.setText(value == null ? "" : value.toString());
  }

  @Override
  public void updateUI() {
    SwingUtilities.updateComponentTreeUI(myTextField);
  }
}