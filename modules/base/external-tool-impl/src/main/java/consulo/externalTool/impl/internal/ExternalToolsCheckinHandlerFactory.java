/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.externalTool.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.externalTool.impl.internal.localize.ExternalToolLocalize;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.change.CommitContext;
import consulo.versionControlSystem.checkin.CheckinHandler;
import consulo.versionControlSystem.checkin.CheckinHandlerFactory;
import consulo.versionControlSystem.checkin.CheckinProjectPanel;
import consulo.versionControlSystem.ui.RefreshableOnComponent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ExtensionImpl
public class ExternalToolsCheckinHandlerFactory extends CheckinHandlerFactory {

    public static final Object NONE_TOOL = new Object();

    @Override
    public CheckinHandler createHandler(final CheckinProjectPanel panel, CommitContext commitContext) {
        Project project = panel.getProject();
        final ToolsProjectConfig config = ToolsProjectConfig.getInstance(project);

        return new CheckinHandler() {
            @Override
            @RequiredUIAccess
            public @Nullable RefreshableOnComponent getAfterCheckinConfigurationPanel(Disposable parentDisposable) {
                List<Object> elements = getComboBoxElements();
                if (elements.isEmpty() || (elements.size() == 1 && elements.get(0) == NONE_TOOL)) {
                    return null;
                }

                MutableFlatDataModel<Object> model = FlatDataModel.of(elements);
                ComboBox<Object> comboBox = ComboBox.create(model);
                comboBox.setRenderer((presentation, item) -> {
                    Object value = item.getValue();
                    if (value instanceof ToolsGroup group) {
                        presentation.append(LocalizeValue.of(StringUtil.notNullize(group.getName(), ExternalToolLocalize.toolsUnnamedGroup().get())));
                    }
                    else if (value instanceof Tool tool) {
                        presentation.append(LocalizeValue.of("  " + StringUtil.notNullize(tool.getName())));
                    }
                    else {
                        presentation.append(ExternalToolLocalize.toolsListItemNone());
                    }
                });

                Button settingsButton = Button.create(LocalizeValue.empty());
                settingsButton.setIcon(PlatformIconGroup.generalGearplain());
                settingsButton.addStyle(ButtonStyle.INPLACE);
                settingsButton.setToolTipText(ExternalToolLocalize.toolsSettingsTitle());
                settingsButton.addClickListener(event -> {
                    UIAccess uiAccess = UIAccess.current();
                    ShowSettingsUtil.getInstance().showAndSelect(project, ToolConfigurable.class).whenCompleteAsync((result, error) -> {
                        Object selected = comboBox.getValue();
                        model.removeAll();
                        for (Object element : getComboBoxElements()) {
                            model.add(element);
                        }
                        comboBox.setValue(selected);
                    }, uiAccess);
                });

                VerticalLayout layout = VerticalLayout.create();
                layout.add(Label.create(ExternalToolLocalize.toolsAfterCommitDescription()));
                layout.add(DockLayout.create().center(comboBox).right(settingsButton));

                return new RefreshableOnComponent() {
                    @Override
                    @RequiredUIAccess
                    public Component getUIComponent() {
                        return layout;
                    }

                    @Override
                    @RequiredUIAccess
                    public void refresh() {
                        String id = config.getAfterCommitToolsId();
                        if (id == null) {
                            comboBox.setValue(NONE_TOOL);
                            return;
                        }
                        for (Object element : elements) {
                            if (element instanceof Tool tool && id.equals(tool.getActionId())) {
                                comboBox.setValue(tool);
                                return;
                            }
                        }
                        comboBox.setValue(NONE_TOOL);
                    }

                    @Override
                    public void saveState() {
                        Object item = comboBox.getValue();
                        config.setAfterCommitToolId(item instanceof Tool tool ? tool.getActionId() : null);
                    }

                    @Override
                    @RequiredUIAccess
                    public void restoreState() {
                        refresh();
                    }
                };
            }

            @Override
            public void checkinSuccessful() {
                String id = config.getAfterCommitToolsId();
                if (id == null) {
                    return;
                }
                DataManager.getInstance().getDataContextFromFocus().doWhenDone(context -> {
                    Application application = project.getApplication();
                    if (application.isDispatchThread()) {
                        ToolAction.runTool(id, context);
                    }
                    else {
                        application.invokeLater(() -> ToolAction.runTool(id, context), project.getDisposed());
                    }
                });
            }
        };
    }

    private static List<Object> getComboBoxElements() {
        List<Object> result = new ArrayList<>();
        ToolManager manager = ToolManager.getInstance();
        result.add(NONE_TOOL);
        for (ToolsGroup group : manager.getGroups()) {
            result.add(group);
            Collections.addAll(result, manager.getTools(group.getName()));
        }

        return result;
    }
}
