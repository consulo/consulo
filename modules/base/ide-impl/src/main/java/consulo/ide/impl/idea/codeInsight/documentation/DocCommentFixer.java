/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.documentation;

import consulo.language.OldLanguageExtension;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiComment;
import consulo.container.plugin.PluginIds;

import javax.annotation.Nonnull;

/**
 * @author Denis Zhdanov
 * @since 9/20/12 8:37 PM
 */
public interface DocCommentFixer {

  OldLanguageExtension<DocCommentFixer> EXTENSION = new OldLanguageExtension<>(PluginIds.CONSULO_BASE + ".lang.documentationFixer");

  // TODO den add doc
  void fixComment(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiComment comment);
}
