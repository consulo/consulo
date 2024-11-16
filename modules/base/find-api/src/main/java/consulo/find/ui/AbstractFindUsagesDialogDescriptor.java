/*
 * Copyright 2013-2024 consulo.io
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
package consulo.find.ui;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.find.FindSettings;
import consulo.find.FindUsagesOptions;
import consulo.find.PersistentFindUsagesOptions;
import consulo.find.localize.FindLocalize;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.AdvancedLabel;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.TextItemPresentation;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.StateRestoringCheckBoxWrapper;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.usage.UsageViewContentManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2024-11-16
 */
public abstract class AbstractFindUsagesDialogDescriptor extends DialogDescriptor {
    private final Project myProject;
    protected final FindUsagesOptions myFindUsagesOptions;

    private final boolean myToShowInNewTab;
    private final boolean myIsShowInNewTabEnabled;
    private final boolean myIsShowInNewTabVisible;

    private final boolean mySearchForTextOccurrencesAvailable;

    private final boolean mySearchInLibrariesAvailable;

    private CheckBox myCbToOpenInNewTab;

    protected StateRestoringCheckBoxWrapper myCbToSearchForTextOccurrences;
    protected StateRestoringCheckBoxWrapper myCbToSkipResultsWhenOneUsage;

    private ScopeChooserCombo myScopeCombo;

    private final VerticalLayout myOptionsLayout = VerticalLayout.create();

    @RequiredUIAccess
    public AbstractFindUsagesDialogDescriptor(@Nonnull Project project,
                                              @Nonnull FindUsagesOptions findUsagesOptions,
                                              boolean toShowInNewTab,
                                              boolean mustOpenInNewTab,
                                              boolean isSingleFile,
                                              boolean searchForTextOccurrencesAvailable,
                                              boolean searchInLibrariesAvailable) {
        super(isSingleFile ? FindLocalize.findUsagesInFileDialogTitle() : FindLocalize.findUsagesDialogTitle());

        myProject = project;
        myFindUsagesOptions = findUsagesOptions;
        myToShowInNewTab = toShowInNewTab;
        myIsShowInNewTabEnabled = !mustOpenInNewTab && UsageViewContentManager.getInstance(myProject).getReusableContentsCount() > 0;
        myIsShowInNewTabVisible = !isSingleFile;
        mySearchForTextOccurrencesAvailable = searchForTextOccurrencesAvailable;
        mySearchInLibrariesAvailable = searchInLibrariesAvailable;

        if (myFindUsagesOptions instanceof PersistentFindUsagesOptions) {
            ((PersistentFindUsagesOptions) myFindUsagesOptions).setDefaults(myProject);
        }
    }

    public final FindUsagesOptions calcFindUsagesOptions() {
        calcFindUsagesOptions(myFindUsagesOptions);
        if (myFindUsagesOptions instanceof PersistentFindUsagesOptions) {
            ((PersistentFindUsagesOptions) myFindUsagesOptions).storeDefaults(myProject);
        }
        return myFindUsagesOptions;
    }

    public void calcFindUsagesOptions(FindUsagesOptions options) {
        options.searchScope =
            myScopeCombo == null || myScopeCombo.getSelectedScope() == null ? GlobalSearchScope.allScope(myProject) : myScopeCombo.getSelectedScope();
        options.isSearchForTextOccurrences = isToChange(myCbToSearchForTextOccurrences) && isSelected(myCbToSearchForTextOccurrences);
    }

    @RequiredUIAccess
    protected void addTextFindUsageOptions() {
        if (mySearchForTextOccurrencesAvailable) {
            myCbToSearchForTextOccurrences = addCheckbox(
                FindLocalize.findOptionsSearchForTextOccurrencesCheckbox(),
                myFindUsagesOptions.isSearchForTextOccurrences,
                false
            );
        }

        if (myIsShowInNewTabVisible) {
            myCbToSkipResultsWhenOneUsage = addCheckbox(
                FindLocalize.findOptionsSkipResultsTabWithOneUsageCheckbox(),
                FindSettings.getInstance().isSkipResultsWithOneUsage(),
                false
            );
        }
    }

    @RequiredUIAccess
    protected StateRestoringCheckBoxWrapper addCheckbox(LocalizeValue textValue, boolean toSelect,  boolean toUpdate) {
        StateRestoringCheckBoxWrapper cb = new StateRestoringCheckBoxWrapper(textValue);
        cb.setValue(toSelect);

        myOptionsLayout.add(cb.getComponent());

        if (toUpdate) {
            cb.addValueListener(event -> update());
        }
        return cb;
    }

    @Nullable
    @RequiredUIAccess
    private Component createSearchScopePanel(@Nonnull Disposable uiDisposable) {
        if (isInFileOnly()) {
            return null;
        }
        String scope = myFindUsagesOptions.searchScope.getDisplayName();
        myScopeCombo = new ScopeChooserCombo(myProject, mySearchInLibrariesAvailable, true, scope);
        Disposer.register(uiDisposable, myScopeCombo);

        return LabeledLayout.create(FindLocalize.findScopeLabel(), TargetAWT.wrap(myScopeCombo)); // TODO [VISTALL] remove wrap after rewrite ScopeCombo to new UI
    }

    @RequiredUIAccess
    protected abstract void configureLabelComponent(@Nonnull TextItemPresentation presentation, @Nonnull Disposable uiDisposable);

    protected static boolean isToChange(StateRestoringCheckBoxWrapper cb) {
        return cb != null && cb.getParentComponent() != null;
    }

    protected static boolean isSelected(StateRestoringCheckBoxWrapper cb) {
        return cb != null && cb.getParentComponent() != null && cb.getValue();
    }

    @RequiredUIAccess
    public boolean isShowInSeparateWindow() {
        return myCbToOpenInNewTab != null && myCbToOpenInNewTab.getValueOrError();
    }

    protected void update() {
    }

    @Override
    public boolean canHandle(@Nonnull AnAction action, @Nullable DialogValue value) {
        if (value == DialogValue.OK_VALUE) {
            return myScopeCombo == null || myScopeCombo.getSelectedScope() != null;
        }
        return true;
    }

    protected boolean isInFileOnly() {
        return !myIsShowInNewTabVisible;
    }

    @Override
    public boolean hasBorderAtButtonLayout() {
        return false;
    }

    @Nonnull
    @Override
    protected DialogOkAction createOkAction() {
        return new DialogOkAction(FindLocalize.findDialogFindButton());
    }

    @Nullable
    protected Component getPreferredFocusedControl() {
        return null;
    }

    @Override
    @RequiredUIAccess
    public Component getPreferredFocusedComponent() {
        if (myScopeCombo != null) {
            return TargetAWT.wrap(myScopeCombo.getComboBox());    // TODO [VISTALL] remove wrap after rewrite ScopeCombo to new UI
        }
        return getPreferredFocusedControl();
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public Component createCenterComponent(@Nonnull Disposable uiDisposable) {
        VerticalLayout layout = VerticalLayout.create();

        AdvancedLabel label = AdvancedLabel.create();

        label.updatePresentation(presentation -> configureLabelComponent(presentation, uiDisposable));

        layout.add(label);

        layout.add(LabeledLayout.create(FindLocalize.findWhatGroup(), myOptionsLayout));

        addTextFindUsageOptions();

        Component scopePanel = createSearchScopePanel(uiDisposable);
        if (scopePanel != null) {
            layout.add(scopePanel);
        }

        if (myIsShowInNewTabVisible) {
            myCbToOpenInNewTab = CheckBox.create(FindLocalize.findOpenInNewTabCheckbox());
            myCbToOpenInNewTab.setValue(myToShowInNewTab);
            myCbToOpenInNewTab.setEnabled(myIsShowInNewTabEnabled);
            
            layout.add(myCbToOpenInNewTab);
        }

        return layout;
    }
}
