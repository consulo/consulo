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
package consulo.project.ui.impl.internal.wm.action;

import consulo.ide.localize.IdeLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.ModifiedInputDetails;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.ex.localize.UILocalize;

/**
 * @author VISTALL
 * @since 2026-11-09
 */
public class CloseContentTabAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    private final ContentManager myManager;
    private final Content myContent;

    public CloseContentTabAction(ContentManager manager, Content content) {
        myManager = manager;
        myContent = content;

        setShortcutSet(() -> KeymapUtil.getActiveKeymapShortcuts(IdeActions.ACTION_CLOSE_ACTIVE_TAB).getShortcuts());
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        if (!canBeClosed()) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        presentation.setEnabledAndVisible(true);

        if (myContent.isPinned()) {
            presentation.setIcon(PlatformIconGroup.actionsPintab());
            presentation.setText(IdeLocalize.actionUnpinTabTooltip());
        }
        else {
            presentation.setIcon(PlatformIconGroup.actionsClose());
            presentation.setText(UILocalize.tabbedPaneCloseTabActionName());
        }
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        if (!canBeClosed()) {
            return;
        }

        if (myContent.isPinned()) {
            myContent.setPinned(false);
            return;
        }

        if (e.getInputDetails() instanceof ModifiedInputDetails details && details.withAlt()) {
            for (Content content : myManager.getContents()) {
                if (content != myContent && content.isCloseable()) {
                    myManager.removeContent(content, true);
                }
            }
            return;
        }

        myManager.removeContent(myContent, true);
    }

    private boolean canBeClosed() {
        return myManager.canCloseContents() && myContent.isCloseable() && myManager.getIndexOfContent(myContent) >= 0;
    }
}
