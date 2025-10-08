/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.idea.codeInsight.intention.impl;

import consulo.language.editor.intention.HighPriorityAction;
import consulo.language.editor.intention.LowPriorityAction;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

/**
 * @author Danila Ponomarenko
 */
public abstract class PriorityLocalQuickFixWrapper implements LocalQuickFix {
  private final LocalQuickFix fix;

  private PriorityLocalQuickFixWrapper(@Nonnull LocalQuickFix fix) {
    this.fix = fix;
  }

  @Nonnull
  @Override
  public LocalizeValue getName() {
    return fix.getName();
  }

  @Override
  public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
    fix.applyFix(project, descriptor);
  }

  private static class HighPriorityLocalQuickFixWrapper extends PriorityLocalQuickFixWrapper implements HighPriorityAction {
    protected HighPriorityLocalQuickFixWrapper(@Nonnull LocalQuickFix fix) {
      super(fix);
    }
  }

  private static class NormalPriorityLocalQuickFixWrapper extends PriorityLocalQuickFixWrapper {
    protected NormalPriorityLocalQuickFixWrapper(@Nonnull LocalQuickFix fix) {
      super(fix);
    }
  }


  private static class LowPriorityLocalQuickFixWrapper extends PriorityLocalQuickFixWrapper implements LowPriorityAction {
    protected LowPriorityLocalQuickFixWrapper(@Nonnull LocalQuickFix fix) {
      super(fix);
    }
  }

  @Nonnull
  public static LocalQuickFix highPriority(@Nonnull LocalQuickFix fix) {
    return new HighPriorityLocalQuickFixWrapper(fix);
  }

  @Nonnull
  public static LocalQuickFix normalPriority(@Nonnull LocalQuickFix fix) {
    return new NormalPriorityLocalQuickFixWrapper(fix);
  }

  @Nonnull
  public static LocalQuickFix lowPriority(@Nonnull LocalQuickFix fix) {
    return new LowPriorityLocalQuickFixWrapper(fix);
  }
}
