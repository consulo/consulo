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
package consulo.language.editor.refactoring.rename;

import consulo.usage.MoveRenameUsageInfo;
import consulo.language.psi.PsiElement;

/**
 * @author dsl
 * @since 2002-06-05
 */
public class CollisionUsageInfo extends MoveRenameUsageInfo {
  public CollisionUsageInfo(PsiElement element, PsiElement referencedElement) {
    super(element, null, referencedElement);
  }
}
