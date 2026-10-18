/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change.patch;

import consulo.application.util.UserHomeFileUtil;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TextComponentAccessor;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.ui.util.FormBuilder;
import consulo.util.io.CharsetToolkit;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.encoding.EncodingProjectManager;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Patch file, base path, direction and encoding of a patch to create. The values are kept outside the components, so
 * the patch can be written from a background thread.
 *
 * @author yole
 * @since 2006-11-14
 */
public class CreatePatchConfigurationPanel {
    private static final int TEXT_FIELD_WIDTH = 70;

    private Runnable myStateChanged = () -> {
    };
    private final FileChooserTextBoxBuilder.Controller myFileNameField;
    private final FileChooserTextBoxBuilder.Controller myBasePathField;
    private final CheckBox myReversePatchCheckBox;
    private final ComboBox<Charset> myEncoding;
    private final Label myWarningLabel;
    private final Component myComponent;
    private @Nullable File myCommonParentDir;

    private volatile String myFileName = "";
    private volatile String myBaseDirName = "";
    private volatile boolean myReversePatch;
    private volatile Charset myCharset;

    @RequiredUIAccess
    public CreatePatchConfigurationPanel(Project project) {
        myFileNameField = FileChooserTextBoxBuilder.create(project)
            .dialogTitle(VcsLocalize.patchCreationSaveToTitle())
            .fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFolderDescriptor())
            .textBoxAccessor(new KeepFileNameAccessor())
            .build();
        myFileNameField.getComponent().setVisibleLength(TEXT_FIELD_WIDTH);
        myFileNameField.getComponent().addValueListener(event -> {
            myFileName = UserHomeFileUtil.expandUserHome(StringUtil.notNullize(event.getValue()).trim());
            fireStateChanged();
        });

        myBasePathField = FileChooserTextBoxBuilder.create(project)
            .fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFolderDescriptor())
            .build();
        myBasePathField.getComponent().setVisibleLength(TEXT_FIELD_WIDTH);
        myBasePathField.getComponent().addValueListener(event -> {
            myBaseDirName = UserHomeFileUtil.expandUserHome(StringUtil.notNullize(event.getValue()).trim());
            fireStateChanged();
        });

        myReversePatchCheckBox = CheckBox.create(VcsLocalize.createPatchReverseCheckbox());
        myReversePatchCheckBox.addValueListener(event -> myReversePatch = Boolean.TRUE.equals(event.getValue()));

        myCharset = EncodingProjectManager.getInstance(project).getDefaultCharset();
        myEncoding = ComboBox.create(List.of(CharsetToolkit.getAvailableCharsets()));
        myEncoding.setTextRenderer(charset -> charset == null ? LocalizeValue.empty() : LocalizeValue.of(charset.displayName()));
        myEncoding.setValue(myCharset);
        myEncoding.addValueListener(event -> {
            Charset charset = event.getValue();
            if (charset != null) {
                myCharset = charset;
            }
        });

        myWarningLabel = Label.create();
        myWarningLabel.setForegroundColor(ComponentColors.ERROR_FOREGROUND);

        VerticalLayout layout = VerticalLayout.create();
        layout.add(FormBuilder.create()
            .addLabeled(VcsLocalize.createPatchFilePath(), myFileNameField.getComponent())
            .addLabeled(VcsLocalize.patchCreationBasePathField(), myBasePathField.getComponent())
            .addLabeled(VcsLocalize.createPatchEncoding(), myEncoding)
            .build());
        layout.add(myReversePatchCheckBox);
        layout.add(myWarningLabel);
        myComponent = layout;

        VirtualFile baseDir = project.getBaseDir();
        if (baseDir != null) {
            selectBasePath(baseDir);
        }
    }

    public void setStateChanged(Runnable stateChanged) {
        myStateChanged = stateChanged;
    }

    @RequiredUIAccess
    public Component getComponent() {
        return myComponent;
    }

    @RequiredUIAccess
    public void selectBasePath(VirtualFile baseDir) {
        myBasePathField.getComponent().setValue(baseDir.getPresentableUrl());
    }

    public Charset getEncoding() {
        return myCharset;
    }

    public void setCommonParentPath(@Nullable File commonParentPath) {
        myCommonParentDir = commonParentPath == null || commonParentPath.isDirectory() ? commonParentPath : commonParentPath.getParentFile();
    }

    public String getFileName() {
        return myFileName;
    }

    public String getBaseDirName() {
        return myBaseDirName;
    }

    @RequiredUIAccess
    public void setFileName(File file) {
        myFileNameField.getComponent().setValue(file.getPath());
    }

    public boolean isReversePatch() {
        return myReversePatch;
    }

    @RequiredUIAccess
    public void setReversePatch(boolean reverse) {
        myReversePatchCheckBox.setValue(reverse);
    }

    public boolean isOkToExecute() {
        return validateFields().isEmpty();
    }

    public LocalizeValue validateFields() {
        LocalizeValue nameError = PatchNameChecker.validateName(myFileName);
        if (nameError.isNotEmpty()) {
            return nameError;
        }
        return verifyBaseDirPath();
    }

    private LocalizeValue verifyBaseDirPath() {
        String baseDirName = myBaseDirName;
        if (StringUtil.isEmptyOrSpaces(baseDirName)) {
            return VcsLocalize.patchCreationEmptyBasePathError();
        }
        File baseFile = new File(baseDirName);
        if (!baseFile.exists()) {
            return VcsLocalize.patchCreationBaseDirDoesNotExistError();
        }
        File commonParentDir = myCommonParentDir;
        if (commonParentDir != null && !FileUtil.isAncestor(baseFile, commonParentDir, false)) {
            return VcsLocalize.patchCreationWrongBasePathForChangesError(commonParentDir.getPath());
        }
        return LocalizeValue.empty();
    }

    @RequiredUIAccess
    private void fireStateChanged() {
        String fileName = myFileName;
        myWarningLabel.setText(
            !fileName.isEmpty() && new File(fileName).exists()
                ? VcsLocalize.patchCreationFileAlreadyExistsWarning(fileName)
                : LocalizeValue.empty()
        );
        myStateChanged.run();
    }

    private static class KeepFileNameAccessor implements TextComponentAccessor<TextBox> {
        @Override
        @RequiredUIAccess
        public String getValue(TextBox textBox) {
            return StringUtil.notNullize(textBox.getValue());
        }

        @Override
        @RequiredUIAccess
        public void setValue(TextBox textBox, String text, boolean fireListeners) {
            String name = new File(UserHomeFileUtil.expandUserHome(getValue(textBox).trim())).getName();
            textBox.setValue(name.isEmpty() ? text : new File(text, name).getPath(), fireListeners);
        }
    }
}
