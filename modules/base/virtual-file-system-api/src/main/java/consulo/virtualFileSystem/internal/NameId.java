/*
 * Copyright 2013-2025 consulo.io
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
package consulo.virtualFileSystem.internal;

import jakarta.annotation.Nonnull;

public final class NameId {
    public static final NameId[] EMPTY_ARRAY = new NameId[0];

    public final int id;
    public final CharSequence name;
    public final int nameId;

    public NameId(int id, int nameId, @Nonnull CharSequence name) {
        this.id = id;
        this.nameId = nameId;
        this.name = name;
        if (id <= 0 || nameId <= 0) {
            throw new IllegalArgumentException("invalid arguments id: " + id + "; nameId: " + nameId);
        }
    }

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }
}
