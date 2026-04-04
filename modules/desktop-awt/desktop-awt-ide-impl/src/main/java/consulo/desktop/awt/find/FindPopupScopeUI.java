/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.desktop.awt.find;

import consulo.find.FindModel;
import consulo.find.FindSettings;
import consulo.ui.ex.awt.ValidationInfo;
import consulo.util.lang.Pair;
import consulo.localize.LocalizeValue;

import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public interface FindPopupScopeUI {
 
  List<Pair<ScopeType, JComponent>> getComponents();

 
  ScopeType initByModel(FindModel findModel);
  void applyTo(FindSettings findSettings, FindPopupScopeUI.ScopeType selectedScope);
  void applyTo(FindModel findModel, FindPopupScopeUI.ScopeType selectedScope);

  /**
   *
   * @param model
   * @param selectedScope
   * @return null means OK
   */
  default ValidationInfo validate(FindModel model, FindPopupScopeUI.@Nullable ScopeType selectedScope) {
    return null;
  }

  boolean hideAllPopups();

  class ScopeType {
    public final String name;
    public final LocalizeValue text;

    public ScopeType(String name, LocalizeValue text) {
      this.name = name;
      this.text = text;
    }
  }
}
