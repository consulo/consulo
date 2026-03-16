/*
 * Copyright 2013-2020 consulo.io
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
package consulo.project.ui.wm.action;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.internal.ToolWindowManagerEx;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.toolWindow.ToolWindow;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * from kotlin
 */
public class ToolWindowTabRenameAction extends ToolWindowContextMenuActionBase {
    private final String myToolWindowId;
    private final LocalizeValue myLabelText;
    
    private final BiConsumer<Content, String> myOnRenameConsumer;

    public ToolWindowTabRenameAction(String toolWindowId,
                                     LocalizeValue labelText) {
        this(toolWindowId, labelText, Content::setDisplayName);
    }

    public ToolWindowTabRenameAction(String toolWindowId,
                                     LocalizeValue labelText,
                                     BiConsumer<Content, String> onRenameConsumer) {
        myToolWindowId = toolWindowId;
        myLabelText = labelText;
        myOnRenameConsumer = onRenameConsumer;
    }

    @Override
    public void update(AnActionEvent e, ToolWindow toolWindow, @Nullable Content content) {
        String id = toolWindow.getId();
        e.getPresentation().setEnabledAndVisible(e.hasData(Project.KEY) && myToolWindowId.equals(id) && content != null);
    }

    @Override
    public void actionPerformed(AnActionEvent e, ToolWindow toolWindow, @Nullable Content content) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        ToolWindowManagerEx.getInstanceEx(project).doContentRename(e.getDataContext(), toolWindow, content, myLabelText, myOnRenameConsumer);
    }
}
