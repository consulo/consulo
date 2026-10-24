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

import java.util.List;
import java.util.function.Consumer;

final class BatchesSubscription<T> {
    private final SharedBatches<T> myBatches;
    private final Consumer<List<T>> myConsumer;
    private int myCurrentPagesCount;

    BatchesSubscription(SharedBatches<T> batches, Consumer<List<T>> consumer) {
        myBatches = batches;
        myConsumer = consumer;
    }

    synchronized void drain() {
        int pagesCount = myBatches.pagesCount();
        if (pagesCount <= myCurrentPagesCount) {
            return;
        }
        List<T> batch = myBatches.pagesFrom(myCurrentPagesCount);
        myCurrentPagesCount = pagesCount;
        myConsumer.accept(batch);
    }
}
