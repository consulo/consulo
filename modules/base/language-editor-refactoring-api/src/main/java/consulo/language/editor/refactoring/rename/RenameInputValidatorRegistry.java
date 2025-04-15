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

/**
 * @author Dmitry Avdeev
 */
package consulo.language.editor.refactoring.rename;

import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import consulo.util.lang.function.Condition;

import jakarta.annotation.Nullable;

import java.util.function.Function;

public class RenameInputValidatorRegistry {
    private RenameInputValidatorRegistry() {
    }

    @Nullable
    public static Condition<String> getInputValidator(PsiElement element) {
        for (RenameInputValidator validator : RenameInputValidator.EP_NAME.getExtensionList()) {
            ProcessingContext context = new ProcessingContext();
            if (validator.getPattern().accepts(element, context)) {
                return s -> validator.isInputValid(s, element, context);
            }
        }
        return null;
    }

    @Nullable
    public static Function<String, String> getInputErrorValidator(PsiElement element) {
        for (RenameInputValidator validator : RenameInputValidator.EP_NAME.getExtensionList()) {
            if (validator instanceof RenameInputValidatorEx renameInputValidatorEx) {
                ProcessingContext context = new ProcessingContext();
                if (validator.getPattern().accepts(element, context)) {
                    return newName -> renameInputValidatorEx.getErrorMessage(newName, element.getProject());
                }
            }
        }
        return null;
    }
}
