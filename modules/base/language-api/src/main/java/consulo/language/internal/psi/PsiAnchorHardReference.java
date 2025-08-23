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
package consulo.language.internal.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiAnchor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;

public class PsiAnchorHardReference implements PsiAnchor {
    private final PsiElement myElement;

    public PsiAnchorHardReference(@Nonnull PsiElement element) {
        myElement = element;
    }

    @Override
    public PsiElement retrieve() {
        return myElement.isValid() ? myElement : null;
    }

    @Override
    public PsiFile getFile() {
        return myElement.getContainingFile();
    }

    @Override
    @RequiredReadAction
    public int getStartOffset() {
        return myElement.getTextRange().getStartOffset();
    }

    @Override
    @RequiredReadAction
    public int getEndOffset() {
        return myElement.getTextRange().getEndOffset();
    }

    @Override
    public boolean equals(Object o) {
        return o == this
            || o instanceof PsiAnchorHardReference that
            && myElement.equals(that.myElement);
    }

    @Override
    public int hashCode() {
        return myElement.hashCode();
    }
}
