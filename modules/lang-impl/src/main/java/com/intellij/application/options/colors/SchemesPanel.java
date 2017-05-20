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

package com.intellij.application.options.colors;

import com.intellij.application.options.SaveSchemeDialog;
import com.intellij.application.options.SkipSelfSearchComponent;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.util.EventDispatcher;
import com.intellij.util.containers.ContainerUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class SchemesPanel extends JPanel implements SkipSelfSearchComponent {
  private final ColorAndFontOptions myOptions;

  private JComboBox mySchemeComboBox;

  private JButton myDeleteButton;

  private final EventDispatcher<ColorAndFontSettingsListener> myDispatcher = EventDispatcher.create(ColorAndFontSettingsListener.class);

  public SchemesPanel(ColorAndFontOptions options) {
    super(new BorderLayout());
    myOptions = options;

    JPanel schemesGroup = new JPanel(new BorderLayout());

    JPanel panel = new JPanel(new BorderLayout());
    schemesGroup.add(createSchemePanel(), BorderLayout.NORTH);
    schemesGroup.add(panel, BorderLayout.CENTER);
    add(schemesGroup, BorderLayout.CENTER);

    mySchemeComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (mySchemeComboBox.getSelectedIndex() != -1) {
          EditorColorsScheme selected = myOptions.selectScheme((String)mySchemeComboBox.getSelectedItem());
          if (ColorAndFontOptions.isReadOnly(selected)) {
            myDeleteButton.setEnabled(false);
          }
          else if (ColorSettingsUtil.isSharedScheme(selected)) {
            myDeleteButton.setEnabled(true);
          }
          else {
            myDeleteButton.setEnabled(true);
          }

          if (areSchemesLoaded()) {
            myDispatcher.getMulticaster().schemeChanged(SchemesPanel.this);
          }
        }
      }
    });
  }

  private boolean myListLoaded = false;

  public boolean areSchemesLoaded() {
    return myListLoaded;
  }

  public void clearSearch() {
  }

  private JPanel createSchemePanel() {
    JPanel panel = new JPanel(new GridBagLayout());

    panel.add(new JLabel(ApplicationBundle.message("combobox.scheme.name")),
              new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 5), 0,
                                     0));

    mySchemeComboBox = new JComboBox();
    panel.add(mySchemeComboBox,
              new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 10), 0,
                                     0));

    JButton saveAsButton = new JButton(ApplicationBundle.message("button.save.as"));
    saveAsButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        showSaveAsDialog();
      }
    });
    panel.add(saveAsButton,
              new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 5), 0,
                                     0));

    myDeleteButton = new JButton(ApplicationBundle.message("button.delete"));
    myDeleteButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (mySchemeComboBox.getSelectedIndex() != -1) {
          myOptions.removeScheme((String)mySchemeComboBox.getSelectedItem());
        }
      }
    });
    panel.add(myDeleteButton,
              new GridBagConstraints(3, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0));

    return panel;
  }

  private void showSaveAsDialog() {
    List<String> names = ContainerUtil.newArrayList(myOptions.getSchemeNames());
    String selectedName = myOptions.getSelectedScheme().getName();
    SaveSchemeDialog dialog = new SaveSchemeDialog(this, ApplicationBundle.message("title.save.color.scheme.as"), names, selectedName);
    if (dialog.showAndGet()) {
      myOptions.saveSchemeAs(dialog.getSchemeName());
    }
  }

  private void changeToScheme() {
    updateDescription(false);
  }

  public boolean updateDescription(boolean modified) {
    EditorColorsScheme scheme = myOptions.getSelectedScheme();

    if (modified && (ColorAndFontOptions.isReadOnly(scheme) || ColorSettingsUtil.isSharedScheme(scheme))) {
      FontOptions.showReadOnlyMessage(this, ColorSettingsUtil.isSharedScheme(scheme));
      return false;
    }

    return true;
  }

  public void resetSchemesCombo(final Object source) {
    if (this != source) {
      setListLoaded(false);

      String selectedSchemeBackup = myOptions.getSelectedScheme().getName();
      mySchemeComboBox.removeAllItems();

      String[] schemeNames = myOptions.getSchemeNames();
      for (String schemeName : schemeNames) {
        mySchemeComboBox.addItem(schemeName);
      }

      mySchemeComboBox.setSelectedItem(selectedSchemeBackup);
      setListLoaded(true);

      changeToScheme();

      myDispatcher.getMulticaster().schemeChanged(this);
    }
  }

  private void setListLoaded(final boolean b) {
    myListLoaded = b;
  }

  public void addListener(ColorAndFontSettingsListener listener) {
    myDispatcher.addListener(listener);
  }

  public void disposeUIResources() {
    
  }
}
