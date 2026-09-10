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
package consulo.ui;

import consulo.localize.LocalizeValue;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * The two checks nearly every prompt needs. Messages are supplied by the caller, since this module
 * has no localize library of its own.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
public final class InputValidators {
    public static InputValidator<String> nonEmpty(LocalizeValue whenEmpty) {
        return value -> StringUtil.isEmptyOrSpaces(value) ? InputProblem.error(whenEmpty) : null;
    }

    public static InputValidator<String> unique(Supplier<Collection<String>> taken, LocalizeValue whenTaken) {
        return uniqueExcept(taken, null, whenTaken);
    }

    /**
     * @param allowed the value being renamed, which is free to keep its own name
     */
    public static InputValidator<String> uniqueExcept(Supplier<Collection<String>> taken,
                                                      @Nullable String allowed,
                                                      LocalizeValue whenTaken) {
        return value -> {
            String trimmed = value.trim();
            if (allowed != null && allowed.equals(trimmed)) {
                return null;
            }
            return taken.get().contains(trimmed) ? InputProblem.error(whenTaken) : null;
        };
    }

    private InputValidators() {
    }
}
