/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.ui.awt;

import consulo.execution.configuration.EnvironmentVariable;
import consulo.execution.configuration.EnvironmentVariablesData;
import consulo.execution.localize.ExecutionLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.cmd.GeneralCommandLine;
import consulo.proxy.EventDispatcher;
import consulo.ui.TextBoxWithExtensions;
import consulo.ui.ex.UserActivityProviderComponent;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Table;
import consulo.ui.TableItemEditor;
import consulo.ui.TextBox;
import consulo.ui.UIAccess;
import consulo.ui.ValueComponent;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.toolbar.AddAction;
import consulo.ui.ex.toolbar.DownMoveAction;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilderFactory;
import consulo.ui.ex.toolbar.UpMoveAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class EnvironmentVariablesTextFieldWithBrowseButton implements UserActivityProviderComponent {

  private final EventDispatcher<ChangeListener> myListeners = EventDispatcher.create(ChangeListener.class);

  private EnvironmentVariablesData myData = EnvironmentVariablesData.DEFAULT;

  private final TextBoxWithExtensions myTextBox;

  public EnvironmentVariablesTextFieldWithBrowseButton() {
    myTextBox = TextBoxWithExtensions.create();
    myTextBox.setPlaceholder(LocalizeValue.localizeTODO("Separate variables with semicolon: VAR=value; VAR1=value1"));

    myTextBox.addLastExtension(new TextBoxWithExtensions.Extension(false, PlatformIconGroup.generalInlinevariables(), PlatformIconGroup.generalInlinevariableshover(),
                                                                   event -> showEnvironmentVariablesDialog()));

    myTextBox.addValueListener(event -> {
      if (!StringUtil.equals(stringifyEnvs(myData), event.getValue())) {
        Map<String, String> textEnvs = EnvVariablesTable.parseEnvsFromText(event.getValue());
        myData = myData.with(textEnvs);
        fireStateChanged();
      }
    });
  }

  
  public Component getComponent() {
    return myTextBox;
  }

  /**
   * @return unmodifiable Map instance
   */
  public Map<String, String> getEnvs() {
    return myData.getEnvs();
  }

  /**
   * @param envs Map instance containing user-defined environment variables
   *             (iteration order should be reliable user-specified, like {@link LinkedHashMap} or {@link ImmutableMap})
   */
  public void setEnvs(Map<String, String> envs) {
    setData(EnvironmentVariablesData.create(envs, myData.isPassParentEnvs()));
  }

  
  public EnvironmentVariablesData getData() {
    return myData;
  }

  public void setData(EnvironmentVariablesData data) {
    EnvironmentVariablesData oldData = myData;
    myData = data;
    myTextBox.setValue(stringifyEnvs(data.getEnvs()));
    if (!oldData.equals(data)) {
      fireStateChanged();
    }
  }

  
  protected String stringifyEnvs(EnvironmentVariablesData evd) {
    if (evd.getEnvs().isEmpty()) {
      return "";
    }
    StringBuilder buf = new StringBuilder();
    for (Map.Entry<String, String> entry : evd.getEnvs().entrySet()) {
      if (buf.length() > 0) {
        buf.append(";");
      }
      buf.append(StringUtil.escapeChar(entry.getKey(), ';')).append("=").append(StringUtil.escapeChar(entry.getValue(), ';'));
    }
    return buf.toString();
  }

  
  private static String stringifyEnvs(Map<String, String> envs) {
    if (envs.isEmpty()) {
      return "";
    }
    StringBuilder buf = new StringBuilder();
    for (Map.Entry<String, String> entry : envs.entrySet()) {
      if (buf.length() > 0) {
        buf.append(";");
      }
      buf.append(entry.getKey()).append("=").append(entry.getValue());
    }
    return buf.toString();
  }

  public boolean isPassParentEnvs() {
    return myData.isPassParentEnvs();
  }

  public void setPassParentEnvs(boolean passParentEnvs) {
    setData(EnvironmentVariablesData.create(myData.getEnvs(), passParentEnvs));
  }

  @Override
  public void addChangeListener(ChangeListener changeListener) {
    myListeners.addListener(changeListener);
  }

  @Override
  public void removeChangeListener(ChangeListener changeListener) {
    myListeners.removeListener(changeListener);
  }

  private void fireStateChanged() {
    myListeners.getMulticaster().stateChanged(new ChangeEvent(this));
  }

  @RequiredUIAccess
  private void showEnvironmentVariablesDialog() {
    EnvironmentVariablesDialogDescriptor descriptor = new EnvironmentVariablesDialogDescriptor(myData);
    UIAccess uiAccess = UIAccess.current();
    Application.get().getInstance(DialogService.class).build(myTextBox, descriptor).showAsync().whenComplete((value, error) -> {
      if (error != null || value == null) {
        return;
      }
      uiAccess.give(() -> setData(descriptor.getData()));
    });
  }

  @RequiredUIAccess
  private static void showParentEnvironmentDialog(Component parent) {
    Map<String, String> parentEnvironment = new TreeMap<>(new GeneralCommandLine().getParentEnvironment());
    Application.get().getInstance(DialogService.class).build(parent, new SystemEnvironmentDialogDescriptor(parentEnvironment)).showAsync();
  }

  @RequiredUIAccess
  private static Table<EnvironmentVariable> createVariablesTable(MutableFlatDataModel<EnvironmentVariable> model, boolean editable) {
    Table<EnvironmentVariable> table = Table.create(model);
    table.addColumn(ExecutionLocalize.environmentVariablesNameColumn(), EnvironmentVariable::getName)
      .setEditor(editable ? new TableItemEditor<>() {
        @RequiredUIAccess
        @Override
        public ValueComponent<String> createComponent(EnvironmentVariable variable) {
          return TextBox.create(variable.getName());
        }

        @RequiredUIAccess
        @Override
        public void commit(EnvironmentVariable variable, @Nullable String value) {
          variable.setName(StringUtil.notNullize(value));
        }
      } : null);
    table.addColumn(ExecutionLocalize.environmentVariablesValueColumn(), EnvironmentVariable::getValue)
      .setEditor(editable ? new TableItemEditor<>() {
        @RequiredUIAccess
        @Override
        public ValueComponent<String> createComponent(EnvironmentVariable variable) {
          return TextBox.create(variable.getValue());
        }

        @RequiredUIAccess
        @Override
        public void commit(EnvironmentVariable variable, @Nullable String value) {
          variable.setValue(StringUtil.notNullize(value));
        }
      } : null);
    return table;
  }

  private static List<EnvironmentVariable> convertToVariables(Map<String, String> map, boolean readOnly) {
    List<EnvironmentVariable> variables = new ArrayList<>(map.size());
    for (Map.Entry<String, String> entry : map.entrySet()) {
      variables.add(new EnvironmentVariable(entry.getKey(), entry.getValue(), readOnly));
    }
    return variables;
  }

  private static class EnvironmentVariablesDialogDescriptor extends DialogDescriptor {
    private final MutableFlatDataModel<EnvironmentVariable> myModel;
    private boolean myPassParentEnvs;

    private EnvironmentVariablesDialogDescriptor(EnvironmentVariablesData data) {
      super(ExecutionLocalize.environmentVariablesDialogTitle());
      myModel = FlatDataModel.of(convertToVariables(data.getEnvs(), false));
      myPassParentEnvs = data.isPassParentEnvs();
    }

    @Override
    public WidthAndHeight getInitialSize() {
      return WidthAndHeight.ofFont(50, 25);
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
      Table<EnvironmentVariable> table = createVariablesTable(myModel, true);

      Component tablePanel = ToolbarDecoratorBuilderFactory.getInstance()
        .create(table)
        .addOrReplaceAction(new AddAction<>() {
          @Override
          @RequiredUIAccess
          protected void doAdd(AnActionEvent e) {
            EnvironmentVariable variable = new EnvironmentVariable("", "", false);
            myModel.add(variable);
            table.select(variable);
          }
        })
        .disableAction(UpMoveAction.class)
        .disableAction(DownMoveAction.class)
        .build();

      CheckBox passParentEnvsBox = CheckBox.create(ExecutionLocalize.envVarsCheckboxTitle(), myPassParentEnvs);
      passParentEnvsBox.addValueListener(event -> myPassParentEnvs = Boolean.TRUE.equals(event.getValue()));

      Hyperlink showSystemLink = Hyperlink.create(ExecutionLocalize.envVarsShowSystem(), event -> showParentEnvironmentDialog(tablePanel));

      DockLayout bottomPanel = DockLayout.create();
      bottomPanel.center(passParentEnvsBox);
      bottomPanel.right(showSystemLink);

      DockLayout panel = DockLayout.create();
      panel.center(tablePanel);
      panel.bottom(bottomPanel);
      return panel;
    }

    private EnvironmentVariablesData getData() {
      Map<String, String> envs = new LinkedHashMap<>();
      for (int i = 0; i < myModel.getSize(); i++) {
        EnvironmentVariable variable = myModel.get(i);
        if (!StringUtil.isEmpty(variable.getName())) {
          envs.put(variable.getName(), variable.getValue());
        }
      }
      return EnvironmentVariablesData.create(envs, myPassParentEnvs);
    }
  }

  private static class SystemEnvironmentDialogDescriptor extends DialogDescriptor {
    private final Map<String, String> myEnvironment;

    private SystemEnvironmentDialogDescriptor(Map<String, String> environment) {
      super(ExecutionLocalize.environmentVariablesSystemDialogTitle());
      myEnvironment = environment;
    }

    @Override
    public WidthAndHeight getInitialSize() {
      return WidthAndHeight.ofFont(70, 35);
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
      return createVariablesTable(FlatDataModel.of(convertToVariables(myEnvironment, true)), false);
    }

    @Override
    public AnAction[] createActions(boolean inverseOrder) {
      return new AnAction[]{createOkAction()};
    }
  }
}
