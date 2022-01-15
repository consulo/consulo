/*
 * Copyright 2013-2019 consulo.io
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
package consulo.moduleImport.ui;

import com.intellij.CommonBundle;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBCardLayout;
import consulo.moduleImport.ModuleImportContext;
import consulo.moduleImport.ModuleImportProvider;
import consulo.start.WelcomeFrameManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.Size;
import consulo.ui.wizard.WizardSession;
import consulo.ui.wizard.WizardStep;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-08-26
 */
public class ModuleImportDialog<C extends ModuleImportContext> extends DialogWrapper {

  private final WizardSession<C> myWizardSession;

  private Runnable myNextRunnable;
  private Runnable myBackRunnable;
  private final C myContext;

  protected ModuleImportDialog(@Nullable Project project, @Nonnull VirtualFile targetFile, @Nonnull ModuleImportProvider<C> moduleImportProvider) {
    super(project);

    myContext = moduleImportProvider.createContext(project);

    String pathToImport = moduleImportProvider.getPathToBeImported(targetFile);
    myContext.setPath(pathToImport);
    myContext.setName(new File(pathToImport).getName());
    myContext.setFileToImport(targetFile.getPath());

    List<WizardStep<C>> steps = new ArrayList<>();
    moduleImportProvider.buildSteps(steps::add, myContext);

    myWizardSession = new WizardSession<>(myContext, steps);

    if (!myWizardSession.hasNext()) {
      throw new IllegalArgumentException("no steps for show");
    }

    setTitle("Import from " + moduleImportProvider.getName());
    setOKButtonText(IdeBundle.message("button.create"));

    Size size = WelcomeFrameManager.getDefaultWindowSize();
    setScalableSize(size.getWidth(), size.getHeight());

    init();
  }

  @Nonnull
  public C getContext() {
    return myContext;
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
  public void doCancelAction(AWTEvent source) {
    if (source instanceof WindowEvent) {
      // if it's window event - close it via X
      super.doCancelAction();
      return;
    }
    super.doCancelAction(source);
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

    WizardStep<C> first = myWizardSession.next();

    int currentStepIndex = myWizardSession.getCurrentStepIndex();

    String id = "step-" + currentStepIndex;
    contentPanel.add(first.getSwingComponent(myContext, getDisposable()), id);

    layout.show(contentPanel, id);

    updateButtonPresentation(contentPanel);
    return contentPanel;
  }

  @RequiredUIAccess
  @SuppressWarnings("deprecation")
  private void gotoStep(JPanel rightContentPanel, WizardStep<C> step) {
    Component swingComponent = step.getSwingComponent(myContext, getDisposable());

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
