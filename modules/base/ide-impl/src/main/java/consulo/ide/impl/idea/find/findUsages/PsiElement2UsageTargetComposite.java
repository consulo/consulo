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
package consulo.ide.impl.idea.find.findUsages;

import consulo.find.FindManager;
import consulo.find.FindSettings;
import consulo.ide.impl.idea.find.impl.FindManagerImpl;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.find.FindUsagesHandler;
import consulo.find.FindUsagesOptions;
import consulo.ide.impl.find.PsiElement2UsageTargetAdapter;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.usage.UsageInfoToUsageConverter;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import jakarta.annotation.Nonnull;

import java.util.Set;

public class PsiElement2UsageTargetComposite extends PsiElement2UsageTargetAdapter {
  private final UsageInfoToUsageConverter.TargetElementsDescriptor myDescriptor;
  public PsiElement2UsageTargetComposite(@Nonnull PsiElement[] primaryElements,
                                         @Nonnull PsiElement[] secondaryElements,
                                         @Nonnull FindUsagesOptions options) {
    super(primaryElements[0], options);
    myDescriptor = new UsageInfoToUsageConverter.TargetElementsDescriptor(primaryElements, secondaryElements);
  }

  @Override
  public void findUsages() {
    PsiElement element = getElement();
    if (element == null) return;
    FindUsagesManager findUsagesManager = ((FindManagerImpl)FindManager.getInstance(element.getProject())).getFindUsagesManager();
    FindUsagesHandler handler = findUsagesManager.getFindUsagesHandler(element, false);
    boolean skipResultsWithOneUsage = FindSettings.getInstance().isSkipResultsWithOneUsage();
    findUsagesManager.findUsages(myDescriptor.getPrimaryElements(), myDescriptor.getAdditionalElements(), handler, myOptions, skipResultsWithOneUsage);
  }

  @Override
  public VirtualFile[] getFiles() {
    Set<VirtualFile> files = ContainerUtil.map2Set(myDescriptor.getAllElements(), element -> PsiUtilCore.getVirtualFile(element));
    return VfsUtilCore.toVirtualFileArray(files);
  }

  @Nonnull
  public PsiElement[] getPrimaryElements() {
    return myDescriptor.getPrimaryElements();
  }
  @Nonnull
  public PsiElement[] getSecondaryElements() {
    return myDescriptor.getAdditionalElements();
  }
}
