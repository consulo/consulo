/*
 * Copyright 2013-2020 consulo.io
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
package consulo.diff.impl.internal.external;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.diff.localize.DiffLocalize;
import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.HtmlLabel;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.FormBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * @author VISTALL
 * @since 11/29/2020
 */
@ExtensionImpl
public class ExternalDiffSettingsConfigurable extends SimpleConfigurableByProperties implements ApplicationConfigurable {
  private final Provider<ExternalDiffSettings> myExternalDiffSettings;

  @Inject
  public ExternalDiffSettingsConfigurable(Provider<ExternalDiffSettings> externalDiffSettings) {
    myExternalDiffSettings = externalDiffSettings;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  protected Component createLayout(PropertyBuilder propertyBuilder, @Nonnull Disposable uiDisposable) {
    VerticalLayout rootLayout = VerticalLayout.create();

    ExternalDiffSettings externalDiffSettings = myExternalDiffSettings.get();

    CheckBox enableForFiles = CheckBox.create(DiffLocalize.diffOptionsUseExternalToolForFilesCheckbox());
    rootLayout.add(enableForFiles);
    propertyBuilder.add(enableForFiles, externalDiffSettings::isDiffEnabled, externalDiffSettings::setDiffEnabled);

    FormBuilder diffPanel = FormBuilder.create();

    FileChooserTextBoxBuilder diffBoxBuilder = FileChooserTextBoxBuilder.create(null);
    diffBoxBuilder.dialogTitle(DiffLocalize.selectExternalDiffProgramDialogTitle());
    diffBoxBuilder.fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor());
    diffBoxBuilder.uiDisposable(uiDisposable);

    FileChooserTextBoxBuilder.Controller diffBox = diffBoxBuilder.build();
    diffPanel.addLabeled(LocalizeValue.localizeTODO("Path to executable"), diffBox.getComponent());
    propertyBuilder.add(diffBox::getValue, diffBox::setValue, externalDiffSettings::getDiffExePath, externalDiffSettings::setDiffExePath);

    TextBox diffParameters = TextBox.create();
    diffPanel.addLabeled(LocalizeValue.localizeTODO("Parameters"), diffParameters);
    propertyBuilder.add(diffParameters, externalDiffSettings::getDiffParameters, externalDiffSettings::setDiffParameters);

    rootLayout.add(diffPanel.build());

    enableForFiles.addValueListener(event -> {
      diffParameters.setEnabled(enableForFiles.getValue());
      diffBox.getComponent().setEnabled(enableForFiles.getValue());
    });

    CheckBox enableForMerge = CheckBox.create(DiffLocalize.diffOptionsUseExternalMergeToolForFilesCheckbox());
    rootLayout.add(enableForMerge);
    propertyBuilder.add(enableForMerge, externalDiffSettings::isMergeEnabled, externalDiffSettings::setMergeEnabled);

    FormBuilder mergePanel = FormBuilder.create();

    FileChooserTextBoxBuilder mergeBoxBuilder = FileChooserTextBoxBuilder.create(null);
    mergeBoxBuilder.dialogTitle(DiffLocalize.selectExternalMergeProgramDialogTitle());
    mergeBoxBuilder.fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor());
    mergeBoxBuilder.uiDisposable(uiDisposable);

    FileChooserTextBoxBuilder.Controller mergeBox = mergeBoxBuilder.build();
    mergePanel.addLabeled(LocalizeValue.localizeTODO("Path to executable"), mergeBox.getComponent());
    propertyBuilder.add(mergeBox::getValue, mergeBox::setValue, externalDiffSettings::getMergeExePath, externalDiffSettings::setMergeExePath);

    TextBox mergeParameters = TextBox.create();
    mergePanel.addLabeled(LocalizeValue.localizeTODO("Parameters"), mergeParameters);
    propertyBuilder.add(mergeParameters, externalDiffSettings::getMergeParameters, externalDiffSettings::setMergeParameters);

    rootLayout.add(mergePanel.build());

    enableForMerge.addValueListener(event -> {
      mergeParameters.setEnabled(enableForMerge.getValue());
      mergeBox.getComponent().setEnabled(enableForMerge.getValue());
    });

    rootLayout.add(HtmlLabel.create(LocalizeValue.localizeTODO(
            "Different merge tools have different parameters. It's important to specify all necessary parameters in proper order<br> <b>%1</b> - left (Local changes)<br> <b>%2</b> - base (Current version without local changes)<br> <b>%3</b> - right (Server content)<br> <b>%4</b> - output path")));
    return rootLayout;
  }

  @Nonnull
  @Override
  public String getId() {
    return "diff.external";
  }

  @Nullable
  @Override
  public String getParentId() {
    return "diff.base";
  }

  @Nonnull
  @Override
  public LocalizeValue getDisplayName() {
    return LocalizeValue.localizeTODO("External Diff Tools");
  }
}
