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
package com.intellij.usages;


import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Factory;
import com.intellij.psi.PsiElement;
import com.intellij.usages.rules.PsiElementUsage;
import javax.annotation.Nonnull;

/**
 * @author max
 */
public abstract class UsageViewManager {
  public static UsageViewManager getInstance (Project project) {
    return ServiceManager.getService(project, UsageViewManager.class);
  }

  @Nonnull
  public abstract UsageView createUsageView(@Nonnull UsageTarget[] targets, @Nonnull Usage[] usages, @Nonnull UsageViewPresentation presentation, Factory<UsageSearcher> usageSearcherFactory);

  @Nonnull
  public abstract UsageView showUsages(@Nonnull UsageTarget[] searchedFor, @Nonnull Usage[] foundUsages, @Nonnull UsageViewPresentation presentation, Factory<UsageSearcher> factory);

  @Nonnull
  public abstract UsageView showUsages(@Nonnull UsageTarget[] searchedFor, @Nonnull Usage[] foundUsages, @Nonnull UsageViewPresentation presentation);

  /**
   * @return returns null in case of no usages found or usage view not shown for one usage
   */
  @javax.annotation.Nullable
  public abstract UsageView searchAndShowUsages(@Nonnull UsageTarget[] searchFor,
                                                @Nonnull Factory<UsageSearcher> searcherFactory,
                                                boolean showPanelIfOnlyOneUsage,
                                                boolean showNotFoundMessage,
                                                @Nonnull UsageViewPresentation presentation,
                                                @javax.annotation.Nullable UsageViewStateListener listener);

  public interface UsageViewStateListener {
    void usageViewCreated(@Nonnull UsageView usageView);
    void findingUsagesFinished(UsageView usageView);
  }

  public abstract void searchAndShowUsages(@Nonnull UsageTarget[] searchFor,
                                           @Nonnull Factory<UsageSearcher> searcherFactory,
                                           @Nonnull FindUsagesProcessPresentation processPresentation,
                                           @Nonnull UsageViewPresentation presentation,
                                           @javax.annotation.Nullable UsageViewStateListener listener);

  @javax.annotation.Nullable
  public abstract UsageView getSelectedUsageView();

  public static boolean isSelfUsage(@Nonnull final Usage usage, @Nonnull final UsageTarget[] searchForTarget) {
    if (!(usage instanceof PsiElementUsage)) return false;
    return ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
      @Override
      public Boolean compute() {
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
