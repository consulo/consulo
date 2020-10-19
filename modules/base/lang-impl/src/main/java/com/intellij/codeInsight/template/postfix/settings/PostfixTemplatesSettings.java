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

import com.intellij.codeInsight.template.impl.TemplateSettings;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplatesUtils;
import com.intellij.openapi.components.*;
import com.intellij.openapi.util.Factory;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import jakarta.inject.Singleton;
import org.jdom.Element;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Map;
import java.util.Set;

@Singleton
@State(name = "PostfixTemplatesSettings", storages = @Storage("postfixTemplates.xml"))
public class PostfixTemplatesSettings implements PersistentStateComponent<Element> {

  private Map<String, Set<String>> myLangToDisabledTemplates = ContainerUtil.newHashMap();

  private boolean postfixTemplatesEnabled = true;
  private boolean templatesCompletionEnabled = true;
  private int myShortcut = TemplateSettings.TAB_CHAR;

  public boolean isTemplateEnabled(@Nonnull PostfixTemplate template, @Nonnull PostfixTemplateProvider provider) {
    String langForProvider = PostfixTemplatesUtils.getLangForProvider(provider);
    return isTemplateEnabled(template, langForProvider);
  }

  public boolean isTemplateEnabled(PostfixTemplate template, @Nonnull String strictLangForProvider) {
    Set<String> result = myLangToDisabledTemplates.get(strictLangForProvider);
    return result == null || !result.contains(template.getKey());
  }

  public void disableTemplate(@Nonnull PostfixTemplate template, @Nonnull PostfixTemplateProvider provider) {
    String langForProvider = PostfixTemplatesUtils.getLangForProvider(provider);
    disableTemplate(template, langForProvider);
  }

  public void disableTemplate(PostfixTemplate template, String langForProvider) {
    Set<String> state = ContainerUtil.getOrCreate(myLangToDisabledTemplates, langForProvider, (Factory<Set<String>>)ContainerUtil::newHashSet);
    state.add(template.getKey());
  }

  public void enableTemplate(@Nonnull PostfixTemplate template, @Nonnull PostfixTemplateProvider provider) {
    String langForProvider = PostfixTemplatesUtils.getLangForProvider(provider);
    enableTemplate(template, langForProvider);
  }

  public void enableTemplate(PostfixTemplate template, String langForProvider) {
    Set<String> state = ContainerUtil.getOrCreate(myLangToDisabledTemplates, langForProvider, (Factory<Set<String>>)ContainerUtil::newHashSet);
    state.remove(template.getKey());
  }

  public boolean isPostfixTemplatesEnabled() {
    return postfixTemplatesEnabled;
  }

  public void setPostfixTemplatesEnabled(boolean postfixTemplatesEnabled) {
    this.postfixTemplatesEnabled = postfixTemplatesEnabled;
  }

  public boolean isTemplatesCompletionEnabled() {
    return templatesCompletionEnabled;
  }

  public void setTemplatesCompletionEnabled(boolean templatesCompletionEnabled) {
    this.templatesCompletionEnabled = templatesCompletionEnabled;
  }

  @Nonnull
  @MapAnnotation(entryTagName = "disabled-postfix-templates", keyAttributeName = "lang", surroundWithTag = false)
  public Map<String, Set<String>> getLangDisabledTemplates() {
    return myLangToDisabledTemplates;
  }

  public void setLangDisabledTemplates(@Nonnull Map<String, Set<String>> templatesState) {
    myLangToDisabledTemplates = templatesState;
  }

  public int getShortcut() {
    return myShortcut;
  }

  public void setShortcut(int shortcut) {
    myShortcut = shortcut;
  }

  @Nonnull
  public static PostfixTemplatesSettings getInstance() {
    return ServiceManager.getService(PostfixTemplatesSettings.class);
  }

  @Nullable
  @Override
  public Element getState() {
    return XmlSerializer.serialize(this, new SkipDefaultValuesSerializationFilters());
  }

  @Override
  public void loadState(Element settings) {
    XmlSerializer.deserializeInto(this, settings);
  }
}
