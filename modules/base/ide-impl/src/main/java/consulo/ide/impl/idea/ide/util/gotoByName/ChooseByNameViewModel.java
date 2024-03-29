// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.util.gotoByName;

import consulo.application.progress.ProgressIndicator;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.application.util.function.Processor;
import jakarta.annotation.Nonnull;

/**
 * Represents the state and settings of a "choose by name" popup from the point of view of the logic which fills it with items.
 *
 * @author yole
 * @see DefaultChooseByNameItemProvider#filterElements(ChooseByNameViewModel, String, boolean, ProgressIndicator, PsiElement, Processor)
 */
public interface ChooseByNameViewModel {
  Project getProject();

  @Nonnull
  ChooseByNameModel getModel();

  /**
   * If true, the pattern entered in the dialog should be searched anywhere in the text of the candidate items, not just in the beginning.
   */
  boolean isSearchInAnyPlace();

  /**
   * Transforms text entered by the user in the dialog into the search pattern (for example, removes irrelevant suffixes like "line ...")
   */
  @Nonnull
  String transformPattern(@Nonnull String pattern);

  /**
   * If true, top matching candidates should be shown in the popup also when the entered pattern is empty. If false, an empty list is
   * displayed when the user has not entered any pattern.
   */
  boolean canShowListForEmptyPattern();

  /**
   * Returns the maximum number of candidates to show in the popup.
   */
  int getMaximumListSizeLimit();
}
