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
    public static Condition<String> getInputValidator(final PsiElement element) {
        for (final RenameInputValidator validator : RenameInputValidator.EP_NAME.getExtensionList()) {
            final ProcessingContext context = new ProcessingContext();
            if (validator.getPattern().accepts(element, context)) {
                return new Condition<String>() {
                    @Override
                    public boolean value(final String s) {
                        return validator.isInputValid(s, element, context);
                    }
                };
            }
        }
        return null;
    }

    @Nullable
    public static Function<String, String> getInputErrorValidator(final PsiElement element) {
        for (final RenameInputValidator validator : RenameInputValidator.EP_NAME.getExtensionList()) {
            if (!(validator instanceof RenameInputValidatorEx)) {
                continue;
            }
            final ProcessingContext context = new ProcessingContext();
            if (validator.getPattern().accepts(element, context)) {
                return newName -> ((RenameInputValidatorEx)validator).getErrorMessage(newName, element.getProject());
            }
        }
        return null;
    }
}
