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
import consulo.language.version.LanguageVersion;
import consulo.logging.Logger;
import consulo.util.collection.MultiMap;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Owns the registered languages. A language registers itself while it is constructed, which can happen before the
 * application exists, so the storage is static and the service reads it.
 *
 * @author VISTALL
 * @since 2026-06-16
 */
@ServiceImpl
@Singleton
public class LanguageRegistryImpl implements LanguageRegistry {
    private static final Logger LOG = Logger.getInstance(LanguageRegistryImpl.class);

    private static final Map<Class<? extends Language>, Language> ourLanguagesByClass = new ConcurrentHashMap<>();
    private static final Map<String, Language> ourLanguagesById = new ConcurrentHashMap<>();
    private static volatile @Nullable MultiMap<String, LanguageVersion> ourVersionsByMimeType;
    private static final AtomicLong ourModificationCount = new AtomicLong();

    private final Map<String, NamedPointer<Language>> myPointerCache = new ConcurrentHashMap<>();

    public static void registerLanguage(Language language) {
        Class<? extends Language> languageClass = language.getClass();
        Language prev = ourLanguagesByClass.put(languageClass, language);
        if (prev != null) {
            LOG.error("Language of '" + languageClass + "' is already registered: " + prev);
            return;
        }

        prev = ourLanguagesById.put(language.getID(), language);
        if (prev != null) {
            LOG.error("Language with ID '" + language.getID() + "' is already registered: " + prev.getClass());
        }
        ourVersionsByMimeType = null;
        ourModificationCount.incrementAndGet();
    }

    /**
     * Removes languages of a plugin unloaded at runtime.
     */
    public static void unregisterLanguages(ClassLoader classLoader) {
        ourLanguagesByClass.values().removeIf(language -> language.getClass().getClassLoader() == classLoader);
        ourLanguagesById.values().removeIf(language -> language.getClass().getClassLoader() == classLoader);
        ourVersionsByMimeType = null;
        ourModificationCount.incrementAndGet();
    }

    public static Collection<Language> getRegisteredLanguages() {
        return Collections.unmodifiableCollection(ourLanguagesByClass.values());
    }

    @SuppressWarnings("unchecked")
    public static <T extends Language> @Nullable T findLanguageByClass(Class<T> languageClass) {
        return (T) ourLanguagesByClass.get(languageClass);
    }

    public static @Nullable Language findLanguageById(String languageId) {
        return ourLanguagesById.get(languageId);
    }

    public static MultiMap<String, LanguageVersion> getVersionsByMimeType() {
        MultiMap<String, LanguageVersion> map = ourVersionsByMimeType;
        if (map != null) {
            return map;
        }

        map = new MultiMap<>();
        for (Language language : ourLanguagesByClass.values()) {
            LanguageVersion[] versions = language.getVersions();
            for (LanguageVersion version : versions) {
                for (String mimeType : version.getMimeTypes()) {
                    map.putValue(mimeType, version);
                }
            }

            for (String mimeType : language.getMimeTypes()) {
                for (LanguageVersion version : versions) {
                    map.putValue(mimeType, version);
                }
            }
        }

        ourVersionsByMimeType = map;
        return map;
    }

    @Override
    public @Nullable Language findLanguage(String languageId) {
        return findLanguageById(languageId);
    }

    @Override
    public NamedPointer<Language> createLanguagePointer(String languageId) {
        return myPointerCache.computeIfAbsent(languageId, _ -> new LanguagePointerImpl(languageId, this));
    }

    @Override
    public Collection<Language> getLanguages() {
        return getRegisteredLanguages();
    }

    @Override
    public long getModificationCount() {
        return ourModificationCount.get();
    }
}
