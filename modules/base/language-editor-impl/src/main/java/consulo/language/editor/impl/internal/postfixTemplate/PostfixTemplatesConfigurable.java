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
package consulo.language.editor.impl.internal.postfixTemplate;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.configurable.*;
import consulo.disposer.Disposable;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.internal.TemplateConstants;
import consulo.language.editor.postfixTemplate.PostfixTemplateProvider;
import consulo.language.editor.postfixTemplate.PostfixTemplatesSettings;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.LabeledBuilder;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@ExtensionImpl
public class PostfixTemplatesConfigurable extends SimpleConfigurable<PostfixTemplatesConfigurable.Layout> implements Configurable.Composite, SearchableConfigurable, ApplicationConfigurable {
  protected static class Layout implements Supplier<Component> {
    private final CheckBox myPostfixTemplatesEnabled;
    private final CheckBox myCompletionEnabledCheckbox;
    private final ComboBox<Character> myShortcutComboBox;
    private final VerticalLayout myLayout;

    @RequiredUIAccess
    public Layout() {
      myLayout = VerticalLayout.create();

      myPostfixTemplatesEnabled = CheckBox.create(LocalizeValue.localizeTODO("&Enable postfix templates"));
      myLayout.add(myPostfixTemplatesEnabled);

      myCompletionEnabledCheckbox = CheckBox.create(LocalizeValue.localizeTODO("&Show postfix templates in completion autopopup"));
      myLayout.add(myCompletionEnabledCheckbox);

      ComboBox.Builder<Character> builder = ComboBox.<Character>builder();
      builder.add(TemplateConstants.SPACE_CHAR, CodeInsightBundle.message("template.shortcut.space"));
      builder.add(TemplateConstants.ENTER_CHAR, CodeInsightBundle.message("template.shortcut.enter"));
      builder.add(TemplateConstants.TAB_CHAR, CodeInsightBundle.message("template.shortcut.tab"));
      myShortcutComboBox = builder.build();

      myLayout.add(LabeledBuilder.sided(LocalizeValue.localizeTODO("Expand templates with"), myShortcutComboBox));

      myPostfixTemplatesEnabled.addValueListener(event -> updateComponents());
    }

    @RequiredUIAccess
    private void updateComponents() {
      boolean pluginEnabled = myPostfixTemplatesEnabled.getValue();
      myCompletionEnabledCheckbox.setVisible(true);
      myCompletionEnabledCheckbox.setEnabled(pluginEnabled);
      myShortcutComboBox.setEnabled(pluginEnabled);
    }

    @Nonnull
    @Override
    public Component get() {
      return myLayout;
    }
  }

  private Configurable[] myChildren;

  @Nonnull
  @Override
  public String getId() {
    return "editing.postfixCompletion";
  }

  @Nullable
  @Override
  public String getParentId() {
    return "editor.preferences.completion";
  }

  @Nonnull
  @Override
  public String getDisplayName() {
    return "Postfix Completion";
  }

  protected Configurable[] buildConfigurables() {
    List<PostfixTemplateProvider> providers = Application.get().getExtensionList(PostfixTemplateProvider.class);
    List<Configurable> list = new ArrayList<>(providers.size());
    for (PostfixTemplateProvider postfixTemplateProvider : providers) {
      list.add(new PostfixTemplatesChildConfigurable(postfixTemplateProvider));
    }
    Collections.sort(list, (o1, o2) -> StringUtil.compare(o1.getDisplayName(), o2.getDisplayName(), true));
    return list.toArray(new Configurable[list.size()]);
  }

  @Nonnull
  @Override
  public Configurable[] getConfigurables() {
    if (myChildren == null) {
      myChildren = buildConfigurables();
    }
    return myChildren;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  protected Layout createPanel(@Nonnull Disposable uiDisposable) {
    return new Layout();
  }

  @RequiredUIAccess
  @Override
  protected boolean isModified(@Nonnull Layout component) {
    PostfixTemplatesSettings templatesSettings = PostfixTemplatesSettings.getInstance();
    return component.myPostfixTemplatesEnabled.getValue() != templatesSettings.isPostfixTemplatesEnabled() ||
           component.myCompletionEnabledCheckbox.getValue() != templatesSettings.isTemplatesCompletionEnabled() ||
           component.myShortcutComboBox.getValue() != templatesSettings.getShortcut();
  }

  @RequiredUIAccess
  @Override
  protected void apply(@Nonnull Layout component) throws ConfigurationException {
    PostfixTemplatesSettings templatesSettings = PostfixTemplatesSettings.getInstance();

    templatesSettings.setPostfixTemplatesEnabled(component.myPostfixTemplatesEnabled.getValue());
    templatesSettings.setTemplatesCompletionEnabled(component.myCompletionEnabledCheckbox.getValue());
    templatesSettings.setShortcut(component.myShortcutComboBox.getValue());
  }

  @RequiredUIAccess
  @Override
  protected void reset(@Nonnull Layout component) {
    PostfixTemplatesSettings templatesSettings = PostfixTemplatesSettings.getInstance();

    component.myPostfixTemplatesEnabled.setValue(templatesSettings.isPostfixTemplatesEnabled());
    component.myCompletionEnabledCheckbox.setValue(templatesSettings.isTemplatesCompletionEnabled());
    component.myShortcutComboBox.setValue((char)templatesSettings.getShortcut());

    component.updateComponents();
  }

  @RequiredUIAccess
  @Override
  protected void disposeUIResources(@Nonnull Layout component) {
    super.disposeUIResources(component);

    myChildren = null;
  }

  @Nullable
  public PostfixTemplatesChildConfigurable findConfigurable(PostfixTemplateProvider postfixTemplateProvider) {
    for (Configurable configurable : getConfigurables()) {
      PostfixTemplatesChildConfigurable childConfigurable = (PostfixTemplatesChildConfigurable)configurable;

      if (childConfigurable.getPostfixTemplateProvider() == postfixTemplateProvider) {
        return childConfigurable;
      }
    }
    return null;
  }
}
