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
package consulo.versionControlSystem.impl.internal.change.ui;

import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.dialog.DialogService;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsShowConfirmationOption;
import consulo.versionControlSystem.ui.SelectFilesDialogBuilder;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class UnifiedSelectFilesDialogBuilder<T> implements SelectFilesDialogBuilder<T> {
    private final Project myProject;
    private final List<T> myItems;
    private final Function<T, FilePath> myToPath;
    private final Predicate<T> myValid;
    private final @Nullable Supplier<DeleteProvider> myDeleteProviderFactory;

    private LocalizeValue myTitle = LocalizeValue.empty();
    private LocalizeValue myPrompt = LocalizeValue.empty();
    private LocalizeValue myOkText = CommonLocalize.buttonOk();
    private LocalizeValue myCancelText = CommonLocalize.buttonCancel();
    private boolean mySelectable;
    private boolean myDeletable;
    private boolean myOkOnly;
    private @Nullable VcsShowConfirmationOption myConfirmationOption;

    public UnifiedSelectFilesDialogBuilder(Project project,
                                           List<T> items,
                                           Function<T, FilePath> toPath,
                                           Predicate<T> valid,
                                           @Nullable Supplier<DeleteProvider> deleteProviderFactory) {
        myProject = project;
        myItems = List.copyOf(items);
        myToPath = toPath;
        myValid = valid;
        myDeleteProviderFactory = deleteProviderFactory;
    }

    @Override
    public SelectFilesDialogBuilder<T> title(LocalizeValue title) {
        myTitle = title;
        return this;
    }

    @Override
    public SelectFilesDialogBuilder<T> prompt(LocalizeValue prompt) {
        myPrompt = prompt;
        return this;
    }

    @Override
    public SelectFilesDialogBuilder<T> selectable() {
        mySelectable = true;
        return this;
    }

    @Override
    public SelectFilesDialogBuilder<T> deletable() {
        myDeletable = true;
        return this;
    }

    @Override
    public SelectFilesDialogBuilder<T> okText(LocalizeValue okText) {
        myOkText = okText;
        return this;
    }

    @Override
    public SelectFilesDialogBuilder<T> cancelText(LocalizeValue cancelText) {
        myCancelText = cancelText;
        return this;
    }

    @Override
    public SelectFilesDialogBuilder<T> okOnly() {
        myOkOnly = true;
        return this;
    }

    @Override
    public SelectFilesDialogBuilder<T> confirmationOption(VcsShowConfirmationOption option) {
        myConfirmationOption = option;
        return this;
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<Optional<List<T>>> showAsync() {
        Supplier<DeleteProvider> deleteProviderFactory = myDeleteProviderFactory;
        DeleteProvider deleteProvider = myDeletable && deleteProviderFactory != null ? deleteProviderFactory.get() : null;

        UnifiedSelectFilesDialog<T> descriptor = new UnifiedSelectFilesDialog<>(
            myTitle,
            myProject,
            myItems,
            myToPath,
            myValid,
            mySelectable,
            deleteProvider,
            myPrompt,
            myOkText,
            myCancelText,
            myOkOnly,
            myConfirmationOption
        );

        return myProject.getApplication().getInstance(DialogService.class).build(descriptor).showAsync().handle((value, error) -> {
            List<T> selection = descriptor.getSelection();
            return error == null && selection != null ? Optional.of(selection) : Optional.empty();
        });
    }
}
