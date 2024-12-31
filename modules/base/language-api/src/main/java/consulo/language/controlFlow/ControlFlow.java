package consulo.language.controlFlow;

import jakarta.annotation.Nonnull;

/**
 * @author oleg
 */
public interface ControlFlow {
    @Nonnull
    Instruction[] getInstructions();
}
