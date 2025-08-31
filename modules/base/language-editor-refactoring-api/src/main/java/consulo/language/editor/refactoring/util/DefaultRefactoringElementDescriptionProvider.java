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

package consulo.language.editor.refactoring.util;

import consulo.language.findUsage.DescriptiveNameUtil;
import consulo.language.psi.ElementDescriptionProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.ElementDescriptionLocation;
import consulo.usage.UsageViewUtil;
import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
public class DefaultRefactoringElementDescriptionProvider implements ElementDescriptionProvider {
  public static final DefaultRefactoringElementDescriptionProvider INSTANCE = new DefaultRefactoringElementDescriptionProvider();

  @Override
  public String getElementDescription(@Nonnull PsiElement element, @Nonnull ElementDescriptionLocation location) {
    String typeString = UsageViewUtil.getType(element);
    String name = DescriptiveNameUtil.getDescriptiveName(element);
    return typeString + " " + CommonRefactoringUtil.htmlEmphasize(name);
  }
}
