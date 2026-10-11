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
package consulo.ide.impl.roots.ui.configuration;

import consulo.content.ContentFolderPropertyProvider;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
@SuppressWarnings("rawtypes")
public final class ContentFolderPropertyItem {
    private final @Nullable ContentFolderPropertyProvider myProvider;
    private final Key myKey;
    private @Nullable Object myValue;

    public ContentFolderPropertyItem(@Nullable ContentFolderPropertyProvider provider, Key key, @Nullable Object value) {
        myProvider = provider;
        myKey = key;
        myValue = value;
    }

    public @Nullable ContentFolderPropertyProvider getProvider() {
        return myProvider;
    }

    public Key getKey() {
        return myKey;
    }

    public @Nullable Object getValue() {
        return myValue;
    }

    public void setValue(@Nullable Object value) {
        myValue = value;
    }
}
