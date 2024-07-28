// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.wizard;

import consulo.application.HelpManager;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.ide.impl.idea.ui.mac.TouchbarDataKeys;
import consulo.ide.localize.IdeLocalize;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractWizard<T extends Step> extends DialogWrapper {
  private static final Logger LOG = Logger.getInstance(AbstractWizard.class);

  protected int myCurrentStep;
  protected final ArrayList<T> mySteps;
  private Button myPreviousButton;
  private Button myNextButton;
  private Button myCancelButton;
  private JButton myHelpButton;
  protected JPanel myContentPanel;
  protected TallImageComponent myIcon;
  private Component myCurrentStepComponent;
  private JBCardLayout.SwipeDirection myTransitionDirection = JBCardLayout.SwipeDirection.AUTO;
  private final Map<Component, String> myComponentToIdMap = new HashMap<>();
  private final StepListener myStepListener = this::updateStep;

  public AbstractWizard(final String title, final Component dialogParent) {
    super(dialogParent, true);
    mySteps = new ArrayList<>();
    initWizard(title);
  }

  public AbstractWizard(final String title, @Nullable final Project project) {
    super(project, true);
    mySteps = new ArrayList<>();
    initWizard(title);
  }

  private void initWizard(final String title) {
    setTitle(title);
    myCurrentStep = 0;
    myPreviousButton = Button.create(IdeLocalize.buttonWizardPrevious());
    myNextButton = Button.create(IdeLocalize.buttonWizardNext());
    myCancelButton = Button.create(CommonLocalize.buttonCancel());
    myHelpButton = new JButton(CommonLocalize.buttonHelp().get());
    myContentPanel = new JPanel(new JBCardLayout());

    myIcon = new TallImageComponent(null);

    JRootPane rootPane = getRootPane();
    if (rootPane != null) {        // it will be null in headless mode, i.e. tests
      rootPane.registerKeyboardAction(
        e -> helpAction(),
        KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0),
        JComponent.WHEN_IN_FOCUSED_WINDOW
      );

      rootPane.registerKeyboardAction(
        e -> helpAction(),
        KeyStroke.getKeyStroke(KeyEvent.VK_HELP, 0),
        JComponent.WHEN_IN_FOCUSED_WINDOW
      );
    }
  }

  @Override
  @RequiredUIAccess
  protected JComponent createSouthPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

    JPanel buttonPanel = new JPanel();

    if (Platform.current().os().isMac()) {
      panel.add(buttonPanel, BorderLayout.EAST);
      buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
      myHelpButton.putClientProperty("JButton.buttonType", "help");

      int index = 0;
      JPanel leftPanel = new JPanel();
      leftPanel.add(myHelpButton);
      TouchbarDataKeys.putDialogButtonDescriptor(myHelpButton, index++);
      JComponent cancelButton = (JComponent)TargetAWT.to(myCancelButton);
      leftPanel.add(cancelButton);
      TouchbarDataKeys.putDialogButtonDescriptor(cancelButton, index++);
      panel.add(leftPanel, BorderLayout.WEST);

      if (mySteps.size() > 1) {
        buttonPanel.add(Box.createHorizontalStrut(5));
        JComponent prevButton = (JComponent)TargetAWT.to(myPreviousButton);
        buttonPanel.add(prevButton);
        TouchbarDataKeys.putDialogButtonDescriptor(prevButton, index++).setMainGroup(true);
      }
      buttonPanel.add(Box.createHorizontalStrut(5));

      JComponent nextButton = (JComponent)TargetAWT.to(myNextButton);
      buttonPanel.add(nextButton);
      TouchbarDataKeys.putDialogButtonDescriptor(nextButton, index++).setMainGroup(true).setDefault(true);
    }
    else {
      panel.add(buttonPanel, BorderLayout.CENTER);
      GroupLayout layout = new GroupLayout(buttonPanel);
      buttonPanel.setLayout(layout);
      layout.setAutoCreateGaps(true);

      final GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();
      final GroupLayout.ParallelGroup vGroup = layout.createParallelGroup();
      final Collection<Component> buttons = new ArrayList<>(5);

      add(hGroup, vGroup, null, Box.createHorizontalGlue());
      if (mySteps.size() > 1) {
        add(hGroup, vGroup, buttons, TargetAWT.to(myPreviousButton));
      }
      add(hGroup, vGroup, buttons, TargetAWT.to(myNextButton), TargetAWT.to(myCancelButton));
      add(hGroup, vGroup, buttons, myHelpButton);

      layout.setHorizontalGroup(hGroup);
      layout.setVerticalGroup(vGroup);
      layout.linkSize(buttons.toArray(new Component[0]));
    }

    myPreviousButton.setEnabled(false);
    myPreviousButton.addClickListener(e -> doPreviousAction());
    myNextButton.addClickListener(e -> {
      if (isLastStep()) {
        // Commit data of current step and perform OK action
        final Step currentStep = mySteps.get(myCurrentStep);
        LOG.assertTrue(currentStep != null);
        try {
          currentStep._commit(true);
          doOKAction();
        }
        catch (final CommitStepException exc) {
          String message = exc.getMessage();
          if (message != null) {
            Messages.showErrorDialog(myContentPanel, message);
          }
        }
      }
      else {
        doNextAction();
      }
    });

    myCancelButton.addClickListener(e -> doCancelAction());
    myHelpButton.addActionListener(e -> helpAction());

    return panel;
  }

  public JPanel getContentComponent() {
    return myContentPanel;
  }

  private static void add(final GroupLayout.Group hGroup, final GroupLayout.Group vGroup, @Nullable final Collection<? super Component> collection, final Component... components) {
    for (Component component : components) {
      hGroup.addComponent(component);
      vGroup.addComponent(component);
      if (collection != null) collection.add(component);
    }
  }

  public static class TallImageComponent extends OpaquePanel {
    private Icon myIcon;

    private TallImageComponent(Icon icon) {
      myIcon = icon;
    }

    @Override
    protected void paintChildren(Graphics g) {
      if (myIcon == null) return;

      paintIcon(g);
    }

    public void paintIcon(Graphics g) {
      if (myIcon == null) {
        return;
      }
      final BufferedImage image = UIUtil.createImage(g, myIcon.getIconWidth(), myIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
      final Graphics2D gg = image.createGraphics();
      myIcon.paintIcon(this, gg, 0, 0);

      final Rectangle bounds = g.getClipBounds();
      int y = myIcon.getIconHeight() - 1;
      while (y < bounds.y + bounds.height) {
        g.drawImage(image, bounds.x, y, bounds.x + bounds.width, y + 1, 0, myIcon.getIconHeight() - 1, bounds.width, myIcon.getIconHeight(), this);

        y++;
      }


      g.drawImage(image, 0, 0, this);
    }

    public void setIcon(Icon icon) {
      myIcon = icon;
      revalidate();
      repaint();
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(myIcon != null ? myIcon.getIconWidth() : 0, 0);
    }

    @Override
    public Dimension getMinimumSize() {
      return new Dimension(myIcon != null ? myIcon.getIconWidth() : 0, 0);
    }
  }

  @Override
  protected JComponent createCenterPanel() {
    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(myContentPanel, BorderLayout.CENTER);
    panel.add(myIcon, BorderLayout.WEST);
    return panel;
  }

  public int getCurrentStep() {
    return myCurrentStep;
  }

  public int getStepCount() {
    return mySteps.size();
  }

  public T getCurrentStepObject() {
    return mySteps.get(myCurrentStep);
  }

  public void addStep(@Nonnull final T step) {
    addStep(step, mySteps.size());
  }

  public void addStep(@Nonnull final T step, int index) {
    mySteps.add(index, step);

    if (step instanceof StepAdapter stepAdapter) {
      stepAdapter.registerStepListener(myStepListener);
    }
    // card layout is used
    final Component component = step.getComponent();
    if (component != null) {
      addStepComponent(component);
    }
  }

  @Override
  protected void init() {
    super.init();
    updateStep();
  }


  protected String addStepComponent(final Component component) {
    String id = myComponentToIdMap.get(component);
    if (id == null) {
      id = Integer.toString(myComponentToIdMap.size());
      myComponentToIdMap.put(component, id);
      myContentPanel.add(component, id);
    }
    return id;
  }

  private void showStepComponent(final Component component) {
    String id = myComponentToIdMap.get(component);
    if (id == null) {
      id = addStepComponent(component);
      myContentPanel.revalidate();
      myContentPanel.repaint();
    }
    ((JBCardLayout)myContentPanel.getLayout()).swipe(myContentPanel, id, myTransitionDirection);
  }

  protected void doPreviousAction() {
    // Commit data of current step
    final Step currentStep = mySteps.get(myCurrentStep);
    LOG.assertTrue(currentStep != null);
    try {
      currentStep._commit(false);
    }
    catch (final CommitStepException exc) {
      Messages.showErrorDialog(myContentPanel, exc.getMessage());
      return;
    }

    myCurrentStep = getPreviousStep(myCurrentStep);
    updateStep(JBCardLayout.SwipeDirection.BACKWARD);
  }

  protected final void updateStep(JBCardLayout.SwipeDirection direction) {
    //it would be better to pass 'direction' to 'updateStep' as a parameter, but since that method is used and overridden in plugins
    // we cannot do it without breaking compatibility
    try {
      myTransitionDirection = direction;
      updateStep();
    }
    finally {
      myTransitionDirection = JBCardLayout.SwipeDirection.AUTO;
    }
  }

  protected void doNextAction() {
    // Commit data of current step
    final Step currentStep = mySteps.get(myCurrentStep);
    LOG.assertTrue(currentStep != null);
    LOG.assertTrue(!isLastStep(), "steps: " + mySteps + " current: " + currentStep);
    try {
      currentStep._commit(false);
    }
    catch (final CommitStepException exc) {
      Messages.showErrorDialog(myContentPanel, exc.getMessage());
      return;
    }

    myCurrentStep = getNextStep(myCurrentStep);
    updateStep(JBCardLayout.SwipeDirection.FORWARD);
  }

  /**
   * override this to provide alternate step order
   *
   * @param step index
   * @return the next step's index
   */
  protected int getNextStep(int step) {
    final int stepCount = mySteps.size();
    if (++step >= stepCount) {
      step = stepCount - 1;
    }
    return step;
  }

  protected final int getNextStep() {
    return getNextStep(getCurrentStep());
  }

  protected T getNextStepObject() {
    int step = getNextStep();
    return mySteps.get(step);
  }

  /**
   * override this to provide alternate step order
   *
   * @param step index
   * @return the previous step's index
   */
  protected int getPreviousStep(int step) {
    if (--step < 0) {
      step = 0;
    }
    return step;
  }

  protected final int getPreviousStep() {
    return getPreviousStep(getCurrentStep());
  }

  protected void updateStep() {
    if (mySteps.isEmpty()) {
      return;
    }

    final Step step = mySteps.get(myCurrentStep);
    LOG.assertTrue(step != null);
    step._init();
    myCurrentStepComponent = step.getComponent();
    LOG.assertTrue(myCurrentStepComponent != null);
    showStepComponent(myCurrentStepComponent);

    Icon icon = step.getIcon();
    if (icon != null) {
      myIcon.setIcon(icon);
      myIcon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
    }

    updateButtons();

    JComponent component = mySteps.get(getCurrentStep()).getPreferredFocusedComponent();
    requestFocusTo(component != null ? component : TargetAWT.to(myNextButton));
  }

  private static void requestFocusTo(final Component component) {
    UiNotifyConnector.doWhenFirstShown(component, () -> {
      final IdeFocusManager focusManager = IdeFocusManager.findInstanceByComponent(component);
      focusManager.requestFocus(component, false);
    });
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    JComponent component = getCurrentStepObject().getPreferredFocusedComponent();
    return component == null ? super.getPreferredFocusedComponent() : component;
  }

  protected boolean canGoNext() {
    return true;
  }

  protected boolean canFinish() {
    return isLastStep() && canGoNext();
  }

  @RequiredUIAccess
  protected void updateButtons() {
    boolean lastStep = isLastStep();
    updateButtons(lastStep, lastStep ? canFinish() : canGoNext(), isFirstStep());
  }

  public void updateWizardButtons() {
    if (!mySteps.isEmpty() && getRootPane() != null) updateButtons();
  }

  @RequiredUIAccess
  public void updateButtons(boolean lastStep, boolean canGoNext, boolean firstStep) {
    if (lastStep) {
      if (mySteps.size() > 1) {
        myNextButton.setText(IdeLocalize.buttonFinish());
      }
      else {
        myNextButton.setText(IdeLocalize.buttonOk());
      }
    }
    else {
      myNextButton.setText(IdeLocalize.buttonWizardNext());
    }
    myNextButton.setEnabled(canGoNext);

    if (myNextButton.isEnabled() && getRootPane() != null) {
      getRootPane().setDefaultButton((JButton)TargetAWT.to(myNextButton));
    }

    myPreviousButton.setEnabled(!firstStep);
  }

  protected boolean isFirstStep() {
    return myCurrentStep == 0;
  }

  protected boolean isLastStep() {
    return myCurrentStep == mySteps.size() - 1 || getCurrentStep() == getNextStep(getCurrentStep());
  }

  protected Button getNextButton() {
    return myNextButton;
  }

  protected Button getPreviousButton() {
    return myPreviousButton;
  }

  protected JButton getHelpButton() {
    return myHelpButton;
  }

  public Button getCancelButton() {
    return myCancelButton;
  }

  public Component getCurrentStepComponent() {
    return myCurrentStepComponent;
  }

  protected void helpAction() {
    HelpManager.getInstance().invokeHelp(getHelpID());
  }

  @Override
  protected void doHelpAction() {
    HelpManager.getInstance().invokeHelp(getHelpID());
  }

  protected int getNumberOfSteps() {
    return mySteps.size();
  }

  @Nullable
  protected abstract String getHelpID();
}
