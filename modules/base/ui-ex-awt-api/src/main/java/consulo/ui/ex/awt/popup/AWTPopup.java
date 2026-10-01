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
package consulo.ui.ex.awt.popup;

import consulo.annotation.DeprecationInfo;
import consulo.ui.ex.popup.JBPopup;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2026-10-01
 */
@Deprecated
@DeprecationInfo("Do not depend to Swing classes")
public interface AWTPopup extends JBPopup {
    JComponent getComponent();

    void focusPreferredComponent();

    void setShowHints(boolean show);

    @Nullable String getDimensionServiceKey();

    Border getPopupBorder();

    Dimension getHeaderPreferredSize();

    Dimension getFooterPreferredSize();
}
