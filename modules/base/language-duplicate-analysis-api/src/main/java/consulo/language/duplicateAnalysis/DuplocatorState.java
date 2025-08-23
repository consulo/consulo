package consulo.language.duplicateAnalysis;

/**
 * @author Eugene.Kudelevsky
 */
public interface DuplocatorState {
  
  int getLowerBound();
  
  int getDiscardCost();

}
