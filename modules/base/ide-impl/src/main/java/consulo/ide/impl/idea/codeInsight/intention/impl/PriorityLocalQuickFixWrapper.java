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

/**
 * @author Danila Ponomarenko
 */
public abstract class PriorityLocalQuickFixWrapper implements LocalQuickFix {
  private final LocalQuickFix fix;

  private PriorityLocalQuickFixWrapper(LocalQuickFix fix) {
    this.fix = fix;
  }

  
  @Override
  public LocalizeValue getName() {
    return fix.getName();
  }

  @Override
  public void applyFix(Project project, ProblemDescriptor descriptor) {
    fix.applyFix(project, descriptor);
  }

  private static class HighPriorityLocalQuickFixWrapper extends PriorityLocalQuickFixWrapper implements HighPriorityAction {
    protected HighPriorityLocalQuickFixWrapper(LocalQuickFix fix) {
      super(fix);
    }
  }

  private static class NormalPriorityLocalQuickFixWrapper extends PriorityLocalQuickFixWrapper {
    protected NormalPriorityLocalQuickFixWrapper(LocalQuickFix fix) {
      super(fix);
    }
  }


  private static class LowPriorityLocalQuickFixWrapper extends PriorityLocalQuickFixWrapper implements LowPriorityAction {
    protected LowPriorityLocalQuickFixWrapper(LocalQuickFix fix) {
      super(fix);
    }
  }

  
  public static LocalQuickFix highPriority(LocalQuickFix fix) {
    return new HighPriorityLocalQuickFixWrapper(fix);
  }

  
  public static LocalQuickFix normalPriority(LocalQuickFix fix) {
    return new NormalPriorityLocalQuickFixWrapper(fix);
  }

  
  public static LocalQuickFix lowPriority(LocalQuickFix fix) {
    return new LowPriorityLocalQuickFixWrapper(fix);
  }
}
