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
package consulo.ide.impl.ui.app.impl.settings;

import consulo.configurable.Configurable;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface OptionsEditorHost {
    Collection<Configurable> getTreeConfigurables();

    String getFilterText();

    void clearFilter();

    CompletableFuture<?> refilter(String text, boolean adjustSelection, boolean now);

    @RequiredUIAccess
    void rebuildTree(Runnable afterRebuild);

    CompletableFuture<?> selectInTree(@Nullable Configurable configurable);

    boolean isPageCreated(Configurable configurable);

    default boolean isPageEditable(Configurable configurable) {
        return true;
    }

    @RequiredUIAccess
    CompletableFuture<?> createPage(Configurable configurable);

    boolean isDisposed();
}
