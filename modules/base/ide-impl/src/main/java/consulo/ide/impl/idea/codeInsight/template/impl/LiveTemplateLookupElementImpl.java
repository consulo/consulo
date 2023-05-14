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
package consulo.ide.impl.idea.codeInsight.template.impl;

import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.template.LiveTemplateLookupElement;
import consulo.language.editor.template.Template;
import consulo.language.editor.template.TemplateManager;

import jakarta.annotation.Nonnull;

public class LiveTemplateLookupElementImpl extends LiveTemplateLookupElement {
  private final Template myTemplate;

  public LiveTemplateLookupElementImpl(@Nonnull Template template, boolean sudden) {
    super(template.getKey(), StringUtil.notNullize(template.getDescription()), sudden, false);
    myTemplate = template;
  }

  @Nonnull
  @Override
  public String getLookupString() {
    return myTemplate.getKey();
  }

  @Nonnull
  public Template getTemplate() {
    return myTemplate;
  }

  @Override
  public char getTemplateShortcut() {
    return TemplateSettingsImpl.getInstanceImpl().getShortcutChar(myTemplate);
  }

  @Override
  public void handleInsert(InsertionContext context) {
    context.getDocument().deleteString(context.getStartOffset(), context.getTailOffset());
    context.setAddCompletionChar(false);
    TemplateManager.getInstance(context.getProject()).startTemplate(context.getEditor(), myTemplate);
  }
}
