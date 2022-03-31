/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.find.findUsages;

import consulo.find.FindUsagesHandler;
import consulo.find.FindUsagesOptions;
import consulo.language.findUsage.DescriptiveNameUtil;
import consulo.application.ui.HelpManager;
import consulo.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.search.PsiSearchHelper;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.usage.UsageViewUtil;
import javax.annotation.Nonnull;

import javax.swing.*;

/**
 * @author max
 */
public class CommonFindUsagesDialog extends AbstractFindUsagesDialog {
  @Nonnull
  protected final PsiElement myPsiElement;

  public CommonFindUsagesDialog(@Nonnull PsiElement element,
                                @Nonnull Project project,
                                @Nonnull FindUsagesOptions findUsagesOptions,
                                boolean toShowInNewTab,
                                boolean mustOpenInNewTab,
                                boolean isSingleFile,
                                FindUsagesHandler handler) {
    super(project, findUsagesOptions, toShowInNewTab, mustOpenInNewTab, isSingleFile, isTextSearch(element, isSingleFile, handler),
          !isSingleFile && !element.getManager().isInProject(element));
    myPsiElement = element;
    init();
  }

  private static boolean isTextSearch(PsiElement element, boolean isSingleFile, FindUsagesHandler handler) {
    return FindUsagesUtil.isSearchForTextOccurrencesAvailable(element, isSingleFile, handler);
  }

  @Override
  protected boolean isInFileOnly() {
    return super.isInFileOnly() ||
           PsiSearchHelper.SERVICE.getInstance(myPsiElement.getProject()).getUseScope(myPsiElement) instanceof LocalSearchScope;
  }

  @Override
  protected JPanel createFindWhatPanel() {
    return null;
  }

  @Override
  public void configureLabelComponent(@Nonnull SimpleColoredComponent coloredComponent) {
    coloredComponent.append(StringUtil.capitalize(UsageViewUtil.getType(myPsiElement)));
    coloredComponent.append(" ");
    coloredComponent.append(DescriptiveNameUtil.getDescriptiveName(myPsiElement), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
  }

  @Override
  protected void doHelpAction() {
    HelpManager.getInstance().invokeHelp(FindUsagesManager.getHelpID(myPsiElement));
  }
}
