package com.intellij.diagnostic.errordialog;

import com.intellij.diagnostic.DiagnosticBundle;
import com.intellij.diagnostic.IdeErrorsDialog;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.ui.IdeBorderFactory;
import consulo.application.ui.awt.UIUtil;

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
    myCommentsArea.setTitle(DiagnosticBundle.message("error.dialog.comment.prompt"));
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

  public void addCommentsListener(final LabeledTextComponent.TextListener l) {
    myCommentsArea.addCommentsListener(l);
  }
}
