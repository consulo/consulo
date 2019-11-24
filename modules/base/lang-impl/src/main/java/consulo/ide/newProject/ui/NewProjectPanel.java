/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.newProject.ui;

import com.intellij.CommonBundle;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.JBCardLayout;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.ide.newProject.NewModuleBuilder;
import consulo.ide.newProject.NewModuleBuilderProcessor;
import consulo.ide.newProject.NewModuleContext;
import consulo.ide.welcomeScreen.BaseWelcomeScreenPanel;
import consulo.ide.wizard.newModule.NewModuleWizardContext;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.SwingUIDecorator;
import consulo.ui.wizard.WizardSession;
import consulo.ui.wizard.WizardStep;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 14-Sep-16
 */
public abstract class NewProjectPanel extends BaseWelcomeScreenPanel implements Disposable {
  private static final Logger LOG = Logger.getInstance(NewProjectPanel.class);

  private static final String EMPTY_PANEL = "empty-panel";

  // per module builder fields
  private WizardSession<NewModuleWizardContext> myWizardSession;
  private NewModuleWizardContext myWizardContext;
  private NewModuleBuilderProcessor<NewModuleWizardContext> myProcessor;
  @Nullable
  private final VirtualFile myModuleHome;

  private JBList<Object> myList;

  @RequiredUIAccess
  public NewProjectPanel(@Nonnull Disposable parentDisposable, @Nullable Project project, @Nullable VirtualFile moduleHome) {
    super(parentDisposable);
    myModuleHome = moduleHome;
    setOKActionText(IdeBundle.message("button.create"));
    setCancelText(CommonBundle.message("button.cancel"));
  }

  @Nullable
  public NewModuleBuilderProcessor<NewModuleWizardContext> getProcessor() {
    return myProcessor;
  }

  @Nullable
  public NewModuleWizardContext getWizardContext() {
    return myWizardContext;
  }

  public boolean isModuleCreation() {
    return myModuleHome != null;
  }

  @Nonnull
  @RequiredUIAccess
  protected abstract JComponent createSouthPanel();

  public abstract void setOKActionEnabled(boolean enabled);

  public abstract void setOKActionText(@Nonnull String text);

  public abstract void setOKAction(@Nullable Runnable action);

  public abstract void setCancelText(@Nonnull String text);

  public abstract void setCancelAction(@Nullable Runnable action);

  @Nonnull
  @Override
  protected JComponent createLeftComponent(@Nonnull Disposable parentDisposable) {
    NewModuleContext context = new NewModuleContext();

    NewModuleBuilder.EP_NAME.composite().setupContext(context);

    CollectionListModel<Object> model = new CollectionListModel<>();
    myList = new JBList<>(model);
    myList.setBackground(SwingUIDecorator.get(SwingUIDecorator::getSidebarColor));
    myList.setCellRenderer(new ColoredListCellRenderer<Object>() {
      @Override
      protected void customizeCellRenderer(@Nonnull JList list, Object value, int index, boolean selected, boolean hasFocus) {
        setFont(UIUtil.getFont(UIUtil.FontSize.BIGGER, null));

        if (value instanceof NewModuleContext.Group) {
          setSeparator(StringUtil.nullize(((NewModuleContext.Group)value).getName()));
        }
        else if (value instanceof NewModuleContext.Item) {
          setIcon(((NewModuleContext.Item)value).getIcon());
          append(((NewModuleContext.Item)value).getName());
        }
      }

      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        Dimension preferredSize = component.getPreferredSize();
        component.setPreferredSize(new Dimension(preferredSize.width, JBUI.scale(25)));
        return component;
      }
    });

    NewModuleContext.Group[] groups = context.getGroups();

    for (NewModuleContext.Group group : groups) {
      // do not add simple line
      if (!(groups.length == 1 && group.getId().equals(NewModuleContext.UGROUPED))) {
        model.add(group);
      }

      for (NewModuleContext.Item item : group.getItems()) {
        model.add(item);
      }
    }
    return ScrollPaneFactory.createScrollPane(myList, true);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  @SuppressWarnings({"deprecation", "unchecked", "RequiredXAction"})
  protected JComponent createRightComponent() {
    final JPanel panel = new JPanel(new VerticalFlowLayout());

    JPanel rightPanel = new JPanel(new BorderLayout());

    JBCardLayout rightContentLayout = new JBCardLayout();
    JPanel rightContentPanel = new JPanel(rightContentLayout);
    rightContentPanel.setBorder(JBUI.Borders.empty(5));

    rightPanel.add(rightContentPanel, BorderLayout.CENTER);

    final JPanel nullPanel = new JPanel(new BorderLayout());
    JBLabel nodeLabel = new JBLabel(myModuleHome == null ? "Please select project type" : "Please select module type", UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER);
    nodeLabel.setHorizontalAlignment(SwingConstants.CENTER);
    nullPanel.add(nodeLabel, BorderLayout.CENTER);

    rightContentPanel.add(nullPanel, EMPTY_PANEL);
    rightContentLayout.show(rightContentPanel, EMPTY_PANEL);

    myList.addListSelectionListener(e -> {
      rightContentPanel.removeAll();

      if (myWizardSession != null) {
        myWizardSession.finish();
        myWizardSession.dispose();
        myWizardSession = null;
      }

      Object selectedValue = myList.getSelectedValue();

      myProcessor = selectedValue instanceof NewModuleContext.Item ? (NewModuleBuilderProcessor<NewModuleWizardContext>)((NewModuleContext.Item)selectedValue).getProcessor() : null;

      String id = null;
      Component toShow = null;

      if (selectedValue instanceof NewModuleContext.Item) {
        if (myProcessor != null) {
          myWizardContext = myProcessor.createContext(!isModuleCreation());

          if (myModuleHome != null) {
            myWizardContext.setName(myModuleHome.getName());
            myWizardContext.setPath(myModuleHome.getPath());
          }
          else {
            String baseDir = ProjectUtil.getBaseDir();
            File suggestedProjectDirectory = FileUtil.findSequentNonexistentFile(new File(baseDir), "untitled", "");

            myWizardContext.setName(suggestedProjectDirectory.getName());
            myWizardContext.setPath(suggestedProjectDirectory.getPath());
          }

          List<WizardStep<NewModuleWizardContext>> steps = new ArrayList<>();
          myProcessor.buildSteps(steps::add, myWizardContext);

          myWizardSession = new WizardSession<>(myWizardContext, steps);

          if (myWizardSession.hasNext()) {
            WizardStep<NewModuleWizardContext> step = myWizardSession.next();

            toShow = step.getSwingComponent();
          }
          else {
            LOG.error("There no visible steps for " + selectedValue);
            toShow = new JPanel();
          }

          id = "step-" + myWizardSession.getCurrentStepIndex();
        }
        rightContentPanel.add(panel, BorderLayout.CENTER);
      }


      if (myProcessor == null) {
        rightContentPanel.add(nullPanel, EMPTY_PANEL);

        rightContentLayout.show(rightContentPanel, EMPTY_PANEL);
      }
      else {
        assert toShow != null;

        rightContentPanel.add(toShow, id);

        rightContentLayout.show(rightContentPanel, id);
      }

      updateButtonPresentation(rightContentPanel);
    });

    JPanel root = new JPanel(new BorderLayout());
    root.add(rightPanel, BorderLayout.CENTER);
    JComponent southPanel = createSouthPanel();
    southPanel.setBorder(JBUI.Borders.empty(DialogWrapper.ourDefaultBorderInsets));
    root.add(southPanel, BorderLayout.SOUTH);
    return root;
  }

  @RequiredUIAccess
  private void updateButtonPresentation(JPanel rightContentPanel) {
    if (myProcessor != null) {
      assert myWizardSession != null;

      boolean hasNext = myWizardSession.hasNext();

      if (hasNext) {
        setOKActionText(CommonBundle.getNextButtonText());
        setOKAction(() -> gotoStep(rightContentPanel, myWizardSession.next()));
      }
      else {
        setOKActionText(IdeBundle.message("button.create"));
        setOKAction(null);
      }

      int currentStepIndex = myWizardSession.getCurrentStepIndex();
      if (currentStepIndex != 0) {
        setCancelAction(() -> gotoStep(rightContentPanel, myWizardSession.prev()));
        setCancelText(CommonBundle.message("button.back"));
      }
      else {
        setCancelAction(null);
        setCancelText(CommonBundle.message("button.cancel"));
      }

      setOKActionEnabled(true);
    }
    else {
      setOKActionEnabled(false);

      setOKActionText(IdeBundle.message("button.create"));
      setOKAction(null);
      setCancelAction(null);
    }
  }

  public void finish() {
    myWizardSession.finish();
  }

  @RequiredUIAccess
  private void gotoStep(JPanel rightContentPanel, WizardStep<NewModuleWizardContext> step) {
    Component swingComponent = step.getSwingComponent();

    String id = "step-" + myWizardSession.getCurrentStepIndex();

    JBCardLayout layout = (JBCardLayout)rightContentPanel.getLayout();

    rightContentPanel.add(swingComponent, id);

    layout.swipe(rightContentPanel, id, JBCardLayout.SwipeDirection.FORWARD);

    updateButtonPresentation(rightContentPanel);
  }

  @Override
  public void dispose() {
    if (myWizardSession != null) {
      myWizardSession.dispose();
    }
  }
}
