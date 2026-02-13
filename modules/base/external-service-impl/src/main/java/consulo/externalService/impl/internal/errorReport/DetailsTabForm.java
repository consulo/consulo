package consulo.externalService.impl.internal.errorReport;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awtUnsafe.TargetAWT;
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
    private JComboBox<Developer> myAssigneeComboBox;
    private JPanel myAssigneePanel;

    private long myAssigneeId;
    private boolean myProcessEvents = true;

    public DetailsTabForm(@Nullable Action analyzeAction, boolean internalMode) {
        createUIComponents();
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
        myAssigneeComboBox.addActionListener(new ActionListenerProxy(e -> myAssigneeId = getAssigneeId()));
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

    @RequiredUIAccess
    public void setDevelopers(Collection<Developer> developers) {
        myAssigneeComboBox.setModel(new DefaultComboBoxModel(developers.toArray()));
        updateSelectedDeveloper();
    }

    @RequiredUIAccess
    public void setAssigneeId(long assigneeId) {
        myAssigneeId = assigneeId;
        if (myAssigneeComboBox.getItemCount() > 0) {
            updateSelectedDeveloper();
        }
    }

    @RequiredUIAccess
    private void updateSelectedDeveloper() {
        myProcessEvents = false;

        Integer index = null;
        for (int i = 0, n = myAssigneeComboBox.getItemCount(); i < n; i++) {
            Developer developer = myAssigneeComboBox.getItemAt(i);
            if (ComparatorUtil.equalsNullable(developer.getId(), myAssigneeId)) {
                index = i;
                break;
            }
        }
        setSelectedAssigneeIndex(index);

        myProcessEvents = true;
    }

    @RequiredUIAccess
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

    private void createUIComponents() {
        myContentPane = new JPanel();
        myContentPane.setLayout(new GridLayoutManager(4, 1, JBUI.emptyInsets(), -1, -1));
        myContentPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), null));
        myDetailsHolder = new JPanel();
        myDetailsHolder.setLayout(new GridLayoutManager(1, 1, JBUI.emptyInsets(), -1, -1));
        myContentPane.add(
            myDetailsHolder,
            new GridConstraints(
                0,
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
        JBScrollPane jBScrollPane1 = new JBScrollPane();
        jBScrollPane1.setHorizontalScrollBarPolicy(32);
        myDetailsHolder.add(
            jBScrollPane1,
            new GridConstraints(
                0,
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
        myDetailsPane = new JTextPane();
        myDetailsPane.setEditable(false);
        myDetailsPane.putClientProperty("JEditorPane.honorDisplayProperties", Boolean.TRUE);
        jBScrollPane1.setViewportView(myDetailsPane);
        myAnalyzeStacktraceButton = new JButton();
        myAnalyzeStacktraceButton.setText("Analyze Stacktrace");
        myContentPane.add(
            myAnalyzeStacktraceButton,
            new GridConstraints(
                1,
                0,
                1,
                1,
                GridConstraints.ANCHOR_EAST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        myAssigneePanel = new JPanel();
        myAssigneePanel.setLayout(new GridLayoutManager(1, 2, JBUI.emptyInsets(), -1, -1));
        myContentPane.add(
            myAssigneePanel,
            new GridConstraints(
                3,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_VERTICAL,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
        Label label1 = Label.create(LocalizeValue.localizeTODO("&Assignee:"));
        myAssigneePanel.add(
            TargetAWT.to(label1),
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
        myAssigneeComboBox = new JComboBox<>();
        myAssigneePanel.add(
            myAssigneeComboBox,
            new GridConstraints(
                0,
                1,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
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
                2,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                new Dimension(-1, 150),
                null,
                0,
                false
            )
        );
        label1.setTarget(TargetAWT.wrap(myAssigneeComboBox));
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
