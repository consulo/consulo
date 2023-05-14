/*
 * Copyright 2013-2023 consulo.io
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

import consulo.annotation.access.RequiredReadAction;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.inspection.SuppressQuickFix;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.spellchecker.editor.SpellcheckerEngine;
import consulo.language.spellchecker.editor.SpellcheckerEngineManager;
import consulo.language.spellchecker.editor.SpellcheckerSeverities;
import consulo.language.spellcheker.SpellcheckingStrategy;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 26/03/2023
 */
public abstract class SpellcheckerInspection extends LocalInspectionTool {
  private final SpellcheckerEngineManager mySpellcheckerEngineManager;
  private final String mySpellcheckerEngineId;

  protected SpellcheckerInspection(SpellcheckerEngineManager spellcheckerEngineManager, String spellcheckerEngineId) {
    mySpellcheckerEngineManager = spellcheckerEngineManager;
    mySpellcheckerEngineId = spellcheckerEngineId;
  }

  @Nonnull
  public final String getSpellcheckerEngineId() {
    return mySpellcheckerEngineId;
  }

  protected static SpellcheckingStrategy getSpellcheckingStrategy(@Nonnull PsiElement element, @Nonnull Language language) {
    for (SpellcheckingStrategy strategy : SpellcheckingStrategy.forLanguage(language)) {
      if (strategy.isMyContext(element)) {
        return strategy;
      }
    }
    return null;
  }

  @Nonnull
  @Override
  public final PsiElementVisitor buildVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
    return super.buildVisitor(holder, isOnTheFly);
  }

  @Nonnull
  @Override
  public final PsiElementVisitor buildVisitor(@Nonnull ProblemsHolder holder,
                                        boolean isOnTheFly,
                                        @Nonnull LocalInspectionToolSession session,
                                        @Nonnull Object state) {
    SpellcheckerEngine engine = mySpellcheckerEngineManager.getActiveEngine();
    if (engine == null || !mySpellcheckerEngineId.equals(engine.getId())) {
      return PsiElementVisitor.EMPTY_VISITOR;
    }

    return buildVisitorImpl(holder, isOnTheFly, session, state);
  }

  @Nonnull
  public abstract PsiElementVisitor buildVisitorImpl(@Nonnull ProblemsHolder holder,
                                                     boolean isOnTheFly,
                                                     @Nonnull LocalInspectionToolSession session,
                                                     @Nonnull Object state);

  @Nonnull
  @Override
  public String getGroupDisplayName() {
    return "Spelling";
  }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }

  @Override
  @Nonnull
  public final HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.find(SpellcheckerSeverities.TYPO);
  }

  @Nonnull
  @Override
  @RequiredReadAction
  public SuppressQuickFix[] getBatchSuppressActions(@Nullable PsiElement element) {
    if (element != null) {
      final Language language = element.getLanguage();
      SpellcheckingStrategy strategy = getSpellcheckingStrategy(element, language);
      if (strategy instanceof SuppressibleSpellcheckingStrategy) {
        return ((SuppressibleSpellcheckingStrategy)strategy).getSuppressActions(element, getShortName());
      }
    }
    return super.getBatchSuppressActions(element);
  }

  @Override
  @RequiredReadAction
  public boolean isSuppressedFor(@Nonnull PsiElement element) {
    final Language language = element.getLanguage();
    SpellcheckingStrategy strategy = getSpellcheckingStrategy(element, language);
    if (strategy instanceof SuppressibleSpellcheckingStrategy) {
      return ((SuppressibleSpellcheckingStrategy)strategy).isSuppressedFor(element, getShortName());
    }
    return super.isSuppressedFor(element);
  }
}
