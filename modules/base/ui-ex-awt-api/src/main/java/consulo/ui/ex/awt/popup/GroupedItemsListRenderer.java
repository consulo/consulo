/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ui.ex.awt.popup;

import consulo.ui.ex.awt.*;
import consulo.ui.image.Image;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class GroupedItemsListRenderer<E> extends GroupedElementsRenderer.List implements ListCellRenderer<E> {
    protected ListItemDescriptor<E> myDescriptor;

    protected JLabel myNextStepLabel;
    protected int myCurrentIndex;

    public JLabel getNextStepLabel() {
        return myNextStepLabel;
    }

    public GroupedItemsListRenderer(ListItemDescriptor<E> descriptor) {
        myDescriptor = descriptor;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends E> list, E value, int index, boolean isSelected, boolean cellHasFocus) {
        if (myDescriptor.isSeparator(value)) {
            String text = myDescriptor.getTextFor(value);

            TitledSeparator separator = new TitledSeparator(text);
            separator.setBorder(JBUI.Borders.empty());
            separator.setOpaque(false);
            separator.setBackground(UIUtil.TRANSPARENT_COLOR);
            separator.getLabel().setOpaque(false);
            separator.getLabel().setBackground(UIUtil.TRANSPARENT_COLOR);
            return separator;
        }

        Image icon = isSelected ? myDescriptor.getSelectedIconFor(value) : myDescriptor.getIconFor(value);
        JComponent result = configureComponent(myDescriptor.getTextFor(value), myDescriptor.getTooltipFor(value), icon, icon, isSelected, false, null, -1);
        myCurrentIndex = index;
        customizeComponent(list, value, isSelected);

        Border border = null;
        if (cellHasFocus) {
            if (isSelected) {
                border = UIManager.getBorder("List.focusSelectedCellHighlightBorder");
            }
            if (border == null) {
                border = UIManager.getBorder( "List.focusCellHighlightBorder");
            }
        }

        if (border == null) {
            border = UIManager.getBorder("List.cellNoFocusBorder");
        }

        if (border == null) {
            border = JBUI.Borders.empty();
        }
        result.setBorder(border);
        return result;
    }

    @Override
    protected JComponent createItemComponent() {
        createLabel();
        return layoutComponent(myTextLabel);
    }

    protected void createLabel() {
        myTextLabel = new ErrorLabel();
        myTextLabel.setBorder(JBUI.Borders.emptyBottom(1));
        myTextLabel.setOpaque(false);
    }

    protected final JComponent layoutComponent(JComponent middleItemComponent) {
        myNextStepLabel = new JLabel();
        myNextStepLabel.setOpaque(false);
        return JBUI.Panels.simplePanel(middleItemComponent)
            .addToRight(myNextStepLabel)
            .andTransparent()
            .withBorder(getDefaultItemComponentBorder());
    }

    protected void customizeComponent(JList<? extends E> list, E value, boolean isSelected) {
    }
}
