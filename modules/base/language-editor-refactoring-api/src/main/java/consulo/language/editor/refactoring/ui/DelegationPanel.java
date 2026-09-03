/*
 * Copyright 2013-2024 consulo.io
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
package consulo.language.editor.refactoring.ui;

import consulo.ui.RadioGroup;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.ui.Label;
import consulo.ui.RadioButton;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;

/**
 * @author VISTALL
 * @since 30-Jul-24
 */
public class DelegationPanel {
  protected final HorizontalLayout myLayout;
  protected final RadioButton myRbModifyCalls;
  protected final RadioButton myRbGenerateDelegate;

  protected final RadioGroup<Boolean> myValueGroup;

  @RequiredUIAccess
  public DelegationPanel() {
    myLayout = HorizontalLayout.create();
    myLayout.add(Label.create(RefactoringLocalize.delegationPanelMethodCallsLabel()));

    myValueGroup = RadioGroup.create();

    myRbModifyCalls = myValueGroup.newButton(RefactoringLocalize.delegationPanelModifyRadio(), true);
    myRbGenerateDelegate = myValueGroup.newButton(RefactoringLocalize.delegationPanelDelegateViaOverloadingMethod(), false);
    myValueGroup.setValue(true);

    myLayout.add(myRbModifyCalls);
    myLayout.add(myRbGenerateDelegate);

    myValueGroup.addValueListener(value -> stateModified());
  }

  public final HorizontalLayout getComponent() {
    return myLayout;
  }

  protected void stateModified() {
  }

  @RequiredUIAccess
  public boolean isModifyCalls() {
    return myValueGroup.getValueOrError();
  }

  @RequiredUIAccess
  public boolean isGenerateDelegate() {
    return !myValueGroup.getValueOrError();
  }
}
