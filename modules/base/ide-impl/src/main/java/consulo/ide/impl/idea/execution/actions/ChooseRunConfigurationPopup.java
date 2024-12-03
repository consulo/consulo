/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.ide.impl.idea.execution.actions;

import consulo.application.AllIcons;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.execution.*;
import consulo.execution.action.ConfigurationContext;
import consulo.execution.action.ConfigurationFromContext;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorRegistry;
import consulo.execution.impl.internal.configuration.RunManagerImpl;
import consulo.execution.impl.internal.configuration.RunnerAndConfigurationSettingsImpl;
import consulo.execution.impl.internal.configuration.UnknownConfigurationType;
import consulo.execution.internal.PreferredProducerFind;
import consulo.execution.internal.RunManagerEx;
import consulo.execution.runner.ProgramRunner;
import consulo.ide.impl.idea.execution.impl.EditConfigurationsDialog;
import consulo.ide.impl.idea.execution.impl.RunDialog;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.ui.popup.WizardPopup;
import consulo.ide.impl.idea.ui.popup.list.ListPopupImpl;
import consulo.ide.impl.idea.ui.popup.list.PopupListElementRenderer;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.speedSearch.SpeedSearch;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.ui.ex.popup.ListSeparator;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class ChooseRunConfigurationPopup implements ExecutorProvider {

  private final Project myProject;
  @Nonnull
  private final String myAddKey;
  @Nonnull
  private final Executor myDefaultExecutor;
  @Nullable
  private final Executor myAlternativeExecutor;

  private Executor myCurrentExecutor;
  private boolean myEditConfiguration;
  private final RunListPopup myPopup;

  public ChooseRunConfigurationPopup(
    @Nonnull Project project,
    @Nonnull String addKey,
    @Nonnull Executor defaultExecutor,
    @Nullable Executor alternativeExecutor
  ) {
    myProject = project;
    myAddKey = addKey;
    myDefaultExecutor = defaultExecutor;
    myAlternativeExecutor = alternativeExecutor;

    myPopup = new RunListPopup(
      project,
      null,
      new ConfigurationListPopupStep(this, myProject, this, myDefaultExecutor.getActionName().get()),
      null
    );
  }

  public void show() {

    final String adText = getAdText(myAlternativeExecutor);
    if (adText != null) {
      myPopup.setAdText(adText);
    }

    myPopup.showCenteredInCurrentWindow(myProject);
  }

  protected static boolean canRun(@Nonnull final Executor executor, final RunnerAndConfigurationSettings settings) {
    return ProgramRunnerUtil.getRunner(executor.getId(), settings) != null;
  }

  @Nullable
  protected String getAdText(final Executor alternateExecutor) {
    final PropertiesComponent properties = PropertiesComponent.getInstance();
    if (alternateExecutor != null && !properties.isTrueValue(myAddKey)) {
      return String.format(
        "Hold %s to %s",
        KeymapUtil.getKeystrokeText(KeyStroke.getKeyStroke("SHIFT")),
        alternateExecutor.getActionName().get()
      );
    }

    if (!properties.isTrueValue("run.configuration.edit.ad")) {
      return String.format("Press %s to Edit", KeymapUtil.getKeystrokeText(KeyStroke.getKeyStroke("F4")));
    }

    if (!properties.isTrueValue("run.configuration.delete.ad")) {
      return String.format("Press %s to Delete configuration", KeymapUtil.getKeystrokeText(KeyStroke.getKeyStroke("DELETE")));
    }

    return null;
  }

  private void registerActions(final RunListPopup popup) {
    popup.registerAction("alternateExecutor", KeyStroke.getKeyStroke("shift pressed SHIFT"), new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myCurrentExecutor = myAlternativeExecutor;
        updatePresentation();
      }
    });

    popup.registerAction("restoreDefaultExecutor", KeyStroke.getKeyStroke("released SHIFT"), new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myCurrentExecutor = myDefaultExecutor;
        updatePresentation();
      }
    });


    popup.registerAction("invokeAction", KeyStroke.getKeyStroke("shift ENTER"), new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        popup.handleSelect(true);
      }
    });

    popup.registerAction("editConfiguration", KeyStroke.getKeyStroke("F4"), new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myEditConfiguration = true;
        popup.handleSelect(true);
      }
    });


    popup.registerAction("deleteConfiguration", KeyStroke.getKeyStroke("DELETE"), new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        popup.removeSelected();
      }
    });

    popup.registerAction("deleteConfiguration_bksp", KeyStroke.getKeyStroke("BACK_SPACE"), new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        SpeedSearch speedSearch = popup.getSpeedSearch();
        if (speedSearch.isHoldingFilter()) {
          speedSearch.backspace();
          speedSearch.update();
        }
        else {
          popup.removeSelected();
        }
      }
    });

    final Action action0 = createNumberAction(0, popup, myDefaultExecutor);
    final Action action0_ = createNumberAction(0, popup, myAlternativeExecutor);
    popup.registerAction("0Action", KeyStroke.getKeyStroke("0"), action0);
    popup.registerAction("0Action_", KeyStroke.getKeyStroke("shift pressed 0"), action0_);
    popup.registerAction("0Action1", KeyStroke.getKeyStroke("NUMPAD0"), action0);
    popup.registerAction("0Action_1", KeyStroke.getKeyStroke("shift pressed NUMPAD0"), action0_);

    final Action action1 = createNumberAction(1, popup, myDefaultExecutor);
    final Action action1_ = createNumberAction(1, popup, myAlternativeExecutor);
    popup.registerAction("1Action", KeyStroke.getKeyStroke("1"), action1);
    popup.registerAction("1Action_", KeyStroke.getKeyStroke("shift pressed 1"), action1_);
    popup.registerAction("1Action1", KeyStroke.getKeyStroke("NUMPAD1"), action1);
    popup.registerAction("1Action_1", KeyStroke.getKeyStroke("shift pressed NUMPAD1"), action1_);

    final Action action2 = createNumberAction(2, popup, myDefaultExecutor);
    final Action action2_ = createNumberAction(2, popup, myAlternativeExecutor);
    popup.registerAction("2Action", KeyStroke.getKeyStroke("2"), action2);
    popup.registerAction("2Action_", KeyStroke.getKeyStroke("shift pressed 2"), action2_);
    popup.registerAction("2Action1", KeyStroke.getKeyStroke("NUMPAD2"), action2);
    popup.registerAction("2Action_1", KeyStroke.getKeyStroke("shift pressed NUMPAD2"), action2_);

    final Action action3 = createNumberAction(3, popup, myDefaultExecutor);
    final Action action3_ = createNumberAction(3, popup, myAlternativeExecutor);
    popup.registerAction("3Action", KeyStroke.getKeyStroke("3"), action3);
    popup.registerAction("3Action_", KeyStroke.getKeyStroke("shift pressed 3"), action3_);
    popup.registerAction("3Action1", KeyStroke.getKeyStroke("NUMPAD3"), action3);
    popup.registerAction("3Action_1", KeyStroke.getKeyStroke("shift pressed NUMPAD3"), action3_);
  }

  private void updatePresentation() {
    myPopup.setCaption(getExecutor().getActionName().get());
  }

  static void execute(final ItemWrapper itemWrapper, final Executor executor) {
    if (executor == null) {
      return;
    }

    final DataContext dataContext = DataManager.getInstance().getDataContext();
    final Project project = dataContext.getData(Project.KEY);
    if (project != null) {
      SwingUtilities.invokeLater(() -> itemWrapper.perform(project, executor, dataContext));
    }
  }

  void editConfiguration(@Nonnull final Project project, @Nonnull final RunnerAndConfigurationSettings configuration) {
    final Executor executor = getExecutor();
    PropertiesComponent.getInstance().setValue("run.configuration.edit.ad", Boolean.toString(true));
    if (RunDialog.editConfiguration(project, configuration, "Edit configuration settings", executor)) {
      RunManagerEx.getInstanceEx(project).setSelectedConfiguration(configuration);
      ExecutionUtil.runConfiguration(configuration, executor);
    }
  }

  private static void deleteConfiguration(final Project project, @Nonnull final RunnerAndConfigurationSettings configurationSettings) {
    final RunManager manager = RunManager.getInstance(project);
    manager.removeConfiguration(configurationSettings);
  }

  @Override
  @Nonnull
  public Executor getExecutor() {
    return myCurrentExecutor == null ? myDefaultExecutor : myCurrentExecutor;
  }

  private static Action createNumberAction(final int number, final ListPopupImpl listPopup, final Executor executor) {
    return new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (listPopup.getSpeedSearch().isHoldingFilter()) return;
        for (final Object item : listPopup.getListStep().getValues()) {
          if (item instanceof ItemWrapper itemWrapper && itemWrapper.getMnemonic() == number) {
            listPopup.setFinalRunnable(() -> execute(itemWrapper, executor));
            listPopup.closeOk(null);
          }
        }
      }
    };
  }

  private abstract static class Wrapper {
    private int myMnemonic = -1;
    private final boolean myAddSeparatorAbove;
    private boolean myChecked;

    protected Wrapper(boolean addSeparatorAbove) {
      myAddSeparatorAbove = addSeparatorAbove;
    }

    public int getMnemonic() {
      return myMnemonic;
    }

    public boolean isChecked() {
      return myChecked;
    }

    public void setChecked(boolean checked) {
      myChecked = checked;
    }

    public void setMnemonic(int mnemonic) {
      myMnemonic = mnemonic;
    }

    public boolean addSeparatorAbove() {
      return myAddSeparatorAbove;
    }

    @Nullable
    public abstract Image getIcon();

    public abstract String getText();

    public boolean canBeDeleted() {
      return false;
    }

    @Override
    public String toString() {
      return "Wrapper[" + getText() + "]";
    }
  }

  public abstract static class ItemWrapper<T> extends Wrapper {
    private final T myValue;
    private boolean myDynamic;

    protected ItemWrapper(@Nullable final T value) {
      this(value, false);
    }

    protected ItemWrapper(@Nullable final T value, boolean addSeparatorAbove) {
      super(addSeparatorAbove);
      myValue = value;
    }

    public T getValue() {
      return myValue;
    }

    public boolean isDynamic() {
      return myDynamic;
    }

    public void setDynamic(final boolean b) {
      myDynamic = b;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof ItemWrapper)) return false;

      ItemWrapper that = (ItemWrapper)o;

      return myValue != null ? myValue.equals(that.myValue) : that.myValue == null;
    }

    @Override
    public int hashCode() {
      return myValue != null ? myValue.hashCode() : 0;
    }

    public abstract void perform(@Nonnull final Project project, @Nonnull final Executor executor, @Nonnull final DataContext context);

    @Nullable
    public ConfigurationType getType() {
      return null;
    }

    public boolean available(Executor executor) {
      return false;
    }

    public boolean hasActions() {
      return false;
    }

    public PopupStep getNextStep(Project project, ChooseRunConfigurationPopup action) {
      return PopupStep.FINAL_CHOICE;
    }

    public static ItemWrapper wrap(
      @Nonnull final Project project,
      @Nonnull final RunnerAndConfigurationSettings settings,
      final boolean dynamic
    ) {
      final ItemWrapper result = wrap(project, settings);
      result.setDynamic(dynamic);
      return result;
    }

    public static ItemWrapper wrap(@Nonnull final Project project, @Nonnull final RunnerAndConfigurationSettings settings) {
      return new ItemWrapper<>(settings) {
        @Override
        public void perform(@Nonnull Project project, @Nonnull Executor executor, @Nonnull DataContext context) {
          RunnerAndConfigurationSettings config = getValue();
          RunManagerEx.getInstanceEx(project).setSelectedConfiguration(config);
          ExecutionUtil.runConfiguration(config, executor);
        }

        @Override
        public ConfigurationType getType() {
          return getValue().getType();
        }

        @Override
        public Image getIcon() {
          return RunManagerEx.getInstanceEx(project).getConfigurationIcon(getValue());
        }

        @Override
        public String getText() {
          return getValue().getName();
        }

        @Override
        public boolean hasActions() {
          return true;
        }

        @Override
        public boolean available(Executor executor) {
          return ProgramRunnerUtil.getRunner(executor.getId(), getValue()) != null;
        }

        @Override
        public PopupStep getNextStep(@Nonnull final Project project, @Nonnull final ChooseRunConfigurationPopup action) {
          return new ConfigurationActionsStep(project, action, getValue(), isDynamic());
        }
      };
    }

    @Override
    public boolean canBeDeleted() {
      return !isDynamic() && getValue() instanceof RunnerAndConfigurationSettings;
    }
  }

  private static final class ConfigurationListPopupStep extends BaseListPopupStep<ItemWrapper> {
    private final Project myProject;
    private final ChooseRunConfigurationPopup myAction;
    private int myDefaultConfiguration = -1;

    @RequiredUIAccess
    private ConfigurationListPopupStep(
      @Nonnull final ChooseRunConfigurationPopup action,
      @Nonnull final Project project,
      @Nonnull final ExecutorProvider executorProvider,
      @Nonnull final String title
    ) {
      super(title, createSettingsList(project, executorProvider, true));
      myProject = project;
      myAction = action;

      if (-1 == getDefaultOptionIndex()) {
        myDefaultConfiguration = getDynamicIndex();
      }
    }

    private int getDynamicIndex() {
      int i = 0;
      for (final ItemWrapper wrapper : getValues()) {
        if (wrapper.isDynamic()) {
          return i;
        }
        i++;
      }

      return -1;
    }

    @Override
    public boolean isAutoSelectionEnabled() {
      return false;
    }

    @Override
    public ListSeparator getSeparatorAbove(ItemWrapper value) {
      if (value.addSeparatorAbove()) return new ListSeparator();

      final List<ItemWrapper> configurations = getValues();
      final int index = configurations.indexOf(value);
      if (index > 0 && index <= configurations.size() - 1) {
        final ItemWrapper aboveConfiguration = configurations.get(index - 1);

        if (aboveConfiguration != null && aboveConfiguration.isDynamic() != value.isDynamic()) {
          return new ListSeparator();
        }

        final ConfigurationType currentType = value.getType();
        final ConfigurationType aboveType = aboveConfiguration == null ? null : aboveConfiguration.getType();
        if (aboveType != currentType && currentType != null) {
          return new ListSeparator(); // new ListSeparator(currentType.getDisplayName());
        }
      }

      return null;
    }

    @Override
    public boolean isSpeedSearchEnabled() {
      return true;
    }

    @Override
    public int getDefaultOptionIndex() {
      final RunnerAndConfigurationSettings currentConfiguration = RunManager.getInstance(myProject).getSelectedConfiguration();
      if (currentConfiguration == null && myDefaultConfiguration != -1) {
        return myDefaultConfiguration;
      }

      return currentConfiguration instanceof RunnerAndConfigurationSettingsImpl
        ? getValues().indexOf(ItemWrapper.wrap(myProject, currentConfiguration)) : -1;
    }

    @Override
    public PopupStep onChosen(final ItemWrapper wrapper, boolean finalChoice) {
      if (myAction.myEditConfiguration) {
        final Object o = wrapper.getValue();
        if (o instanceof RunnerAndConfigurationSettingsImpl runnerAndConfigurationSettings) {
          return doFinalStep(() -> myAction.editConfiguration(myProject, runnerAndConfigurationSettings));
        }
      }

      if (finalChoice && wrapper.available(myAction.getExecutor())) {
        return doFinalStep(() -> {
          if (myAction.getExecutor() == myAction.myAlternativeExecutor) {
            PropertiesComponent.getInstance().setValue(myAction.myAddKey, Boolean.toString(true));
          }

          wrapper.perform(myProject, myAction.getExecutor(), DataManager.getInstance().getDataContext());
        });
      }
      else {
        return wrapper.getNextStep(myProject, myAction);
      }
    }

    @Override
    public boolean hasSubstep(ItemWrapper selectedValue) {
      return selectedValue.hasActions();
    }

    @Nonnull
    @Override
    public String getTextFor(ItemWrapper value) {
      return value.getText();
    }

    @Override
    public Image getIconFor(ItemWrapper value) {
      return value.getIcon();
    }
  }

  private static final class ConfigurationActionsStep extends BaseListPopupStep<ActionWrapper> {

    @Nonnull
    private final RunnerAndConfigurationSettings mySettings;
    @Nonnull
    private final Project myProject;

    private ConfigurationActionsStep(
      @Nonnull final Project project,
      ChooseRunConfigurationPopup action,
      @Nonnull final RunnerAndConfigurationSettings settings,
      final boolean dynamic
    ) {
      super(null, buildActions(project, action, settings, dynamic));
      myProject = project;
      mySettings = settings;
    }

    @Nonnull
    public RunnerAndConfigurationSettings getSettings() {
      return mySettings;
    }

    public String getName() {
      return mySettings.getName();
    }

    public Image getIcon() {
      return RunManagerEx.getInstanceEx(myProject).getConfigurationIcon(mySettings);
    }

    @Override
    public ListSeparator getSeparatorAbove(ActionWrapper value) {
      return value.addSeparatorAbove() ? new ListSeparator() : null;
    }

    private static ActionWrapper[] buildActions(
      @Nonnull final Project project,
      final ChooseRunConfigurationPopup action,
      @Nonnull final RunnerAndConfigurationSettings settings,
      final boolean dynamic
    ) {
      final List<ActionWrapper> result = new ArrayList<>();

      final ExecutionTarget active = ExecutionTargetManager.getActiveTarget(project);
      for (final ExecutionTarget eachTarget : ExecutionTargetManager.getTargetsToChooseFor(project, settings.getConfiguration())) {
        result.add(new ActionWrapper(eachTarget.getDisplayName(), eachTarget.getIcon()) {
          {
            setChecked(eachTarget.equals(active));
          }

          @Override
          public void perform() {
            final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
            if (dynamic) {
              manager.setTemporaryConfiguration(settings);
            }
            manager.setSelectedConfiguration(settings);

            ExecutionTargetManager.setActiveTarget(project, eachTarget);
            ExecutionUtil.runConfiguration(settings, action.getExecutor());
          }
        });
      }

      boolean isFirst = true;
      for (final Executor executor : ExecutorRegistry.getInstance().getRegisteredExecutors()) {
        final ProgramRunner runner = RunnerRegistry.getInstance().getRunner(executor.getId(), settings.getConfiguration());
        if (runner != null) {
          result.add(new ActionWrapper(executor.getActionName().get(), executor.getIcon(), isFirst) {
            @Override
            public void perform() {
              final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
              if (dynamic) {
                manager.setTemporaryConfiguration(settings);
              }
              manager.setSelectedConfiguration(settings);
              ExecutionUtil.runConfiguration(settings, executor);
            }
          });
          isFirst = false;
        }
      }

      result.add(new ActionWrapper("Edit...", AllIcons.Actions.EditSource, true) {
        @Override
        public void perform() {
          final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
          if (dynamic) manager.setTemporaryConfiguration(settings);
          action.editConfiguration(project, settings);
        }
      });

      if (settings.isTemporary() || dynamic) {
        result.add(new ActionWrapper("Save configuration", AllIcons.Actions.Menu_saveall) {
          @Override
          public void perform() {
            final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
            if (dynamic) manager.setTemporaryConfiguration(settings);
            manager.makeStable(settings);
          }
        });
      }

      return result.toArray(new ActionWrapper[result.size()]);
    }

    @Override
    public PopupStep onChosen(final ActionWrapper selectedValue, boolean finalChoice) {
      return doFinalStep(() -> selectedValue.perform());
    }

    @Override
    public Image getIconFor(ActionWrapper aValue) {
      return aValue.getIcon();
    }

    @Nonnull
    @Override
    public String getTextFor(ActionWrapper value) {
      return value.getText();
    }
  }

  private abstract static class ActionWrapper extends Wrapper {
    private final String myName;
    private final Image myIcon;

    private ActionWrapper(String name, Image icon) {
      this(name, icon, false);
    }

    private ActionWrapper(String name, Image icon, boolean addSeparatorAbove) {
      super(addSeparatorAbove);
      myName = name;
      myIcon = icon;
    }

    public abstract void perform();

    @Override
    public String getText() {
      return myName;
    }

    @Override
    public Image getIcon() {
      return myIcon;
    }
  }

  private static class RunListElementRenderer extends PopupListElementRenderer {
    private JLabel myLabel;
    private final ListPopupImpl myPopup1;
    private final boolean myHasSideBar;

    private RunListElementRenderer(ListPopupImpl popup, boolean hasSideBar) {
      super(popup);

      myPopup1 = popup;
      myHasSideBar = hasSideBar;
    }

    @Override
    protected JComponent createItemComponent() {
      if (myLabel == null) {
        myLabel = new JLabel();
        myLabel.setPreferredSize(new JLabel("8.").getPreferredSize());
      }

      final JComponent result = super.createItemComponent();
      result.add(myLabel, BorderLayout.WEST);
      return result;
    }

    @Override
    protected void customizeComponent(JList list, Object value, boolean isSelected) {
      super.customizeComponent(list, value, isSelected);

      myLabel.setVisible(myHasSideBar);

      ListPopupStep<Object> step = myPopup1.getListStep();
      boolean isSelectable = step.isSelectable(value);
      myLabel.setEnabled(isSelectable);
      myLabel.setIcon(null);

      if (isSelected) {
        setSelected(myLabel);
      }
      else {
        setDeselected(myLabel);
      }

      if (value instanceof Wrapper wrapper) {
        final int mnemonic = wrapper.getMnemonic();
        if (mnemonic != -1 && !myPopup1.getSpeedSearch().isHoldingFilter()) {
          myLabel.setText(mnemonic + ".");
          myLabel.setDisplayedMnemonicIndex(0);
        }
        else {
          if (wrapper.isChecked()) {
            myTextLabel.setIcon(TargetAWT.to(
              isSelected ? PlatformIconGroup.actionsChecked_selected() : PlatformIconGroup.actionsChecked()
            ));
          }
          else {
            if (myTextLabel.getIcon() == null) {
              myTextLabel.setIcon(TargetAWT.to(Image.empty(Image.DEFAULT_ICON_SIZE)));
            }
          }
          myLabel.setText("");
        }
      }
    }
  }

  private class RunListPopup extends ListPopupImpl {
    RunListPopup(Project project, WizardPopup aParent, ListPopupStep aStep, Object parentValue) {
      super(project, aParent, aStep, parentValue);
      registerActions(this);
    }

    @Override
    protected WizardPopup createPopup(WizardPopup parent, PopupStep step, Object parentValue) {
      return new RunListPopup(getProject(), parent, (ListPopupStep)step, parentValue);
    }

    @Override
    public void handleSelect(boolean handleFinalChoices, InputEvent e) {
      if (e instanceof MouseEvent && e.isShiftDown()) {
        handleShiftClick(handleFinalChoices, e, this);
        return;
      }

      _handleSelect(handleFinalChoices, e);
    }

    private void _handleSelect(boolean handleFinalChoices, InputEvent e) {
      super.handleSelect(handleFinalChoices, e);
    }

    protected void handleShiftClick(boolean handleFinalChoices, final InputEvent inputEvent, final RunListPopup popup) {
      myCurrentExecutor = myAlternativeExecutor;
      popup._handleSelect(handleFinalChoices, inputEvent);
    }

    @Override
    protected ListCellRenderer getListElementRenderer() {
      boolean hasSideBar = false;
      for (Object each : getListStep().getValues()) {
        if (each instanceof Wrapper wrapper && wrapper.getMnemonic() != -1) {
          hasSideBar = true;
          break;
        }
      }
      return new RunListElementRenderer(this, hasSideBar);
    }

    public void removeSelected() {
      final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
      if (!propertiesComponent.isTrueValue("run.configuration.delete.ad")) {
        propertiesComponent.setValue("run.configuration.delete.ad", Boolean.toString(true));
      }

      final int index = getSelectedIndex();
      if (index == -1) {
        return;
      }

      final Object o = getListModel().get(index);
      if (o != null && o instanceof ItemWrapper itemWrapper && itemWrapper.canBeDeleted()) {
        deleteConfiguration(myProject, (RunnerAndConfigurationSettings)itemWrapper.getValue());
        getListModel().deleteItem(o);
        final List<Object> values = getListStep().getValues();
        values.remove(o);

        if (index < values.size()) {
          onChildSelectedFor(values.get(index));
        }
        else if (index - 1 >= 0) {
          onChildSelectedFor(values.get(index - 1));
        }
      }
    }
  }

  private static class FolderWrapper extends ItemWrapper<String> {
    private final Project myProject;
    private final ExecutorProvider myExecutorProvider;
    private final List<RunnerAndConfigurationSettings> myConfigurations;

    private FolderWrapper(
      Project project,
      ExecutorProvider executorProvider,
      @Nullable String value,
      List<RunnerAndConfigurationSettings> configurations
    ) {
      super(value);
      myProject = project;
      myExecutorProvider = executorProvider;
      myConfigurations = configurations;
    }

    @Override
    public void perform(@Nonnull Project project, @Nonnull Executor executor, @Nonnull DataContext context) {
      RunnerAndConfigurationSettings selectedConfiguration = RunManagerEx.getInstanceEx(project).getSelectedConfiguration();
      if (myConfigurations.contains(selectedConfiguration)) {
        RunManagerEx.getInstanceEx(project).setSelectedConfiguration(selectedConfiguration);
        ExecutionUtil.runConfiguration(selectedConfiguration, myExecutorProvider.getExecutor());
      }
    }

    @Nullable
    @Override
    public Image getIcon() {
      return AllIcons.Nodes.Folder;
    }

    @Override
    public String getText() {
      return getValue();
    }

    @Override
    public boolean hasActions() {
      return true;
    }

    @Override
    public PopupStep getNextStep(Project project, ChooseRunConfigurationPopup action) {
      List<ConfigurationActionsStep> steps = new ArrayList<>();
      for (RunnerAndConfigurationSettings settings : myConfigurations) {
        steps.add(new ConfigurationActionsStep(project, action, settings, false));
      }
      return new FolderStep(myProject, myExecutorProvider, null, steps);
    }
  }

  private static final class FolderStep extends BaseListPopupStep<ConfigurationActionsStep> {
    private final Project myProject;
    private final ExecutorProvider myExecutorProvider;

    private FolderStep(Project project, ExecutorProvider executorProvider, String folderName, List<ConfigurationActionsStep> children) {
      super(folderName, children, new ArrayList<>());
      myProject = project;
      myExecutorProvider = executorProvider;
    }

    @Override
    public PopupStep onChosen(final ConfigurationActionsStep selectedValue, boolean finalChoice) {
      if (finalChoice) {
        return doFinalStep(() -> {
          RunnerAndConfigurationSettings settings = selectedValue.getSettings();
          RunManagerEx.getInstanceEx(myProject).setSelectedConfiguration(settings);
          ExecutionUtil.runConfiguration(settings, myExecutorProvider.getExecutor());
        });
      }
      else {
        return selectedValue;
      }
    }

    @Override
    public Image getIconFor(ConfigurationActionsStep aValue) {
      return aValue.getIcon();
    }

    @Nonnull
    @Override
    public String getTextFor(ConfigurationActionsStep value) {
      return value.getName();
    }

    @Override
    public boolean hasSubstep(ConfigurationActionsStep selectedValue) {
      return !selectedValue.getValues().isEmpty();
    }
  }

  public static List<ItemWrapper> createFlatSettingsList(@Nonnull Project project) {
    return RunManagerImpl.getInstanceImpl(project)
      .getConfigurationsGroupedByTypeAndFolder(false)
      .values()
      .stream()
      .flatMap(map -> map.values().stream().flatMap(Collection::stream))
      .map(settings -> ItemWrapper.wrap(project, settings))
      .collect(Collectors.toList());
  }

  @RequiredUIAccess
  public static ItemWrapper[] createSettingsList(
    @Nonnull Project project,
    @Nonnull ExecutorProvider executorProvider,
    boolean createEditAction
  ) {
    List<ItemWrapper> result = new ArrayList<>();

    if (createEditAction) {
      ItemWrapper<Void> edit = new ItemWrapper<>(null) {
        @Override
        public Image getIcon() {
          return AllIcons.Actions.EditSource;
        }

        @Override
        public String getText() {
          return UIUtil.removeMnemonic(ActionLocalize.actionEditrunconfigurationsText().get());
        }

        @Override
        @RequiredUIAccess
        public void perform(@Nonnull final Project project, @Nonnull final Executor executor, @Nonnull DataContext context) {
          if (new EditConfigurationsDialog(project) {
            @Override
            protected void init() {
              setOKButtonText(executor.getStartActionText());
              setOKButtonIcon(TargetAWT.to(executor.getIcon()));
              myExecutor = executor;
              super.init();
            }
          }.showAndGet()) {
            project.getApplication().invokeLater(() -> {
              RunnerAndConfigurationSettings configuration = RunManager.getInstance(project).getSelectedConfiguration();
              if (configuration != null) {
                ExecutionUtil.runConfiguration(configuration, executor);
              }
            }, project.getDisposed());
          }
        }

        @Override
        public boolean available(Executor executor) {
          return true;
        }
      };
      edit.setMnemonic(0);
      result.add(edit);
    }

    RunManagerEx manager = RunManagerEx.getInstanceEx(project);
    final RunnerAndConfigurationSettings selectedConfiguration = manager.getSelectedConfiguration();
    if (selectedConfiguration != null) {
      boolean isFirst = true;
      final ExecutionTarget activeTarget = ExecutionTargetManager.getActiveTarget(project);
      for (ExecutionTarget eachTarget : ExecutionTargetManager.getTargetsToChooseFor(project, selectedConfiguration.getConfiguration())) {
        result.add(new ItemWrapper<>(eachTarget, isFirst) {
          {
            setChecked(getValue().equals(activeTarget));
          }

          @Override
          public Image getIcon() {
            return getValue().getIcon();
          }

          @Override
          public String getText() {
            return getValue().getDisplayName();
          }

          @Override
          public void perform(@Nonnull final Project project, @Nonnull final Executor executor, @Nonnull DataContext context) {
            ExecutionTargetManager.setActiveTarget(project, getValue());
            ExecutionUtil.runConfiguration(selectedConfiguration, executor);
          }

          @Override
          public boolean available(Executor executor) {
            return true;
          }
        });
        isFirst = false;
      }
    }

    Map<RunnerAndConfigurationSettings, ItemWrapper> wrappedExisting = new LinkedHashMap<>();
    for (ConfigurationType type : manager.getConfigurationFactories()) {
      if (!(type instanceof UnknownConfigurationType)) {
        Map<String, List<RunnerAndConfigurationSettings>> structure = manager.getStructure(type);
        for (Map.Entry<String, List<RunnerAndConfigurationSettings>> entry : structure.entrySet()) {
          if (entry.getValue().isEmpty()) {
            continue;
          }

          final String key = entry.getKey();
          if (key != null) {
            boolean isSelected = entry.getValue().contains(selectedConfiguration);
            if (isSelected) {
              assert selectedConfiguration != null;
            }
            FolderWrapper folderWrapper = new FolderWrapper(
              project,
              executorProvider,
              key + (isSelected ? "  (mnemonic is to \"" + selectedConfiguration.getName() + "\")" : ""),
              entry.getValue()
            );
            if (isSelected) {
              folderWrapper.setMnemonic(1);
            }
            result.add(folderWrapper);
          }
          else {
            for (RunnerAndConfigurationSettings configuration : entry.getValue()) {
              final ItemWrapper wrapped = ItemWrapper.wrap(project, configuration);
              if (configuration == selectedConfiguration) {
                wrapped.setMnemonic(1);
              }
              wrappedExisting.put(configuration, wrapped);
            }
          }
        }
      }
    }
    if (!DumbService.isDumb(project)) {
      populateWithDynamicRunners(result, wrappedExisting, project, manager, selectedConfiguration);
    }
    result.addAll(wrappedExisting.values());
    return result.toArray(new ItemWrapper[result.size()]);
  }

  @Nonnull
  @RequiredUIAccess
  private static List<RunnerAndConfigurationSettings> populateWithDynamicRunners(
    final List<ItemWrapper> result,
    Map<RunnerAndConfigurationSettings, ItemWrapper> existing,
    final Project project,
    final RunManagerEx manager,
    final RunnerAndConfigurationSettings selectedConfiguration
  ) {
    final ArrayList<RunnerAndConfigurationSettings> contextConfigurations = new ArrayList<>();
    if (!EventQueue.isDispatchThread()) {
      return Collections.emptyList();
    }

    final DataContext dataContext = DataManager.getInstance().getDataContext();
    final ConfigurationContext context = ConfigurationContext.getFromContext(dataContext);

    final List<ConfigurationFromContext> producers =
      PreferredProducerFind.getConfigurationsFromContext(context.getLocation(), context, false);
    if (producers == null) return Collections.emptyList();

    Collections.sort(producers, ConfigurationFromContext.NAME_COMPARATOR);

    final RunnerAndConfigurationSettings[] preferred = {null};

    int i = 2; // selectedConfiguration == null ? 1 : 2;
    for (final ConfigurationFromContext fromContext : producers) {
      final RunnerAndConfigurationSettings configuration = fromContext.getConfigurationSettings();
      if (existing.keySet().contains(configuration)) {
        final ItemWrapper wrapper = existing.get(configuration);
        if (wrapper.getMnemonic() != 1) {
          wrapper.setMnemonic(i);
          i++;
        }
      }
      else {
        if (selectedConfiguration != null && configuration.equals(selectedConfiguration)) continue;
        contextConfigurations.add(configuration);

        if (preferred[0] == null) {
          preferred[0] = configuration;
        }

        //noinspection unchecked
        final ItemWrapper wrapper = new ItemWrapper(configuration) {
          @Override
          public Image getIcon() {
            return RunManagerEx.getInstanceEx(project).getConfigurationIcon(configuration);
          }

          @Override
          public String getText() {
            return configuration.getName();
          }

          @Override
          public boolean available(Executor executor) {
            return canRun(executor, configuration);
          }

          @Override
          public void perform(@Nonnull Project project, @Nonnull Executor executor, @Nonnull DataContext context) {
            manager.setTemporaryConfiguration(configuration);
            RunManagerEx.getInstanceEx(project).setSelectedConfiguration(configuration);
            ExecutionUtil.runConfiguration(configuration, executor);
          }

          @Override
          public PopupStep getNextStep(@Nonnull final Project project, @Nonnull final ChooseRunConfigurationPopup action) {
            return new ConfigurationActionsStep(project, action, configuration, isDynamic());
          }

          @Override
          public boolean hasActions() {
            return true;
          }
        };

        wrapper.setDynamic(true);
        wrapper.setMnemonic(i);
        result.add(wrapper);
        i++;
      }
    }

    return contextConfigurations;
  }
}

