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
package consulo.language.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.component.util.pointer.NamedPointer;
import consulo.language.Language;
import consulo.language.LanguageRegistry;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO rework in future, for now its a bridge to new api
 *
 * @author VISTALL
 * @since 2026-06-16
 */
@ServiceImpl
@Singleton
public class LanguageRegistryImpl implements LanguageRegistry {
    private final Map<String, NamedPointer<Language>> myPointerCache = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("deprecation")
    public @Nullable Language findLanguage(String languageId) {
        return Language.findLanguageByID(languageId);
    }

    @Override
    public NamedPointer<Language> createLanguagePointer(String languageId) {
        return myPointerCache.computeIfAbsent(languageId, _ -> new LanguagePointerImpl(languageId, this));
    }

    @Override
    @SuppressWarnings("deprecation")
    public Collection<Language> getLanguages() {
        return Language.getRegisteredLanguages();
    }
}
