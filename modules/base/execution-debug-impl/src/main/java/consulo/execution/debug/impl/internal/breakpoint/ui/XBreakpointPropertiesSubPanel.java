/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import consulo.execution.debug.XBreakpointManager;
import consulo.execution.debug.breakpoint.XBreakpointType;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointBase;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

public abstract class XBreakpointPropertiesSubPanel {
    protected Project myProject;
    protected XBreakpointManager myBreakpointManager;
    protected XBreakpointBase myBreakpoint;
    protected XBreakpointType myBreakpointType;

    @RequiredUIAccess
    public void init(Project project, XBreakpointManager breakpointManager, XBreakpointBase breakpoint) {
        myProject = project;
        myBreakpointManager = breakpointManager;
        myBreakpoint = breakpoint;
        myBreakpointType = breakpoint.getType();
    }

    public abstract Component getComponent();

    @RequiredUIAccess
    abstract void loadProperties();

    @RequiredUIAccess
    abstract void saveProperties();

    @RequiredUIAccess
    public boolean lightVariant(boolean showAllOptions) {
        return false;
    }
}
