// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.util;

import org.jspecify.annotations.Nullable;

/**
 * A value together with a hint whether it is the last one in a sequence.
 * <p>
 * {@link #isLast()} is an optional optimization: a consumer may finish as soon as it sees {@code isLast == true} instead of asking for
 * one more item only to discover the sequence has ended. Consumers that do not care may simply ignore it.
 * <p>
 * A part of an incrementally computed list is a {@code SequenceItem<List<T>>}: a sequence item whose value is one chunk of a larger list.
 */
public interface SequenceItem<T extends @Nullable Object> {
    T getValue();

    boolean isLast();

    /**
     * Creates a {@link SequenceItem} holding {@code value} and flagged with {@code isLast}.
     */
    static <T extends @Nullable Object> SequenceItem<T> of(T value, boolean isLast) {
        return new SequenceItemImpl<>(value, isLast);
    }
}
