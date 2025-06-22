package consulo.ide.impl.idea.ide.favoritesTreeView;

import consulo.bookmark.ui.view.FavoritesTreeNodeDescriptor;
import consulo.ide.impl.idea.ide.projectView.impl.GroupByTypeComparator;
import consulo.ui.ex.tree.NodeDescriptor;

/**
 * @author anna
 * @author Konstantin Bulenkov
 * @since 2007-04-05
 */
final class FavoriteComparator extends GroupByTypeComparator {
  public FavoriteComparator() {
    super(null, FavoritesViewTreeBuilder.ID);
  }

  @Override
  public int compare(NodeDescriptor d1, NodeDescriptor d2) {
    if (d1 instanceof FavoritesTreeNodeDescriptor && d2 instanceof FavoritesTreeNodeDescriptor) {
      d1 = ((FavoritesTreeNodeDescriptor)d1).getElement();
      d2 = ((FavoritesTreeNodeDescriptor)d2).getElement();
    }
    return super.compare(d1, d2);
  }
}