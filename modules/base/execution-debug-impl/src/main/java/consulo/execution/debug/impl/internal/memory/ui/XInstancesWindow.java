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
package consulo.execution.debug.impl.internal.memory.ui;

import consulo.disposer.Disposer;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.memory.TypeInfo;
import consulo.execution.debug.memory.XMemoryViewHandler;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * The instances of a type, as values.
 *
 * @author VISTALL
 * @since 2026-11-17
 */
public class XInstancesWindow extends InstancesWindowBase {
    private final XInstancesView myView;

    public XInstancesWindow(XDebugSession session, XMemoryViewHandler handler, TypeInfo type) {
        super(session, type.name());
        myView = new XInstancesView(session, handler, type, this::addWarningMessage);
        Disposer.register(myDisposable, myView);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        myView.setPreferredSize(new Dimension(DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT));
        return myView;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return myView.getInstancesTree();
    }
}
