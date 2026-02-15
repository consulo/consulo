package consulo.externalService.impl.internal.errorReport;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.LabeledComponent;
import consulo.ui.ex.awt.UIUtil;

import javax.swing.*;
import java.awt.*;

/**
 * @author ksafonov
 */
public class CommentsTabForm {
    private LabeledTextComponent myCommentsArea;
    private JPanel myContentPane;
    private LabeledComponent<JTextField> myErrorComponent;

    public CommentsTabForm() {
        createUIComponents();
        myCommentsArea.setTitle(ExternalServiceLocalize.errorDialogCommentPrompt().get());
        myErrorComponent.getComponent().setPreferredSize(new Dimension(IdeErrorsDialog.COMPONENTS_WIDTH, -1));
        myErrorComponent.setLabelLocation(BorderLayout.NORTH);
        myErrorComponent.getComponent().setEditable(false);
        myErrorComponent.getComponent().setBackground(UIUtil.getTextFieldBackground());
        myErrorComponent.getComponent().setBorder(IdeBorderFactory.createBorder());
    }

    public JPanel getContentPane() {
        return myContentPane;
    }

    public void setErrorText(String s) {
        myErrorComponent.getComponent().setText(s);
        myErrorComponent.getComponent().setCaretPosition(0);
    }

    public void setCommentText(String s) {
        LabeledTextComponent.setText(myCommentsArea.getTextComponent(), s, true);
    }

    public JComponent getPreferredFocusedComponent() {
        return myCommentsArea.getTextComponent();
    }

    public void setCommentsTextEnabled(boolean b) {
        myCommentsArea.getTextComponent().setEnabled(b);
    }

    public void addCommentsListener(LabeledTextComponent.TextListener l) {
        myCommentsArea.addCommentsListener(l);
    }

    private void createUIComponents() {
        myContentPane = new JPanel();
        myContentPane.setLayout(new GridLayoutManager(2, 2, JBUI.emptyInsets(), -1, 15));
        myContentPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), null));
        myErrorComponent = new LabeledComponent<>();
        myErrorComponent.setComponent(new JTextField());
        myErrorComponent.setText("&Error message");
        myContentPane.add(
            myErrorComponent,
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_NORTH,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
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
                1,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_VERTICAL,
                1,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
        myCommentsArea = new LabeledTextComponent();
        myContentPane.add(
            myCommentsArea.getContentPane(),
            new GridConstraints(
                1,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
    }
}
