/*
/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.ide.impl.idea.application.options.codeStyle;

import consulo.language.codeStyle.CodeStyleScheme;
import consulo.language.codeStyle.ui.setting.CodeStyleSchemesModel;
import consulo.localize.LocalizeValue;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderStyle;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.model.MutableListModel;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CodeStyleSchemesPanel {

  private final CodeStyleSchemesModel myModel;

  private boolean myIsReset = false;

  private final HorizontalLayout myLayout;
  private final ComboBox<CodeStyleScheme> mySchemeComboBox;
  private final MutableListModel<CodeStyleScheme> mySchemeComboBoxModel;

  @RequiredUIAccess
  public CodeStyleSchemesPanel(CodeStyleSchemesModel model) {
    myModel = model;

    myLayout = HorizontalLayout.create();
    myLayout.add(Label.create(LocalizeValue.localizeTODO("Scheme:")));

    mySchemeComboBoxModel = MutableListModel.of(List.of());
    mySchemeComboBox = ComboBox.create(mySchemeComboBoxModel);
    mySchemeComboBox.setRender((render, index, scheme) -> {
      if (scheme == null) {
        return;
      }

      if (scheme.isDefault() || myModel.isProjectScheme(scheme)) {
        render.append(scheme.getName(), TextAttribute.REGULAR_BOLD);
      }
      else {
        render.append(scheme.getName());
      }
    });
    mySchemeComboBox.addValueListener(e -> {
      if (!myIsReset) {
        UIAccess.current().give(this::onCombo);
      }
    });
    myLayout.add(mySchemeComboBox);
    myLayout.addMirrorBorders(BorderStyle.EMPTY, null, 5, 8);

    Button manageButton = Button.create(LocalizeValue.localizeTODO("&Manage..."));
    manageButton.addClickListener(event -> showManageSchemesDialog());

    myLayout.add(manageButton);
  }

  private void onCombo() {
    CodeStyleScheme selected = getSelectedScheme();
    if (selected != null) {
      if (myModel.isProjectScheme(selected)) {
        myModel.setUsePerProjectSettings(true);
      }
      else {
        myModel.selectScheme(selected, this);
        myModel.setUsePerProjectSettings(false);
      }
    }
  }

  @Nullable
  private CodeStyleScheme getSelectedScheme() {
    return mySchemeComboBox.getValue();
  }

  @RequiredUIAccess
  public void resetSchemesCombo() {
    myIsReset = true;
    try {
      List<CodeStyleScheme> schemes = new ArrayList<>();
      schemes.addAll(myModel.getAllSortedSchemes());
      mySchemeComboBoxModel.replaceAll(schemes);

      if (myModel.isUsePerProjectSettings()) {
        mySchemeComboBox.setValue(myModel.getProjectScheme());
      }
      else {
        mySchemeComboBox.setValue(myModel.getSelectedGlobalScheme());
      }
    }
    finally {
      myIsReset = false;
    }
  }

  @RequiredUIAccess
  public void onSelectedSchemeChanged() {
    myIsReset = true;
    try {
      if (myModel.isUsePerProjectSettings()) {
        mySchemeComboBox.setValue(myModel.getProjectScheme());
      }
      else {
        mySchemeComboBox.setValue(myModel.getSelectedGlobalScheme());
      }
    }
    finally {
      myIsReset = false;
    }
  }

  public Component getLayout() {
    return myLayout;
  }

  @RequiredUIAccess
  private void showManageSchemesDialog() {
    ManageCodeStyleSchemesDialog manageSchemesDialog = new ManageCodeStyleSchemesDialog(TargetAWT.to(myLayout), myModel);
    manageSchemesDialog.showAsync();
  }

  @RequiredUIAccess
  public void usePerProjectSettingsOptionChanged() {
    if (myModel.isProjectScheme(myModel.getSelectedScheme())) {
      mySchemeComboBox.setValue(myModel.getProjectScheme());
    }
    else {
      mySchemeComboBox.setValue(myModel.getSelectedScheme());
    }
  }
}
