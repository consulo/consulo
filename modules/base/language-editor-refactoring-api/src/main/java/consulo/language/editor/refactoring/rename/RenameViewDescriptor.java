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

package consulo.language.editor.refactoring.rename;

import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.logging.Logger;
import consulo.usage.UsageViewBundle;
import consulo.usage.UsageViewDescriptor;
import consulo.usage.UsageViewUtil;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

public class RenameViewDescriptor implements UsageViewDescriptor{
  private static final Logger LOG = Logger.getInstance(RenameViewDescriptor.class);
  private final String myProcessedElementsHeader;
  private final String myCodeReferencesText;
  private final PsiElement[] myElements;

  public RenameViewDescriptor(LinkedHashMap<PsiElement, String> renamesMap) {

    myElements = ContainerUtil.toArray(renamesMap.keySet(), PsiElement.ARRAY_FACTORY);

    Set<String> processedElementsHeaders = new HashSet<String>();
    Set<String> codeReferences = new HashSet<String>();

    for (PsiElement element : myElements) {
      LOG.assertTrue(element.isValid(), "Invalid element: " + element.toString());
      String newName = renamesMap.get(element);

      String prefix = "";
      if (element instanceof PsiDirectory/* || element instanceof PsiClass*/) {
        String fullName = UsageViewUtil.getLongName(element);
        int lastDot = fullName.lastIndexOf('.');
        if (lastDot >= 0) {
          prefix = fullName.substring(0, lastDot + 1);
        }
      }

      processedElementsHeaders.add(
        StringUtil.capitalize(RefactoringLocalize.zeroToBeRenamedTo12(UsageViewUtil.getType(element), prefix, newName).get())
      );
      codeReferences.add(UsageViewUtil.getType(element) + " " + UsageViewUtil.getLongName(element));
    }


    myProcessedElementsHeader = StringUtil.join(ArrayUtil.toStringArray(processedElementsHeaders), ", ");
    myCodeReferencesText =
      RefactoringLocalize.referencesInCodeTo0(StringUtil.join(ArrayUtil.toStringArray(codeReferences), ", ")).get();
  }

  @Override
  @Nonnull
  public PsiElement[] getElements() {
    return myElements;
  }

  @Override
  public String getProcessedElementsHeader() {
    return myProcessedElementsHeader;
  }

  @Override
  public String getCodeReferencesText(int usagesCount, int filesCount) {
    return myCodeReferencesText + UsageViewBundle.getReferencesString(usagesCount, filesCount);
  }

  @Override
  public String getCommentReferencesText(int usagesCount, int filesCount) {
    return RefactoringLocalize.commentsElementsHeader(UsageViewBundle.getOccurencesString(usagesCount, filesCount)).get();
  }
}