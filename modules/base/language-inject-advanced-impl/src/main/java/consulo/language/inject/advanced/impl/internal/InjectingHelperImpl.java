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
package consulo.language.inject.advanced.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.inject.MultiHostRegistrar;
import consulo.language.inject.advanced.internal.InjectingHelper;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2025-01-12
 */
@Singleton
@ServiceImpl
public class InjectingHelperImpl implements InjectingHelper {
    @Override
    public void injectReference(@Nonnull MultiHostRegistrar registrar, @Nonnull Language language, @Nonnull String prefix, @Nonnull String suffix, @Nonnull PsiLanguageInjectionHost host, @Nonnull TextRange rangeInsideHost) {
        InjectedLanguageUtil.injectReference(registrar, language, prefix, suffix, host, rangeInsideHost);
    }

    @Override
    public <T> void putInjectedFileUserData(@Nonnull PsiElement element, @Nonnull Language language, @Nonnull Key<T> key, @Nullable T value) {
        InjectedLanguageUtil.putInjectedFileUserData(element, language, key, value);
    }
}
