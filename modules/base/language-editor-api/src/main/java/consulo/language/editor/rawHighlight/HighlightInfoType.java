/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.language.editor.rawHighlight;

import consulo.codeEditor.CodeInsightColors;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.TextAttributesKey;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.psi.PsiElement;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public interface HighlightInfoType {
    HighlightInfoType ERROR = new HighlightInfoTypeImpl(HighlightSeverity.ERROR, CodeInsightColors.ERRORS_ATTRIBUTES);
    HighlightInfoType WARNING = new HighlightInfoTypeImpl(HighlightSeverity.WARNING, CodeInsightColors.WARNINGS_ATTRIBUTES);
    /**
     * @deprecated use {@link #WEAK_WARNING} instead
     */
    HighlightInfoType INFO = new HighlightInfoTypeImpl(HighlightSeverity.INFO, CodeInsightColors.INFO_ATTRIBUTES);
    HighlightInfoType WEAK_WARNING = new HighlightInfoTypeImpl(HighlightSeverity.WEAK_WARNING, CodeInsightColors.WEAK_WARNING_ATTRIBUTES);
    HighlightInfoType INFORMATION = new HighlightInfoTypeImpl(HighlightSeverity.INFORMATION, CodeInsightColors.INFORMATION_ATTRIBUTES);

    HighlightInfoType WRONG_REF = new HighlightInfoTypeImpl(HighlightSeverity.ERROR, CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES);

    HighlightInfoType GENERIC_WARNINGS_OR_ERRORS_FROM_SERVER =
        new HighlightInfoTypeImpl(HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING, CodeInsightColors.GENERIC_SERVER_ERROR_OR_WARNING);

    HighlightInfoType DUPLICATE_FROM_SERVER =
        new HighlightInfoTypeImpl(HighlightSeverity.INFORMATION, CodeInsightColors.DUPLICATE_FROM_SERVER);

    HighlightInfoType RAW_UNUSED_SYMBOL = new HighlightInfoTypeImpl(
        HighlightSeverity.INFORMATION,
        CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES
    );

    HighlightInfoType RAW_DEPRECATED = new HighlightInfoTypeImpl(
        HighlightSeverity.WARNING,
        CodeInsightColors.DEPRECATED_ATTRIBUTES
    );

    HighlightInfoType RAW_MARKED_FOR_REMOVAL = new HighlightInfoTypeImpl(
        HighlightSeverity.WARNING,
        CodeInsightColors.MARKED_FOR_REMOVAL_ATTRIBUTES
    );

    HighlightSeverity SYMBOL_TYPE_SEVERITY = new HighlightSeverity("SYMBOL_TYPE_SEVERITY", HighlightSeverity.INFORMATION.myVal - 2);

    HighlightInfoType TODO = new HighlightInfoTypeImpl(HighlightSeverity.INFORMATION, CodeInsightColors.TODO_DEFAULT_ATTRIBUTES, false);
    // these are default attributes, can be configured differently for specific patterns
    HighlightInfoType UNHANDLED_EXCEPTION = new HighlightInfoTypeImpl(HighlightSeverity.ERROR, CodeInsightColors.ERRORS_ATTRIBUTES);

    HighlightSeverity INJECTED_FRAGMENT_SEVERITY = new HighlightSeverity("INJECTED_FRAGMENT", SYMBOL_TYPE_SEVERITY.myVal - 1);
    HighlightInfoType INJECTED_LANGUAGE_FRAGMENT =
        new HighlightInfoTypeImpl(SYMBOL_TYPE_SEVERITY, CodeInsightColors.INFORMATION_ATTRIBUTES);
    HighlightInfoType INJECTED_LANGUAGE_BACKGROUND =
        new HighlightInfoTypeImpl(INJECTED_FRAGMENT_SEVERITY, CodeInsightColors.INFORMATION_ATTRIBUTES);

    HighlightSeverity ELEMENT_UNDER_CARET_SEVERITY = new HighlightSeverity("ELEMENT_UNDER_CARET", HighlightSeverity.ERROR.myVal + 1);
    HighlightInfoType ELEMENT_UNDER_CARET_READ =
        new HighlightInfoTypeImpl(ELEMENT_UNDER_CARET_SEVERITY, EditorColors.IDENTIFIER_UNDER_CARET_ATTRIBUTES);
    HighlightInfoType ELEMENT_UNDER_CARET_WRITE =
        new HighlightInfoTypeImpl(ELEMENT_UNDER_CARET_SEVERITY, EditorColors.WRITE_IDENTIFIER_UNDER_CARET_ATTRIBUTES);

    /**
     * @see RangeHighlighter#VISIBLE_IF_FOLDED
     */
    Set<HighlightInfoType> VISIBLE_IF_FOLDED = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        ELEMENT_UNDER_CARET_READ,
        ELEMENT_UNDER_CARET_WRITE,
        WARNING,
        ERROR,
        WRONG_REF
    )));

    @Nonnull
    HighlightSeverity getSeverity(@Nullable PsiElement psiElement);

    TextAttributesKey getAttributesKey();

    interface Iconable {
        @Nullable
        Image getIcon();
    }

    interface UpdateOnTypingSuppressible {
        boolean needsUpdateOnTyping();
    }
}
