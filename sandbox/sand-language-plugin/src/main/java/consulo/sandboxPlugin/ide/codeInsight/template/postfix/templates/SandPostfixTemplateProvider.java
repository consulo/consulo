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
package consulo.sandboxPlugin.ide.codeInsight.template.postfix.templates;

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import java.util.HashSet;
import javax.annotation.Nonnull;

import java.util.Set;

/**
 * @author VISTALL
 * @since 16.08.14
 */
public class SandPostfixTemplateProvider implements PostfixTemplateProvider {
  @Nonnull
  @Override
  public Set<PostfixTemplate> getTemplates() {
    return new HashSet<PostfixTemplate>() {
      {
        add(new TestTemplate());
        add(new DDTemplate());
      }
    };
  }

  @Override
  public boolean isTerminalSymbol(char currentChar) {
    return currentChar == '\t';
  }

  @Override
  public void preExpand(@Nonnull PsiFile file, @Nonnull Editor editor) {

  }

  @Override
  public void afterExpand(@Nonnull PsiFile file, @Nonnull Editor editor) {

  }

  @Nonnull
  @Override
  public PsiFile preCheck(@Nonnull PsiFile copyFile, @Nonnull Editor realEditor, int currentOffset) {
    return copyFile;
  }

}
