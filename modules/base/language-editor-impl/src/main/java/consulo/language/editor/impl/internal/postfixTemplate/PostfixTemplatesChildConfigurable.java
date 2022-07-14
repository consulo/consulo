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
package consulo.language.editor.impl.internal.postfixTemplate;

import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.editor.internal.postfixTemplate.PostfixTemplateMetaData;
import consulo.language.editor.postfixTemplate.PostfixTemplate;
import consulo.language.editor.postfixTemplate.PostfixTemplateProvider;
import consulo.language.editor.postfixTemplate.PostfixTemplatesSettings;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.CheckBoxList;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.OnePixelSplitter;
import consulo.ui.ex.awt.ScrollPaneFactory;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.ArrayList;

/**
 * @author VISTALL
 * @since 16.08.14
 */
public class PostfixTemplatesChildConfigurable implements Configurable, Configurable.NoScroll {
  private PostfixTemplateProvider myPostfixTemplateProvider;
  private PostfixDescriptionPanel myPostfixDescriptionPanel;
  private CheckBoxList<PostfixTemplate> myCheckBoxList;
  private PostfixTemplatesSettings myTemplatesSettings;

  public PostfixTemplatesChildConfigurable(PostfixTemplateProvider postfixTemplateProvider) {
    myPostfixTemplateProvider = postfixTemplateProvider;
    myTemplatesSettings = PostfixTemplatesSettings.getInstance();
  }

  public PostfixTemplateProvider getPostfixTemplateProvider() {
    return myPostfixTemplateProvider;
  }

  @Nls
  @Override
  public String getDisplayName() {
    return myPostfixTemplateProvider.getLanguage().getDisplayName();
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public JComponent createComponent(@Nonnull Disposable uiDisposable) {
    OnePixelSplitter splitter = new OnePixelSplitter();
    splitter.setSplitterProportionKey("PostfixTemplatesChildConfigurable.splitter");

    myCheckBoxList = new CheckBoxList<>();

    splitter.setFirstComponent(ScrollPaneFactory.createScrollPane(myCheckBoxList, true));

    myPostfixDescriptionPanel = new PostfixDescriptionPanel();
    JPanel component = myPostfixDescriptionPanel.getComponent();
    component.setBorder(JBUI.Borders.empty(0, 8, 0, 0));
    splitter.setSecondComponent(component);

    myCheckBoxList.setItems(new ArrayList<>(myPostfixTemplateProvider.getTemplates()), PostfixTemplate::getPresentableName, postfixTemplate -> Boolean.TRUE);

    myCheckBoxList.addListSelectionListener(e -> {
      PostfixTemplate itemAt = myCheckBoxList.getItemAt(myCheckBoxList.getSelectedIndex());

      myPostfixDescriptionPanel.reset(PostfixTemplateMetaData.createMetaData(itemAt));
    });
    return splitter;
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    int size = myCheckBoxList.getItemsCount();
    for (int i = 0; i < size; i++) {
      PostfixTemplate itemAt = myCheckBoxList.getItemAt(i);

      if (myTemplatesSettings.isTemplateEnabled(itemAt, myPostfixTemplateProvider) != myCheckBoxList.isItemSelected(i)) {
        return true;
      }
    }
    return false;
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    int size = myCheckBoxList.getItemsCount();
    for (int i = 0; i < size; i++) {
      PostfixTemplate itemAt = myCheckBoxList.getItemAt(i);

      if (myCheckBoxList.isItemSelected(i)) {
        myTemplatesSettings.enableTemplate(itemAt, myPostfixTemplateProvider);
      }
      else {
        myTemplatesSettings.disableTemplate(itemAt, myPostfixTemplateProvider);
      }
    }
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    int size = myCheckBoxList.getItemsCount();

    for (int i = 0; i < size; i++) {
      PostfixTemplate itemAt = myCheckBoxList.getItemAt(i);

      myCheckBoxList.setItemSelected(itemAt, myTemplatesSettings.isTemplateEnabled(itemAt, myPostfixTemplateProvider));
    }
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    if (myPostfixDescriptionPanel != null) {
      Disposer.dispose(myPostfixDescriptionPanel);
    }
  }

  public void focusTemplate(PostfixTemplate template) {
    int itemIndex = myCheckBoxList.getItemIndex(template);
    if (itemIndex == -1) {
      return;
    }
    myCheckBoxList.setSelectedIndex(itemIndex);
  }
}
