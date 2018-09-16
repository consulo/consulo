/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.template.CustomLiveTemplateBase;
import com.intellij.codeInsight.template.CustomTemplateCallback;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CustomLiveTemplateLookupElement extends LiveTemplateLookupElement {
  @Nonnull
  private final CustomLiveTemplateBase myCustomLiveTemplate;

  @Nonnull
  private final String myTemplateKey;
  @Nonnull
  private final String myItemText;

  public CustomLiveTemplateLookupElement(@Nonnull CustomLiveTemplateBase customLiveTemplate,
                                         @Nonnull String templateKey,
                                         @Nonnull String itemText,
                                         @Nullable String description,
                                         boolean sudden,
                                         boolean worthShowingInAutoPopup) {
    super(templateKey, description, sudden, worthShowingInAutoPopup);
    myCustomLiveTemplate = customLiveTemplate;
    myTemplateKey = templateKey;
    myItemText = itemText;
  }

  @Nonnull
  @Override
  protected String getItemText() {
    return myItemText;
  }

  @Nonnull
  public CustomLiveTemplateBase getCustomLiveTemplate() {
    return myCustomLiveTemplate;
  }

  @Override
  public char getTemplateShortcut() {
    return myCustomLiveTemplate.getShortcut();
  }

  @Override
  public void handleInsert(InsertionContext context) {
    context.setAddCompletionChar(false);
    expandTemplate(context.getEditor(), context.getFile());
  }

  public void expandTemplate(@Nonnull Editor editor, @Nonnull PsiFile file) {
    myCustomLiveTemplate.expand(myTemplateKey, new CustomTemplateCallback(editor, file));
  }
}
