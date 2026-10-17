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
package consulo.versionControlSystem.ui;

import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public interface VcsFileListBuilder<T> {
    /**
     * Shows a checkbox for every file - all checked at first.
     */
    VcsFileListBuilder<T> selectable();

    /**
     * Shows a toolbar with expand / collapse and group by directory above the list.
     */
    VcsFileListBuilder<T> toolbar();

    VcsFileListBuilder<T> inclusionListener(Runnable listener);

    /**
     * @param parentDisposable owns the list - the list is released with it
     */
    @RequiredUIAccess
    VcsFileList<T> build(Disposable parentDisposable);
}
