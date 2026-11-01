/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.spellchecker.editor.impl.inspection;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.spellchecker.editor.SpellcheckerEngine;
import consulo.language.spellchecker.editor.SpellcheckerEngineManager;
import consulo.language.spellchecker.editor.SpellcheckerFeature;
import consulo.language.spellchecker.editor.SpellcheckerTextProblem;
import consulo.language.spellchecker.editor.localize.SpellCheckerLocalize;
import consulo.language.spellcheker.text.TextContent;
import consulo.language.spellcheker.text.CommonTextExtractor;
import consulo.language.spellcheker.text.TextExtractionFilter;
import consulo.language.spellcheker.text.TextExtractor;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.localize.LocalizeValue;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reports problems of one {@link SpellcheckerFeature} found by {@link SpellcheckerEngine#checkText} in natural language texts.
 * Texts come from {@link TextExtractor}s; languages without an extractor use {@link CommonTextExtractor}.
 *
 * @author VISTALL
 * @since 2026-11-01
 */
public abstract class TextProblemInspection extends LocalInspectionTool {
    private static final int MAX_FIXES = 3;

    private record CachedProblems(SpellcheckerEngine engine, List<SpellcheckerTextProblem> problems) {
    }

    // shared by all text problem inspections: the engine checks each text once
    private static final Key<CachedProblems> PROBLEMS = Key.create("SpellcheckerTextProblems");

    private final SpellcheckerEngineManager myEngineManager;
    private final SpellcheckerFeature myFeature;
    private final HighlightSeverity mySeverity;

    protected TextProblemInspection(SpellcheckerEngineManager engineManager, SpellcheckerFeature feature, HighlightSeverity severity) {
        myEngineManager = engineManager;
        myFeature = feature;
        mySeverity = severity;
    }

    @Override
    public LocalizeValue getGroupDisplayName() {
        return SpellCheckerLocalize.spelling();
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.find(mySeverity);
    }

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly, LocalInspectionToolSession session, Object state) {
        SpellcheckerEngine engine = myEngineManager.getActiveEngine();
        if (engine == null || !engine.getFeatures().contains(myFeature)) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        Set<TextContent> checked = new HashSet<>();
        return new PsiElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitElement(PsiElement element) {
                for (TextContent content : findTexts(element)) {
                    // adjacent line comments are joined: every comment of the group yields the same text
                    if (!checked.add(content)) {
                        continue;
                    }
                    for (SpellcheckerTextProblem problem : getProblems(engine, content)) {
                        if (problem.kind() == myFeature && !content.hasUnknownFragmentsIn(problem.range())) {
                            register(holder, content, problem);
                        }
                    }
                }
            }
        };
    }

    @RequiredReadAction
    private static List<TextContent> findTexts(PsiElement element) {
        if (TextExtractor.forLanguage(element.getLanguage()) != null) {
            return TextExtractor.findUniqueTextsAt(element, TextContent.TextDomain.ALL);
        }
        TextExtractionFilter filter = element.getApplication().getInstance(TextExtractionFilter.class);
        List<TextContent> contents = new ArrayList<>();
        for (TextContent content : CommonTextExtractor.buildCommonTextContents(element, TextContent.TextDomain.ALL)) {
            if (!filter.shouldIgnore(content)) {
                contents.add(content);
            }
        }
        return contents;
    }

    private static List<SpellcheckerTextProblem> getProblems(SpellcheckerEngine engine, TextContent content) {
        CachedProblems cached = content.getUserData(PROBLEMS);
        if (cached != null && cached.engine() == engine) {
            return cached.problems();
        }
        List<SpellcheckerTextProblem> problems = engine.checkText(content.getContainingFile().getProject(), content);
        content.putUserData(PROBLEMS, new CachedProblems(engine, problems));
        return problems;
    }

    @RequiredReadAction
    private static void register(ProblemsHolder holder, TextContent content, SpellcheckerTextProblem problem) {
        TextRange fileRange = content.textRangeToFile(problem.range());
        PsiElement parent = content.getCommonParent();
        TextRange rangeInParent = fileRange.shiftLeft(parent.getTextRange().getStartOffset());

        List<LocalQuickFix> fixes = new ArrayList<>();
        for (String suggestion : problem.suggestions()) {
            if (fixes.size() >= MAX_FIXES) {
                break;
            }
            fixes.add(new ReplaceTextFix(fileRange, suggestion));
        }

        holder.newProblem(problem.message())
            .range(parent, rangeInParent)
            .withFixes(fixes.toArray(LocalQuickFix.EMPTY_ARRAY))
            .create();
    }
}
