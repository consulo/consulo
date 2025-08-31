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
package consulo.language.codeStyle;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author lesya
 */
public class PostFormatProcessorHelper {
    private final CommonCodeStyleSettings mySettings;
    private TextRange myResultTextRange;

    public PostFormatProcessorHelper(CommonCodeStyleSettings settings) {
        mySettings = settings;
    }

    public CommonCodeStyleSettings getSettings() {
        return mySettings;
    }

    public void updateResultRange(int oldTextLength, int newTextLength) {
        if (myResultTextRange == null) {
            return;
        }

        myResultTextRange = new TextRange(myResultTextRange.getStartOffset(),
            myResultTextRange.getEndOffset() - oldTextLength + newTextLength);
    }

    @RequiredReadAction
    public boolean isElementPartlyInRange(@Nonnull PsiElement element) {
        if (myResultTextRange == null) {
            return true;
        }

        TextRange elementRange = element.getTextRange();
        if (elementRange.getEndOffset() < myResultTextRange.getStartOffset()) {
            return false;
        }
        return elementRange.getStartOffset() <= myResultTextRange.getEndOffset();

    }

    @RequiredReadAction
    public boolean isElementFullyInRange(PsiElement element) {
        if (myResultTextRange == null) {
            return true;
        }

        TextRange elementRange = element.getTextRange();

        return elementRange.getStartOffset() >= myResultTextRange.getStartOffset()
            && elementRange.getEndOffset() <= myResultTextRange.getEndOffset();
    }

    @RequiredReadAction
    public static boolean isMultiline(@Nullable PsiElement statement) {
        if (statement == null) {
            return false;
        }
        else {
            return statement.textContains('\n');
        }
    }

    public void setResultTextRange(TextRange resultTextRange) {
        myResultTextRange = resultTextRange;
    }

    public TextRange getResultTextRange() {
        return myResultTextRange;
    }
}
