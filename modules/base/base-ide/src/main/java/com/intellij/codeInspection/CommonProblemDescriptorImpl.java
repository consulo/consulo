package com.intellij.codeInspection;

import com.intellij.util.ArrayUtil;
import com.intellij.util.FunctionUtil;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;

/**
 * User: anna
 * Date: 04-Jan-2006
 */
public class CommonProblemDescriptorImpl implements CommonProblemDescriptor {
  private final QuickFix[] myFixes;
  private final String myDescriptionTemplate;

  public CommonProblemDescriptorImpl(final QuickFix[] fixes, @Nonnull final String descriptionTemplate) {
    if (fixes == null) {
      myFixes = null;
    }
    else if (fixes.length == 0) {
      myFixes = QuickFix.EMPTY_ARRAY;
    }
    else {
      // no copy in most cases
      myFixes = ArrayUtil.contains(null, fixes) ? ContainerUtil.mapNotNull(fixes, FunctionUtil.<QuickFix>id(), QuickFix.EMPTY_ARRAY) : fixes;
    }
    myDescriptionTemplate = descriptionTemplate;
  }

  @Override
  @Nonnull
  public String getDescriptionTemplate() {
    return myDescriptionTemplate;
  }

  @Override
  @javax.annotation.Nullable
  public QuickFix[] getFixes() {
    return myFixes;
  }

  public String toString() {
    return myDescriptionTemplate;
  }
}
