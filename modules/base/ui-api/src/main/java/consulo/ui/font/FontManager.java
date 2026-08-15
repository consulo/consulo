/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ui.font;

import consulo.ui.internal.UIInternal;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2020-06-04
 */
public interface FontManager {
    static FontManager get() {
        return UIInternal.get()._FontManager_get();
    }

    /**
     * Env can't return font and sync call, also require ask user for permission before call {@link #getAvailableFontNames()}
     */
    boolean isRequiredPermission();

    /**
     * Return list of fonts which is installed in env, but can return if not checked {@link #isRequiredPermission()}
     */
    CompletableFuture<Set<String>> getAvailableFontNamesAsync();

    @Deprecated
    Set<String> getAvailableFontNames();

    Font createFont(String fontName, int fontSize, int fontStyles);
}
