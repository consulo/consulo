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
package consulo.language.editor.rawHighlight;

import consulo.language.editor.internal.InspectionCache;
import consulo.language.editor.internal.InspectionCacheService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class HighlightDisplayKey {
    @Nullable
    public static HighlightDisplayKey findById(@Nonnull String id) {
        InspectionCache cache = InspectionCacheService.getInstance().get();
        return cache.findById(id);
    }

    @Nullable
    public static HighlightDisplayKey find(@Nonnull String name) {
        InspectionCache cache = InspectionCacheService.getInstance().get();
        return cache.find(name);
    }

    private final String myName;

    private final String myID;

    public HighlightDisplayKey(@Nonnull String name, @Nonnull String id) {
        myName = name;
        myID = id;
    }

    @Nonnull
    public String getID() {
        return myID;
    }

    @Override
    public String toString() {
        return myName;
    }
}
