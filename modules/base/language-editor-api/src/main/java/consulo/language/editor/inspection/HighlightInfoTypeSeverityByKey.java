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
package consulo.language.editor.inspection;

import consulo.colorScheme.TextAttributesKey;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProfileManager;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;

public class HighlightInfoTypeSeverityByKey implements HighlightInfoType {
    private final TextAttributesKey myAttributesKey;
    private final HighlightDisplayKey myToolKey;

    public HighlightInfoTypeSeverityByKey(HighlightDisplayKey severityKey, TextAttributesKey attributesKey) {
        myToolKey = severityKey;
        myAttributesKey = attributesKey;
    }

    @Override
    @Nonnull
    public HighlightSeverity getSeverity(PsiElement psiElement) {
        InspectionProfile profile;
        if (psiElement == null) {
            profile = (InspectionProfile) InspectionProfileManager.getInstance().getRootProfile();
        }
        else {
            profile = InspectionProjectProfileManager.getInstance(psiElement.getProject()).getInspectionProfile();
        }
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
