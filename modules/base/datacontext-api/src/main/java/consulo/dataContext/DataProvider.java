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
package consulo.dataContext;

import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Allows a component hosting actions to provide context information to the actions. When a specific
 * data item is requested, the component hierarchy is walked up from the currently focused component,
 * and every component implementing the <code>DataProvider</code> interface is queried for the data
 * until one of them returns the data. Data items can also be mapped to each other - for example,
 * if a data provider provides an item for {@link PlatformDataKeys#NAVIGATABLE}, an item for
 * {@link PlatformDataKeys#NAVIGATABLE_ARRAY} can be generated from it automatically.
 *
 * @see DataContext
 */
public interface DataProvider {
    /**
     * Returns the object corresponding to the specified data identifier. Some of the supported
     * data identifiers are defined in the {@link consulo.ide.impl.idea.openapi.actionSystem.PlatformDataKeys} class.
     *
     * @param dataId the data identifier for which the value is requested.
     * @return the value, or null if no value is available in the current context for this identifier.
     */
    @Nullable
    Object getData(@Nonnull Key<?> dataId);

    @Nullable
    @SuppressWarnings("unchecked")
    default <T> T getDataUnchecked(@Nonnull Key<T> key) {
        return (T)getData(key);
    }
}
