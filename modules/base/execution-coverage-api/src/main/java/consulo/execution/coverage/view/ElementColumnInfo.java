package consulo.execution.coverage.view;

import consulo.ui.ex.tree.AlphaComparator;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.awt.ColumnInfo;

import java.util.Comparator;

/**
 * User: anna
 * Date: 1/9/12
 */
public class ElementColumnInfo extends ColumnInfo<NodeDescriptor, String> {
    public ElementColumnInfo() {
        super("Element");
    }

    @Override
    public Comparator<NodeDescriptor> getComparator() {
        return AlphaComparator.INSTANCE;
    }

    @Override
    public String valueOf(NodeDescriptor node) {
        return node.toString();
    }
}
