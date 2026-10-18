/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

import consulo.component.util.text.UniqueNameGenerator;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilder;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.language.plain.PlainTextLanguage;
import consulo.language.spellchecker.editor.SpellcheckingEditorCustomizationProvider;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Length;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.util.FormBuilder;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.EditChangelistSupport;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.localize.VcsLocalize;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The name, the comment and "make active" of a change list being created or edited, plus the controls every
 * {@link EditChangelistSupport} adds.
 */
public abstract class NewEditChangelistPanel {
    protected final Project myProject;
    private final EditorBox myNameTextField;
    protected final EditorBox myDescriptionTextArea;
    private final HorizontalLayout myAdditionalControlsPanel = HorizontalLayout.create();
    private final CheckBox myMakeActiveCheckBox;
    private final List<Consumer<LocalChangeList>> myConsumers = new ArrayList<>();
    private final DockLayout myContent = DockLayout.create();

    @RequiredUIAccess
    protected NewEditChangelistPanel(Project project) {
        myProject = project;
        myNameTextField = createComponentWithTextField(project);
        myNameTextField.setValue(createNameForChangeList(project, VcsLocalize.changesNewChangelist().get()));
        myNameTextField.selectAll();
        myDescriptionTextArea = createEditorField(project, true).build();
        myDescriptionTextArea.setMinHeight(Length.ofFont(5));
        myMakeActiveCheckBox = CheckBox.create(VcsLocalize.newChangelistMakeActiveCheckbox());
        myAdditionalControlsPanel.add(myMakeActiveCheckBox);
    }

    @RequiredUIAccess
    public void init(@Nullable LocalChangeList initial) {
        myMakeActiveCheckBox.setValue(VcsConfiguration.getInstance(myProject).MAKE_NEW_CHANGELIST_ACTIVE);
        myProject.getExtensionPoint(EditChangelistSupport.class).forEach(support -> {
            support.installSearch(myNameTextField, myDescriptionTextArea);
            Consumer<LocalChangeList> consumer = support.addControls(myAdditionalControlsPanel, initial);
            if (consumer != null) {
                myConsumers.add(consumer);
            }
        });
        myNameTextField.addValueListener(event -> nameChangedImpl(initial));
        nameChangedImpl(initial);
        buildMainPanel();
    }

    @RequiredUIAccess
    private void buildMainPanel() {
        Label descriptionLabel = Label.create(VcsLocalize.editChangelistDescription());
        descriptionLabel.setTarget(myDescriptionTextArea);

        DockLayout description = DockLayout.create();
        description.top(descriptionLabel);
        description.center(myDescriptionTextArea);

        myContent.top(FormBuilder.create().addLabeled(VcsLocalize.editChangelistName(), myNameTextField).build());
        myContent.center(description);
        myContent.bottom(myAdditionalControlsPanel);
    }

    @RequiredUIAccess
    protected void nameChangedImpl(@Nullable LocalChangeList initial) {
        String name = getChangeListName();
        if (name.isBlank()) {
            nameChanged(VcsLocalize.newChangelistEmptyNameError());
        }
        else if ((initial == null || !name.equals(initial.getName()))
            && ChangeListManager.getInstance(myProject).findChangeList(name) != null) {
            nameChanged(VcsLocalize.newChangelistDuplicateNameError());
        }
        else {
            nameChanged(LocalizeValue.empty());
        }
    }

    public void changelistCreatedOrChanged(LocalChangeList list) {
        for (Consumer<LocalChangeList> consumer : myConsumers) {
            consumer.accept(list);
        }
    }

    @RequiredUIAccess
    public void setChangeListName(String name) {
        myNameTextField.setValue(name);
    }

    public String getChangeListName() {
        return myNameTextField.getValueOrError();
    }

    @RequiredUIAccess
    public void setDescription(@Nullable String description) {
        myDescriptionTextArea.setValue(description == null ? "" : description);
    }

    public String getDescription() {
        return myDescriptionTextArea.getValueOrError();
    }

    public CheckBox getMakeActiveCheckBox() {
        return myMakeActiveCheckBox;
    }

    public Component getContent() {
        return myContent;
    }

    public Component getPreferredFocusedComponent() {
        return myNameTextField;
    }

    /**
     * @param errorMessage why the name can not be used, empty when it can
     */
    @RequiredUIAccess
    protected abstract void nameChanged(LocalizeValue errorMessage);

    @RequiredUIAccess
    protected EditorBox createComponentWithTextField(Project project) {
        return createEditorField(project, false).build();
    }

    protected static EditorBoxBuilder createEditorField(Project project, boolean multiline) {
        EditorBoxBuilder builder = project.getApplication().getInstance(EditorBoxBuilderFactory.class)
            .create(project)
            .language(PlainTextLanguage.INSTANCE);
        SpellcheckingEditorCustomizationProvider.getInstance().getCustomizationOpt(true).ifPresent(builder::customize);
        if (multiline) {
            builder.multiline();
        }
        return builder;
    }

    /**
     * The first line of the message, with its runs of spaces collapsed, made unique among the change lists.
     */
    public static String createNameForChangeList(Project project, String commitMessage) {
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        String message = commitMessage.trim();
        int lineEnd = message.indexOf('\n');
        String proposedName = (lineEnd >= 0 ? message.substring(0, lineEnd) : message).trim().replaceAll("[ ]{2,}", " ");
        return UniqueNameGenerator.generateUniqueName(proposedName, "", "", "-", "", name -> changeListManager.findChangeList(name) == null);
    }
}
