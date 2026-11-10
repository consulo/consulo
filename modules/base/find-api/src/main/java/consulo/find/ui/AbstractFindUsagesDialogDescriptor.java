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

import consulo.content.scope.ScopeDescriptor;
import consulo.content.scope.SearchScope;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.find.FindSettings;
import consulo.find.FindUsagesOptions;
import consulo.find.PersistentFindUsagesOptions;
import consulo.find.internal.FindApiInternal;
import consulo.find.localize.FindLocalize;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.AdvancedLabel;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.TextItemPresentation;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.StateRestoringCheckBoxWrapper;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.usage.UsageViewContentManager;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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

    private @Nullable CheckBox myCbToOpenInNewTab;

    protected @Nullable StateRestoringCheckBoxWrapper myCbToSearchForTextOccurrences;
    protected @Nullable StateRestoringCheckBoxWrapper myCbToSkipResultsWhenOneUsage;

    private @Nullable ComboBox<ScopeDescriptor> myScopeCombo;
    private @Nullable MutableFlatDataModel<ScopeDescriptor> myScopeModel;

    private final VerticalLayout myFindWhatLayout = VerticalLayout.create();
    private final VerticalLayout myOptionsLayout = VerticalLayout.create();
    private boolean myHasFindWhatOptions;
    private boolean myHasUsagesOptions;
    private boolean myComponentsCreated;

    @RequiredUIAccess
    public AbstractFindUsagesDialogDescriptor(
        Project project,
        FindUsagesOptions findUsagesOptions,
        boolean toShowInNewTab,
        boolean mustOpenInNewTab,
        boolean isSingleFile,
        boolean searchForTextOccurrencesAvailable,
        boolean searchInLibrariesAvailable
    ) {
        super(isSingleFile ? FindLocalize.findUsagesInFileDialogTitle() : FindLocalize.findUsagesDialogTitle());

        myProject = project;
        myFindUsagesOptions = findUsagesOptions;
        myToShowInNewTab = toShowInNewTab;
        myIsShowInNewTabEnabled = !mustOpenInNewTab && UsageViewContentManager.getInstance(myProject).getReusableContentsCount() > 0;
        myIsShowInNewTabVisible = !isSingleFile;
        mySearchForTextOccurrencesAvailable = searchForTextOccurrencesAvailable;
        mySearchInLibrariesAvailable = searchInLibrariesAvailable;

        if (myFindUsagesOptions instanceof PersistentFindUsagesOptions persistentOptions) {
            persistentOptions.setDefaults(myProject);
        }
    }

    public Project getProject() {
        return myProject;
    }

    public final FindUsagesOptions calcFindUsagesOptions() {
        calcFindUsagesOptions(myFindUsagesOptions);
        if (myFindUsagesOptions instanceof PersistentFindUsagesOptions persistentOptions) {
            persistentOptions.storeDefaults(myProject);
        }
        return myFindUsagesOptions;
    }

    /**
     * Copies the dialog state into the options. When the dialog was never shown, the options keep their stored values.
     */
    public void calcFindUsagesOptions(FindUsagesOptions options) {
        if (!myComponentsCreated) {
            return;
        }
        SearchScope selectedScope = getSelectedScope();
        options.searchScope = selectedScope == null ? GlobalSearchScope.allScope(myProject) : selectedScope;
        options.isSearchForTextOccurrences = isToChange(myCbToSearchForTextOccurrences) && isSelected(myCbToSearchForTextOccurrences);
    }

    /**
     * Adds the checkboxes of the "Find what" group, through {@link #addFindWhatCheckbox(LocalizeValue, boolean, boolean)}.
     */
    @RequiredUIAccess
    protected void addFindWhatOptions() {
    }

    /**
     * Adds the checkboxes of the "Options" group, through {@link #addUsagesOptionCheckbox(LocalizeValue, boolean, boolean)}.
     */
    @RequiredUIAccess
    protected void addUsagesOptions() {
        if (mySearchForTextOccurrencesAvailable) {
            myCbToSearchForTextOccurrences = addUsagesOptionCheckbox(
                FindLocalize.findOptionsSearchForTextOccurrencesCheckbox(),
                myFindUsagesOptions.isSearchForTextOccurrences,
                false
            );
        }

        if (myIsShowInNewTabVisible) {
            myCbToSkipResultsWhenOneUsage = addUsagesOptionCheckbox(
                FindLocalize.findOptionsSkipResultsTabWithOneUsageCheckbox(),
                FindSettings.getInstance().isSkipResultsWithOneUsage(),
                false
            );
        }
    }

    /**
     * @param toUpdate whether toggling the checkbox runs {@link #update()} and refreshes the Find button
     */
    @RequiredUIAccess
    protected StateRestoringCheckBoxWrapper addFindWhatCheckbox(LocalizeValue text, boolean toSelect, boolean toUpdate) {
        myHasFindWhatOptions = true;
        return addCheckbox(myFindWhatLayout, text, toSelect, toUpdate);
    }

    /**
     * @param toUpdate whether toggling the checkbox runs {@link #update()} and refreshes the Find button
     */
    @RequiredUIAccess
    protected StateRestoringCheckBoxWrapper addUsagesOptionCheckbox(LocalizeValue text, boolean toSelect, boolean toUpdate) {
        myHasUsagesOptions = true;
        return addCheckbox(myOptionsLayout, text, toSelect, toUpdate);
    }

    @RequiredUIAccess
    private StateRestoringCheckBoxWrapper addCheckbox(VerticalLayout layout, LocalizeValue text, boolean toSelect, boolean toUpdate) {
        StateRestoringCheckBoxWrapper checkBox = new StateRestoringCheckBoxWrapper(text);
        checkBox.setValue(toSelect);

        layout.add(checkBox.getComponent());

        if (toUpdate) {
            checkBox.addValueListener(event -> {
                update();
                updateOkButtonState();
            });
        }
        return checkBox;
    }

    @RequiredUIAccess
    protected abstract void configureLabelComponent(TextItemPresentation presentation, Disposable uiDisposable);

    protected static boolean isToChange(@Nullable StateRestoringCheckBoxWrapper checkBox) {
        return checkBox != null && checkBox.getParentComponent() != null;
    }

    protected static boolean isSelected(@Nullable StateRestoringCheckBoxWrapper checkBox) {
        return checkBox != null && checkBox.getParentComponent() != null && checkBox.getValue();
    }

    @RequiredUIAccess
    public boolean isShowInSeparateWindow() {
        CheckBox openInNewTab = myCbToOpenInNewTab;
        if (openInNewTab == null) {
            return !myComponentsCreated && myIsShowInNewTabVisible && myToShowInNewTab;
        }
        return openInNewTab.getValueOrError();
    }

    /**
     * Called whenever a checkbox added with {@code toUpdate} changes, and once the dialog is built.
     */
    @RequiredUIAccess
    protected void update() {
    }

    /**
     * @return whether the Find button is enabled for the current choice
     */
    protected boolean isOkActionEnabled() {
        return true;
    }

    @Override
    public boolean doUpdateOkButtonState() {
        return isOkActionEnabled();
    }

    /**
     * Stores what the user chose for the next search; called once Find is pressed.
     */
    @RequiredUIAccess
    protected void doOKAction() {
        FindSettings settings = FindSettings.getInstance();

        ComboBox<ScopeDescriptor> scopeCombo = myScopeCombo;
        ScopeDescriptor selectedScope = scopeCombo == null ? null : scopeCombo.getValue();
        if (selectedScope != null) {
            settings.setDefaultScopeName(selectedScope.getDisplayName());
        }

        StateRestoringCheckBoxWrapper searchForTextOccurrences = myCbToSearchForTextOccurrences;
        if (mySearchForTextOccurrencesAvailable && searchForTextOccurrences != null && searchForTextOccurrences.isEnabled()) {
            myFindUsagesOptions.isSearchForTextOccurrences = searchForTextOccurrences.getValue();
        }

        StateRestoringCheckBoxWrapper skipResultsWhenOneUsage = myCbToSkipResultsWhenOneUsage;
        if (skipResultsWhenOneUsage != null) {
            settings.setSkipResultsWithOneUsage(skipResultsWhenOneUsage.getValue());
        }
    }

    @RequiredUIAccess
    @Override
    public boolean canHandle(AnAction action, @Nullable DialogValue value) {
        if (value == DialogValue.OK_VALUE) {
            if (myScopeCombo != null && getSelectedScope() == null) {
                return false;
            }
            doOKAction();
        }
        return true;
    }

    protected boolean isInFileOnly() {
        return !myIsShowInNewTabVisible;
    }

    @Override
    protected DialogOkAction createOkAction() {
        return new DialogOkAction(FindLocalize.findDialogFindButton());
    }

    protected @Nullable Component getPreferredFocusedControl() {
        return null;
    }

    @Override
    @RequiredUIAccess
    public @Nullable Component getPreferredFocusedComponent() {
        if (myScopeCombo != null) {
            return myScopeCombo;
        }
        return getPreferredFocusedControl();
    }

    @RequiredUIAccess
    protected void buildElementComponent(Consumer<Component> consumer, Disposable uiDisposable) {
        AdvancedLabel label = AdvancedLabel.create();
        label.updatePresentation(presentation -> configureLabelComponent(presentation, uiDisposable));
        consumer.accept(label);
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        VerticalLayout layout = VerticalLayout.create();

        buildElementComponent(layout::add, uiDisposable);

        addFindWhatOptions();
        addUsagesOptions();

        if (myHasFindWhatOptions || myHasUsagesOptions) {
            DockLayout groups = DockLayout.create(Space.MEDIUM);
            if (myHasFindWhatOptions) {
                groups.left(LabeledLayout.create(FindLocalize.findWhatGroup(), myFindWhatLayout));
            }
            if (myHasUsagesOptions) {
                groups.center(LabeledLayout.create(FindLocalize.findOptionsGroup(), myOptionsLayout));
            }
            layout.add(groups);
        }

        if (!isInFileOnly()) {
            layout.add(LabeledLayout.create(FindLocalize.findScopeLabel(), createScopeCombo()));
        }

        if (myIsShowInNewTabVisible) {
            CheckBox openInNewTab = CheckBox.create(FindLocalize.findOpenInNewTabCheckbox());
            openInNewTab.setValue(myToShowInNewTab);
            openInNewTab.setEnabled(myIsShowInNewTabEnabled);
            myCbToOpenInNewTab = openInNewTab;
            layout.add(openInNewTab);
        }

        myComponentsCreated = true;
        update();
        return layout;
    }

    private @Nullable SearchScope getSelectedScope() {
        ComboBox<ScopeDescriptor> scopeCombo = myScopeCombo;
        ScopeDescriptor descriptor = scopeCombo == null ? null : scopeCombo.getValue();
        return descriptor == null ? null : descriptor.getScope();
    }

    @RequiredUIAccess
    private ComboBox<ScopeDescriptor> createScopeCombo() {
        MutableFlatDataModel<ScopeDescriptor> model = FlatDataModel.of(collectScopes());
        ComboBox<ScopeDescriptor> scopeCombo = ComboBox.create(model);
        scopeCombo.setRenderer((presentation, item) -> {
            ScopeDescriptor descriptor = item.getValue();
            if (descriptor != null) {
                presentation.withIcon(descriptor.getIcon());
                presentation.append(StringUtil.notNullize(descriptor.getDisplayName()));
            }
        });
        myScopeModel = model;
        myScopeCombo = scopeCombo;
        selectScope(myFindUsagesOptions.searchScope.getDisplayName());

        ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar(
            "FindUsagesScope",
            ActionGroup.newImmutableBuilder().add(new EditScopesAction()).build(),
            ActionToolbar.Style.INPLACE
        );
        toolbar.setTargetUIComponent(scopeCombo);
        toolbar.updateActionsAsync();
        scopeCombo.setSuffixComponent(toolbar.getUIComponent());
        return scopeCombo;
    }

    private List<ScopeDescriptor> collectScopes() {
        int options = ScopeChooserCombo.OPT_FROM_SELECTION | ScopeChooserCombo.OPT_USAGE_VIEW | ScopeChooserCombo.OPT_SEARCH_RESULTS;
        if (mySearchInLibrariesAvailable) {
            options |= ScopeChooserCombo.OPT_LIBRARIES;
        }

        DataContext dataContext = DataContext.builder().add(Project.KEY, myProject).build();
        List<ScopeDescriptor> scopes = new ArrayList<>();
        ScopeChooserCombo.processScopes(myProject, dataContext, options, descriptor -> {
            if (!ScopeChooserCombo.isSeparator(descriptor)) {
                scopes.add(descriptor);
            }
            return true;
        });
        return scopes;
    }

    @RequiredUIAccess
    private void selectScope(@Nullable Object selection) {
        ComboBox<ScopeDescriptor> scopeCombo = myScopeCombo;
        if (scopeCombo == null) {
            return;
        }
        if (selection instanceof SearchScope scope) {
            scopeCombo.setValueByCondition(descriptor -> descriptor.scopeEquals(scope));
        }
        else if (selection instanceof String name) {
            scopeCombo.setValueByCondition(descriptor -> name.equals(descriptor.getDisplayName()));
        }
        if (scopeCombo.getValue() == null) {
            scopeCombo.selectFirst();
        }
    }

    private final class EditScopesAction extends DumbAwareAction {
        private EditScopesAction() {
            super(FindLocalize.findScopeLabel(), LocalizeValue.empty(), PlatformIconGroup.generalEllipsis());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            ComboBox<ScopeDescriptor> scopeCombo = myScopeCombo;
            ScopeDescriptor selected = scopeCombo == null ? null : scopeCombo.getValue();
            SearchScope selectedScope = selected == null ? null : selected.getScope();
            UIAccess uiAccess = UIAccess.current();
            myProject.getApplication().getInstance(FindApiInternal.class)
                .openScopeConfigurable(myProject, selected == null ? null : selected.getDisplayName())
                .whenCompleteAsync((value, error) -> {
                    MutableFlatDataModel<ScopeDescriptor> model = myScopeModel;
                    if (error != null || model == null) {
                        return;
                    }
                    model.replaceAll(collectScopes());
                    selectScope(selectedScope);
                }, uiAccess);
        }
    }
}
