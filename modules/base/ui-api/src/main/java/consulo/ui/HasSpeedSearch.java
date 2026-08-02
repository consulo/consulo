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

import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * Type to find an item, for the collection components.
 * <p/>
 * Highlighting the match is the backend's job, so a render never has to consult
 * {@link #getSpeedSearchText()} to get matches highlighted.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
public interface HasSpeedSearch<Item> extends Component {
    /**
     * @param converter item to searchable text, {@code null} turns speed search off
     */
    void setSpeedSearchConverter(@Nullable Function<Item, String> converter);

    /**
     * Current query, for callers driving their own UI off it.
     */
    @Nullable
    String getSpeedSearchText();
}
