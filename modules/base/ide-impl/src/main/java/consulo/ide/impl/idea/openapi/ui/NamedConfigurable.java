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

package consulo.ide.impl.idea.openapi.ui;

import consulo.configurable.Configurable;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.JBUI;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.configurable.MasterDetailsConfigurable;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;

/**
 * @author anna
 * @since 2006-05-26
 */
public abstract class NamedConfigurable<T> implements Configurable, MasterDetailsConfigurable<T> {
  private JTextField myNameField;
  protected JPanel myNamePanel;
  private JPanel myWholePanel;
  private JPanel myOptionsPanel;
  private JPanel myTopRightPanel;
  private JComponent myOptionsComponent;
  private boolean myNameEditable;

  protected NamedConfigurable() {
    this(false, null);
  }

  protected NamedConfigurable(boolean isNameEditable, @Nullable final Runnable updateTree) {
    myNameEditable = isNameEditable;
    myNamePanel.setVisible(myNameEditable);
    if (myNameEditable) {
      myNameField.getDocument().addDocumentListener(new DocumentAdapter() {
        @Override
        protected void textChanged(DocumentEvent e) {
          setDisplayName(myNameField.getText());
          if (updateTree != null) {
            updateTree.run();
          }
        }
      });
    }
    
    setBorder();
  }

  protected void setBorder() {
    myNamePanel.setBorder(JBUI.Borders.empty(10, 10, 6, 10));
  }

  public boolean isNameEditable() {
    return myNameEditable;
  }

  public void setNameFieldShown(boolean shown) {
    if (myNamePanel.isVisible() == shown) return;

    myNamePanel.setVisible(shown);
    myWholePanel.revalidate();
    myWholePanel.repaint();
  }

  public abstract void setDisplayName(String name);

  public abstract T getEditableObject();

  public abstract String getBannerSlogan();

  @RequiredUIAccess
  @Override
  public final JComponent createComponent(@Nonnull Disposable parentDisposable) {
    if (myOptionsComponent == null) {
      myOptionsComponent = createOptionsPanel(parentDisposable);
      final JComponent component = createTopRightComponent(myNameField);
      if (component == null) {
        myTopRightPanel.setVisible(false);
      }
      else {
        myTopRightPanel.add(component, BorderLayout.CENTER);
      }
    }
    if (myOptionsComponent != null) {
      myOptionsPanel.add(myOptionsComponent, BorderLayout.CENTER);
    }
    else {
      Logger.getInstance(getClass()).error("Options component is null for " + getClass());
    }
    updateName();
    return myWholePanel;
  }

  @Nullable
  protected JComponent createTopRightComponent(JTextField nameField) {
    return null;
  }

  protected void resetOptionsPanel() {
    myOptionsComponent = null;
    myOptionsPanel.removeAll();
  }

  public void updateName() {
    myNameField.setText(getDisplayName());
  }

  @RequiredUIAccess
  @SuppressWarnings("deprecation")
  public JComponent createOptionsPanel(@Nonnull Disposable parentDisposable) {
    return createOptionsPanel();
  }

  @Deprecated
  public JComponent createOptionsPanel() {
    throw new AbstractMethodError();
  }

  @Nullable
  @Override
  public Image getIcon() {
    return getIcon(false);
  }

  @Nullable
  public Image getIcon(boolean expanded) {
    return null;
  }
}
