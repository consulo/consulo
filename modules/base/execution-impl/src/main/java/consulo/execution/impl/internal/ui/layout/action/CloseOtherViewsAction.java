/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.execution.impl.internal.ui.layout.action;

import consulo.execution.internal.layout.ViewContext;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;

public class CloseOtherViewsAction extends BaseViewAction {
    @Override
    protected void update(AnActionEvent e, ViewContext context, Content[] content) {
        setEnabled(e, isEnabled(context, content, e.getPlace()));
    }

    @Override
    protected void actionPerformed(AnActionEvent e, ViewContext context, Content[] content) {
        ContentManager manager = context.getContentManager();
        for (Content c : manager.getContents()) {
            if (c != content[0] && c.isCloseable()) {
                manager.removeContent(c, context.isToDisposeRemovedContent());
            }
        }
    }

    public static boolean isEnabled(ViewContext context, Content[] content, String place) {
        if (content.length != 1) {
            return false;
        }
        int closeable = 0;
        for (Content c : context.getContentManager().getContents()) {
            if (c == content[0]) {
                continue;
            }
            if (c.isCloseable()) {
                closeable++;
            }
        }
        return closeable > 0;
    }
}