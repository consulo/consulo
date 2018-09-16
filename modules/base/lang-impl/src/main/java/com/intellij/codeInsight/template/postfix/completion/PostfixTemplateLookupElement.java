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
package com.intellij.codeInsight.template.postfix.completion;

import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.template.impl.CustomLiveTemplateLookupElement;
import com.intellij.codeInsight.template.postfix.templates.PostfixLiveTemplate;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.UIUtil;
import javax.annotation.Nonnull;

public class PostfixTemplateLookupElement extends CustomLiveTemplateLookupElement {
  @Nonnull
  private final PostfixTemplate myTemplate;
  @Nonnull
  private final PostfixTemplateProvider myProvider;


  public PostfixTemplateLookupElement(@Nonnull PostfixLiveTemplate liveTemplate,
                                      @Nonnull PostfixTemplate postfixTemplate,
                                      @Nonnull String templateKey,
                                      @Nonnull PostfixTemplateProvider provider,
                                      boolean sudden) {
    super(liveTemplate, templateKey, StringUtil.trimStart(templateKey, "."), postfixTemplate.getDescription(), sudden, true);
    myTemplate = postfixTemplate;
    myProvider = provider;
  }

  @Nonnull
  public PostfixTemplate getPostfixTemplate() {
    return myTemplate;
  }

  @Nonnull
  public PostfixTemplateProvider getProvider() {
    return myProvider;
  }

  @Override
  public void renderElement(LookupElementPresentation presentation) {
    super.renderElement(presentation);
    if (sudden) {
      presentation.setTailText(" " + UIUtil.rightArrow() + " " + myTemplate.getExample());
    }
    else {
      presentation.setTypeText(myTemplate.getExample());
      presentation.setTypeGrayed(true);
    }
  }
}
