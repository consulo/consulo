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

import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.uast.util.UastImplementationUtil;
import consulo.language.uast.util.UastLazyPart;
import consulo.language.uast.visitor.UastTypedVisitor;
import consulo.language.uast.visitor.UastVisitor;
import org.jspecify.annotations.Nullable;

public class UComment implements UElement {
    private final PsiComment mySourcePsi;
    private final @Nullable UElement myGivenParent;

    private final UastLazyPart<@Nullable UElement> myUastParentPart = new UastLazyPart<>();

    public UComment(PsiComment sourcePsi, @Nullable UElement givenParent) {
        mySourcePsi = sourcePsi;
        myGivenParent = givenParent;
    }

    @Override
    public PsiComment getSourcePsi() {
        return mySourcePsi;
    }

    /**
     * @deprecated see the base property description
     */
    @Deprecated
    @Override
    public PsiComment getPsi() {
        return getSourcePsi();
    }

    @Override
    public @Nullable UElement getUastParent() {
        return UastLazyPart.getOrBuild(myUastParentPart, () -> {
            if (myGivenParent != null) {
                return myGivenParent;
            }
            PsiElement parent = mySourcePsi.getParent();
            return parent == null ? null : UastFacade.toUElement(parent);
        });
    }

    public String getText() {
        return asSourceString();
    }

    // Consulo: the core UastVisitor has no visitComment/afterVisitComment hooks, comments dispatch through the generic element hooks
    @Override
    public void accept(UastVisitor visitor) {
        if (visitor.visitElement(this)) {
            return;
        }
        visitor.afterVisitElement(this);
    }

    @Override
    public <D, R> R accept(UastTypedVisitor<D, R> visitor, D data) {
        return visitor.visitElement(this, data);
    }

    @Override
    public String asLogString() {
        return UastImplementationUtil.log(this);
    }

    @Override
    public String asRenderString() {
        return asSourceString();
    }

    @Override
    public String asSourceString() {
        return mySourcePsi.getText();
    }
}
