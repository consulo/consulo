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
package consulo.desktop.awt.execution.terminal;

import com.jediterm.terminal.model.StyleState;
import com.jediterm.terminal.model.TerminalTextBuffer;
import com.jediterm.terminal.ui.JediTermWidget;
import com.jediterm.terminal.ui.settings.SettingsProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.ui.terminal.JediTerminalConsole;
import consulo.ui.Component;
import consulo.ui.ex.awt.JBScrollBar;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.swing.*;

public class JBTerminalWidget extends JediTermWidget implements Disposable, JediTerminalConsole {
    public JBTerminalWidget(JBTerminalSystemSettingsProvider settingsProvider) {
        super(settingsProvider);
    }

    @Override
    public String getSessionName() {
        return "";
    }

    @Override
    public BoundedRangeModel getTerminalVerticalScrollModel() {
        return myTerminalPanel.getVerticalScrollModel();
    }

    @Override
    public Component getUIComponent() {
        return TargetAWT.wrap(getComponent());
    }

    @Override
    protected JBTerminalPanel createTerminalPanel(
        SettingsProvider settingsProvider,
        StyleState styleState,
        TerminalTextBuffer textBuffer
    ) {
        JBTerminalPanel panel =
            new JBTerminalPanel((JBTerminalSystemSettingsProvider) settingsProvider, textBuffer, styleState);
        Disposer.register(this, panel);
        return panel;
    }

    @Override
    protected JScrollBar createScrollBar() {
        return new JBScrollBar();
    }

    @Override
    public void dispose() {
    }
}
