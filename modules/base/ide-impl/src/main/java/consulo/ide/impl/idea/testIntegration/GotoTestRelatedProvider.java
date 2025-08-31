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

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.testIntegration.TestFinderHelper;
import consulo.language.navigation.GotoRelatedItem;
import consulo.language.navigation.GotoRelatedProvider;
import consulo.dataContext.DataContext;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
@ExtensionImpl
public class GotoTestRelatedProvider extends GotoRelatedProvider {
  @Nonnull
  @Override
  public List<? extends GotoRelatedItem> getItems(@Nonnull DataContext context) {
    PsiFile file = context.getData(PsiFile.KEY);
    List<PsiElement> result;
    boolean isTest = TestFinderHelper.isTest(file);
    if (isTest) {
      result = TestFinderHelper.findClassesForTest(file);
    } else {
      result = TestFinderHelper.findTestsForClass(file);
    }

    if (!result.isEmpty()) {
      List<GotoRelatedItem> items = new ArrayList<>();
      for (PsiElement element : result) {
        items.add(new GotoRelatedItem(element, isTest ? "Tests" : "Testee classes"));
      }
      return items;
    }
    return super.getItems(context);
  }
}
