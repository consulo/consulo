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
package consulo.language.inject.advanced.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.inject.MultiHostRegistrar;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2025-01-12
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface InjectingHelper {
    void injectReference(@Nonnull MultiHostRegistrar registrar,
                         @Nonnull Language language,
                         @Nonnull String prefix,
                         @Nonnull String suffix,
                         @Nonnull PsiLanguageInjectionHost host,
                         @Nonnull TextRange rangeInsideHost);

    <T> void putInjectedFileUserData(@Nonnull PsiElement element, @Nonnull Language language, @Nonnull Key<T> key, @Nullable T value);
}
