/*
 * Copyright 2013-2024 consulo.io
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

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.service.BaseServiceToolWindowFactory;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2024-05-12
 */
@ExtensionImpl
public class ServiceViewToolWindowFactory extends BaseServiceToolWindowFactory {
    @Nonnull
    @Override
    public String getId() {
        return ToolWindowId.SERVICES;
    }

    @Nonnull
    @Override
    public ToolWindowAnchor getAnchor() {
        return ToolWindowAnchor.BOTTOM;
    }

    @Nonnull
    @Override
    public Image getIcon() {
        return PlatformIconGroup.toolwindowsToolwindowservices();
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return ExecutionLocalize.toolwindowServicesDisplayName();
    }
}
