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
package consulo.versionControlSystem.distributed.branch;

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.distributed.repository.Repository;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
public interface DvcsNewBranchDialogBuilder<R extends Repository> {
    DvcsNewBranchDialogBuilder<R> title(LocalizeValue title);

    DvcsNewBranchDialogBuilder<R> okText(LocalizeValue okText);

    DvcsNewBranchDialogBuilder<R> nameLabel(LocalizeValue nameLabel);

    DvcsNewBranchDialogBuilder<R> initialName(@Nullable String initialName);

    DvcsNewBranchDialogBuilder<R> nameCleaner(UnaryOperator<String> nameCleaner);

    DvcsNewBranchDialogBuilder<R> validator(DvcsNewBranchValidator<R> validator);

    DvcsNewBranchDialogBuilder<R> repositoryChooser(Collection<R> allRepositories, @Nullable LocalizeValue singleRepositoryWarning);

    DvcsNewBranchDialogBuilder<R> option(DvcsNewBranchOption<R> option);

    @RequiredUIAccess
    CompletableFuture<@Nullable DvcsNewBranchInput<R>> showAsync();
}
