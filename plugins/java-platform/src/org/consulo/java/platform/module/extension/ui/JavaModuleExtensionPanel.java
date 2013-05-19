/*
 * Copyright 2013 Consulo.org
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
package org.consulo.java.platform.module.extension.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.ui.ListCellRendererWrapper;
import org.consulo.java.platform.module.extension.JavaMutableModuleExtension;
import org.consulo.module.extension.ui.ModuleExtensionWithSdkPanel;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * @author VISTALL
 * @since 15:23/19.05.13
 */
public class JavaModuleExtensionPanel extends JPanel {
  private final JavaMutableModuleExtension myMutableModuleExtension;
  private final Runnable myClasspathStateUpdater;
  private ComboBox myLanguageLevelComboBox;
  private ModuleExtensionWithSdkPanel myModuleExtensionWithSdkPanel;
  private JPanel myRoot;

  public JavaModuleExtensionPanel(JavaMutableModuleExtension mutableModuleExtension, Runnable classpathStateUpdater) {
    myMutableModuleExtension = mutableModuleExtension;
    myClasspathStateUpdater = classpathStateUpdater;
  }

  private void createUIComponents() {
    myRoot = this;
    myModuleExtensionWithSdkPanel = new ModuleExtensionWithSdkPanel(myMutableModuleExtension, myClasspathStateUpdater);
    myLanguageLevelComboBox = new ComboBox();
    myLanguageLevelComboBox.setRenderer(new ListCellRendererWrapper() {
      @Override
      public void customize(final JList list, final Object value, final int index, final boolean selected, final boolean hasFocus) {
        if (value instanceof LanguageLevel) {
          setText(((LanguageLevel)value).getPresentableText());
        }
        else if (value instanceof String) {
          setText((String)value);
        }
      }
    });

    for(LanguageLevel languageLevel : LanguageLevel.values()) {
      myLanguageLevelComboBox.addItem(languageLevel);
    }
    myLanguageLevelComboBox.setSelectedItem(myMutableModuleExtension.getLanguageLevel());
    myLanguageLevelComboBox.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        myMutableModuleExtension.setLanguageLevel((LanguageLevel)myLanguageLevelComboBox.getSelectedItem());
      }
    });
  }
}
