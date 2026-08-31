/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.internal.wm.toolWindow;

import consulo.ide.impl.wm.impl.UnifiedToolWindowHeader;
import consulo.ide.impl.wm.impl.UnifiedToolWindowInternalDecorator;
import consulo.project.Project;
import consulo.project.ui.impl.internal.wm.UnifiedToolWindowImpl;
import consulo.project.ui.internal.WindowInfoImpl;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.vaadin.VaadinSizeUtil;

/**
 * @author VISTALL
 * @since 12-Oct-17
 */
public class WebToolWindowInternalDecorator extends UnifiedToolWindowInternalDecorator {
    public static final String HEADER_CLASS_NAME = "web-tool-window-header";

    @RequiredUIAccess
    public WebToolWindowInternalDecorator(
        Project project,
        WindowInfoImpl windowInfo,
        UnifiedToolWindowImpl toolWindow,
        boolean canWorkInDumbMode
    ) {
        super(project, windowInfo, toolWindow);

        UnifiedToolWindowHeader header = getHeader();

        // the north slot of the dock layout does not stretch its child, and the header has to reach the right
        // edge for its action row to sit there
        VaadinSizeUtil.setWidthFull(header.getComponent());

        TargetVaadin.to(header.getComponent()).addClassName(HEADER_CLASS_NAME);
    }
}
