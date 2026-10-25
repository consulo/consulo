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
package consulo.versionControlSystem.review.impl.internal.diff;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * One child scope of {@code parentDisposable} at a time: starting the next one disposes the previous.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
final class SequentialDisposable {
    private final Disposable myParentDisposable;
    private @Nullable Disposable myCurrent;

    SequentialDisposable(Disposable parentDisposable) {
        myParentDisposable = parentDisposable;
    }

    void restart(Consumer<Disposable> body) {
        Disposable previous = myCurrent;
        myCurrent = null;
        if (previous != null) {
            Disposer.dispose(previous);
        }
        if (Disposer.isDisposed(myParentDisposable)) {
            return;
        }
        Disposable current = Disposable.newDisposable("SequentialDisposable");
        Disposer.register(myParentDisposable, current);
        myCurrent = current;
        body.accept(current);
    }
}
