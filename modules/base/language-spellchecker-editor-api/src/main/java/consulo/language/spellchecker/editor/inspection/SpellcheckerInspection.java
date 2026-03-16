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

import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2023-03-26
 */
public abstract class SpellcheckerInspection extends LocalInspectionTool {
    private final SpellcheckerEngineManager mySpellcheckerEngineManager;
    private final String mySpellcheckerEngineId;

    protected SpellcheckerInspection(SpellcheckerEngineManager spellcheckerEngineManager, String spellcheckerEngineId) {
        mySpellcheckerEngineManager = spellcheckerEngineManager;
        mySpellcheckerEngineId = spellcheckerEngineId;
    }

    
    public final String getSpellcheckerEngineId() {
        return mySpellcheckerEngineId;
    }

    @RequiredReadAction
    protected static SpellcheckingStrategy getSpellcheckingStrategy(PsiElement element) {
        for (SpellcheckingStrategy strategy : SpellcheckingStrategy.forLanguage(element.getLanguage())) {
            if (strategy.isMyContext(element)) {
                return strategy;
            }
        }
        return null;
    }

    
    @Override
    public final PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return super.buildVisitor(holder, isOnTheFly);
    }

    
    @Override
    public final PsiElementVisitor buildVisitor(
        ProblemsHolder holder,
        boolean isOnTheFly,
        LocalInspectionToolSession session,
        Object state
    ) {
        SpellcheckerEngine engine = mySpellcheckerEngineManager.getActiveEngine();
        if (engine == null || !mySpellcheckerEngineId.equals(engine.getId())) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        return buildVisitorImpl(holder, isOnTheFly, session, state);
    }

    
    public abstract PsiElementVisitor buildVisitorImpl(
        ProblemsHolder holder,
        boolean isOnTheFly,
        LocalInspectionToolSession session,
        Object state
    );

    
    @Override
    public LocalizeValue getGroupDisplayName() {
        return LocalizeValue.localizeTODO("Spelling");
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    
    public final HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.find(SpellcheckerSeverities.TYPO);
    }

    
    @Override
    @RequiredReadAction
    public SuppressQuickFix[] getBatchSuppressActions(@Nullable PsiElement element) {
        if (element != null && getSpellcheckingStrategy(element) instanceof SuppressibleSpellcheckingStrategy strategy) {
            return strategy.getSuppressActions(element, getShortName());
        }
        return super.getBatchSuppressActions(element);
    }

    @Override
    @RequiredReadAction
    public boolean isSuppressedFor(PsiElement element) {
        if (getSpellcheckingStrategy(element) instanceof SuppressibleSpellcheckingStrategy strategy) {
            return strategy.isSuppressedFor(element, getShortName());
        }
        return super.isSuppressedFor(element);
    }
}
