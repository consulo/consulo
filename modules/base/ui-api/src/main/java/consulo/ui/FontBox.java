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
package consulo.ui;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;

/**
 * Chooser of an installed font family, by family name. Which families exist and how a family is
 * previewed are the environment's business, not the caller's - a desktop toolkit draws each row in
 * its own typeface, a browser leaves it to the document - so the widget stays with the frontend
 * rather than being assembled from a {@link ComboBox} here.
 *
 * @author VISTALL
 * @since 2026-09-03
 */
public interface FontBox extends ValueComponent<String>, HasFocus {
    @RequiredUIAccess
    static FontBox create() {
        return UIInternal.get()._Components_fontBox();
    }

    /**
     * Restricts the list to families whose glyphs are of one width, which is what an editor font has
     * to be. A frontend which cannot tell the two apart offers everything rather than nothing.
     */
    void setMonospacedOnly(boolean monospacedOnly);

    boolean isMonospacedOnly();

    default FontBox withMonospacedOnly(boolean monospacedOnly) {
        setMonospacedOnly(monospacedOnly);
        return this;
    }
}
