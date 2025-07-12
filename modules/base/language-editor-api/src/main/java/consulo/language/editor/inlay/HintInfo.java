/*
 * Copyright 2013-2025 consulo.io
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
package consulo.language.editor.inlay;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.editor.localize.LanguageEditorLocalize;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Base class for hint info variants.
 */
public abstract sealed class HintInfo permits HintInfo.MethodInfo, HintInfo.OptionInfo {
    /**
     * Method-based hint information.
     */
    public static final class MethodInfo extends HintInfo {
        private final String fullyQualifiedName;
        private final List<String> paramNames;
        private final Language language;

        public MethodInfo(String fullyQualifiedName, List<String> paramNames, Language language) {
            this.fullyQualifiedName = fullyQualifiedName;
            this.paramNames = paramNames;
            this.language = language;
        }

        public MethodInfo(String fullyQualifiedName, List<String> paramNames) {
            this(fullyQualifiedName, paramNames, null);
        }

        public List<String> getParamNames() {
            return paramNames;
        }

        public Language getLanguage() {
            return language;
        }

        public String getFullyQualifiedName() {
            return fullyQualifiedName;
        }

        public String getMethodName() {
            int start = fullyQualifiedName.lastIndexOf('.') + 1;
            return fullyQualifiedName.substring(start);
        }

        @Nonnull
        public LocalizeValue getDisableHintText() {
            return LanguageEditorLocalize.inlayHintsShowSettings(getMethodName());
        }
    }

    /**
     * Option-based hint information.
     */
    public static final class OptionInfo extends HintInfo {
        private final Option option;

        public OptionInfo(Option option) {
            this.option = option;
        }

        public void disable() {
            alternate();
        }

        public void enable() {
            alternate();
        }

        private void alternate() {
            boolean current = option.get();
            option.set(!current);
        }

        @Nonnull
        public LocalizeValue getOptionName() {
            return option.getName();
        }

        public boolean isOptionEnabled() {
            return option.isEnabled();
        }
    }

    @RequiredReadAction
    public boolean isOwnedByPsiElement(PsiElement elem, Editor editor) {
        if (elem.getTextRange() == TextRange.EMPTY_RANGE) {
            return false;
        }

        int start = elem.getTextRange().isEmpty() ? elem.getTextRange().getStartOffset()
            : elem.getTextRange().getStartOffset() + 1;
        return editor.getInlayModel().hasInlineElementsInRange(start, elem.getTextRange().getEndOffset());
    }
}
