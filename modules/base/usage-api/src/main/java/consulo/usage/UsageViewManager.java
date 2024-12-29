/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.usage;


import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.ApplicationManager;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.usage.rule.PsiElementUsage;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Supplier;

/**
 * @author max
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class UsageViewManager {
  public static UsageViewManager getInstance (Project project) {
    return project.getInstance(UsageViewManager.class);
  }

  @Nonnull
  public abstract UsageView createUsageView(@Nonnull UsageTarget[] targets, @Nonnull Usage[] usages, @Nonnull UsageViewPresentation presentation, Supplier<UsageSearcher> usageSearcherFactory);

  @Nonnull
  public abstract UsageView showUsages(@Nonnull UsageTarget[] searchedFor, @Nonnull Usage[] foundUsages, @Nonnull UsageViewPresentation presentation, Supplier<UsageSearcher> factory);

  @Nonnull
  public abstract UsageView showUsages(@Nonnull UsageTarget[] searchedFor, @Nonnull Usage[] foundUsages, @Nonnull UsageViewPresentation presentation);

  /**
   * @return returns null in case of no usages found or usage view not shown for one usage
   */
  @Nullable
  public abstract UsageView searchAndShowUsages(@Nonnull UsageTarget[] searchFor,
                                                @Nonnull Supplier<UsageSearcher> searcherFactory,
                                                boolean showPanelIfOnlyOneUsage,
                                                boolean showNotFoundMessage,
                                                @Nonnull UsageViewPresentation presentation,
                                                @Nullable UsageViewStateListener listener);

  public interface UsageViewStateListener {
    void usageViewCreated(@Nonnull UsageView usageView);

    @RequiredUIAccess
    void findingUsagesFinished(UsageView usageView);
  }

  public abstract void searchAndShowUsages(@Nonnull UsageTarget[] searchFor,
                                           @Nonnull Supplier<UsageSearcher> searcherFactory,
                                           @Nonnull FindUsagesProcessPresentation processPresentation,
                                           @Nonnull UsageViewPresentation presentation,
                                           @Nullable UsageViewStateListener listener);

  @Nullable
  public abstract UsageView getSelectedUsageView();

  public static boolean isSelfUsage(@Nonnull final Usage usage, @Nonnull final UsageTarget[] searchForTarget) {
    if (!(usage instanceof PsiElementUsage)) return false;
    return ApplicationManager.getApplication().runReadAction(new Supplier<Boolean>() {
      @Override
      public Boolean get() {
        final PsiElement element = ((PsiElementUsage)usage).getElement();
        if (element == null) return false;

        for (UsageTarget ut : searchForTarget) {
          if (ut instanceof PsiElementUsageTarget) {
            if (isSelfUsage(element, ((PsiElementUsageTarget)ut).getElement())) {
              return true;
            }
          }
        }
        return false;
      }
    });
  }

  private static boolean isSelfUsage(@Nonnull PsiElement element, PsiElement psiElement) {
    return element.getParent() == psiElement; // self usage might be configurable
  }
}
