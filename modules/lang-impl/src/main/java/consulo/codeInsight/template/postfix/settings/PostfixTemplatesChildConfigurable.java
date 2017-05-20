/*
 * Copyright 2013-2016 consulo.io
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
package consulo.codeInsight.template.postfix.settings;

import com.intellij.codeInsight.template.postfix.settings.PostfixDescriptionPanel;
import com.intellij.codeInsight.template.postfix.settings.PostfixTemplateMetaData;
import com.intellij.codeInsight.template.postfix.settings.PostfixTemplatesSettings;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageExtensionPoint;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.ScrollPaneFactory;
import consulo.annotations.RequiredDispatchThread;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.ArrayList;

/**
 * @author VISTALL
 * @since 16.08.14
 */
public class PostfixTemplatesChildConfigurable implements Configurable, Configurable.NoScroll {
  private LanguageExtensionPoint<PostfixTemplateProvider> myExtensionPoint;
  private PostfixDescriptionPanel myPostfixDescriptionPanel;
  private CheckBoxList<PostfixTemplate> myCheckBoxList;
  private PostfixTemplatesSettings myTemplatesSettings;

  @SuppressWarnings("unchecked")
  public PostfixTemplatesChildConfigurable(LanguageExtensionPoint extensionPoint) {
    myExtensionPoint = extensionPoint;
    myTemplatesSettings = PostfixTemplatesSettings.getInstance();
  }

  public PostfixTemplateProvider getPostfixTemplateProvider() {
    return myExtensionPoint.getInstance();
  }

  @Nls
  @Override
  public String getDisplayName() {
    Language languageByID = Language.findLanguageByID(myExtensionPoint.getKey());
    return languageByID == null ? myExtensionPoint.getKey() : languageByID.getDisplayName();
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return null;
  }

  @RequiredDispatchThread
  @Nullable
  @Override
  public JComponent createComponent() {
    PostfixTemplateProvider postfixTemplateProvider = myExtensionPoint.getInstance();
    if (postfixTemplateProvider == null) {
      return null;
    }

    OnePixelSplitter splitter = new OnePixelSplitter();
    splitter.setSplitterProportionKey("PostfixTemplatesChildConfigurable.splitter");

    myCheckBoxList = new CheckBoxList<>();

    splitter.setFirstComponent(ScrollPaneFactory.createScrollPane(myCheckBoxList, true));

    myPostfixDescriptionPanel = new PostfixDescriptionPanel();
    JPanel component = myPostfixDescriptionPanel.getComponent();
    component.setBorder(new EmptyBorder(0, 8, 0, 0));
    splitter.setSecondComponent(component);

    myCheckBoxList.setItems(new ArrayList<>(postfixTemplateProvider.getTemplates()), PostfixTemplate::getPresentableName, postfixTemplate -> Boolean.TRUE);

    myCheckBoxList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        PostfixTemplate itemAt = myCheckBoxList.getItemAt(myCheckBoxList.getSelectedIndex());

        myPostfixDescriptionPanel.reset(PostfixTemplateMetaData.createMetaData(itemAt));
      }
    });
    return splitter;
  }

  @RequiredDispatchThread
  @Override
  public boolean isModified() {
    int size = myCheckBoxList.getItemsCount();
    for (int i = 0; i < size; i++) {
      PostfixTemplate itemAt = myCheckBoxList.getItemAt(i);

      if (myTemplatesSettings.isTemplateEnabled(itemAt, myExtensionPoint.getInstance()) != myCheckBoxList.isItemSelected(i)) {
        return true;
      }
    }
    return false;
  }

  @RequiredDispatchThread
  @Override
  public void apply() throws ConfigurationException {
    int size = myCheckBoxList.getItemsCount();
    for (int i = 0; i < size; i++) {
      PostfixTemplate itemAt = myCheckBoxList.getItemAt(i);

      if (myCheckBoxList.isItemSelected(i)) {
        myTemplatesSettings.enableTemplate(itemAt, myExtensionPoint.getInstance());
      }
      else {
        myTemplatesSettings.disableTemplate(itemAt, myExtensionPoint.getInstance());
      }
    }
  }

  @RequiredDispatchThread
  @Override
  public void reset() {
    int size = myCheckBoxList.getItemsCount();

    for (int i = 0; i < size; i++) {
      PostfixTemplate itemAt = myCheckBoxList.getItemAt(i);

      myCheckBoxList.setItemSelected(itemAt, myTemplatesSettings.isTemplateEnabled(itemAt, myExtensionPoint.getInstance()));
    }
  }

  @RequiredDispatchThread
  @Override
  public void disposeUIResources() {
    Disposer.dispose(myPostfixDescriptionPanel);
  }

  public void focusTemplate(PostfixTemplate template) {
    int itemIndex = myCheckBoxList.getItemIndex(template);
    if(itemIndex == -1) {
      return;
    }
    myCheckBoxList.setSelectedIndex(itemIndex);
  }
}
