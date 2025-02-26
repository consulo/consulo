/*
 * Copyright 2013-2016 consulo.io
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
package consulo.desktop.awt.ui.impl.layout;

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.LayoutStyle;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 15-Jun-16
 */
public class DesktopLabeledLayoutImpl extends DesktopLayoutBase<JPanel> implements LabeledLayout {
    class LabelJPanel extends MyJPanel {
        private final LocalizeValue myLabelValue;

        private LabelJPanel(LayoutManager layout, LocalizeValue labelValue) {
            super(layout);
            myLabelValue = labelValue;
        }

        @Override
        public void updateUI() {
            super.updateUI();

            updateBorder();
        }

        public void updateBorder() {
            // first component create
            if (myLabelValue != null) {
                setBorder(IdeBorderFactory.createTitledBorder(myLabelValue.getValue()));
            }
        }
    }

    public DesktopLabeledLayoutImpl(LocalizeValue label) {
        LabelJPanel component = new LabelJPanel(new BorderLayout(), label);
        component.updateBorder();
        initialize(component);
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public LabeledLayout set(@Nonnull Component component) {
        add(component, BorderLayout.CENTER);
        return this;
    }
}
