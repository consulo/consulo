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
package consulo.language.editor.inspection;

import consulo.project.Project;

import jakarta.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
public abstract class LocalQuickFixBase implements LocalQuickFix {
  private final String myName;
  private final String myFamilyName;

  /**
   *
   * @param name the name of the quick fix
   */
  protected LocalQuickFixBase(@Nonnull String name) {
    this(name, name);
  }

  /**
   *
   * @param name the name of the quick fix
   * @param familyName text to appear in "Apply Fix" popup when multiple Quick Fixes exist (in the results of batch code inspection). For example,
   *        if the name of the quickfix is "Create template &lt;filename&gt", the return value of getFamilyName() should be "Create template".
   *        If the name of the quickfix does not depend on a specific element, simply return getName().
   */
  protected LocalQuickFixBase(@Nonnull String name, @Nonnull String familyName) {
    myName = name;
    myFamilyName = familyName;
  }

  @Nonnull
  @Override
  public String getName() {
    return myName;
  }

  @Nonnull
  @Override
  public String getFamilyName() {
    return myFamilyName;
  }

  @Override
  public abstract void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor);
}
