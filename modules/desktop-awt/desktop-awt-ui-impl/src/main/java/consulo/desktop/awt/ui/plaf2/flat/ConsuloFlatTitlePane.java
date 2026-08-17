/*
 * Copyright 2013-2025 consulo.io
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
package consulo.desktop.awt.ui.plaf2.flat;

import com.formdev.flatlaf.ui.FlatTitlePane;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2025-05-07
 */
public class ConsuloFlatTitlePane extends FlatTitlePane {
    public ConsuloFlatTitlePane(JRootPane rootPane) {
        super(rootPane);
    }

    @Override
    protected String getWindowTitle() {
//        blinking bug https://github.com/JFormDesigner/FlatLaf/issues/998
//        String title = ClientProperty.get(rootPane, ClientProperties.CUSTOM_WINDOW_TITLE);
//        if (title != null) {
//            return title;
//        }

        return super.getWindowTitle();
    }
}
