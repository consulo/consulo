package consulo.execution.coverage.view;

import consulo.project.ui.view.tree.ProjectViewNodeDecorator;
import consulo.execution.coverage.CoverageDataManager;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;

/**
 * @author Roman.Chernyatchik
 */
public abstract class AbstractCoverageProjectViewNodeDecorator implements ProjectViewNodeDecorator {
    private final CoverageDataManager myCoverageDataManager;

    public AbstractCoverageProjectViewNodeDecorator(final CoverageDataManager coverageDataManager) {
        myCoverageDataManager = coverageDataManager;
    }

    protected CoverageDataManager getCoverageDataManager() {
        return myCoverageDataManager;
    }

    protected static void appendCoverageInfo(ColoredTreeCellRenderer cellRenderer, String coverageInfo) {
        if (coverageInfo != null) {
            cellRenderer.append(" (" + coverageInfo + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
        }
    }
}
