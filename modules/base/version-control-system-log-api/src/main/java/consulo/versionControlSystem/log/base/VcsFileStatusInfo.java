// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.versionControlSystem.log.base;

import consulo.versionControlSystem.change.Change;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class VcsFileStatusInfo {
    private final byte myTypeByte;
    private final CharSequence myFirst;
    private final @Nullable CharSequence mySecond;

    public VcsFileStatusInfo(byte typeByte, CharSequence first, @Nullable CharSequence second) {
        myTypeByte = typeByte;
        myFirst = first;
        mySecond = second;
    }

    public VcsFileStatusInfo(Change.Type type, String firstPath, @Nullable String secondPath) {
        this(type, (CharSequence) firstPath, secondPath);
    }

    public VcsFileStatusInfo(Change.Type type, CharSequence firstPath, @Nullable CharSequence secondPath) {
        this((byte) type.ordinal(), firstPath, secondPath);
    }

    public byte getTypeByte() {
        return myTypeByte;
    }

    public CharSequence getFirst() {
        return myFirst;
    }

    public @Nullable CharSequence getSecond() {
        return mySecond;
    }

    public String getFirstPath() {
        return myFirst.toString();
    }

    public @Nullable String getSecondPath() {
        return mySecond == null ? null : mySecond.toString();
    }

    public Change.Type getType() {
        return Change.Type.values()[myTypeByte];
    }

    // NB watch out for the CharSequence#toString performance for the FS-interned paths
    public boolean isRenamed() {
        if (getType() != Change.Type.MOVED || mySecond == null) {
            return false;
        }
        return !getFirstPath().equals(getSecondPath());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof VcsFileStatusInfo info
            && myTypeByte == info.myTypeByte
            && myFirst.equals(info.myFirst)
            && Objects.equals(mySecond, info.mySecond);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myTypeByte, myFirst, mySecond);
    }

    @Override
    public String toString() {
        String s = getType() + " " + myFirst;
        if (mySecond != null) {
            s += " -> " + mySecond;
        }
        return s;
    }
}
