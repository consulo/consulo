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
package consulo.language.impl.psi.path;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.impl.psi.FakePsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.SyntheticElement;
import consulo.platform.Platform;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Eugene.Kudelevsky
 */
public class WebReference extends PsiReferenceBase<PsiElement> {
    @Nullable
    private final String myUrl;

    public WebReference(@Nonnull PsiElement element) {
        this(element, (String) null);
    }

    public WebReference(@Nonnull PsiElement element, @Nullable String url) {
        super(element, true);
        myUrl = url;
    }

    public WebReference(@Nonnull PsiElement element, @Nonnull TextRange textRange) {
        this(element, textRange, null);
    }

    public WebReference(@Nonnull PsiElement element, TextRange textRange, @Nullable String url) {
        super(element, textRange, true);
        myUrl = url;
    }

    @Override
    @RequiredReadAction
    public PsiElement resolve() {
        return new MyFakePsiElement();
    }

    public String getUrl() {
        return myUrl != null ? myUrl : getValue();
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public Object[] getVariants() {
        return EMPTY_ARRAY;
    }

    public class MyFakePsiElement extends FakePsiElement implements SyntheticElement {
        @Override
        public PsiElement getParent() {
            return myElement;
        }

        @Override
        public void navigate(boolean requestFocus) {
            Platform.current().openInBrowser(getUrl());
        }

        @Override
        public String getPresentableText() {
            return getUrl();
        }


        @Nullable
        @Override
        @RequiredReadAction
        public String getName() {
            return getUrl();
        }

        @Nonnull
        @Override
        @RequiredReadAction
        public TextRange getTextRange() {
            TextRange rangeInElement = getRangeInElement();
            TextRange elementRange = myElement.getTextRange();
            return rangeInElement.shiftRight(elementRange.getStartOffset());
        }
    }
}
