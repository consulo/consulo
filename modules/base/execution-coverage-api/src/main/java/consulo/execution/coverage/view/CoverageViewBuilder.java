package consulo.execution.coverage.view;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.execution.coverage.localize.CoverageLocalize;
import consulo.project.Project;
import consulo.project.ui.view.commander.AbstractListBuilder;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.table.JBTable;
import consulo.ui.ex.awt.util.TableUtil;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.AlphaComparator;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatusListener;
import consulo.virtualFileSystem.status.FileStatusManager;

import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author anna
 * @since 2012-01-02
 */
public class CoverageViewBuilder extends AbstractListBuilder {
    private final JBTable myTable;
    private final FileStatusListener myFileStatusListener;
    private CoverageViewExtension myCoverageViewExtension;

    CoverageViewBuilder(
        final Project project,
        JList list,
        Model model,
        AbstractTreeStructure treeStructure,
        final JBTable table
    ) {
        super(project, list, model, treeStructure, AlphaComparator.INSTANCE, false);
        myTable = table;
        ProgressManager.getInstance().run(new Task.Backgroundable(project, CoverageLocalize.coverageViewBuildingCoverageReport()) {
            @Override
            public void run(@Nonnull ProgressIndicator indicator) {
                buildRoot();
            }

            @RequiredUIAccess
            @Override
            public void onSuccess() {
                ensureSelectionExist();
                updateParentTitle();
            }
        });
        myFileStatusListener = new FileStatusListener() {
            @Override
            public void fileStatusesChanged() {
                table.repaint();
            }

            @Override
            public void fileStatusChanged(@Nonnull VirtualFile virtualFile) {
                table.repaint();
            }
        };
        myCoverageViewExtension = ((CoverageViewTreeStructure) myTreeStructure).myData
            .getCoverageEngine().createCoverageViewExtension(myProject, ((CoverageViewTreeStructure) myTreeStructure).myData,
                ((CoverageViewTreeStructure) myTreeStructure).myStateBean
            );
        FileStatusManager.getInstance(myProject).addFileStatusListener(myFileStatusListener);
    }

    @Override
    public void dispose() {
        FileStatusManager.getInstance(myProject).removeFileStatusListener(myFileStatusListener);
        super.dispose();
    }

    @Override
    protected boolean shouldEnterSingleTopLevelElement(Object rootChild) {
        return false;
    }

    @Override
    protected boolean shouldAddTopElement() {
        return false;
    }

    @Override
    protected boolean nodeIsAcceptableForElement(AbstractTreeNode node, Object element) {
        return Comparing.equal(node.getValue(), element);
    }

    @Override
    protected List<AbstractTreeNode> getAllAcceptableNodes(Object[] childElements, VirtualFile file) {
        List<AbstractTreeNode> result = new ArrayList<>();

        for (Object childElement1 : childElements) {
            CoverageListNode childElement = (CoverageListNode) childElement1;
            if (childElement.contains(file)) {
                result.add(childElement);
            }
        }

        return result;
    }

    @Override
    protected void updateParentTitle() {
        if (myParentTitle == null) {
            return;
        }

        Object rootElement = myTreeStructure.getRootElement();
        AbstractTreeNode node = getParentNode();
        if (node == null) {
            node = (AbstractTreeNode) rootElement;
        }

        if (node instanceof CoverageListRootNode) {
            myParentTitle.setText(myCoverageViewExtension.getSummaryForRootNode(node));
        }
        else {
            myParentTitle.setText(myCoverageViewExtension.getSummaryForNode(node));
        }
    }

    @Override
    public Object getSelectedValue() {
        int row = myTable.getSelectedRow();
        if (row == -1) {
            return null;
        }
        return myModel.getElementAt(myTable.convertRowIndexToModel(row));
    }

    @Override
    protected void ensureSelectionExist() {
        TableUtil.ensureSelectionExists(myTable);
    }

    @Override
    protected void selectItem(int i) {
        TableUtil.selectRows(myTable, new int[]{myTable.convertRowIndexToView(i)});
        TableUtil.scrollSelectionToVisible(myTable);
    }

    @RequiredReadAction
    public boolean canSelect(VirtualFile file) {
        return myCoverageViewExtension.canSelectInCoverageView(file);
    }

    @RequiredUIAccess
    public void select(Object object) {
        selectElement(myCoverageViewExtension.getElementToSelect(object), myCoverageViewExtension.getVirtualFile(object));
    }
}
