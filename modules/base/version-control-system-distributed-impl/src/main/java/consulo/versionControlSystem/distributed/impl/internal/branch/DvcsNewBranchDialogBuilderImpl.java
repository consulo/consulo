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
package consulo.versionControlSystem.distributed.impl.internal.branch;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogService;
import consulo.versionControlSystem.distributed.branch.DvcsNewBranchDialogBuilder;
import consulo.versionControlSystem.distributed.branch.DvcsNewBranchInput;
import consulo.versionControlSystem.distributed.branch.DvcsNewBranchOption;
import consulo.versionControlSystem.distributed.branch.DvcsNewBranchValidator;
import consulo.versionControlSystem.distributed.localize.DistributedVcsLocalize;
import consulo.versionControlSystem.distributed.repository.Repository;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
public class DvcsNewBranchDialogBuilderImpl<R extends Repository> implements DvcsNewBranchDialogBuilder<R> {
    private final Project myProject;
    private final List<R> myRepositories;

    private LocalizeValue myTitle = LocalizeValue.empty();
    private LocalizeValue myOkText = DistributedVcsLocalize.newBranchDialogOperationCreateName();
    private LocalizeValue myNameLabel = DistributedVcsLocalize.newBranchDialogBranchName();
    private @Nullable String myInitialName;
    private UnaryOperator<String> myNameCleaner = UnaryOperator.identity();
    private DvcsNewBranchValidator<R> myValidator = (input, onApply) -> null;
    private List<R> myAllRepositories = List.of();
    private @Nullable LocalizeValue mySingleRepositoryWarning;
    private final List<DvcsNewBranchOption<R>> myOptions = new ArrayList<>();

    public DvcsNewBranchDialogBuilderImpl(Project project, Collection<R> repositories) {
        myProject = project;
        myRepositories = List.copyOf(repositories);
    }

    @Override
    public DvcsNewBranchDialogBuilder<R> title(LocalizeValue title) {
        myTitle = title;
        return this;
    }

    @Override
    public DvcsNewBranchDialogBuilder<R> okText(LocalizeValue okText) {
        myOkText = okText;
        return this;
    }

    @Override
    public DvcsNewBranchDialogBuilder<R> nameLabel(LocalizeValue nameLabel) {
        myNameLabel = nameLabel;
        return this;
    }

    @Override
    public DvcsNewBranchDialogBuilder<R> initialName(@Nullable String initialName) {
        myInitialName = initialName;
        return this;
    }

    @Override
    public DvcsNewBranchDialogBuilder<R> nameCleaner(UnaryOperator<String> nameCleaner) {
        myNameCleaner = nameCleaner;
        return this;
    }

    @Override
    public DvcsNewBranchDialogBuilder<R> validator(DvcsNewBranchValidator<R> validator) {
        myValidator = validator;
        return this;
    }

    @Override
    public DvcsNewBranchDialogBuilder<R> repositoryChooser(Collection<R> allRepositories, @Nullable LocalizeValue singleRepositoryWarning) {
        myAllRepositories = List.copyOf(allRepositories);
        mySingleRepositoryWarning = singleRepositoryWarning;
        return this;
    }

    @Override
    public DvcsNewBranchDialogBuilder<R> option(DvcsNewBranchOption<R> option) {
        myOptions.add(option);
        return this;
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<@Nullable DvcsNewBranchInput<R>> showAsync() {
        DvcsNewBranchDialogDescriptor<R> descriptor = new DvcsNewBranchDialogDescriptor<>(
            myTitle,
            myOkText,
            myNameLabel,
            myInitialName == null ? "" : myInitialName,
            myNameCleaner,
            myValidator,
            myRepositories,
            myAllRepositories,
            mySingleRepositoryWarning,
            List.copyOf(myOptions)
        );
        Dialog dialog = myProject.getApplication().getInstance(DialogService.class).build(myProject, descriptor);
        return dialog.showAsync().handle((value, error) -> error == null && value != null ? descriptor.getInput() : null);
    }
}
