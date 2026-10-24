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
package consulo.review.async;

import consulo.disposer.Disposable;
import consulo.review.util.ComputableSequence;
import consulo.review.util.SequenceComputer;
import consulo.review.util.SequenceItem;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public final class ListPartSequenceWithMutableCache<T> implements ComputableSequence<SequenceItem<List<T>>> {
    private final Disposable myParentDisposable;
    private final ComputableSequence<SequenceItem<List<T>>> mySequence;
    private @Nullable SharedListPartSequence<T> mySharedSequence;

    public ListPartSequenceWithMutableCache(Disposable parentDisposable, ComputableSequence<SequenceItem<List<T>>> sequence) {
        myParentDisposable = parentDisposable;
        mySequence = sequence;
    }

    @Override
    public SequenceComputer<SequenceItem<List<T>>> getComputer() {
        return shared().getComputer();
    }

    public void updateItem(Function<T, @Nullable T> updater) {
        SharedListPartSequence<T> shared;
        synchronized (this) {
            shared = mySharedSequence;
        }
        if (shared != null) {
            shared.updateItem(updater);
        }
    }

    public void reset() {
        SharedListPartSequence<T> shared;
        synchronized (this) {
            shared = mySharedSequence;
            mySharedSequence = null;
        }
        if (shared != null) {
            shared.cancel();
        }
    }

    private synchronized SharedListPartSequence<T> shared() {
        SharedListPartSequence<T> shared = mySharedSequence;
        if (shared == null) {
            shared = new SharedListPartSequence<>(myParentDisposable, mySequence.getComputer());
            mySharedSequence = shared;
        }
        return shared;
    }
}
