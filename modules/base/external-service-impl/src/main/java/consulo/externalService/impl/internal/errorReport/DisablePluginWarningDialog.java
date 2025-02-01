package consulo.externalService.impl.internal.errorReport;

import consulo.application.Application;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * @author ksafonov
 */
public class DisablePluginWarningDialog extends DialogWrapper {
    private JLabel myPromptLabel;
    private JLabel myRestartLabel;
    private JPanel myContentPane;

    public static final int DISABLE_EXIT_CODE = OK_EXIT_CODE;
    public static final int DISABLE_AND_RESTART_EXIT_CODE = NEXT_USER_EXIT_CODE;
    private final boolean myRestartCapable;

    public DisablePluginWarningDialog(Project project, String pluginName, boolean hasDependants, boolean restartCapable) {
        super(project, false);
        myRestartCapable = restartCapable;
        myPromptLabel.setText(
            hasDependants
                ? ExternalServiceLocalize.errorDialogDisablePluginPromptDependants(pluginName).get()
                : ExternalServiceLocalize.errorDialogDisablePluginPrompt(pluginName).get());
        myRestartLabel.setText(
            restartCapable
                ? ExternalServiceLocalize.errorDialogDisablePluginRestart(Application.get().getName()).get()
                : ExternalServiceLocalize.errorDialogDisablePluginNorestart(Application.get().getName()).get()
        );

        setTitle(ExternalServiceLocalize.errorDialogDisablePluginTitle());
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        return myContentPane;
    }

    @Nonnull
    @Override
    protected Action[] createActions() {
        if (Platform.current().os().isMac()) {
            if (myRestartCapable) {
                return new Action[]{getCancelAction(), new DisableAction(), new DisableAndRestartAction()};
            }
            else {
                return new Action[]{getCancelAction(), new DisableAction()};
            }
        }
        else {
            if (myRestartCapable) {
                return new Action[]{new DisableAction(), new DisableAndRestartAction(), getCancelAction()};
            }
            else {
                return new Action[]{new DisableAction(), getCancelAction()};
            }
        }
    }

    private class DisableAction extends DialogWrapperAction {
        protected DisableAction() {
            super(ExternalServiceLocalize.errorDialogDisablePluginActionDisable());
        }

        @Override
        protected void doAction(ActionEvent e) {
            close(DISABLE_EXIT_CODE);
        }
    }

    private class DisableAndRestartAction extends DialogWrapperAction {
        protected DisableAndRestartAction() {
            super(ExternalServiceLocalize.errorDialogDisablePluginActionDisableandrestart());
        }

        @Override
        protected void doAction(ActionEvent e) {
            close(DISABLE_AND_RESTART_EXIT_CODE);
        }
    }
}
