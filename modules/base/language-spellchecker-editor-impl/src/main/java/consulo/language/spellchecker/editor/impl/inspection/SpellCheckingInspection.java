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
import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.inspection.SuppressQuickFix;
import consulo.language.editor.inspection.InspectionToolState;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.refactoring.NamesValidator;
import consulo.language.parser.ParserDefinition;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.spellchecker.editor.SpellCheckerManager;
import consulo.language.spellchecker.editor.SpellcheckerSeverities;
import consulo.language.spellchecker.editor.SpellcheckingUtil;
import consulo.language.spellchecker.editor.inspection.SuppressibleSpellcheckingStrategy;
import consulo.language.spellchecker.editor.localize.SpellCheckerLocalize;
import consulo.language.spellcheker.SpellcheckingStrategy;
import consulo.language.spellcheker.tokenizer.TokenConsumer;
import consulo.language.spellcheker.tokenizer.Tokenizer;
import consulo.language.spellcheker.tokenizer.splitter.SplitContext;
import consulo.language.spellcheker.tokenizer.splitter.TokenSplitter;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The platform spellchecking inspection. Words are validated by {@link SpellCheckerManager}: user and bundled dictionaries first,
 * then the active {@link consulo.language.spellchecker.editor.SpellcheckerEngine}.
 */
@ExtensionImpl
public class SpellCheckingInspection extends LocalInspectionTool {
    public static final String SPELL_CHECKING_INSPECTION_TOOL_NAME = SpellcheckingUtil.SPELL_CHECKING_INSPECTION_TOOL_NAME;

    /**
     * Used for comments of languages which have no {@link SpellcheckingStrategy}.
     */
    private static final SpellcheckingStrategy COMMENT_STRATEGY = new SpellcheckingStrategy() {
        @Override
        public Language getLanguage() {
            return Language.ANY;
        }
    };

    @Override
    public LocalizeValue getGroupDisplayName() {
        return SpellCheckerLocalize.spelling();
    }

    @Override
    public LocalizeValue getDisplayName() {
        return SpellCheckerLocalize.spellcheckingInspectionName();
    }

    @Override
    public String getShortName() {
        return SPELL_CHECKING_INSPECTION_TOOL_NAME;
    }

    @Override
    public LocalizeValue getDescription() {
        return SpellCheckerLocalize.spellcheckingInspectionDescription();
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.find(SpellcheckerSeverities.TYPO);
    }

    @Override
    public InspectionToolState<?> createStateProvider() {
        return new SpellCheckingInspectionState();
    }

    @Override
    @RequiredReadAction
    public SuppressQuickFix[] getBatchSuppressActions(@Nullable PsiElement element) {
        if (element != null && SpellcheckingUtil.getSpellcheckingStrategy(element) instanceof SuppressibleSpellcheckingStrategy strategy) {
            return strategy.getSuppressActions(element, getShortName());
        }
        return super.getBatchSuppressActions(element);
    }

    @Override
    @RequiredReadAction
    public boolean isSuppressedFor(PsiElement element) {
        if (SpellcheckingUtil.getSpellcheckingStrategy(element) instanceof SuppressibleSpellcheckingStrategy strategy) {
            return strategy.isSuppressedFor(element, getShortName());
        }
        return super.isSuppressedFor(element);
    }

    @Override
    public PsiElementVisitor buildVisitor(
        ProblemsHolder holder,
        boolean isOnTheFly,
        LocalInspectionToolSession session,
        Object state
    ) {
        SpellCheckingInspectionState localState = (SpellCheckingInspectionState) state;
        SpellCheckerManager manager = SpellCheckerManager.getInstance(holder.getProject());
        return new PsiElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitElement(PsiElement element) {
                ASTNode node = element.getNode();
                if (node == null) {
                    return;
                }
                // Extract parser definition from element
                Language language = element.getLanguage();
                IElementType elementType = node.getElementType();
                ParserDefinition parserDefinition = ParserDefinition.forLanguage(element.getApplication(), language);

                // Handle selected options
                if (parserDefinition != null) {
                    if (parserDefinition.getStringLiteralElements(element.getLanguageVersion()).contains(elementType)) {
                        if (!localState.processLiterals) {
                            return;
                        }
                    }
                    else if (parserDefinition.getCommentTokens(element.getLanguageVersion()).contains(elementType)) {
                        if (!localState.processComments) {
                            return;
                        }
                    }
                    else if (!localState.processCode) {
                        return;
                    }
                }

                tokenize(element, new MyTokenConsumer(manager, holder, NamesValidator.forLanguage(language)));
            }
        };
    }

    @RequiredReadAction
    @SuppressWarnings("unchecked")
    private static void tokenize(PsiElement element, TokenConsumer consumer) {
        SpellcheckingStrategy strategy = SpellcheckingUtil.getSpellcheckingStrategy(element);
        if (strategy == null && element instanceof PsiComment) {
            strategy = COMMENT_STRATEGY;
        }
        if (strategy == null) {
            return;
        }
        Tokenizer tokenizer = strategy.getTokenizer(element);
        tokenizer.tokenize(element, consumer);
    }

    @RequiredReadAction
    private static void addRegularDescriptor(
        PsiElement element,
        TextRange textRange,
        ProblemsHolder holder,
        boolean useRename,
        String wordWithTypo
    ) {
        LocalQuickFix[] fixes = SpellcheckerQuickFixes.getDefaultRegularFixes(useRename, wordWithTypo, element, textRange);

        holder.newProblem(SpellCheckerLocalize.typoInWordRef())
            .range(element, textRange)
            .withFixes(fixes)
            .onTheFly()
            .create();
    }

    private static class MyTokenConsumer extends TokenConsumer implements Consumer<TextRange> {
        private final Set<String> myAlreadyChecked = new HashSet<>();
        private final SpellCheckerManager myManager;
        private final ProblemsHolder myHolder;
        private final @Nullable NamesValidator myNamesValidator;
        private @Nullable PsiElement myElement;
        private @Nullable String myText;
        private boolean myUseRename;
        private int myOffset;

        public MyTokenConsumer(SpellCheckerManager manager, ProblemsHolder holder, @Nullable NamesValidator namesValidator) {
            myManager = manager;
            myHolder = holder;
            myNamesValidator = namesValidator;
        }

        @Override
        @RequiredReadAction
        public void consumeToken(
            PsiElement element,
            String text,
            boolean useRename,
            int offset,
            TextRange rangeToCheck,
            TokenSplitter splitter
        ) {
            myElement = element;
            myText = text;
            myUseRename = useRename;
            myOffset = offset;
            splitter.split(SplitContext.of(text, this), rangeToCheck);
        }

        @Override
        @RequiredReadAction
        public void accept(TextRange textRange) {
            PsiElement element = myElement;
            String text = myText;
            if (element == null || text == null) {
                return;
            }

            String word = textRange.substring(text);
            if (!myHolder.isOnTheFly() && myAlreadyChecked.contains(word)) {
                return;
            }

            boolean keyword = myNamesValidator != null && myNamesValidator.isKeyword(word, element.getProject());
            if (keyword) {
                return;
            }

            if (myManager.hasProblem(word)) {
                //Use tokenizer to generate accurate range in element (e.g. in case of escape sequences in element)
                SpellcheckingStrategy strategy = SpellcheckingUtil.getSpellcheckingStrategy(element);

                Tokenizer tokenizer = strategy != null ? strategy.getTokenizer(element) : null;
                TextRange range = tokenizer != null ? tokenizer.getHighlightingRange(element, myOffset, textRange) : textRange;
                assert range.getStartOffset() >= 0;

                if (myHolder.isOnTheFly()) {
                    addRegularDescriptor(element, range, myHolder, myUseRename, word);
                }
                else {
                    myAlreadyChecked.add(word);
                    myHolder.newProblem(SpellCheckerLocalize.typoInWordRef())
                        .range(element, range)
                        .withFixes(SpellcheckerQuickFixes.getDefaultBatchFixes())
                        .create();
                }
            }
        }
    }
}
