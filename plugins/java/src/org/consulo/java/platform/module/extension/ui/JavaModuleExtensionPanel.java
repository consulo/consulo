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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.ui.ColoredListCellRendererWrapper;
import com.intellij.ui.SimpleTextAttributes;
import org.consulo.java.platform.module.extension.JavaModuleExtensionImpl;
import org.consulo.java.platform.module.extension.JavaMutableModuleExtensionImpl;
import org.consulo.java.platform.module.extension.SpecialDirLocation;
import org.consulo.module.extension.*;
import org.consulo.module.extension.ui.ModuleExtensionWithSdkPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * @author VISTALL
 * @since 15:23/19.05.13
 */
public class JavaModuleExtensionPanel extends JPanel {
  private final JavaMutableModuleExtensionImpl myMutableModuleExtension;
  private final Runnable myClasspathStateUpdater;
  private ComboBox myLanguageLevelComboBox;
  private ModuleExtensionWithSdkPanel myModuleExtensionWithSdkPanel;
  private JPanel myRoot;
  private JRadioButton myModuleDirRadioButton;
  private JRadioButton mySourceDirRadioButton;

  public JavaModuleExtensionPanel(JavaMutableModuleExtensionImpl mutableModuleExtension, Runnable classpathStateUpdater) {
    myMutableModuleExtension = mutableModuleExtension;
    myClasspathStateUpdater = classpathStateUpdater;

    final JRadioButton radioButton =
      myMutableModuleExtension.getSpecialDirLocation() == SpecialDirLocation.MODULE_DIR ? myModuleDirRadioButton : mySourceDirRadioButton;
    radioButton.setSelected(true);

    ChangeListener changeListener = new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        if (mySourceDirRadioButton.isSelected()) {
          myMutableModuleExtension.setSpecialDirLocation(SpecialDirLocation.SOURCE_DIR);
        }
        else if (myModuleDirRadioButton.isSelected()) {
          myMutableModuleExtension.setSpecialDirLocation(SpecialDirLocation.MODULE_DIR);
        }
      }
    };

    myModuleDirRadioButton.addChangeListener(changeListener);
    mySourceDirRadioButton.addChangeListener(changeListener);
  }

  private void createUIComponents() {
    myRoot = this;
    myModuleExtensionWithSdkPanel = new ModuleExtensionWithSdkPanel(myMutableModuleExtension, myClasspathStateUpdater);
    myLanguageLevelComboBox = new ComboBox();
    myLanguageLevelComboBox.setRenderer(new ColoredListCellRendererWrapper<Object>() {
      @Override
      protected void doCustomize(JList list, Object value, int index, boolean selected, boolean hasFocus) {
        if (value instanceof LanguageLevel) {
          final LanguageLevel languageLevel = (LanguageLevel)value;
          append(languageLevel.getShortText(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
          append(" ");
          append(languageLevel.getDescription(), SimpleTextAttributes.GRAY_ATTRIBUTES);
        }
        else if (value instanceof Module) {
          setIcon(AllIcons.Nodes.Module);
          append(((Module)value).getName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);

          final JavaModuleExtensionImpl extension = ModuleUtilCore.getExtension((Module)value, JavaModuleExtensionImpl.class);
          if (extension != null) {
            final LanguageLevel languageLevel = extension.getLanguageLevel();
            append("(" + languageLevel.getShortText() + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
          }
        }
        else if (value instanceof String) {
          setIcon(AllIcons.Nodes.Module);
          append((String)value, SimpleTextAttributes.ERROR_BOLD_ATTRIBUTES);
        }
      }
    });

    for (LanguageLevel languageLevel : LanguageLevel.values()) {
      myLanguageLevelComboBox.addItem(languageLevel);
    }
    insertModuleItems();

    final MutableModuleInheritableNamedPointer<LanguageLevel> inheritableLanguageLevel =
      myMutableModuleExtension.getInheritableLanguageLevel();

    final String moduleName = inheritableLanguageLevel.getModuleName();
    if (moduleName != null) {
      final Module module = inheritableLanguageLevel.getModule();
      if (module != null) {
        myLanguageLevelComboBox.setSelectedItem(module);
      }
      else {
        myLanguageLevelComboBox.addItem(moduleName);
      }
    }
    else {
      myLanguageLevelComboBox.setSelectedItem(inheritableLanguageLevel.get());
    }

    myLanguageLevelComboBox.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        final Object selectedItem = myLanguageLevelComboBox.getSelectedItem();
        if (selectedItem instanceof Module) {
          inheritableLanguageLevel.set(((Module)selectedItem).getName(), null);
        }
        else if (selectedItem instanceof LanguageLevel) {
          inheritableLanguageLevel.set(null, ((LanguageLevel)selectedItem).getName());
        }
        else {
          inheritableLanguageLevel.set(selectedItem.toString(), null);
        }
      }
    });
  }

  public void insertModuleItems() {
    final ModuleExtensionProvider provider = ModuleExtensionProviderEP.findProvider(myMutableModuleExtension.getId());
    if (provider == null) {
      return;
    }

    for (Module module : ModuleManager.getInstance(myMutableModuleExtension.getModule().getProject()).getModules()) {
      // dont add self module
      if (module == myMutableModuleExtension.getModule()) {
        continue;
      }

      final ModuleExtension extension = ModuleUtilCore.getExtension(module, provider.getImmutableClass());
      if (extension instanceof ModuleExtensionWithSdk) {
        final ModuleExtensionWithSdk sdkExtension = (ModuleExtensionWithSdk)extension;
        // recursive depend
        if (sdkExtension.getInheritableSdk().getModule() == myMutableModuleExtension.getModule()) {
          continue;
        }
        myLanguageLevelComboBox.addItem(sdkExtension.getModule());
      }
    }
  }
}
