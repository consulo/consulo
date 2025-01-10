/*
 * Copyright 2013-2023 consulo.io
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
package consulo.configurable.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.configurable.Configurable;
import consulo.configurable.UnnamedConfigurable;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 02/02/2023
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ShowConfigurableService {
    @RequiredUIAccess
    default <T extends UnnamedConfigurable> CompletableFuture<?> showAndSelect(@Nullable Project project, @Nonnull Class<T> toSelect) {
        return showAndSelect(project, toSelect, o -> {
        });
    }

    @RequiredUIAccess
    <T extends UnnamedConfigurable> CompletableFuture<?> showAndSelect(@Nullable Project project,
                                                       @Nonnull Class<T> toSelect,
                                                       @Nonnull Consumer<T> afterSelect);

    @RequiredUIAccess
    CompletableFuture<?> show(@Nullable Project project, @Nonnull Class<? extends UnnamedConfigurable> toSelect);

    @RequiredUIAccess
    @Deprecated
    AsyncResult<Void> editConfigurable(Component parent, Configurable configurable);

    @RequiredUIAccess
    @Deprecated
    AsyncResult<Void> editConfigurable(Project project, Configurable configurable);
}
