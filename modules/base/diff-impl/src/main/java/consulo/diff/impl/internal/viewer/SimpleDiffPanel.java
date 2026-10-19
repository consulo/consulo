/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.impl.internal.viewer;

import consulo.dataContext.UiDataProvider;
import consulo.diff.DiffContext;
import consulo.diff.impl.internal.util.UnifiedDiffUtil;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;

public class SimpleDiffPanel extends DiffPanelBase {
    private static final String GOOD_CONTENT = "GoodContent";
    private static final String ERROR_CONTENT = "ErrorContent";

    @RequiredUIAccess
    public SimpleDiffPanel(Component editorPanel, UiDataProvider dataProvider, DiffContext context) {
        super(context.getProject(), dataProvider, context);
        DockLayout centerPanel = DockLayout.create(Space.NONE);
        centerPanel.top(myNotificationsPanel);
        centerPanel.center(editorPanel);

        addCard(GOOD_CONTENT, centerPanel);
        addCard(ERROR_CONTENT, UnifiedDiffUtil.createMessagePanel(LocalizeValue.of("Error")));

        setCurrentCard(GOOD_CONTENT, false);
    }

    //
    // Card layout
    //

    @RequiredUIAccess
    public void setGoodContent() {
        setCurrentCard(GOOD_CONTENT);
    }

    @RequiredUIAccess
    public void setErrorContent() {
        setCurrentCard(ERROR_CONTENT);
    }

    //
    // Misc
    //

    public boolean isGoodContent() {
        return GOOD_CONTENT.equals(myCurrentCard);
    }
}
