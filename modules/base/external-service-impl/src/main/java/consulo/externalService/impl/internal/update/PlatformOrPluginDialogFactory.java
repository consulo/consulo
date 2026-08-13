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
package consulo.externalService.impl.internal.update;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-08-13
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface PlatformOrPluginDialogFactory {
    static PlatformOrPluginDialogFactory getInstance() {
        return Application.get().getInstance(PlatformOrPluginDialogFactory.class);
    }

    @RequiredUIAccess
    void showAsync(
        @Nullable Project project,
        PlatformOrPluginUpdateResult updateResult,
        @Nullable Predicate<PluginId> greenStrategy,
        @Nullable Consumer<Collection<PluginDescriptor>> afterCallback,
        boolean modalProgress
    );
}
