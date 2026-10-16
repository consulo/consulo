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

import consulo.annotation.component.ExtensionImpl;
import consulo.configuration.editor.ConfigurationFileEditor;
import consulo.configuration.editor.ConfigurationFileEditorProvider;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointBase;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;

@ExtensionImpl
public class BreakpointsEditorProvider implements ConfigurationFileEditorProvider {
    public static final String BREAKPOINT_ID = "BREAKPOINT_ID";

    public static String breakpointId(XBreakpointBase<?, ?, ?> breakpoint) {
        return breakpoint.getType().getId() + "#" + breakpoint.getTimeStamp();
    }
    @Override
    public String getId() {
        return "breakpoints";
    }

    @Override
    public Image getIcon() {
        return ExecutionDebugIconGroup.actionViewbreakpoints();
    }

    @Override
    public LocalizeValue getName() {
        return XDebuggerLocalize.xbreakpointsDialogTitle();
    }

    @Override
    public ConfigurationFileEditor createEditor(Project project, VirtualFile file) {
        return new BreakpointsFileEditor(project, file);
    }
}
