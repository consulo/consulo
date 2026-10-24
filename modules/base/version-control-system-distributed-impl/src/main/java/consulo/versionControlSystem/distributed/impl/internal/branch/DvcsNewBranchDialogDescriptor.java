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

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.InputProblem;
import consulo.ui.Label;
import consulo.ui.NotificationType;
import consulo.ui.TextBox;
import consulo.ui.Toast;
import consulo.ui.ToastStyle;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.FormBuilder;
import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.versionControlSystem.distributed.branch.DvcsNewBranchInput;
import consulo.versionControlSystem.distributed.branch.DvcsNewBranchOption;
import consulo.versionControlSystem.distributed.branch.DvcsNewBranchValidator;
import consulo.versionControlSystem.distributed.localize.DistributedVcsLocalize;
import consulo.versionControlSystem.distributed.repository.Repository;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
public class DvcsNewBranchDialogDescriptor<R extends Repository> extends DialogDescriptor {
    private final LocalizeValue myOkText;
    private final LocalizeValue myNameLabel;
    private final String myInitialName;
    private final UnaryOperator<String> myNameCleaner;
    private final DvcsNewBranchValidator<R> myValidator;
    private final List<R> myInitialRepositories;
    private final List<R> myAllRepositories;
    private final @Nullable LocalizeValue mySingleRepositoryWarning;
    private final List<DvcsNewBranchOption<R>> myOptions;

    private final Map<DvcsNewBranchOption<R>, CheckBox> myOptionBoxes = new LinkedHashMap<>();
    private @Nullable TextBox myNameBox;
    private @Nullable ComboBox<List<R>> myRepositoryBox;
    private @Nullable Toast myErrorToast;
    private @Nullable Toast myWarningToast;
    private @Nullable InputProblem myProblem;
    private boolean myEdited;
    private String myLastName = "";
    private boolean myErrorShown;
    private boolean myWarningShown;

    public DvcsNewBranchDialogDescriptor(LocalizeValue title,
                                         LocalizeValue okText,
                                         LocalizeValue nameLabel,
                                         String initialName,
                                         UnaryOperator<String> nameCleaner,
                                         DvcsNewBranchValidator<R> validator,
                                         List<R> initialRepositories,
                                         List<R> allRepositories,
                                         @Nullable LocalizeValue singleRepositoryWarning,
                                         List<DvcsNewBranchOption<R>> options) {
        super(title);
        myOkText = okText;
        myNameLabel = nameLabel;
        myInitialName = initialName;
        myNameCleaner = nameCleaner;
        myValidator = validator;
        myInitialRepositories = initialRepositories;
        myAllRepositories = allRepositories;
        mySingleRepositoryWarning = singleRepositoryWarning;
        myOptions = options;
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        UIAccess uiAccess = UIAccess.current();

        TextBox nameBox = TextBox.create(myInitialName);
        myNameBox = nameBox;
        myLastName = myInitialName;
        nameBox.addValueListener(event -> {
            String name = textOf(nameBox);
            if (name.equals(myLastName)) {
                return;
            }
            myLastName = name;
            uiAccess.give(() -> cleanName(nameBox));
            edited();
        });

        FormBuilder builder = FormBuilder.create();
        builder.addLabeled(myNameLabel, nameBox);

        if (myAllRepositories.size() > 1) {
            List<List<R>> items = new ArrayList<>();
            items.add(myAllRepositories);
            for (R repository : myAllRepositories) {
                items.add(List.of(repository));
            }
            ComboBox<List<R>> repositoryBox = ComboBox.create(items);
            repositoryBox.setTextRenderer(item -> {
                if (item == null) {
                    return LocalizeValue.empty();
                }
                if (item.size() == 1 && myAllRepositories.size() > 1) {
                    return LocalizeValue.of(DvcsUtil.getShortRepositoryName(item.get(0)));
                }
                return DistributedVcsLocalize.newBranchDialogBranchRootAllName();
            });
            repositoryBox.setValue(myInitialRepositories.containsAll(myAllRepositories) || myInitialRepositories.isEmpty()
                ? myAllRepositories
                : List.of(myInitialRepositories.get(0)));
            repositoryBox.addValueListener(event -> edited());
            myRepositoryBox = repositoryBox;
            builder.addLabeled(DistributedVcsLocalize.newBranchDialogBranchRootName(), repositoryBox);
        }

        if (!myOptions.isEmpty()) {
            HorizontalLayout optionsRow = HorizontalLayout.create();
            for (DvcsNewBranchOption<R> option : myOptions) {
                CheckBox checkBox = CheckBox.create(option.getText());
                checkBox.setValue(option.isSelectedByDefault());
                checkBox.addValueListener(event -> edited());
                myOptionBoxes.put(option, checkBox);
                optionsRow.add(checkBox);
            }
            builder.addLabeled(Label.create(LocalizeValue.empty()), optionsRow);
        }

        Toast errorToast = Toast.create();
        errorToast.addStyle(ToastStyle.ERROR);
        errorToast.setVisible(false);
        myErrorToast = errorToast;

        Toast warningToast = Toast.create();
        warningToast.addStyle(ToastStyle.WARNING);
        warningToast.setVisible(false);
        myWarningToast = warningToast;

        VerticalLayout root = VerticalLayout.create();
        root.add(builder.build());
        root.add(errorToast);
        root.add(warningToast);

        revalidate();
        return root;
    }

    @RequiredUIAccess
    @Override
    public Component getPreferredFocusedComponent() {
        TextBox nameBox = myNameBox;
        if (nameBox != null) {
            nameBox.selectAll();
        }
        return nameBox;
    }

    @Override
    public boolean hasBorderAtButtonLayout() {
        return false;
    }

    @Override
    protected DialogOkAction createOkAction() {
        return new DialogOkAction(myOkText);
    }

    @Override
    public boolean doUpdateOkButtonState() {
        InputProblem problem = myProblem;
        return problem == null || !problem.blocksConfirm();
    }

    @RequiredUIAccess
    @Override
    public boolean canHandle(AnAction action, @Nullable DialogValue value, Window window) {
        if (!(action instanceof DialogOkAction)) {
            return true;
        }
        InputProblem problem = myValidator.validate(getInput(), true);
        myEdited = true;
        showProblem(problem);
        updateOkButtonState();
        return problem == null || !problem.blocksConfirm();
    }

    @RequiredUIAccess
    private void cleanName(TextBox nameBox) {
        String text = textOf(nameBox);
        String cleaned = myNameCleaner.apply(text);
        if (!cleaned.equals(text)) {
            nameBox.setValue(cleaned);
            nameBox.moveCaretTo(cleaned.length());
        }
    }

    @RequiredUIAccess
    private void edited() {
        myEdited = true;
        revalidate();
    }

    @RequiredUIAccess
    private void revalidate() {
        DvcsNewBranchInput<R> input = getInput();
        for (Map.Entry<DvcsNewBranchOption<R>, CheckBox> entry : myOptionBoxes.entrySet()) {
            entry.getValue().setEnabled(entry.getKey().isEnabled(input));
        }
        showProblem(myEdited ? myValidator.validate(input, false) : null);
        updateOkButtonState();
    }

    @RequiredUIAccess
    private void showProblem(@Nullable InputProblem problem) {
        myProblem = problem;

        Toast errorToast = myErrorToast;
        Toast warningToast = myWarningToast;
        if (errorToast == null || warningToast == null) {
            return;
        }

        LocalizeValue warning = LocalizeValue.empty();
        LocalizeValue error = LocalizeValue.empty();
        if (problem != null) {
            if (problem.type() == NotificationType.WARNING) {
                warning = problem.message();
            }
            else {
                error = problem.message();
            }
        }
        if (warning.isEmpty() && isSingleRepositoryWarningVisible()) {
            warning = mySingleRepositoryWarning;
        }

        boolean resize = myErrorShown != error.isNotEmpty() || myWarningShown != warning.isNotEmpty();
        myErrorShown = error.isNotEmpty();
        myWarningShown = warning.isNotEmpty();
        errorToast.setHtmlText(error);
        errorToast.setVisible(myErrorShown);
        warningToast.setHtmlText(warning);
        warningToast.setVisible(myWarningShown);
        if (resize) {
            updateSize();
        }
    }

    private boolean isSingleRepositoryWarningVisible() {
        ComboBox<List<R>> repositoryBox = myRepositoryBox;
        if (mySingleRepositoryWarning == null || repositoryBox == null) {
            return false;
        }
        List<R> selected = repositoryBox.getValue();
        return selected != null && selected.size() == 1 && myInitialRepositories.containsAll(myAllRepositories);
    }

    private static String textOf(TextBox nameBox) {
        String text = nameBox.getValue();
        return text == null ? "" : text;
    }

    public DvcsNewBranchInput<R> getInput() {
        TextBox nameBox = myNameBox;
        String name = nameBox == null ? myInitialName : textOf(nameBox);

        ComboBox<List<R>> repositoryBox = myRepositoryBox;
        List<R> repositories = repositoryBox == null || repositoryBox.getValue() == null ? myInitialRepositories : repositoryBox.getValue();

        Set<DvcsNewBranchOption<R>> selected = new LinkedHashSet<>();
        for (Map.Entry<DvcsNewBranchOption<R>, CheckBox> entry : myOptionBoxes.entrySet()) {
            if (entry.getValue().getValueOrError()) {
                selected.add(entry.getKey());
            }
        }
        if (myOptionBoxes.isEmpty()) {
            for (DvcsNewBranchOption<R> option : myOptions) {
                if (option.isSelectedByDefault()) {
                    selected.add(option);
                }
            }
        }
        return new DvcsNewBranchInputImpl<>(name, repositories, selected);
    }
}
