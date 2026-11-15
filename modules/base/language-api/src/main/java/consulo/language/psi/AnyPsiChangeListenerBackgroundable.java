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
package consulo.language.psi;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.annotation.component.TopicBroadcastDirection;

/**
 * Same as {@link AnyPsiChangeListener}, but runs in place, on the thread that changes the PSI.
 * <p>
 * A physical change runs under the write lock, and the thread can be a background thread.
 * A non-physical change can run on any thread.
 * {@link #beforePsiChanged} and {@link #afterPsiChanged} can run on different threads, so keep no thread-affine state.
 * <p>
 * A subscriber of the legacy {@link AnyPsiChangeListener} can be handed a physical change on the UI thread, which costs the
 * changing thread a wait for the UI thread.
 */
@TopicAPI(value = ComponentScope.PROJECT, direction = TopicBroadcastDirection.TO_PARENT)
public interface AnyPsiChangeListenerBackgroundable extends AnyPsiChangeListener {
    @Override
    default void beforePsiChanged(boolean isPhysical) {
    }

    @Override
    default void afterPsiChanged(boolean isPhysical) {
    }
}
