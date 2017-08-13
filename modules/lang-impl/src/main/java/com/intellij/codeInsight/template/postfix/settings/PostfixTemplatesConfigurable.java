/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.codeInsight.template.postfix.settings;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.template.impl.LiveTemplateCompletionContributor;
import com.intellij.codeInsight.template.impl.TemplateSettings;
import com.intellij.codeInsight.template.postfix.templates.LanguagePostfixTemplate;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider;
import com.intellij.lang.LanguageExtensionPoint;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBCheckBox;
import consulo.codeInsight.template.postfix.settings.PostfixTemplatesChildConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import consulo.annotations.RequiredDispatchThread;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PostfixTemplatesConfigurable extends SearchableConfigurable.Parent.Abstract
        implements Configurable.Composite, SearchableConfigurable, Configurable.NoScroll {
  private PostfixTemplatesSettings myTemplatesSettings;
  private JComponent myPanel;
  private JBCheckBox myCompletionEnabledCheckbox;
  private JBCheckBox myPostfixTemplatesEnabled;
  private ComboBox myShortcutComboBox;

  private static final String SPACE = CodeInsightBundle.message("template.shortcut.space");
  private static final String TAB = CodeInsightBundle.message("template.shortcut.tab");
  private static final String ENTER = CodeInsightBundle.message("template.shortcut.enter");

  @SuppressWarnings("unchecked")
  public PostfixTemplatesConfigurable() {
    myTemplatesSettings = PostfixTemplatesSettings.getInstance();
    myPostfixTemplatesEnabled.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        updateComponents();
      }
    });
    myShortcutComboBox.addItem(TAB);
    myShortcutComboBox.addItem(SPACE);
    myShortcutComboBox.addItem(ENTER);
  }

  @NotNull
  @Override
  public String getId() {
    return "reference.settingsdialog.IDE.editor.postfix.templates";
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return getId();
  }

  @Nls
  @Override
  public String getDisplayName() {
    return null;
  }

  @RequiredDispatchThread
  @NotNull
  @Override
  public JComponent createComponent() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(myPanel, BorderLayout.NORTH);
    return panel;
  }

  @RequiredDispatchThread
  @Override
  public void apply() throws ConfigurationException {
    myTemplatesSettings.setPostfixTemplatesEnabled(myPostfixTemplatesEnabled.isSelected());
    myTemplatesSettings.setTemplatesCompletionEnabled(myCompletionEnabledCheckbox.isSelected());
    myTemplatesSettings.setShortcut(stringToShortcut((String)myShortcutComboBox.getSelectedItem()));
  }

  @RequiredDispatchThread
  @Override
  public void reset() {
    myPostfixTemplatesEnabled.setSelected(myTemplatesSettings.isPostfixTemplatesEnabled());
    myCompletionEnabledCheckbox.setSelected(myTemplatesSettings.isTemplatesCompletionEnabled());
    myShortcutComboBox.setSelectedItem(shortcutToString((char)myTemplatesSettings.getShortcut()));
    updateComponents();
  }

  @RequiredDispatchThread
  @Override
  public boolean isModified() {
    return myPostfixTemplatesEnabled.isSelected() != myTemplatesSettings.isPostfixTemplatesEnabled() ||
           myCompletionEnabledCheckbox.isSelected() != myTemplatesSettings.isTemplatesCompletionEnabled() ||
           stringToShortcut((String)myShortcutComboBox.getSelectedItem()) != myTemplatesSettings.getShortcut();
  }

  @RequiredDispatchThread
  @Override
  public void disposeUIResources() {
  }

  @Nullable
  @Override
  public Runnable enableSearch(String s) {
    return null;
  }

  @Override
  protected Configurable[] buildConfigurables() {
    LanguageExtensionPoint[] extensions = new ExtensionPointName<LanguageExtensionPoint>(LanguagePostfixTemplate.EP_NAME).getExtensions();
    List<Configurable> list = new ArrayList<Configurable>(extensions.length);
    for (LanguageExtensionPoint extensionPoint : extensions) {
      list.add(new PostfixTemplatesChildConfigurable(extensionPoint));
    }
    Collections.sort(list, new Comparator<Configurable>() {
      @Override
      public int compare(Configurable o1, Configurable o2) {
        return StringUtil.compare(o1.getDisplayName(), o2.getDisplayName(), true);
      }
    });
    return list.toArray(new Configurable[list.size()]);
  }

  private void updateComponents() {
    boolean pluginEnabled = myPostfixTemplatesEnabled.isSelected();
    myCompletionEnabledCheckbox.setVisible(!LiveTemplateCompletionContributor.shouldShowAllTemplates());
    myCompletionEnabledCheckbox.setEnabled(pluginEnabled);
    myShortcutComboBox.setEnabled(pluginEnabled);
  }

  private static char stringToShortcut(@NotNull String string) {
    if (SPACE.equals(string)) {
      return TemplateSettings.SPACE_CHAR;
    }
    else if (ENTER.equals(string)) {
      return TemplateSettings.ENTER_CHAR;
    }
    return TemplateSettings.TAB_CHAR;
  }

  private static String shortcutToString(char shortcut) {
    if (shortcut == TemplateSettings.SPACE_CHAR) {
      return SPACE;
    }
    if (shortcut == TemplateSettings.ENTER_CHAR) {
      return ENTER;
    }
    return TAB;
  }

  @Nullable
  public PostfixTemplatesChildConfigurable findConfigurable(PostfixTemplateProvider postfixTemplateProvider) {
    for (Configurable configurable : getConfigurables()) {
      PostfixTemplatesChildConfigurable childConfigurable = (PostfixTemplatesChildConfigurable)configurable;

      if(childConfigurable.getPostfixTemplateProvider() == postfixTemplateProvider) {
        return childConfigurable;
      }
    }
    return null;
  }
}
