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

package consulo.ide.impl.idea.ide.impl.dataRules;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.util.EditSourceUtil;
import consulo.dataContext.DataProvider;
import consulo.language.editor.PlatformDataKeys;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.dataContext.GetDataRule;
import consulo.util.dataholder.Key;
import consulo.navigation.Navigatable;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class NavigatableRule implements GetDataRule<Navigatable> {
  @Nonnull
  @Override
  public Key<Navigatable> getKey() {
    return Navigatable.KEY;
  }

  @Override
  public Navigatable getData(@Nonnull DataProvider dataProvider) {
    final Navigatable navigatable = dataProvider.getDataUnchecked(Navigatable.KEY);
    if (navigatable != null && navigatable instanceof OpenFileDescriptorImpl openFileDescriptor) {
      if (openFileDescriptor.getFile().isValid()) {
        return openFileDescriptor;
      }
    }
    final PsiElement element = dataProvider.getDataUnchecked(PsiElement.KEY);
    if (element instanceof Navigatable navElem) {
      return navElem;
    }
    if (element != null) {
      return EditSourceUtil.getDescriptor(element);
    }

    final Object selection = dataProvider.getDataUnchecked(PlatformDataKeys.SELECTED_ITEM);
    return selection instanceof Navigatable navSel ? navSel : null;
  }
}
