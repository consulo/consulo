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
package com.intellij.codeInsight.template.postfix.templates;

import com.intellij.codeInsight.template.postfix.settings.PostfixTemplateMetaData;
import com.intellij.codeInsight.template.postfix.settings.PostfixTemplatesSettings;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import javax.annotation.Nonnull;

import java.io.IOException;

public abstract class PostfixTemplate {
  @Nonnull
  private final String myPresentableName;
  @Nonnull
  private final String myKey;
  @Nonnull
  private final String myDescription;
  @Nonnull
  private final String myExample;

  protected PostfixTemplate(@Nonnull String name, @Nonnull String example) {
    this(name, "." + name, example);
  }

  protected PostfixTemplate(@Nonnull String name, @Nonnull String key, @Nonnull String example) {
    String tempDescription;
    myPresentableName = name;
    myKey = key;
    myExample = example;

    try {
      tempDescription = new PostfixTemplateMetaData(this).getDescription().getText();
    }
    catch (IOException e) {
      tempDescription = "Under construction";
    }
    myDescription = tempDescription;
  }

  @Nonnull
  public final String getKey() {
    return myKey;
  }

  @Nonnull
  public String getPresentableName() {
    return myPresentableName;
  }

  @Nonnull
  public String getDescription() {
    return myDescription;
  }

  @Nonnull
  public String getExample() {
    return myExample;
  }

  public boolean startInWriteAction() {
    return true;
  }

  public boolean isEnabled(PostfixTemplateProvider provider) {
    final PostfixTemplatesSettings settings = PostfixTemplatesSettings.getInstance();
    return settings != null && settings.isPostfixTemplatesEnabled() && settings.isTemplateEnabled(this, provider);
  }

  public abstract boolean isApplicable(@Nonnull PsiElement context, @Nonnull Document copyDocument, int newOffset);

  public abstract void expand(@Nonnull PsiElement context, @Nonnull Editor editor);
}
