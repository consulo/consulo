package consulo.externalService.impl.internal.errorReport;

import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.ui.ex.awt.IdeBorderFactory;
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
}
