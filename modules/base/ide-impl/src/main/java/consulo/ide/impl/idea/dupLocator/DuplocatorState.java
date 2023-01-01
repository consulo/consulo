package consulo.ide.impl.idea.dupLocator;

/**
 * @author Eugene.Kudelevsky
 */
public interface DuplocatorState {
  
  int getLowerBound();
  
  int getDiscardCost();

}
