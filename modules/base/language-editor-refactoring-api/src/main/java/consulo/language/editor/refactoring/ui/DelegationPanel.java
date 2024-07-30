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

import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.ui.Label;
import consulo.ui.RadioButton;
import consulo.ui.ValueGroup;
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

  protected final ValueGroup<Boolean> myValueGroup;

  @RequiredUIAccess
  public DelegationPanel() {
    myLayout = HorizontalLayout.create();
    myLayout.add(Label.create(RefactoringLocalize.delegationPanelMethodCallsLabel()));

    myValueGroup = ValueGroup.createBool();

    myRbModifyCalls = RadioButton.create(RefactoringLocalize.delegationPanelModifyRadio());
    myRbGenerateDelegate = RadioButton.create(RefactoringLocalize.delegationPanelDelegateViaOverloadingMethod());
    myRbModifyCalls.setValue(true);

    myLayout.add(myRbModifyCalls);
    myLayout.add(myRbGenerateDelegate);

    myValueGroup.add(myRbModifyCalls).add(myRbGenerateDelegate);

    myRbModifyCalls.addValueListener(e -> stateModified());
    myRbGenerateDelegate.addValueListener(e -> stateModified());
  }

  public final HorizontalLayout getComponent() {
    return myLayout;
  }

  protected void stateModified() {
  }

  @RequiredUIAccess
  public boolean isModifyCalls() {
    return myRbModifyCalls.getValueOrError();
  }

  @RequiredUIAccess
  public boolean isGenerateDelegate() {
    return myRbGenerateDelegate.getValueOrError();
  }
}
