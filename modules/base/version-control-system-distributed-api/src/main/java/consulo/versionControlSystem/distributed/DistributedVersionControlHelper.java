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
package consulo.versionControlSystem.distributed;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.ui.awt.LegacyDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2025-09-07
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface DistributedVersionControlHelper {
    @Nonnull
    LegacyDialog createPushDialog(@Nonnull Project project,
                                  @Nonnull List<? extends Repository> selectedRepositories,
                                  @Nullable Repository currentRepo);
}
