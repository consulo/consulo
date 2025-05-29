/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.util.lang;

import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Contract;

public abstract class ImmutableCharSequence implements CharSequence {

    public static CharSequence asImmutable(@Nonnull final CharSequence cs) {
        return isImmutable(cs) ? cs : cs.toString();
    }

    public static boolean isImmutable(@Nonnull final CharSequence cs) {
        if (cs instanceof ImmutableCharSequence) {
            return true;
        }
        return cs instanceof CharSequenceSubSequence && isImmutable(((CharSequenceSubSequence) cs).getBaseSequence());
    }

    @Contract(pure = true)
    @Nonnull
    public ImmutableCharSequence replace(int start, int end, @Nonnull CharSequence seq) {
        return delete(start, end).insert(start, seq);
    }

    public abstract ImmutableCharSequence concat(CharSequence sequence);

    public abstract ImmutableCharSequence insert(int index, CharSequence seq);

    public abstract ImmutableCharSequence delete(int start, int end);

    public abstract ImmutableCharSequence subtext(int start, int end);
}
