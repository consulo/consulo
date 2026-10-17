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

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.ex.dialog.action.DialogCancelAction;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.ui.layout.DockLayout;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsShowConfirmationOption;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class UnifiedSelectFilesDialog<T> extends DialogDescriptor {
    private final Project myProject;
    private final List<T> myItems;
    private final Function<T, FilePath> myToPath;
    private final Predicate<T> myValid;
    private final boolean mySelectable;
    private final @Nullable DeleteProvider myDeleteProvider;
    private final LocalizeValue myPrompt;
    private final LocalizeValue myOkText;
    private final LocalizeValue myCancelText;
    private final boolean myOkOnly;
    private final @Nullable VcsShowConfirmationOption myConfirmationOption;

    private @Nullable UnifiedVcsFileList<T> myList;
    private @Nullable CheckBox myDoNotAsk;
    private @Nullable List<T> mySelection;

    public UnifiedSelectFilesDialog(LocalizeValue title,
                                    Project project,
                                    List<T> items,
                                    Function<T, FilePath> toPath,
                                    Predicate<T> valid,
                                    boolean selectable,
                                    @Nullable DeleteProvider deleteProvider,
                                    LocalizeValue prompt,
                                    LocalizeValue okText,
                                    LocalizeValue cancelText,
                                    boolean okOnly,
                                    @Nullable VcsShowConfirmationOption confirmationOption) {
        super(title);
        myProject = project;
        myItems = items;
        myToPath = toPath;
        myValid = valid;
        mySelectable = selectable;
        myDeleteProvider = deleteProvider;
        myPrompt = prompt;
        myOkText = okText;
        myCancelText = cancelText;
        myOkOnly = okOnly;
        myConfirmationOption = confirmationOption;
    }

    public @Nullable List<T> getSelection() {
        return mySelection;
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        UnifiedVcsFileList<T> list =
            new UnifiedVcsFileList<>(myProject, myItems, myToPath, myValid, mySelectable, true, myDeleteProvider, null, uiDisposable);
        myList = list;

        DockLayout root = DockLayout.create();
        if (myPrompt.isNotEmpty()) {
            root.top(Label.create(myPrompt));
        }
        root.center(list.getComponent());
        if (myConfirmationOption != null) {
            CheckBox doNotAsk = CheckBox.create(CommonLocalize.dialogOptionsDoNotAsk());
            myDoNotAsk = doNotAsk;
            root.bottom(doNotAsk);
        }
        return root;
    }

    @Override
    protected DialogOkAction createOkAction() {
        return new DialogOkAction(myOkText);
    }

    @Override
    public AnAction[] createActions(boolean inverseOrder) {
        if (myOkOnly) {
            return new AnAction[]{createOkAction()};
        }

        AnAction cancel = new DialogCancelAction();
        cancel.getTemplatePresentation().setText(myCancelText);
        return inverseOrder ? new AnAction[]{cancel, createOkAction()} : new AnAction[]{createOkAction(), cancel};
    }

    @RequiredUIAccess
    @Override
    public boolean canHandle(AnAction action, @Nullable DialogValue value) {
        boolean ok = action instanceof DialogOkAction;
        if (ok) {
            UnifiedVcsFileList<T> list = myList;
            mySelection = list == null ? List.of() : list.getIncluded();
        }

        CheckBox doNotAsk = myDoNotAsk;
        VcsShowConfirmationOption option = myConfirmationOption;
        if (doNotAsk != null && option != null && doNotAsk.getValueOrError()) {
            option.setValue(ok ? VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY : VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY);
        }
        return true;
    }

    @Override
    public @Nullable WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofFont(60, 25);
    }

    @Override
    public @Nullable String getDimensionServiceKey() {
        return "UnifiedSelectFilesDialog";
    }
}
