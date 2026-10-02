// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.parameter;

import consulo.application.util.query.Plow;
import consulo.language.pom.PomTargetPsiElement;
import consulo.language.psi.PsiElement;

import java.util.function.Predicate;

public interface PathVariableDefinitionsSearcher {
    boolean processDefinitions(PsiElement context, Predicate<? super PomTargetPsiElement> processor);

    default Plow<PomTargetPsiElement> getPathVariables(PsiElement context) {
        return Plow.of(processor -> processDefinitions(context, processor));
    }
}
