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
package consulo.versionControlSystem.distributed.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import consulo.versionControlSystem.distributed.DistributedVersionControlHelper;
import consulo.versionControlSystem.distributed.impl.internal.push.VcsPushDialog;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.ui.awt.LegacyDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * @author VISTALL
 * @since 2025-09-07
 */
@Singleton
@ServiceImpl
public class DistributedVersionControlHelperImpl implements DistributedVersionControlHelper {
    @Nonnull
    @Override
    public LegacyDialog createPushDialog(@Nonnull Project project, @Nonnull List<? extends Repository> selectedRepositories, @Nullable Repository currentRepo) {
        return new VcsPushDialog(project, selectedRepositories, currentRepo);
    }
}
