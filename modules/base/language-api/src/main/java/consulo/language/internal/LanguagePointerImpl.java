/*
 * Copyright 2013-2016 consulo.io
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

import consulo.component.util.pointer.NamedPointer;
import consulo.language.Language;
import consulo.language.LanguageRegistry;
import consulo.util.lang.lazy.ClearableLazyValue;
import consulo.util.lang.lazy.LazyValue;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2013-08-31
 */
class LanguagePointerImpl implements NamedPointer<Language> {
    private String myId;

    private final LanguageRegistry myLanguageRegistry;

    private final ClearableLazyValue<Language> myValue;

    public LanguagePointerImpl(String id, LanguageRegistry languageRegistry) {
        myId = id;
        myLanguageRegistry = languageRegistry;
        myValue = ClearableLazyValue.nullable(() -> myLanguageRegistry.findLanguage(id));
    }

    public void reset() {
        myValue.clear();
    }

    @Override
    public String getName() {
        return myId;
    }

    @Override
    public @Nullable Language get() {
        return myValue.get();
    }
}
