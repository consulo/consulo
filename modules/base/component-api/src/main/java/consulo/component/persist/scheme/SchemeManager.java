/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.component.persist.scheme;

import consulo.util.lang.function.ThrowableFunction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.List;

public abstract class SchemeManager<T, E extends ExternalizableScheme> {
    @Nonnull
    public abstract Collection<E> loadSchemes();

    public abstract void addNewScheme(@Nonnull T scheme, boolean replaceExisting);

    public abstract void clearAllSchemes();

    @Nonnull
    public abstract List<T> getAllSchemes();

    @Nullable
    public abstract T findSchemeByName(@Nonnull String schemeName);

    public abstract void save();

    public abstract void setCurrentSchemeName(@Nullable String schemeName);

    @Nullable
    public abstract T getCurrentScheme();

    public abstract void removeScheme(@Nonnull T scheme);

    @Nonnull
    public abstract Collection<String> getAllSchemeNames();

    public abstract File getRootDirectory();

    public void loadBundledScheme(@Nonnull URL requestor, @Nonnull ThrowableFunction<Element, T, Throwable> convertor) {
    }
}
