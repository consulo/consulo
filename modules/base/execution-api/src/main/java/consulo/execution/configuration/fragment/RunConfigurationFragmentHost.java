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
package consulo.execution.configuration.fragment;

import consulo.ui.annotation.RequiredUIAccess;

import java.util.function.Consumer;

/**
 * The page holding the fragment editors of one run configuration, given to every editor by
 * {@link RunConfigurationFragmentEditor#attach}. Lets a fragment react to and drive the other fragments of the page by their
 * {@link RunConfigurationFragment#getId() ids} (e.g. a test kind disabling the module selector).
 */
public interface RunConfigurationFragmentHost {
    /**
     * Enables or disables the editor of a fragment, see {@link RunConfigurationFragmentEditor#setEnabled}. Remembered for an editor
     * created later (a hidden fragment gets its editor when it is shown); an unknown id is ignored.
     */
    @RequiredUIAccess
    void setFragmentEnabled(String fragmentId, boolean enabled);

    @RequiredUIAccess
    boolean isFragmentEnabled(String fragmentId);

    /**
     * Whether the fragment is shown on the page; {@code false} for an unknown or not applicable fragment.
     */
    @RequiredUIAccess
    boolean isFragmentVisible(String fragmentId);

    /**
     * Shows or hides a removable fragment, as "Modify options" does.
     */
    @RequiredUIAccess
    void setFragmentVisible(String fragmentId, boolean visible);

    /**
     * Called with the new visibility whenever the fragment is shown or hidden.
     */
    @RequiredUIAccess
    void addVisibilityListener(String fragmentId, Consumer<Boolean> listener);
}
