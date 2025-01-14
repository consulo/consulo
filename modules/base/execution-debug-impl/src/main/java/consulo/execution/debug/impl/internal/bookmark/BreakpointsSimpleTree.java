/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.bookmark;

import consulo.execution.debug.impl.internal.breakpoint.ui.tree.BreakpointItemsTreeController;
import consulo.execution.debug.impl.internal.breakpoint.ui.tree.BreakpointsTreeCellRenderer;
import consulo.project.Project;
import consulo.ui.ex.awt.tree.Tree;

public class BreakpointsSimpleTree extends Tree {
    public BreakpointsSimpleTree(Project project, BreakpointItemsTreeController controller) {
        super(controller.getRoot());
        setCellRenderer(new BreakpointsTreeCellRenderer.BreakpointsSimpleTreeCellRenderer(project));
        setRootVisible(false);
    }
}
