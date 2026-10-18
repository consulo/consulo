/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.awt.ui.impl;

import consulo.ui.ex.awt.CellRendererPanel;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.MouseEvent;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public class DesktopSuffixCellRendererPanel extends CellRendererPanel {
    private final DesktopTextItemSuffixComponent mySuffixComponent;

    private @Nullable JComponent myMainComponent;

    public @Nullable JComponent getMainComponent() {
        return myMainComponent;
    }

    public DesktopSuffixCellRendererPanel() {
        this(new DesktopTextItemSuffixComponent());
    }

    public DesktopSuffixCellRendererPanel(Insets suffixInsets) {
        this(new DesktopTextItemSuffixComponent(suffixInsets));
    }

    private DesktopSuffixCellRendererPanel(DesktopTextItemSuffixComponent suffixComponent) {
        super(new BorderLayout());
        mySuffixComponent = suffixComponent;
    }

    public java.awt.Component compose(
        JComponent main,
        @Nullable Color background,
        DesktopTextItemPresentationImpl presentation,
        @Nullable Color selectionForeground
    ) {
        if (!presentation.hasSuffix()) {
            myMainComponent = null;
            return main;
        }

        myMainComponent = main;

        removeAll();
        setPreferredSize(null);
        setSelected(background != null);
        setBackground(background);
        setFont(main.getFont());

        main.setOpaque(false);

        mySuffixComponent.update(presentation.getSuffixText(), presentation.getSuffixIcon(), selectionForeground, main.getFont());

        add(main, BorderLayout.CENTER);
        add(mySuffixComponent, BorderLayout.EAST);
        return this;
    }

    @Override
    public @Nullable String getToolTipText() {
        JComponent main = myMainComponent;
        return main == null ? null : main.getToolTipText();
    }

    @Override
    public @Nullable String getToolTipText(MouseEvent event) {
        return getToolTipText();
    }
}
