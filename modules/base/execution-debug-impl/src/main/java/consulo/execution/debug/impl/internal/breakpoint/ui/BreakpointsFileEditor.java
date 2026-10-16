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
package consulo.execution.debug.impl.internal.breakpoint.ui;

import consulo.configuration.editor.ConfigurationFileEditor;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.debug.XDebuggerManager;
import consulo.execution.debug.breakpoint.XBreakpoint;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointBase;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointUtil;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public class BreakpointsFileEditor extends ConfigurationFileEditor {
    private final Disposable myUIDisposable = Disposable.newDisposable("BreakpointsFileEditor");

    private @Nullable BreakpointsPanel myPanel;
    private @Nullable Component myComponent;

    public BreakpointsFileEditor(Project project, VirtualFile file) {
        super(project, file);
    }

    @RequiredUIAccess
    private BreakpointsPanel init() {
        BreakpointsPanel panel = myPanel;
        if (panel == null) {
            panel = new BreakpointsPanel(myProject, XBreakpointUtil.collectPanelProviders(), () -> {
                for (BreakpointPanelProvider provider : XBreakpointUtil.collectPanelProviders()) {
                    provider.onDialogClosed(myProject);
                }
            });
            myPanel = panel;
            myComponent = panel.createComponent(myUIDisposable);
        }
        return panel;
    }

    @Override
    public void onUpdateRequestParams(Map<String, String> params) {
        String breakpointId = params.get(BreakpointsEditorProvider.BREAKPOINT_ID);
        if (breakpointId == null) {
            return;
        }

        myProject.getUIAccess().give(() -> {
            for (XBreakpoint<?> breakpoint : XDebuggerManager.getInstance(myProject).getBreakpointManager().getAllBreakpoints()) {
                if (breakpoint instanceof XBreakpointBase<?, ?, ?> base && breakpointId.equals(BreakpointsEditorProvider.breakpointId(base))) {
                    init().requestSelection(breakpoint);
                    return;
                }
            }
        });
    }

    @RequiredUIAccess
    @Override
    public Component getUIComponent() {
        init();
        return myComponent;
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component getPreferredFocusedUIComponent() {
        return init().getPreferredFocusedComponent();
    }

    @Override
    public void dispose() {
        Disposer.dispose(myUIDisposable);
        myPanel = null;
        myComponent = null;
    }
}
