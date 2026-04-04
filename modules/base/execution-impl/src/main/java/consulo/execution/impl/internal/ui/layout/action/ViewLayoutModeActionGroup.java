package consulo.execution.impl.internal.ui.layout.action;

import consulo.application.dumb.DumbAware;
import consulo.execution.ExecutionBundle;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareToggleAction;
import consulo.ui.ex.content.Content;

public final class ViewLayoutModeActionGroup extends DefaultActionGroup implements DumbAware, ViewLayoutModificationAction {

    private final Content myContent;

    public ViewLayoutModeActionGroup(
        Content content,
        CustomContentLayoutOptions customContentLayoutOptions) {
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
    public Content getContent() {
        return myContent;
    }

    public static final class ViewLayoutModeAction extends DumbAwareToggleAction {

        private final CustomContentLayoutOption myOption;

        public ViewLayoutModeAction(
            CustomContentLayoutOption option) {

            myOption = option;
        }

        @Override
        public boolean isDumbAware() {
            return true;
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return myOption.isSelected();
        }

        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            myOption.select();
        }

        @Override
        public void update(AnActionEvent e) {
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
        public String getDisplayName() {
            return ExecutionBundle.message("run.layout.do.not.show.view.option.message");
        }
    }
}
