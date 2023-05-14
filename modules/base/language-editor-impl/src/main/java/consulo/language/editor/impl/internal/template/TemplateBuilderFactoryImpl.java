/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.editor.impl.internal.template;

import consulo.annotation.component.ServiceImpl;
import consulo.language.editor.template.Template;
import consulo.language.editor.template.TemplateBuilder;
import consulo.language.editor.template.TemplateBuilderFactory;
import consulo.language.psi.PsiElement;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
@Singleton
@ServiceImpl
public class TemplateBuilderFactoryImpl extends TemplateBuilderFactory {
  @Override
  public TemplateBuilder createTemplateBuilder(@Nonnull PsiElement element) {
    return new TemplateBuilderImpl(element);
  }

  @Override
  public Template createRawTemplate(@Nonnull String key, String group) {
    return new TemplateImpl(key, group);
  }

  @Override
  public Template createRawTemplate(@Nonnull String key, String group, String text) {
    return new TemplateImpl(key, text, group);
  }
}
