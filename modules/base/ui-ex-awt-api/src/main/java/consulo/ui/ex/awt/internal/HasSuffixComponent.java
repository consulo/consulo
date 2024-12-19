/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ui.ex.awt.internal;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2024-12-19
 */
public interface HasSuffixComponent {
    static boolean isSuffixComponent(JComponent component) {
        if (component instanceof HasSuffixComponent) {
            return true;
        }

        if (component instanceof JTextField) {
            return true;
        }

        if (component instanceof JComboBox) {
            return true;
        }

        return false;
    }

    static void setSuffixComponent(JComponent target, JComponent suffix) {
        if (target instanceof JTextField) {
            target.putClientProperty("JTextField.trailingComponent", suffix);
        }

        if (target instanceof JComboBox) {
            target.putClientProperty("JComboBox.trailingComponent", suffix);
        }

        if (target instanceof HasSuffixComponent hasSuffixComponent) {
            hasSuffixComponent.setSuffixComponent(suffix);
        }
    }

    void setSuffixComponent(JComponent component);
}
