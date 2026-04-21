/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.externalSystem.service.setting;

import consulo.disposer.Disposable;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.service.execution.ExternalSystemSettingsControl;
import consulo.externalSystem.setting.ExternalProjectSettings;
import consulo.externalSystem.ui.awt.ExternalSystemUiUtil;
import consulo.externalSystem.ui.awt.PaintAwarePanel;
import consulo.ui.CheckBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.TargetAWT;

/**
 * Templates class for managing single external project settings (single ide project might contain multiple bindings to external
 * projects, e.g. one module is backed by a single external project and couple of others are backed by a single external multi-project).
 *
 * @author Denis Zhdanov
 * @since 2013-04-24
 */
public abstract class AbstractExternalProjectSettingsControl<S extends ExternalProjectSettings> implements ExternalSystemSettingsControl<S> {
  private S myInitialSettings;

  private CheckBox myUseAutoImportBox;
  private CheckBox myCreateEmptyContentRootDirectoriesBox;
  private boolean myHideUseAutoImportBox;

  protected AbstractExternalProjectSettingsControl(S initialSettings) {
    myInitialSettings = initialSettings;
  }

  public S getInitialSettings() {
    return myInitialSettings;
  }

  public void hideUseAutoImportBox() {
    myHideUseAutoImportBox = true;
  }

  @Override
  @RequiredUIAccess
  public void fillUi(Disposable uiDisposable, PaintAwarePanel canvas, int indentLevel) {
    myUseAutoImportBox = CheckBox.create(ExternalSystemLocalize.settingsLabelUseAutoImport());
    myUseAutoImportBox.setVisible(!myHideUseAutoImportBox);
    canvas.add(TargetAWT.to(myUseAutoImportBox), ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
    myCreateEmptyContentRootDirectoriesBox = CheckBox.create(ExternalSystemLocalize.settingsLabelCreateEmptyContentRootDirectories());
    canvas.add(TargetAWT.to(myCreateEmptyContentRootDirectoriesBox), ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
    fillExtraControls(uiDisposable, canvas, indentLevel);
  }

  protected abstract void fillExtraControls(Disposable uiDisposable, PaintAwarePanel content, int indentLevel);

  @Override
  public boolean isModified() {
    return myUseAutoImportBox.getValue() != getInitialSettings().isUseAutoImport()
      || myCreateEmptyContentRootDirectoriesBox.getValue() != getInitialSettings().isCreateEmptyContentRootDirectories()
      || isExtraSettingModified();
  }

  protected abstract boolean isExtraSettingModified();

  @Override
  @RequiredUIAccess
  public void reset() {
    reset(false);
  }

  @RequiredUIAccess
  public void reset(boolean isDefaultModuleCreation) {
    myUseAutoImportBox.setValue(getInitialSettings().isUseAutoImport());
    myCreateEmptyContentRootDirectoriesBox.setValue(getInitialSettings().isCreateEmptyContentRootDirectories());
    resetExtraSettings(isDefaultModuleCreation);
  }

  protected abstract void resetExtraSettings(boolean isDefaultModuleCreation);

  @Override
  public void apply(S settings) {
    settings.setUseAutoImport(myUseAutoImportBox.getValue());
    settings.setCreateEmptyContentRootDirectories(myCreateEmptyContentRootDirectoriesBox.getValue());
    if (myInitialSettings.getExternalProjectPath() != null) {
      settings.setExternalProjectPath(myInitialSettings.getExternalProjectPath());
    }
    applyExtraSettings(settings);
  }

  protected abstract void applyExtraSettings(S settings);

  @Override
  public void disposeUIResources() {
    ExternalSystemUiUtil.disposeUi(this);
  }

  @Override
  public void showUi(boolean show) {
    ExternalSystemUiUtil.showUi(this, show);
  }

  public void updateInitialSettings() {
    myInitialSettings.setUseAutoImport(myUseAutoImportBox.getValue());
    myInitialSettings.setCreateEmptyContentRootDirectories(myCreateEmptyContentRootDirectoriesBox.getValue());
    updateInitialExtraSettings();
  }

  protected void updateInitialExtraSettings() {
  }
}
