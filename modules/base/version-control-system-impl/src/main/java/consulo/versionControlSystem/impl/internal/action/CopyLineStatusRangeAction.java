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
package consulo.versionControlSystem.impl.internal.action;

import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.util.ActionUtil;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.versionControlSystem.internal.LineStatusTrackerI;
import consulo.versionControlSystem.internal.VcsRange;

import java.awt.datatransfer.StringSelection;

/**
 * @author irengrig
 */
public class CopyLineStatusRangeAction extends BaseLineStatusRangeAction {
    public CopyLineStatusRangeAction(LineStatusTrackerI lineStatusTracker, VcsRange range) {
        super(lineStatusTracker, range);
        ActionUtil.copyFrom(this, IdeActions.ACTION_COPY);
    }

    @Override
    public boolean isEnabled() {
        return VcsRange.DELETED == myRange.getType() || VcsRange.MODIFIED == myRange.getType();
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        String content = myLineStatusTracker.getVcsContent(myRange) + "\n";
        CopyPasteManager.getInstance().setContents(new StringSelection(content));
    }
}
