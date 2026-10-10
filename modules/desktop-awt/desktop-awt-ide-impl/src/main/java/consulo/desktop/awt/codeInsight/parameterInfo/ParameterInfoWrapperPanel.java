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
package consulo.desktop.awt.codeInsight.parameterInfo;

import consulo.ui.ex.awt.JBUI;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
class ParameterInfoWrapperPanel extends JPanel {
    ParameterInfoWrapperPanel() {
        super(new BorderLayout());
        setBorder(JBUI.Borders.empty());
    }

    @Override
    public Color getForeground() {
        return getComponentCount() == 0 ? super.getForeground() : getComponent(0).getForeground();
    }

    @Override
    public Color getBackground() {
        return getComponentCount() == 0 ? super.getBackground() : getComponent(0).getBackground();
    }

    @Override
    public Font getFont() {
        return getComponentCount() == 0 ? super.getFont() : getComponent(0).getFont();
    }

    @Override
    public String toString() {
        return getComponentCount() == 0 ? "<empty>" : getComponent(0).toString();
    }
}
