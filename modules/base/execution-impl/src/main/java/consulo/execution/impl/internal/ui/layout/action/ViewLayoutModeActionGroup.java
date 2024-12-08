package consulo.execution.impl.internal.ui.layout.action;

import consulo.application.dumb.DumbAware;
import consulo.execution.ExecutionBundle;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareToggleAction;
import consulo.ui.ex.content.Content;
import jakarta.annotation.Nonnull;

public final class ViewLayoutModeActionGroup extends DefaultActionGroup implements DumbAware, ViewLayoutModificationAction {

    private final @Nonnull Content myContent;

    public ViewLayoutModeActionGroup(
        @Nonnull Content content,
        @Nonnull CustomContentLayoutOptions customContentLayoutOptions) {
        super(customContentLayoutOptions.getDisplayName(), true);

        add(new ViewLayoutModeAction(new HideContentLayoutModeOption(content, customContentLayoutOptions)));
        for (CustomContentLayoutOption option : customContentLayoutOptions.getAvailableOptions()) {
            add(new ViewLayoutModeAction(option));
        }

        myContent = content;
    }

    @Override
    public boolean isDumbAware() {
        return true;
    }

    @Override
    public @Nonnull Content getContent() {
        return myContent;
    }

    public static final class ViewLayoutModeAction extends DumbAwareToggleAction {

        private final CustomContentLayoutOption myOption;

        public ViewLayoutModeAction(
            @Nonnull CustomContentLayoutOption option) {

            myOption = option;
        }

        @Override
        public boolean isDumbAware() {
            return true;
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return myOption.isSelected();
        }

        @Override
        public @Nonnull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }

        @Override
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            myOption.select();
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            super.update(e);
            e.getPresentation().setEnabled(myOption.isEnabled());
            e.getPresentation().setText(myOption.getDisplayName());
        }
    }

    public static final class HideContentLayoutModeOption implements CustomContentLayoutOption {

        private final Content myContent;
        private final CustomContentLayoutOptions myOptions;

        public HideContentLayoutModeOption(Content content, CustomContentLayoutOptions options) {
            myContent = content;
            myOptions = options;
        }

        @Override
        public boolean isSelected() {
            return myOptions.isHidden();
        }

        @Override
        public void select() {
            myOptions.onHide();
        }

        @Override
        public boolean isEnabled() {
            return myOptions.isHideOptionVisible();
        }

        @Override
        public @Nonnull String getDisplayName() {
            return ExecutionBundle.message("run.layout.do.not.show.view.option.message");
        }
    }
}
