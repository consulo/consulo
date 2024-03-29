package consulo.ide.impl.idea.codeInsight.codeFragment;

import java.util.*;

/**
 * @author oleg
 */
public class CodeFragment {
  private final List<String> inputVariables;
  private final List<String> outputVariables;
  private final boolean returnInstructionInside;

  public CodeFragment(final Set<String> input, final Set<String> output, final boolean returnInside) {
    inputVariables = new ArrayList<String>(input);
    Collections.sort(inputVariables);
    outputVariables = new ArrayList<String>(output);
    Collections.sort(outputVariables);
    returnInstructionInside = returnInside;
  }

  public Collection<String> getInputVariables() {
    return inputVariables;
  }

  public Collection<String> getOutputVariables() {
    return outputVariables;
  }

  public boolean isReturnInstructionInside() {
    return returnInstructionInside;
  }
}
