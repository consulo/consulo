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

import consulo.disposer.Disposable;
import consulo.util.concurrent.coroutine.ObservableValue;

import java.util.List;

/**
 * A helper for implementing a state of an async computed list
 */
public interface AsyncIncrementalListComputer<T> {
    ObservableValue<IncrementallyComputedValue<List<T>>> getState();

    /**
     * Request to compute the next portion of the list
     * Gives no guarantee anything will actually be computed
     */
    void requestMore();

    static <T> AsyncIncrementalListComputer<T> createIn(Disposable parentDisposable, ComputableSequence<SequenceItem<List<T>>> sequence) {
        return createIn(parentDisposable, sequence.getComputer(), false);
    }

    /**
     * @param loadAfterDone if this computer will compute more data after the {@code computer} returns {@link DoneComputationOutcome}
     */
    static <T> AsyncIncrementalListComputer<T> createIn(Disposable parentDisposable,
                                                        SequenceComputer<SequenceItem<List<T>>> computer,
                                                        boolean loadAfterDone) {
        return new AsyncIncrementalListComputerImpl<>(parentDisposable, computer, loadAfterDone);
    }
}
