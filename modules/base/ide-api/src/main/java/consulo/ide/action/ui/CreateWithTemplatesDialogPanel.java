// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.action.ui;

import consulo.ui.TextBoxWithExtensions;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.JBCurrentTheme;
import consulo.ui.image.Image;
import consulo.util.lang.Trinity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
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

    private static final ListCellRenderer<Trinity<String, Image, String>> LIST_RENDERER = new ColoredListCellRenderer<>() {
        @Override
        protected void customizeCellRenderer(@Nonnull JList<? extends Trinity<String, Image, String>> list, Trinity<String, Image, String> value, int index, boolean selected, boolean hasFocus) {
            setBorder(JBCurrentTheme.listCellBorderFull());
            if (value != null) {
                append(value.first);
                setIcon(value.second);
            }
        }
    };
}
