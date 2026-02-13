package consulo.externalService.impl.internal.errorReport;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import consulo.application.Application;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * @author ksafonov
 */
public class DisablePluginWarningDialog extends DialogWrapper {
    private final Label myPromptLabel = Label.create(LocalizeValue.localizeTODO("prompt"));
    private final Label myRestartLabel = Label.create(LocalizeValue.localizeTODO("restart"));
    private JPanel myContentPane;

    public static final int DISABLE_EXIT_CODE = OK_EXIT_CODE;
    public static final int DISABLE_AND_RESTART_EXIT_CODE = NEXT_USER_EXIT_CODE;
    private final boolean myRestartCapable;

    @RequiredUIAccess
    public DisablePluginWarningDialog(Project project, String pluginName, boolean hasDependants, boolean restartCapable) {
        super(project, false);
        createUIComponents();
        myRestartCapable = restartCapable;
        myPromptLabel.setText(
            hasDependants
                ? ExternalServiceLocalize.errorDialogDisablePluginPromptDependants(pluginName)
                : ExternalServiceLocalize.errorDialogDisablePluginPrompt(pluginName));
        myRestartLabel.setText(
            restartCapable
                ? ExternalServiceLocalize.errorDialogDisablePluginRestart(Application.get().getName())
                : ExternalServiceLocalize.errorDialogDisablePluginNorestart(Application.get().getName())
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

    private void createUIComponents() {
        myContentPane = new JPanel();
        myContentPane.setLayout(new GridLayoutManager(3, 1, JBUI.emptyInsets(), -1, -1));
        myContentPane.add(
            TargetAWT.to(myPromptLabel),
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        myContentPane.add(
            TargetAWT.to(myRestartLabel),
            new GridConstraints(
                2,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        myContentPane.add(
            new Spacer(),
            new GridConstraints(
                1,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_VERTICAL,
                1,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                null,
                new Dimension(-1, 15),
                null,
                0,
                false
            )
        );
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
