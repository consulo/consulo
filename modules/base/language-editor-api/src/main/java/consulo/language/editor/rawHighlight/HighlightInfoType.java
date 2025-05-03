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
import consulo.language.editor.DeprecationUtil;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.inspection.InspectionsBundle;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProfileManager;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.language.psi.PsiElement;
import consulo.logging.Logger;
import consulo.ui.image.Image;
import consulo.util.xml.serializer.WriteExternalException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;

import java.util.*;

public interface HighlightInfoType {
    String UNUSED_SYMBOL_SHORT_NAME = "unused";
    String UNUSED_SYMBOL_DISPLAY_NAME = InspectionsBundle.message("inspection.dead.code.display.name");
    @Deprecated
    String UNUSED_SYMBOL_ID = "UnusedDeclaration";

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

    HighlightInfoType UNUSED_SYMBOL = new HighlightInfoTypeSeverityByKey(
        HighlightDisplayKey.findOrRegister(UNUSED_SYMBOL_SHORT_NAME, UNUSED_SYMBOL_DISPLAY_NAME, UNUSED_SYMBOL_SHORT_NAME),
        CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES
    );

    HighlightInfoType DEPRECATED = new HighlightInfoTypeSeverityByKey(
        HighlightDisplayKey.findOrRegister(
            DeprecationUtil.DEPRECATION_SHORT_NAME,
            DeprecationUtil.DEPRECATION_DISPLAY_NAME,
            DeprecationUtil.DEPRECATION_ID
        ),
        CodeInsightColors.DEPRECATED_ATTRIBUTES
    );

    HighlightInfoType MARKED_FOR_REMOVAL = new HighlightInfoTypeSeverityByKey(
        HighlightDisplayKey.findOrRegister(
            DeprecationUtil.FOR_REMOVAL_SHORT_NAME,
            DeprecationUtil.FOR_REMOVAL_DISPLAY_NAME,
            DeprecationUtil.FOR_REMOVAL_ID
        ),
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
        new HighlightInfoType.HighlightInfoTypeImpl(ELEMENT_UNDER_CARET_SEVERITY, EditorColors.IDENTIFIER_UNDER_CARET_ATTRIBUTES);
    HighlightInfoType ELEMENT_UNDER_CARET_WRITE =
        new HighlightInfoType.HighlightInfoTypeImpl(ELEMENT_UNDER_CARET_SEVERITY, EditorColors.WRITE_IDENTIFIER_UNDER_CARET_ATTRIBUTES);

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

    class HighlightInfoTypeImpl implements HighlightInfoType, HighlightInfoType.UpdateOnTypingSuppressible {
        private final HighlightSeverity mySeverity;
        private final TextAttributesKey myAttributesKey;
        private boolean myNeedsUpdateOnTyping;

        //read external only
        public HighlightInfoTypeImpl(@Nonnull Element element) {
            mySeverity = new HighlightSeverity(element);
            myAttributesKey = new TextAttributesKey(element);
        }

        public HighlightInfoTypeImpl(@Nonnull HighlightSeverity severity, TextAttributesKey attributesKey) {
            this(severity, attributesKey, true);
        }

        public HighlightInfoTypeImpl(@Nonnull HighlightSeverity severity, TextAttributesKey attributesKey, boolean needsUpdateOnTyping) {
            mySeverity = severity;
            myAttributesKey = attributesKey;
            myNeedsUpdateOnTyping = needsUpdateOnTyping;
        }

        @Override
        @Nonnull
        public HighlightSeverity getSeverity(@Nullable PsiElement psiElement) {
            return mySeverity;
        }

        @Override
        public TextAttributesKey getAttributesKey() {
            return myAttributesKey;
        }

        @Override
        @SuppressWarnings({"HardCodedStringLiteral"})
        public String toString() {
            return "HighlightInfoTypeImpl[severity=" + mySeverity + ", key=" + myAttributesKey + "]";
        }

        public void writeExternal(Element element) {
            try {
                mySeverity.writeExternal(element);
            }
            catch (WriteExternalException e) {
                throw new RuntimeException(e);
            }
            myAttributesKey.writeExternal(element);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            HighlightInfoTypeImpl that = (HighlightInfoTypeImpl)o;

            return Objects.equals(myAttributesKey, that.myAttributesKey)
                && mySeverity.equals(that.mySeverity);
        }

        @Override
        public int hashCode() {
            int result = mySeverity.hashCode();
            return 29 * result + myAttributesKey.hashCode();
        }

        @Override
        public boolean needsUpdateOnTyping() {
            return myNeedsUpdateOnTyping;
        }
    }

    class HighlightInfoTypeSeverityByKey implements HighlightInfoType {
        static final Logger LOG = Logger.getInstance(HighlightInfoTypeSeverityByKey.class);

        private final TextAttributesKey myAttributesKey;
        private final HighlightDisplayKey myToolKey;

        public HighlightInfoTypeSeverityByKey(HighlightDisplayKey severityKey, TextAttributesKey attributesKey) {
            myToolKey = severityKey;
            myAttributesKey = attributesKey;
        }

        @Override
        @Nonnull
        public HighlightSeverity getSeverity(PsiElement psiElement) {
            InspectionProfile profile = psiElement == null
                ? (InspectionProfile)InspectionProfileManager.getInstance().getRootProfile()
                : InspectionProjectProfileManager.getInstance(psiElement.getProject()).getInspectionProfile();
            return profile.getErrorLevel(myToolKey, psiElement).getSeverity();
        }

        @Override
        public TextAttributesKey getAttributesKey() {
            return myAttributesKey;
        }

        @Override
        @SuppressWarnings({"HardCodedStringLiteral"})
        public String toString() {
            return "HighlightInfoTypeSeverityByKey[severity=" + myToolKey + ", key=" + myAttributesKey + "]";
        }

        public HighlightDisplayKey getSeverityKey() {
            return myToolKey;
        }
    }

    interface Iconable {
        @Nullable
        Image getIcon();
    }

    interface UpdateOnTypingSuppressible {
        boolean needsUpdateOnTyping();
    }
}
