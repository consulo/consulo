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
package consulo.execution.impl.internal.service;

import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * The layout of a unified services view - the counterpart of the swing {@link ServiceViewUi}.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
interface UnifiedServiceViewUi {
    Component getComponent();

    void saveState(ServiceViewState state);

    @RequiredUIAccess
    void setServiceToolbar(UnifiedServiceViewActionProvider actionProvider);

    @RequiredUIAccess
    void setMasterComponent(Component component, UnifiedServiceViewActionProvider actionProvider);

    @RequiredUIAccess
    void setDetailsComponent(@Nullable Component component);

    @RequiredUIAccess
    void setMasterComponentVisible(boolean visible);

    @Nullable Component getDetailsComponent();
}
