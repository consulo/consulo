/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.versionControlSystem.change.diff;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.progress.ProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.component.extension.ExtensionPointName;
import consulo.diff.chain.DiffRequestProducerException;
import consulo.diff.request.DiffRequest;
import consulo.project.Project;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.lang.ThreeState;
import consulo.versionControlSystem.change.Change;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface ChangeDiffRequestProvider {
    ExtensionPointName<ChangeDiffRequestProvider> EP_NAME = ExtensionPointName.create(ChangeDiffRequestProvider.class);

    @Nonnull
    ThreeState isEquals(@Nonnull Change change1, @Nonnull Change change2);

    boolean canCreate(@Nullable Project project, @Nonnull Change change);

    @Nonnull
    DiffRequest process(
        @Nonnull ChangeDiffRequestProducer presentable,
        @Nonnull UserDataHolder context,
        @Nonnull ProgressIndicator indicator
    ) throws DiffRequestProducerException, ProcessCanceledException;
}
