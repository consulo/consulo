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
package consulo.diff.impl.internal.fragment;

import consulo.dataContext.UiDataProvider;
import consulo.diff.DiffContext;
import consulo.diff.comparison.DiffTooBigException;
import consulo.diff.impl.internal.util.UnifiedDiffUtil;
import consulo.diff.impl.internal.viewer.DiffPanelBase;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.VerticalAlignment;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

public class UnifiedDiffPanel extends DiffPanelBase {
    private static final String GOOD_CONTENT = "GoodContent";
    private static final String LOADING_CONTENT = "LoadingContent";
    private static final String TOO_BIG_CONTENT = "TooBigContent";
    private static final String OPERATION_CANCELED_CONTENT = "OperationCanceledContent";
    private static final String ERROR_CONTENT = "ErrorContent";

    @RequiredUIAccess
    public UnifiedDiffPanel(
        @Nullable Project project,
        UnifiedContentPanel content,
        UiDataProvider provider,
        DiffContext context
    ) {
        super(project, provider, context);
        DockLayout centerPanel = DockLayout.create(Space.NONE);
        centerPanel.top(myNotificationsPanel);
        centerPanel.center(content.getComponent());

        Label busyIcon = Label.create();
        busyIcon.setImage(Image.busy());
        VerticalLayout loadingPanel = VerticalLayout.create(Space.NONE, HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
        loadingPanel.add(busyIcon);

        addCard(GOOD_CONTENT, centerPanel);
        addCard(LOADING_CONTENT, loadingPanel);
        addCard(TOO_BIG_CONTENT, UnifiedDiffUtil.createMessagePanel(LocalizeValue.of("Can not calculate diff. " + DiffTooBigException.MESSAGE)));
        addCard(OPERATION_CANCELED_CONTENT, UnifiedDiffUtil.createMessagePanel(LocalizeValue.of("Can not calculate diff. Operation canceled.")));
        addCard(ERROR_CONTENT, UnifiedDiffUtil.createMessagePanel(LocalizeValue.of("Error")));

        setCurrentCard(LOADING_CONTENT, false);
    }

    //
    // Card layout
    //

    @RequiredUIAccess
    public void setLoadingContent() {
        setCurrentCard(LOADING_CONTENT);
    }

    @RequiredUIAccess
    public void setGoodContent() {
        setCurrentCard(GOOD_CONTENT);
    }

    @RequiredUIAccess
    public void setTooBigContent() {
        setCurrentCard(TOO_BIG_CONTENT);
    }

    @RequiredUIAccess
    public void setOperationCanceledContent() {
        setCurrentCard(OPERATION_CANCELED_CONTENT);
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
