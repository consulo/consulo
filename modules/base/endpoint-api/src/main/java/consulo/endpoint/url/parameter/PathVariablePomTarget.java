// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.parameter;

import consulo.document.util.TextRange;
import consulo.endpoint.util.SimpleNamePomTarget;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class PathVariablePomTarget extends SimpleNamePomTarget {
    public PathVariablePomTarget(String name, PsiElement scope, TextRange range, SemDefinitionProvider semDefinitionProvider) {
        super(name);
        myScope = scope;
        myTextRange = range;
        mySemDefinitionProvider = semDefinitionProvider;
    }

    private final PsiElement myScope;
    private final TextRange myTextRange;
    private final SemDefinitionProvider mySemDefinitionProvider;

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        PathVariablePomTarget target = (PathVariablePomTarget) o;
        return myScope.equals(target.myScope) && myTextRange.equals(target.myTextRange);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), myScope, myTextRange);
    }

    public PsiElement getScope() {
        return myScope;
    }

    public TextRange getTextRange() {
        return myTextRange;
    }

    public SemDefinitionProvider getSemDefinitionProvider() {
        return mySemDefinitionProvider;
    }

    public Iterable<PsiElement> findSemDefinitionPsiElement() {
        return mySemDefinitionProvider.findSemDefiningElements(this);
    }
}
