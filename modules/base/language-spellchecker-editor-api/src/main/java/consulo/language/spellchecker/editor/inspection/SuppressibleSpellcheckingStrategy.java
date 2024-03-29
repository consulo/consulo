/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.spellchecker.editor.inspection;

import consulo.language.editor.inspection.SuppressQuickFix;
import consulo.language.psi.PsiElement;
import consulo.language.spellcheker.SpellcheckingStrategy;

import jakarta.annotation.Nonnull;

/**
 * Base class to use to make spellchecking in your language suppressible.
 * Just delegate this to your suppression util code, as you'll do in normal inspection for your language.
 */
public abstract class SuppressibleSpellcheckingStrategy extends SpellcheckingStrategy {
  /**
   * @see consulo.language.editor.inspection.CustomSuppressableInspectionTool#isSuppressedFor(PsiElement)
   */
  public abstract boolean isSuppressedFor(@Nonnull PsiElement element, @Nonnull String name);

  /**
   * @see consulo.language.editor.inspection.BatchSuppressableTool#getBatchSuppressActions(PsiElement)
   */
  public abstract SuppressQuickFix[] getSuppressActions(@Nonnull PsiElement element, @Nonnull String name);
}
