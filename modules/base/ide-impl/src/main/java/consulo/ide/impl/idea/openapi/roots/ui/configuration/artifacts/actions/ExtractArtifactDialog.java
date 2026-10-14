/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.actions;

import consulo.compiler.artifact.ArtifactType;
import consulo.compiler.artifact.PlainArtifactType;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.disposer.Disposable;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.MessageBoxes;
import consulo.ui.TextBox;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.util.FormBuilder;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public class ExtractArtifactDialog extends DialogDescriptor {
    public record Result(String artifactName, ArtifactType artifactType) implements DialogValue {
    }

    private final ArtifactEditorContext myContext;
    private final String myInitialName;

    private @Nullable TextBox myNameField;
    private @Nullable ComboBox<ArtifactType> myTypeBox;

    public ExtractArtifactDialog(ArtifactEditorContext context, String initialName) {
        super(ProjectLocalize.dialogTitleExtractArtifact());
        myContext = context;
        myInitialName = initialName;
    }

    @Override
    @RequiredUIAccess
    public Component createCenterComponent(Disposable uiDisposable) {
        TextBox nameField = TextBox.create(myInitialName);
        nameField.addValueListener(event -> updateOkButtonState());
        myNameField = nameField;

        List<ArtifactType> types = new ArrayList<>();
        myContext.getProject().getApplication().getExtensionPoint(ArtifactType.class).forEach(types::add);

        ComboBox<ArtifactType> typeBox = ComboBox.create(types);
        typeBox.setRenderer((presentation, item) -> {
            ArtifactType type = item.getValue();
            if (type != null) {
                presentation.withIcon(type.getIcon());
                presentation.append(type.getPresentableName());
            }
        });
        typeBox.setValue(PlainArtifactType.getInstance());
        myTypeBox = typeBox;

        FormBuilder formBuilder = FormBuilder.create();
        formBuilder.addLabeled(CommonLocalize.nameLabelText(), nameField);
        formBuilder.addLabeled(ProjectLocalize.labelTextArtifactType(), typeBox);
        return formBuilder.build();
    }

    @Override
    public boolean doUpdateOkButtonState() {
        return !getArtifactName().isEmpty();
    }

    @Override
    public DialogValue getOkValue() {
        return new Result(getArtifactName(), getArtifactType());
    }

    @Override
    @RequiredUIAccess
    public boolean canHandle(AnAction action, @Nullable DialogValue value, Window window) {
        if (value instanceof Result result && myContext.getArtifactModel().findArtifact(result.artifactName()) != null) {
            MessageBoxes.okError(ProjectLocalize.artifactNameAlreadyExistsError(result.artifactName())).showAsync(window);
            return false;
        }
        return super.canHandle(action, value, window);
    }

    @Override
    @RequiredUIAccess
    public @Nullable Component getPreferredFocusedComponent() {
        return myNameField;
    }

    private String getArtifactName() {
        TextBox nameField = myNameField;
        return nameField == null ? myInitialName : StringUtil.notNullize(nameField.getValue()).trim();
    }

    private ArtifactType getArtifactType() {
        ComboBox<ArtifactType> typeBox = myTypeBox;
        ArtifactType type = typeBox == null ? null : typeBox.getValue();
        return type != null ? type : PlainArtifactType.getInstance();
    }
}
