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
package consulo.versionControlSystem.internal;

import java.util.EventListener;

/**
 * Listener for {@link LineStatusTrackerI} state changes.
 *
 * @see LineStatusTrackerI#addListener(LineStatusTrackerListener)
 */
public interface LineStatusTrackerListener extends EventListener {
    /**
     * Fired when the tracker transitions from non-valid to valid state
     * (i.e. the first time ranges are available after initialization or unfreeze).
     */
    default void onBecomingValid() {}

    /**
     * Fired whenever the set of changed-line ranges is rebuilt.
     * Called from EDT after every {@code reinstallRanges()} cycle.
     */
    default void onRangesChanged() {}
}
