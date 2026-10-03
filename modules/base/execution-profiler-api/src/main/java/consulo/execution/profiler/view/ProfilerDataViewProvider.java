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
package consulo.execution.profiler.view;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.disposer.Disposable;
import consulo.execution.profiler.ProfilerData;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * Adds a view for profiler data the generic views do not cover, such as a system trace. Each applicable provider becomes one
 * tab next to the built-in views. Implementations get their project injected.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface ProfilerDataViewProvider {
    boolean isApplicable(ProfilerData data);

    /**
     * @return the tab title
     */
    LocalizeValue getName();

    /**
     * Builds the view from data that is already parsed: it reads no PSI and no files.
     *
     * @param parentDisposable disposed when the view closes
     */
    @RequiredUIAccess
    Component createView(ProfilerData data, Disposable parentDisposable);
}
