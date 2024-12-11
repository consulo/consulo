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
package consulo.ide.impl.base;

import consulo.annotation.component.ServiceImpl;
import consulo.configurable.Configurable;
import consulo.configurable.UnnamedConfigurable;
import consulo.configurable.internal.ShowConfigurableService;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 02/02/2023
 */
@Singleton
@ServiceImpl
public class ShowConfigurableServiceImpl implements ShowConfigurableService {
    private final ShowSettingsUtil myShowSettingsUtil;

    @Inject
    public ShowConfigurableServiceImpl(ShowSettingsUtil showSettingsUtil) {
        myShowSettingsUtil = showSettingsUtil;
    }

    @RequiredUIAccess
    @Override
    public <T extends UnnamedConfigurable> void showAndSelect(@Nullable Project project,
                                                              @Nonnull Class<T> toSelect,
                                                              @Nonnull Consumer<T> afterSelect) {
        myShowSettingsUtil.showAndSelect(project, toSelect, afterSelect);
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<?> show(@Nullable Project project, @Nonnull Class<? extends UnnamedConfigurable> toSelect) {
        CompletableFuture<?> future = new CompletableFuture<>();
        myShowSettingsUtil.showAndSelect(project, toSelect).doWhenDone(() -> future.complete(null));
        return future;
    }

    @RequiredUIAccess
    @Override
    public AsyncResult<Void> editConfigurable(Component parent, Configurable configurable) {
        return myShowSettingsUtil.editConfigurable(parent, configurable);
    }
}
