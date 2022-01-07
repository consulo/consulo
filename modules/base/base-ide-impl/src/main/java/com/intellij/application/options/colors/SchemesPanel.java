/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.application.options.colors;

import com.intellij.application.options.SaveSchemeDialog;
import com.intellij.application.options.SkipSelfSearchComponent;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.util.EventDispatcher;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.platform.base.localize.ApplicationLocalize;
import consulo.ui.Button;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.model.MutableListModel;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class SchemesPanel implements SkipSelfSearchComponent {
  private final ColorAndFontOptions myOptions;

  private Button myDeleteButton;

  private final EventDispatcher<ColorAndFontSettingsListener> myDispatcher = EventDispatcher.create(ColorAndFontSettingsListener.class);

  private DockLayout myLayout;

  private ComboBox<String> mySchemeComboBox;
  private MutableListModel<String> myModel;

  @RequiredUIAccess
  public SchemesPanel(ColorAndFontOptions options) {
    myLayout = DockLayout.create();
    myLayout.addBorder(BorderPosition.BOTTOM, BorderStyle.EMPTY, 5);
    myLayout.addBorder(BorderPosition.RIGHT, BorderStyle.EMPTY, 5);
    myLayout.addBorder(BorderPosition.LEFT, BorderStyle.EMPTY, 5);

    myOptions = options;

    HorizontalLayout topLayout = HorizontalLayout.create();
    myLayout.top(topLayout);

    myModel = MutableListModel.of(new ArrayList<>());
    mySchemeComboBox = ComboBox.create(myModel);

    topLayout.add(Label.create(ApplicationLocalize.comboboxSchemeName()));
    topLayout.add(mySchemeComboBox);

    mySchemeComboBox.addValueListener(event -> {
      Object value = mySchemeComboBox.getValue();
      if (value != null) {
        EditorColorsScheme selected = myOptions.selectScheme((String)value);
        if (ColorAndFontOptions.isReadOnly(selected)) {
          myDeleteButton.setEnabled(false);
        }
        else {
          myDeleteButton.setEnabled(true);
        }

        if (areSchemesLoaded()) {
          myDispatcher.getMulticaster().schemeChanged(SchemesPanel.this);
        }
      }
    });

    Button saveAsButton = Button.create(ApplicationLocalize.buttonSaveAs(), event -> showSaveAsDialog());

    topLayout.add(saveAsButton);

    myDeleteButton = Button.create(ApplicationLocalize.buttonDelete(), event -> {
      Object value = mySchemeComboBox.getValue();
      if (value != null) {
        myOptions.removeScheme((String)value);
      }
    });

    topLayout.add(myDeleteButton);
  }

  private boolean myListLoaded = false;

  public boolean areSchemesLoaded() {
    return myListLoaded;
  }

  @Nonnull
  public Component getComponent() {
    // todo hack
    if (Application.get().isSwingApplication()) {
      JComponent component = (JComponent)TargetAWT.to(myLayout);
      UIUtil.putClientProperty(component, SkipSelfSearchComponent.KEY, Boolean.TRUE);
    }
    return myLayout;
  }

  @RequiredUIAccess
  private void showSaveAsDialog() {
    List<String> names = ContainerUtil.newArrayList(myOptions.getSchemeNames());
    String selectedName = myOptions.getSelectedScheme().getName();
    SaveSchemeDialog dialog = new SaveSchemeDialog(TargetAWT.to(getComponent()), ApplicationBundle.message("title.save.color.scheme.as"), names, selectedName);

    dialog.showAsync().doWhenDone(() -> myOptions.saveSchemeAs(dialog.getSchemeName()));
  }

  private void changeToScheme() {
    updateDescription(false);
  }

  public boolean updateDescription(boolean modified) {
    EditorColorsScheme scheme = myOptions.getSelectedScheme();

    if (modified && (ColorAndFontOptions.isReadOnly(scheme))) {
      FontOptions.showReadOnlyMessage((JComponent)TargetAWT.to(getComponent()), false);
      return false;
    }

    return true;
  }

  @RequiredUIAccess
  public void resetSchemesCombo(final Object source) {
    if (this != source) {
      setListLoaded(false);

      String selectedSchemeBackup = myOptions.getSelectedScheme().getName();
      myModel.removeAll();

      String[] schemeNames = myOptions.getSchemeNames();
      for (String schemeName : schemeNames) {
        myModel.add(schemeName);
      }

      mySchemeComboBox.setValue(selectedSchemeBackup);
      setListLoaded(true);

      changeToScheme();

      myDispatcher.getMulticaster().schemeChanged(this);
    }
  }

  private void setListLoaded(final boolean b) {
    myListLoaded = b;
  }

  public void addListener(ColorAndFontSettingsListener listener) {
    myDispatcher.addListener(listener);
  }
}
