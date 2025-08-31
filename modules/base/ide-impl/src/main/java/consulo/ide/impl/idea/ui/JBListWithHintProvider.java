/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui;

import consulo.ui.ex.popup.JBPopup;
import consulo.language.psi.PsiElement;
import consulo.ui.ex.awt.JBList;
import consulo.ide.ui.popup.HintUpdateSupply;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.Collection;

/**
 * @author pegov
 * @deprecated
 * @see HintUpdateSupply
 */
public abstract class JBListWithHintProvider<T> extends JBList<T> {
  {
    new HintUpdateSupply(this) {
      @Override
      protected PsiElement getPsiElementForHint(Object selectedValue) {
        return JBListWithHintProvider.this.getPsiElementForHint(selectedValue);
      }
    };
  }

  public JBListWithHintProvider() {
  }

  public JBListWithHintProvider(ListModel dataModel) {
    super(dataModel);
  }

  @SafeVarargs
  public JBListWithHintProvider(T... listData) {
    super(listData);
  }

  public JBListWithHintProvider(Collection items) {
    super(items);
  }

  @Nullable
  protected abstract PsiElement getPsiElementForHint(Object selectedValue);

  @Deprecated
  public void registerHint(JBPopup hint) {
    ObjectUtil.assertNotNull(HintUpdateSupply.getSupply(this)).registerHint(hint);
  }

  @Deprecated
  public void hideHint() {
    ObjectUtil.assertNotNull(HintUpdateSupply.getSupply(this)).hideHint();
  }

  @Deprecated
  public void updateHint(PsiElement element) {
    ObjectUtil.assertNotNull(HintUpdateSupply.getSupply(this)).updateHint(element);
  }

}
