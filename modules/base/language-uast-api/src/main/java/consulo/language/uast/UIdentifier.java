/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.language.uast;

import consulo.language.psi.PsiElement;
import consulo.language.uast.util.UastImplementationUtil;
import consulo.language.uast.util.UastLazyPart;
import org.jspecify.annotations.Nullable;

public class UIdentifier implements UElement {
    /**
     * A {@link UIdentifier} whose parent is computed lazily from the source PSI when it was not given explicitly.
     */
    public static class LazyParentUIdentifier extends UIdentifier {
        private @Nullable Object myUastParentValue;

        public LazyParentUIdentifier(@Nullable PsiElement psi, @Nullable UElement givenParent) {
            super(psi, givenParent);
            myUastParentValue = givenParent != null ? givenParent : UastLazyPart.UNINITIALIZED_UAST_PART;
        }

        @Override
        public @Nullable UElement getUastParent() {
            Object currentValue = myUastParentValue;
            if (currentValue != UastLazyPart.UNINITIALIZED_UAST_PART) {
                return (UElement) currentValue;
            }

            UElement newValue = computeParent();
            myUastParentValue = newValue;

            return newValue;
        }

        protected @Nullable UElement computeParent() {
            PsiElement sourcePsi = getSourcePsi();
            PsiElement parent = sourcePsi == null ? null : sourcePsi.getParent();
            return parent == null ? null : UastFacade.toUElement(parent);
        }
    }

    private final @Nullable PsiElement mySourcePsi;
    private final @Nullable UElement myUastParent;

    public UIdentifier(@Nullable PsiElement sourcePsi, @Nullable UElement uastParent) {
        mySourcePsi = sourcePsi;
        myUastParent = uastParent;
    }

    @Override
    public @Nullable PsiElement getSourcePsi() {
        return mySourcePsi;
    }

    @Override
    public @Nullable UElement getUastParent() {
        return myUastParent;
    }

    /**
     * Returns the identifier name.
     */
    public String getName() {
        PsiElement sourcePsi = getSourcePsi();
        return sourcePsi != null ? sourcePsi.getText() : "<error>";
    }

    @Override
    public String asLogString() {
        return UastImplementationUtil.log(this, "Identifier (" + getName() + ")");
    }

    /**
     * @deprecated see the base property description
     */
    @Deprecated
    @Override
    public @Nullable PsiElement getPsi() {
        return getSourcePsi();
    }
}
