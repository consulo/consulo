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
package consulo.versionControlSystem.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.VcsShowConfirmationOption;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * A dialog listing files, optionally with checkboxes to pick some of them.
 *
 * @author VISTALL
 * @since 2026-10-17
 */
public interface SelectFilesDialogBuilder<T> {
    SelectFilesDialogBuilder<T> title(LocalizeValue title);

    SelectFilesDialogBuilder<T> prompt(LocalizeValue prompt);

    /**
     * Shows a checkbox for every file - all checked at first. Without it the dialog only lists the files.
     */
    SelectFilesDialogBuilder<T> selectable();

    /**
     * Lets the user delete the selected files from the disk. Applies to lists of virtual files only.
     */
    SelectFilesDialogBuilder<T> deletable();

    SelectFilesDialogBuilder<T> okText(LocalizeValue okText);

    SelectFilesDialogBuilder<T> cancelText(LocalizeValue cancelText);

    /**
     * Leaves out the cancel button.
     */
    SelectFilesDialogBuilder<T> okOnly();

    /**
     * Shows a "do not ask again" checkbox which stores its answer in the option.
     */
    SelectFilesDialogBuilder<T> confirmationOption(VcsShowConfirmationOption option);

    /**
     * @return the files checked when the user confirmed (every listed file when the dialog is not selectable),
     * or an empty optional when the user cancelled
     */
    @RequiredUIAccess
    CompletableFuture<Optional<List<T>>> showAsync();
}
