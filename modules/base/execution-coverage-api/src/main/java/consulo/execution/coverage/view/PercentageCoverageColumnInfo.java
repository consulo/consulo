package consulo.execution.coverage.view;

import consulo.execution.coverage.CoverageEngine;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.CoverageViewManager;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.ex.awt.ColumnInfo;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.util.lang.Comparing;

import java.util.Comparator;

/**
 * User: anna
 * Date: 1/9/12
 */
public class PercentageCoverageColumnInfo extends ColumnInfo<NodeDescriptor, String> {
    private final int myColumnIdx;
    private final Comparator<NodeDescriptor> myComparator;
    private final CoverageSuitesBundle mySuitesBundle;
    private final CoverageViewManager.StateBean myStateBean;

    public PercentageCoverageColumnInfo(
        int columnIdx,
        String name,
        final CoverageSuitesBundle suitesBundle,
        CoverageViewManager.StateBean stateBean
    ) {
        super(name);
        this.myColumnIdx = columnIdx;
        myComparator = new Comparator<NodeDescriptor>() {
            @Override
            public int compare(NodeDescriptor o1, NodeDescriptor o2) {
                final String val1 = valueOf(o1);
                final String val2 = valueOf(o2);
                if (val1 != null && val2 != null) {
                    final int percentageIndex1 = val1.indexOf('%');
                    final int percentageIndex2 = val2.indexOf('%');
                    if (percentageIndex1 > -1 && percentageIndex2 > -1) {
                        final String percentage1 = val1.substring(0, percentageIndex1);
                        final String percentage2 = val2.substring(0, percentageIndex2);
                        final int compare = Comparing.compare(Integer.parseInt(percentage1), Integer.parseInt(percentage2));
                        if (compare == 0) {
                            final int total1 = val1.indexOf('/');
                            final int total2 = val2.indexOf('/');
                            if (total1 > -1 && total2 > -1) {
                                final int r1 = val1.indexOf(')', total1);
                                final int r2 = val2.indexOf(')', total2);
                                if (r1 > -1 && r2 > -1) {
                                    return Integer.parseInt(val2.substring(total2 + 1, r2)) - Integer.parseInt(val1.substring(
                                        total1 + 1,
                                        r1
                                    ));
                                }
                            }
                        }
                        return compare;
                    }
                    if (percentageIndex1 > -1) {
                        return 1;
                    }
                    if (percentageIndex2 > -1) {
                        return -1;
                    }
                }
                return Comparing.compare(val1, val2);
            }
        };
        mySuitesBundle = suitesBundle;
        myStateBean = stateBean;
    }

    @Override
    public String valueOf(NodeDescriptor node) {
        final CoverageEngine coverageEngine = mySuitesBundle.getCoverageEngine();
        final Project project = ((AbstractTreeNode) node).getProject();
        return coverageEngine.createCoverageViewExtension(project, mySuitesBundle, myStateBean)
            .getPercentage(myColumnIdx, (AbstractTreeNode) node);
    }

    @Override
    public Comparator<NodeDescriptor> getComparator() {
        return myComparator;
    }
}
