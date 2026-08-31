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
package consulo.desktop.awt.editor.impl.internal;

import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.ui.Component;

import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * The whole editor - content, gutter, scrollbars and decorations - as a real ui component.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
public class DesktopEditorUIComponent extends SwingComponentDelegate<JPanel> {
    private class EditorPanel extends JPanel implements FromSwingComponentWrapper {
        private EditorPanel() {
            super(new BorderLayout());
        }

        @Override
        public Component toUIComponent() {
            return DesktopEditorUIComponent.this;
        }
    }

    @Override
    protected JPanel createComponent() {
        return new EditorPanel();
    }
}
