/*
 * Copyright 2000-2009 JetBrains s.r.o.
 * Copyright 2013-2020 consulo.io
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
package consulo.disposer;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * This class marks classes, which require some work done for cleaning up.
 * As a general policy you shouldn't call the {@link #dispose} method directly,
 * but register your object to be chained with a parent disposable via {@link Disposer#register(Disposable, Disposable)}.
 * If you're 100% sure that you should control disposing of your object manually,
 * do not call the {@link #dispose} method either. Use {@link Disposer#dispose(Disposable)} instead, since
 * there might be any object registered in chain.
 */
public interface Disposable {
    @Nonnull
    static Disposable newDisposable() {
        return newDisposable(null);
    }

    @Nonnull
    static Disposable newDisposable(@Nullable String debugName) {
        return new Disposable() {
            @Override
            public void dispose() {
            }

            @Override
            public String toString() {
                return debugName == null ? super.toString() : debugName;
            }
        };
    }

    interface Parent extends Disposable {
        void beforeTreeDispose();
    }

    void dispose();

    default void disposeWithTree() {
        Disposer.dispose(this);
    }
}
