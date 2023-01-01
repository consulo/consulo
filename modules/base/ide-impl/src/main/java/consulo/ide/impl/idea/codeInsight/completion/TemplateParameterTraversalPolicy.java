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
package consulo.ide.impl.idea.codeInsight.completion;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.codeEditor.Editor;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiFile;

/**
 * @author Evgeny Gerashchenko
 * @since 2/1/12
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface TemplateParameterTraversalPolicy {
  ExtensionPointName<TemplateParameterTraversalPolicy> EP_NAME = ExtensionPointName.create(TemplateParameterTraversalPolicy.class);

  boolean isValidForFile(Editor editor, PsiFile file);

  void invoke(Editor editor, PsiFile file, boolean next);
}
