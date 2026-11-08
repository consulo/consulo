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
package consulo.repository.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * A fix of a {@link PackageManagementProblem} - installing the packaging tools of an interpreter.
 */
public interface PackageManagementFix {
    LocalizeValue getName();

    /**
     * Runs the fix. Called in the UI thread; needs to take care of spawning a background task itself and to publish
     * its progress to the listener.
     */
    @RequiredUIAccess
    void run(PackageManagementService.Listener listener);
}
