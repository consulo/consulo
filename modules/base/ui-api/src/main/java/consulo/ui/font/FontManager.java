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

import consulo.ui.UIAccess;
import consulo.ui.internal.UIInternal;

import java.util.List;
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
     * Whether the environment enumerates its families only once the user has agreed to it, as the browser
     * does. Until that is granted such a frontend answers with whatever it can render anyway rather than
     * with nothing.
     */
    boolean isRequiredPermission();

    /**
     * The families installed in the environment, read afresh on every call rather than remembered - what is
     * installed changes while the application runs, and on a frontend which
     * {@link #isRequiredPermission() asks first} so does the answer once the user agrees. Enumerating them is
     * slow enough to be worth keeping off the ui thread everywhere, and it is this call that does the asking,
     * so it is handed the ui doing it rather than reaching for a current one - a frontend can be serving
     * several at once.
     */
    CompletableFuture<List<Typeface>> getAvailableTypefacesAsync(UIAccess uiAccess);

    Font createFont(String fontName, int fontSize, int fontStyles);
}
