/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.project.ui.impl.internal.wm.action;

import consulo.annotation.component.ActionImpl;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.toolWindow.ToolWindow;
import jakarta.inject.Inject;

import javax.swing.*;

@ActionImpl(id = ResizeToolWindowUpAction.ID)
public class ResizeToolWindowUpAction extends ResizeToolWindowAction {
    public static final String ID = "ResizeToolWindowUp";

    @Inject
    public ResizeToolWindowUpAction() {
    }

    public ResizeToolWindowUpAction(ToolWindow toolWindow, JComponent c) {
        super(toolWindow, ID, c);
    }

    @Override
    protected void update(AnActionEvent event, ToolWindow window, ToolWindowManager mgr) {
        event.getPresentation().setEnabled(window.getAnchor().isHorizontal());
    }

    @Override
    protected void actionPerformed(AnActionEvent e, ToolWindow wnd, ToolWindowManager mgr) {
        stretch(wnd, false, true);
    }
}