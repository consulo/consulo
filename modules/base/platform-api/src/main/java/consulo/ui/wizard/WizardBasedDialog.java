/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ui.wizard;

import com.intellij.CommonBundle;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBCardLayout;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;

/**
 * @author VISTALL
 * @since 22/09/2021
 */
public abstract class WizardBasedDialog<C> extends DialogWrapper {
  protected WizardSession<C> myWizardSession;
  protected C myWizardContext;

  private Runnable myNextRunnable;
  private Runnable myBackRunnable;

  protected WizardBasedDialog(@Nullable Project project) {
    super(project);

    // in child myWizardSession and myWizardContext but be initialized
  }

  @Nonnull
  public C getContext() {
    return myWizardContext;
  }

  @Override
  public void doCancelAction(AWTEvent source) {
    if (source instanceof WindowEvent) {
      // if it's window event - close it via X
      super.doCancelAction();
      return;
    }
    super.doCancelAction(source);
  }

  @Nonnull
  @Override
  protected Action[] createActions() {
    return new Action[]{getCancelAction(), getOKAction()};
  }

  @Override
  protected void doOKAction() {
    if (myNextRunnable != null) {
      myNextRunnable.run();
    }
    else {
      myWizardSession.finish();

      super.doOKAction();
    }
  }

  @Override
  public void doCancelAction() {
    if (myBackRunnable != null) {
      myBackRunnable.run();
    }
    else {
      myWizardSession.finish();

      super.doCancelAction();
    }
  }

  @Nullable
  @Override
  protected String getDimensionServiceKey() {
    return getClass().getName();
  }

  @Nullable
  @Override
  @RequiredUIAccess
  @SuppressWarnings("deprecation")
  protected JComponent createCenterPanel() {
    JBCardLayout layout = new JBCardLayout();
    JPanel contentPanel = new JPanel(layout);

    WizardStep<C> first = myWizardSession.nextWithStepEnter();

    int currentStepIndex = myWizardSession.getCurrentStepIndex();

    String id = "step-" + currentStepIndex;
    contentPanel.add(first.getSwingComponent(getDisposable()), id);

    first.onStepEnter(myWizardContext);
    
    layout.show(contentPanel, id);

    updateButtonPresentation(contentPanel);
    return contentPanel;
  }

  @RequiredUIAccess
  @SuppressWarnings("deprecation")
  private void gotoStep(JPanel rightContentPanel, WizardStep<C> step) {
    Component swingComponent = step.getSwingComponent(getDisposable());

    step.onStepEnter(myWizardContext);
    
    String id = "step-" + myWizardSession.getCurrentStepIndex();

    JBCardLayout layout = (JBCardLayout)rightContentPanel.getLayout();

    rightContentPanel.add(swingComponent, id);

    layout.swipe(rightContentPanel, id, JBCardLayout.SwipeDirection.FORWARD);

    updateButtonPresentation(rightContentPanel);
  }

  private void updateButtonPresentation(JPanel contentPanel) {
    boolean hasNext = myWizardSession.hasNext();

    if (hasNext) {
      setOKButtonText(CommonBundle.getNextButtonText());
      myNextRunnable = () -> gotoStep(contentPanel, myWizardSession.next());
    }
    else {
      setOKButtonText(IdeBundle.message("button.create"));
      myNextRunnable = null;
    }

    int currentStepIndex = myWizardSession.getCurrentStepIndex();
    if (currentStepIndex != 0) {
      myBackRunnable = () -> gotoStep(contentPanel, myWizardSession.prev());

      setCancelButtonText(CommonBundle.message("button.back"));
    }
    else {
      myBackRunnable = null;
      setCancelButtonText(CommonBundle.message("button.cancel"));
    }
  }

  @Override
  protected void dispose() {
    myWizardSession.dispose();

    super.dispose();
  }
}
