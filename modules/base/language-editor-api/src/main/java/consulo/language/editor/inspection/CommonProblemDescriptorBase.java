package consulo.language.editor.inspection;

import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Function;

/**
 * User: anna
 * Date: 04-Jan-2006
 */
public class CommonProblemDescriptorBase implements CommonProblemDescriptor {
  private final QuickFix[] myFixes;
  private final String myDescriptionTemplate;

  public CommonProblemDescriptorBase(final QuickFix[] fixes, @Nonnull final String descriptionTemplate) {
    if (fixes == null) {
      myFixes = null;
    }
    else if (fixes.length == 0) {
      myFixes = QuickFix.EMPTY_ARRAY;
    }
    else {
      // no copy in most cases
      myFixes = ArrayUtil.contains(null, fixes) ? ContainerUtil.mapNotNull(fixes, Function.identity(), QuickFix.EMPTY_ARRAY) : fixes;
    }
    myDescriptionTemplate = descriptionTemplate;
  }

  @Override
  @Nonnull
  public String getDescriptionTemplate() {
    return myDescriptionTemplate;
  }

  @Override
  @Nullable
  public QuickFix[] getFixes() {
    return myFixes;
  }

  public String toString() {
    return myDescriptionTemplate;
  }
}
