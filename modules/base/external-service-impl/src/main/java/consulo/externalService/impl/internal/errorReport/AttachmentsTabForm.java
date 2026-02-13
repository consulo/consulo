package consulo.externalService.impl.internal.errorReport;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.logging.attachment.Attachment;
import consulo.proxy.EventDispatcher;
import consulo.ui.Label;
import consulo.ui.ex.awt.ColumnInfo;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.table.ListTableModel;
import consulo.ui.ex.awt.table.TableView;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;

/**
 * @author ksafonov
 */
public class AttachmentsTabForm {
    private JPanel myContentPane;
    private TableView<Attachment> myTable;
    private LabeledTextComponent myFileTextArea;
    private final EventDispatcher<ChangeListener> myInclusionEventDispatcher = EventDispatcher.create(ChangeListener.class);

    private final ColumnInfo<Attachment, Boolean> ENABLED_COLUMN =
        new ColumnInfo<Attachment, Boolean>(ExternalServiceLocalize.errorDialogAttachmentIncludeColumnTitle().get()) {
            @Override
            public Boolean valueOf(Attachment attachment) {
                return attachment.isIncluded();
            }

            @Override
            public Class getColumnClass() {
                return Boolean.class;
            }

            @Override
            public int getWidth(JTable table) {
                return 50;
            }

            @Override
            public boolean isCellEditable(Attachment attachment) {
                return true;
            }

            @Override
            public void setValue(Attachment attachment, Boolean value) {
                attachment.setIncluded(value);
                myInclusionEventDispatcher.getMulticaster().stateChanged(new ChangeEvent(attachment));
            }
        };

    private static final ColumnInfo<Attachment, String> PATH_COLUMN =
        new ColumnInfo<Attachment, String>(ExternalServiceLocalize.errorDialogAttachmentPathColumnTitle().get()) {
            @Override
            public String valueOf(Attachment attachment) {
                return attachment.getPath();
            }
        };

    public AttachmentsTabForm() {
        createUIComponents();
        myFileTextArea.getTextComponent().setEditable(false);
        myFileTextArea.setTitle(ExternalServiceLocalize.errorDialogFilecontentTitle().get());
        myTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            Attachment selection = myTable.getSelectedObject();
            if (selection != null) {
                LabeledTextComponent.setText(myFileTextArea.getTextComponent(), selection.getDisplayText(), true);
            }
            else {
                LabeledTextComponent.setText(myFileTextArea.getTextComponent(), null, true);
            }
        });
        myTable.registerKeyboardAction(
            e -> {
                int[] selectedRows = myTable.getSelectedRows();
                boolean aggregateValue = true;
                for (int selectedRow : selectedRows) {
                    if (selectedRow < 0 || !myTable.isCellEditable(selectedRow, 0)) {
                        return;
                    }
                    Boolean value = (Boolean) myTable.getValueAt(selectedRow, 0);
                    aggregateValue &= value == null || value;
                }
                for (int selectedRow : selectedRows) {
                    myTable.setValueAt(aggregateValue ? Boolean.FALSE : Boolean.TRUE, selectedRow, 0);
                }
                myTable.repaint();
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
            JComponent.WHEN_FOCUSED
        );
    }

    public JComponent getPreferredFocusedComponent() {
        return myTable;
    }

    public void setAttachments(List<Attachment> attachments) {
        myTable.setModelAndUpdateColumns(new ListTableModel<>(new ColumnInfo[]{ENABLED_COLUMN, PATH_COLUMN}, attachments, 1));
        myTable.setBorder(IdeBorderFactory.createBorder());
        myTable.setSelection(Collections.singletonList(attachments.get(0)));
    }

    public JPanel getContentPane() {
        return myContentPane;
    }

    public void addInclusionListener(ChangeListener listener) {
        myInclusionEventDispatcher.addListener(listener);
    }

    public void selectFirstIncludedAttachment() {
        List items = ((ListTableModel) myTable.getModel()).getItems();
        for (Object item : items) {
            if (((Attachment) item).isIncluded()) {
                myTable.setSelection(Collections.singleton((Attachment) item));
                break;
            }
        }
    }

    private void createUIComponents() {
        myContentPane = new JPanel();
        myContentPane.setLayout(new GridLayoutManager(3, 1, JBUI.insets(5), -1, 5));
        myContentPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null));
        JBScrollPane jBScrollPane1 = new JBScrollPane();
        myContentPane.add(
            jBScrollPane1,
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
        myTable = new TableView<>();
        jBScrollPane1.setViewportView(myTable);
        myFileTextArea = new LabeledTextComponent();
        myContentPane.add(
            myFileTextArea.getContentPane(),
            new GridConstraints(
                2,
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
        Label label1 = Label.create(
            "<html>" +
                "These files will be attached to the bug report. We recommend to include all the files providing maximum information.<br/>" +
                "<b>Note:</b> all the data you send will be kept private." +
                "</html>"
        );
        myContentPane.add(
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
    }
}
