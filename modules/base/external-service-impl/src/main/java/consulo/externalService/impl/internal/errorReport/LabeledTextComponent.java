package consulo.externalService.impl.internal.errorReport;

import consulo.ui.ex.awt.LabeledComponent;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.UIUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;

/**
 * @author ksafonov
 */
public class LabeledTextComponent {
  public interface TextListener {
    void textChanged(String newText);
  }

  private LabeledComponent<JPanel> myComponent;
  private JPanel myContentPane;

  private final JTextPane myTextPane;

  public LabeledTextComponent() {
    myTextPane = new JTextPane();

    myComponent.setLabelLocation(BorderLayout.NORTH);
    myComponent.getLabel().setMinimumSize(new Dimension(0, -1));
    myComponent.getComponent().setLayout(new BorderLayout());
    myTextPane.setBackground(UIUtil.getTextFieldBackground());
    myComponent.getComponent().add(new JBScrollPane(myTextPane));
    myComponent.getComponent().setBorder(IdeBorderFactory.createBorder());
  }

  public void setLabelPosition(String position) {
    myComponent.setLabelLocation(position);
  }

  public void setTitle(String title) {
    myComponent.setText(title);
  }

  public JPanel getContentPane() {
    return myContentPane;
  }

  public JTextPane getTextComponent() {
    return myTextPane;
  }

  static void setText(JTextPane pane, String text, boolean caretToTheEnd) {
    pane.setText(text);
    if (text != null && !caretToTheEnd && pane.getCaret() != null) {
      // Upon some strange circumstances caret may be missing from the text component making the following line fail with NPE.
      pane.setCaretPosition(0);
    }
  }

  public void addCommentsListener(final TextListener l) {
    myTextPane.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        l.textChanged(myTextPane.getText());
      }
    });
  }

}
