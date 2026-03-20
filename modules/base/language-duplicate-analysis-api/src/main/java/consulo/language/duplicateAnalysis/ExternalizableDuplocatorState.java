package consulo.language.duplicateAnalysis;

/**
 * @author Eugene.Kudelevsky
 */
public interface ExternalizableDuplocatorState extends DuplocatorState {
  boolean distinguishRole(PsiElementRole role);

  boolean distinguishLiterals();
}
