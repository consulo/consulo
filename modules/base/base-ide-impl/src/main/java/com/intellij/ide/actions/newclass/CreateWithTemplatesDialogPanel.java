// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.newclass;

import com.intellij.ide.ui.newItemPopup.NewItemWithTemplatesPopupPanel;
import com.intellij.openapi.util.Trinity;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.JBUI;
import consulo.awt.TargetAWT;
import consulo.ui.TextBoxWithExtensions;
import consulo.ui.image.Image;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class CreateWithTemplatesDialogPanel extends NewItemWithTemplatesPopupPanel<Trinity<String, Image, String>> {

  public CreateWithTemplatesDialogPanel(@Nonnull List<Trinity<String, Image, String>> templates, @Nullable String selectedItem) {
    super(templates, LIST_RENDERER);
    myTemplatesList.addListSelectionListener(e -> {
      Trinity<String, Image, String> selectedValue = myTemplatesList.getSelectedValue();
      if (selectedValue != null) {
        setTextFieldIcon(selectedValue.second);
      }
    });
    selectTemplate(selectedItem);
    setTemplatesListVisible(templates.size() > 1);
  }

  public TextBoxWithExtensions getNameField() {
    return myTextField;
  }

  @Nonnull
  public String getEnteredName() {
    return myTextField.getValue().trim();
  }

  @Nonnull
  public String getSelectedTemplate() {
    return myTemplatesList.getSelectedValue().third;
  }

  private void setTextFieldIcon(Image icon) {
    myTextField.setExtensions(new TextBoxWithExtensions.Extension(true, icon, null));
  }

  private void selectTemplate(@Nullable String selectedItem) {
    if (selectedItem == null) {
      myTemplatesList.setSelectedIndex(0);
      return;
    }

    ListModel<Trinity<String, Image, String>> model = myTemplatesList.getModel();
    for (int i = 0; i < model.getSize(); i++) {
      String templateID = model.getElementAt(i).getThird();
      if (selectedItem.equals(templateID)) {
        myTemplatesList.setSelectedIndex(i);
        return;
      }
    }
  }

  private static final ListCellRenderer<Trinity<String, Image, String>> LIST_RENDERER = new ListCellRenderer<Trinity<String, Image, String>>() {

    private final ListCellRenderer<Trinity<String, Image, String>> delegateRenderer = SimpleListCellRenderer.create((label, value, index) -> {
      if (value != null) {
        label.setText(value.first);
        label.setIcon(TargetAWT.to(value.second));
      }
    });

    @Override
    public Component getListCellRendererComponent(JList<? extends Trinity<String, Image, String>> list, Trinity<String, Image, String> value, int index, boolean isSelected, boolean cellHasFocus) {
      JComponent delegate = (JComponent)delegateRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      delegate.setBorder(JBUI.Borders.empty(JBUIScale.scale(3), JBUIScale.scale(1)));
      return delegate;
    }
  };
}
