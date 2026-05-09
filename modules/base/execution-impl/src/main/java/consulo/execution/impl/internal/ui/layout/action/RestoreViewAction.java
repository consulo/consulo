// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package consulo.execution.impl.internal.ui.layout.action;

import consulo.execution.internal.layout.RunnerContentUi;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareToggleAction;
import consulo.ui.ex.content.Content;

import java.util.Objects;

public final class RestoreViewAction extends DumbAwareToggleAction implements ViewLayoutModificationAction {

    private final Content myContent;
    private final ContentLayoutStateSettings myLayoutSettings;

    public RestoreViewAction(RunnerContentUi ui, Content content) {
        this(content, new DefaultContentStateSettings(ui, content));
    }

    public RestoreViewAction(Content content, ContentLayoutStateSettings layoutSettings) {
        myContent = content;
        myLayoutSettings = layoutSettings;
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        return myLayoutSettings.isSelected();
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        myLayoutSettings.setSelected(state);
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        e.getPresentation().setText(myLayoutSettings.getDisplayName(), false);
        e.getPresentation().setEnabled(myLayoutSettings.isEnabled());
    }

    public Content getContent() {
        return myContent;
    }

    private static final class DefaultContentStateSettings implements ContentLayoutStateSettings {

        private final RunnerContentUi myUi;
        private final Content myContent;

        public DefaultContentStateSettings(RunnerContentUi ui,
                                           Content content) {
            myUi = ui;
            myContent = content;
        }

        @Override
        public boolean isSelected() {
            return myContent.isValid() && Objects.requireNonNull(myContent.getManager()).getIndexOfContent(myContent) != -1;
        }

        @Override
        public void setSelected(boolean state) {
            if (state) {
                myUi.restore(myContent);
                myUi.select(myContent, true);
            }
            else {
                myUi.minimize(myContent, null);
            }
        }

        @Override
        public void restore() {
            setSelected(true);
        }

        @Override
        public String getDisplayName() {
            return myContent.getDisplayName();
        }

        @Override
        public boolean isEnabled() {
            return !isSelected() || myUi.getContentManager().getContents().length > 1;
        }
    }
}
