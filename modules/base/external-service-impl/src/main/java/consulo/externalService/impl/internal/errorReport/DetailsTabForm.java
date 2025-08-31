package consulo.externalService.impl.internal.errorReport;

import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.ui.ex.awt.ComboboxSpeedSearch;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.lang.ComparatorUtil;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

/**
 * @author ksafonov
 */
public class DetailsTabForm {
    private JTextPane myDetailsPane;
    private JPanel myContentPane;
    private LabeledTextComponent myCommentsArea;
    private JPanel myDetailsHolder;
    private JButton myAnalyzeStacktraceButton;
    private JComboBox myAssigneeComboBox;
    private JPanel myAssigneePanel;

    private long myAssigneeId;
    private boolean myProcessEvents = true;

    public DetailsTabForm(@Nullable Action analyzeAction, boolean internalMode) {
        myCommentsArea.setTitle(ExternalServiceLocalize.errorDialogCommentPrompt().get());
        myCommentsArea.setLabelPosition(BorderLayout.NORTH);
        myDetailsPane.setBackground(UIUtil.getTextFieldBackground());
        myDetailsPane.setPreferredSize(new Dimension(IdeErrorsDialog.COMPONENTS_WIDTH, internalMode ? 500 : 205));
        myDetailsHolder.setBorder(IdeBorderFactory.createBorder());
        if (analyzeAction != null) {
            myAnalyzeStacktraceButton.setAction(analyzeAction);
        }
        else {
            myAnalyzeStacktraceButton.setVisible(false);
        }
        myAssigneeComboBox.setRenderer(new DeveloperRenderer(myAssigneeComboBox.getRenderer()));
        myAssigneeComboBox.setPrototypeDisplayValue(new Developer(0, "Here Goes Some Very Long String"));
        myAssigneeComboBox.addActionListener(new ActionListenerProxy(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                myAssigneeId = getAssigneeId();
            }
        }));
        new ComboboxSpeedSearch(myAssigneeComboBox) {
            @Override
            protected String getElementText(Object element) {
                return element == null ? "" : ((Developer) element).getSearchableText();
            }
        };
    }

    public void setCommentsAreaVisible(boolean b) {
        myCommentsArea.getContentPane().setVisible(b);
    }

    public void setDetailsText(String s) {
        LabeledTextComponent.setText(myDetailsPane, s, false);
    }

    public void setCommentsText(String s) {
        LabeledTextComponent.setText(myCommentsArea.getTextComponent(), s, true);
    }

    public JPanel getContentPane() {
        return myContentPane;
    }

    public JComponent getPreferredFocusedComponent() {
        if (myCommentsArea.getContentPane().isVisible()) {
            return myCommentsArea.getTextComponent();
        }
        return null;
    }

    public void setCommentsTextEnabled(boolean b) {
        if (myCommentsArea.getContentPane().isVisible()) {
            myCommentsArea.getTextComponent().setEnabled(b);
        }
    }

    public void addCommentsListener(LabeledTextComponent.TextListener l) {
        myCommentsArea.addCommentsListener(l);
    }

    public void setAssigneeVisible(boolean visible) {
        myAssigneePanel.setVisible(visible);
    }

    public void setDevelopers(Collection<Developer> developers) {
        myAssigneeComboBox.setModel(new DefaultComboBoxModel(developers.toArray()));
        updateSelectedDeveloper();
    }

    public void setAssigneeId(long assigneeId) {
        myAssigneeId = assigneeId;
        if (myAssigneeComboBox.getItemCount() > 0) {
            updateSelectedDeveloper();
        }
    }

    private void updateSelectedDeveloper() {
        myProcessEvents = false;

        Integer index = null;
        for (int i = 0; i < myAssigneeComboBox.getItemCount(); i++) {
            Developer developer = (Developer) myAssigneeComboBox.getItemAt(i);
            if (ComparatorUtil.equalsNullable(developer.getId(), myAssigneeId)) {
                index = i;
                break;
            }
        }
        setSelectedAssigneeIndex(index);

        myProcessEvents = true;
    }

    private void setSelectedAssigneeIndex(Integer index) {
        if (index == null) {
            myAssigneeComboBox.setSelectedItem(null);
        }
        else {
            myAssigneeComboBox.setSelectedIndex(index);
        }
    }

    public long getAssigneeId() {
        Developer assignee = (Developer) myAssigneeComboBox.getSelectedItem();
        return assignee == null ? 0 : assignee.getId();
    }

    public void addAssigneeListener(ActionListener listener) {
        myAssigneeComboBox.addActionListener(new ActionListenerProxy(listener));
    }

    private class ActionListenerProxy implements ActionListener {
        private final ActionListener myDelegate;

        public ActionListenerProxy(ActionListener delegate) {
            myDelegate = delegate;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (myProcessEvents) {
                myDelegate.actionPerformed(e);
            }
        }
    }
}
