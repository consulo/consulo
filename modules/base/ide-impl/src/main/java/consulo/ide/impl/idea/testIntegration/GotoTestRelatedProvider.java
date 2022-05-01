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
package consulo.ide.impl.idea.testIntegration;

import consulo.language.navigation.GotoRelatedItem;
import consulo.language.navigation.GotoRelatedProvider;
import consulo.dataContext.DataContext;
import consulo.language.editor.LangDataKeys;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class GotoTestRelatedProvider extends GotoRelatedProvider {
  @Nonnull
  @Override
  public List<? extends GotoRelatedItem> getItems(@Nonnull DataContext context) {
    final PsiFile file = context.getData(LangDataKeys.PSI_FILE);
    List<PsiElement> result;
    final boolean isTest = TestFinderHelper.isTest(file);
    if (isTest) {
      result = TestFinderHelper.findClassesForTest(file);
    } else {
      result = TestFinderHelper.findTestsForClass(file);
    }

    if (!result.isEmpty()) {
      final List<GotoRelatedItem> items = new ArrayList<GotoRelatedItem>();
      for (PsiElement element : result) {
        items.add(new GotoRelatedItem(element, isTest ? "Tests" : "Testee classes"));
      }
      return items;
    }
    return super.getItems(context);
  }
}
