/*
 * Copyright 2000-2024 JetBrains s.r.o.
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

import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorComboBox;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.VcsApplicationSettings;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.status.FileStatus;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Chooses an existing change list by name, or names a new one - the combo box text is the change list name.
 */
public final class ChangeListChooserPanel {
    private final Project myProject;
    private final EditorComboBox myExistingListsCombo;
    private final Label myNewLabel;
    private final NewEditChangelistPanel myListPanel;
    private final Consumer<LocalizeValue> myOkEnabledListener;
    private @Nullable String myLastTypedDescription;
    private boolean myNewNameSuggested = false;

    /**
     * @param okEnabledListener gets why the chosen name can not be used, an empty value when it can
     */
    @RequiredUIAccess
    public ChangeListChooserPanel(Project project, Consumer<LocalizeValue> okEnabledListener) {
        myProject = project;
        myOkEnabledListener = okEnabledListener;
        myExistingListsCombo = NewEditChangelistPanel.createEditorField(project, false).buildComboBox();
        myNewLabel = Label.create(VcsLocalize.newChangelistNewLabel());
        myNewLabel.setForegroundColor(FileStatus.ADDED.getColor());
        myExistingListsCombo.setSuffixComponent(myNewLabel);
        myExistingListsCombo.addValueListener(event -> updateNewLabel());

        myListPanel = new NewEditChangelistPanel(myProject) {
            @Override
            @RequiredUIAccess
            protected EditorBox createComponentWithTextField(Project project) {
                return myExistingListsCombo;
            }

            @Override
            @RequiredUIAccess
            protected void nameChanged(LocalizeValue errorMessage) {
                UIAccess.current().give(ChangeListChooserPanel.this::updateDescription);
                myOkEnabledListener.accept(errorMessage);
            }

            @Override
            @RequiredUIAccess
            public void init(@Nullable LocalChangeList initial) {
                super.init(initial);
                myDescriptionTextArea.addBlurListener(event -> {
                    if (getExistingChangelistByName(getChangeListName()) == null) {
                        myLastTypedDescription = getDescription();
                    }
                });
            }

            @Override
            @RequiredUIAccess
            protected void nameChangedImpl(@Nullable LocalChangeList initial) {
                nameChanged(StringUtil.isEmptyOrSpaces(getChangeListName()) ? VcsLocalize.newChangelistEmptyNameError() : LocalizeValue.empty());
            }
        };

        setChangeLists(null);
        setDefaultSelection(null);
    }

    @RequiredUIAccess
    public void init() {
        myListPanel.init(null);
    }

    @RequiredUIAccess
    public void setChangeLists(@Nullable Collection<? extends ChangeList> changeLists) {
        Collection<? extends ChangeList> lists = changeLists == null ? ChangeListManager.getInstance(myProject).getChangeLists() : changeLists;
        List<String> names = new ArrayList<>(lists.size());
        for (ChangeList list : lists) {
            names.add(list.getName());
        }
        myExistingListsCombo.setItems(names);
    }

    @RequiredUIAccess
    public void setSuggestedName(String name) {
        setSuggestedName(name, false);
    }

    @RequiredUIAccess
    public void setSuggestedName(String name, boolean forceCreate) {
        setSuggestedName(name, null, forceCreate);
    }

    @RequiredUIAccess
    public void setSuggestedName(String name, @Nullable String comment, boolean forceCreate) {
        if (StringUtil.isEmptyOrSpaces(name)) {
            return;
        }
        LocalChangeList changelistByName = getExistingChangelistByName(name);
        if (changelistByName != null) {
            myExistingListsCombo.setSelectedItem(changelistByName.getName());
        }
        else if (forceCreate || VcsApplicationSettings.getInstance().getState().CREATE_CHANGELISTS_AUTOMATICALLY) {
            myNewNameSuggested = true;
            List<String> items = new ArrayList<>(myExistingListsCombo.getItems());
            items.add(0, name);
            myExistingListsCombo.setItems(items);
            if (StringUtil.isEmptyOrSpaces(myLastTypedDescription)) {
                setChangeListDescription(comment);
            }
            if (VcsConfiguration.getInstance(myProject).PRESELECT_EXISTING_CHANGELIST) {
                selectActiveChangeListIfExist();
            }
            else {
                myListPanel.setChangeListName(name);
            }
        }
        updateDescription();
    }

    @RequiredUIAccess
    private void selectActiveChangeListIfExist() {
        myExistingListsCombo.setSelectedItem(ChangeListManager.getInstance(myProject).getDefaultChangeList().getName());
    }

    @RequiredUIAccess
    public void updateEnabled() {
        myListPanel.nameChangedImpl(null);
    }

    /**
     * Method used as getResult, usually invoked inside doOkAction
     */
    @RequiredUIAccess
    public @Nullable LocalChangeList getSelectedList(Project project) {
        ChangeListManager manager = ChangeListManager.getInstance(project);
        String changeListName = myListPanel.getChangeListName();
        LocalChangeList localChangeList = manager.findChangeList(changeListName);

        if (localChangeList == null) {
            localChangeList = manager.addChangeList(changeListName, myListPanel.getDescription());
            myListPanel.changelistCreatedOrChanged(localChangeList);
        }
        else {
            manager.editComment(changeListName, myListPanel.getDescription());
        }
        boolean makeActive = myListPanel.getMakeActiveCheckBox().getValueOrError();
        rememberSettings(project, localChangeList.isDefault(), makeActive);
        if (makeActive) {
            manager.setDefaultChangeList(localChangeList);
        }
        return localChangeList;
    }

    private void rememberSettings(Project project, boolean activeListSelected, boolean setActive) {
        if (myNewNameSuggested) {
            VcsConfiguration.getInstance(project).PRESELECT_EXISTING_CHANGELIST = activeListSelected;
        }
        VcsConfiguration.getInstance(project).MAKE_NEW_CHANGELIST_ACTIVE = setActive;
    }

    @RequiredUIAccess
    public void setDefaultSelection(@Nullable ChangeList defaultSelection) {
        if (defaultSelection == null) {
            selectActiveChangeListIfExist();
        }
        else {
            myExistingListsCombo.setSelectedItem(defaultSelection.getName());
        }
        updateDescription();
        updateEnabled();
    }

    @RequiredUIAccess
    public void setChangeListDescription(@Nullable String description) {
        myListPanel.setDescription(description);
        myLastTypedDescription = description;
    }

    @RequiredUIAccess
    private void updateDescription() {
        if (myLastTypedDescription != null) {
            return;
        }
        LocalChangeList list = getExistingChangelistByName(myListPanel.getChangeListName());
        String newText = list != null ? list.getComment() : null;
        if (!StringUtil.equals(myListPanel.getDescription(), StringUtil.notNullize(newText))) {
            myListPanel.setDescription(newText);
        }
    }

    @RequiredUIAccess
    private void updateNewLabel() {
        String changeListName = myExistingListsCombo.getValueOrError();
        myNewLabel.setVisible(!StringUtil.isEmptyOrSpaces(changeListName) && getExistingChangelistByName(changeListName) == null);
    }

    private @Nullable LocalChangeList getExistingChangelistByName(String changeListName) {
        return ChangeListManager.getInstance(myProject).findChangeList(changeListName);
    }

    public Component getComponent() {
        return myListPanel.getContent();
    }

    public Component getPreferredFocusedComponent() {
        return myExistingListsCombo;
    }
}
