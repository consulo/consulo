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
package consulo.ide.impl.idea.profile.codeInspection.ui.header;

import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.Profile;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.InputValidator;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.MutableCollectionComboBoxModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Dmitry Batkovich
 */
public abstract class ProfilesConfigurableComboBox extends JPanel {
  private final ComboBox<InspectionProfile> myProfilesComboBox;

  public ProfilesConfigurableComboBox(final ListCellRenderer<InspectionProfile> comboBoxItemsRenderer) {
    super(new BorderLayout());

    myProfilesComboBox = new ComboBox<>();
    add(myProfilesComboBox, BorderLayout.CENTER);

    myProfilesComboBox.setRenderer(comboBoxItemsRenderer);
    myProfilesComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final InspectionProfileImpl profile = getSelectedProfile();
        if (profile != null) {
          onProfileChosen(profile);
        }
      }
    });
  }

  protected abstract void onProfileChosen(final InspectionProfileImpl inspectionProfile);

  public void showEditCard(final String initialValue, final SaveInputComponentValidator inputValidator) {
    String name = Messages.showInputDialog(this, "Enter name:", "Name", null, initialValue, new InputValidator() {
      @RequiredUIAccess
      @Override
      public boolean checkInput(String inputString) {
        return inputValidator.checkValid(initialValue);
      }
    });

    if (name == null) {
      inputValidator.cancel();
    }
    else {
      inputValidator.doSave(name);
    }
  }

  public void reset(final Collection<InspectionProfile> profiles) {
    myProfilesComboBox.setModel(new MutableCollectionComboBoxModel<>(new ArrayList<>(profiles)));
    myProfilesComboBox.setSelectedIndex(0);
  }

  @SuppressWarnings("unchecked")
  public MutableComboBoxModel<InspectionProfile> getModel() {
    return (MutableComboBoxModel)myProfilesComboBox.getModel();
  }

  public InspectionProfileImpl getSelectedProfile() {
    return (InspectionProfileImpl)myProfilesComboBox.getSelectedItem();
  }

  public void selectProfile(Profile inspectionProfile) {
    myProfilesComboBox.setSelectedItem(inspectionProfile);
  }
}
