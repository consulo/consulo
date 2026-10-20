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
package consulo.ui.font;

/**
 * Typographic feature a {@link Font} can be asked to draw with. A font which does not carry the feature,
 * or an environment which cannot switch it on, draws as if it was not asked.
 *
 * @author VISTALL
 * @since 2026-10-20
 */
public enum FontFeature {
    /**
     * Digits of one width, so numbers stacked in a column line up - dates, times, sizes.
     */
    TABULAR_NUMBERS("tnum");

    private final String myTag;

    FontFeature(String tag) {
        myTag = tag;
    }

    /**
     * @return the OpenType tag of the feature
     */
    public String getTag() {
        return myTag;
    }
}
