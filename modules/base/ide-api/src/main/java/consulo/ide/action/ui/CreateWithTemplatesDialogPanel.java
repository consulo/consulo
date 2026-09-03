// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.action.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.ImageBox;
import consulo.ui.TextBox;
import consulo.ui.TextItemRender;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.lang.Trinity;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class CreateWithTemplatesDialogPanel extends NewItemWithTemplatesPopupPanel<Trinity<LocalizeValue, Image, String>> {

    @RequiredUIAccess
    public CreateWithTemplatesDialogPanel(List<Trinity<LocalizeValue, Image, String>> templates, @Nullable String selectedItem) {
        super(templates, LIST_RENDERER);

        myTemplatesList.addValueListener(event -> {
            Trinity<LocalizeValue, Image, String> selectedValue = event.getValue();
            if (selectedValue != null) {
                setTextFieldIcon(selectedValue.second);
            }
        });

        selectTemplate(templates, selectedItem);
        setTemplatesListVisible(templates.size() > 1);
    }

    public TextBox getNameField() {
        return myTextField;
    }

    public String getEnteredName() {
        return myTextField.getValue().trim();
    }

    public String getSelectedTemplate() {
        return myTemplatesList.getValue().third;
    }

    @RequiredUIAccess
    private void setTextFieldIcon(Image icon) {
        ImageBox box = ImageBox.create(icon);
        myTextField.setPrefixComponent(box);
    }

    @RequiredUIAccess
    private void selectTemplate(List<Trinity<LocalizeValue, Image, String>> templates, @Nullable String selectedItem) {
        if (selectedItem == null) {
            myTemplatesList.setValueByIndex(0);
            return;
        }

        for (int i = 0; i < templates.size(); i++) {
            if (selectedItem.equals(templates.get(i).getThird())) {
                myTemplatesList.setValueByIndex(i);
                return;
            }
        }
    }

    private static final TextItemRender<Trinity<LocalizeValue, Image, String>> LIST_RENDERER = (presentation, item) -> {
        Trinity<LocalizeValue, Image, String> value = item.getValue();
        if (value != null) {
            presentation.withIcon(value.second);
            presentation.append(value.first);
        }
    };
}
