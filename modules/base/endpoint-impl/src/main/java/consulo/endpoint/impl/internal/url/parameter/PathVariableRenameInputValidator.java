// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.parameter;

import consulo.annotation.component.ExtensionImpl;
import consulo.endpoint.url.parameter.PathVariablePsiElement;
import consulo.language.editor.refactoring.rename.RenameInputValidator;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;

@ExtensionImpl
public final class PathVariableRenameInputValidator implements RenameInputValidator {
    @Override
    public ElementPattern<? extends PsiElement> getPattern() {
        return PlatformPatterns.psiElement(PathVariablePsiElement.class);
    }

    @Override
    public boolean isInputValid(String newName, PsiElement element, ProcessingContext context) {
        if (!(element instanceof PathVariablePsiElement pathVariableElement)) {
            return false;
        }
        return pathVariableElement.getNameValidationPattern().matcher(newName).matches();
    }
}
