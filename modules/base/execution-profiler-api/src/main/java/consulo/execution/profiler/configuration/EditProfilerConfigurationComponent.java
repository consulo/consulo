/*
 * Copyright 2013-2026 consulo.io
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
package consulo.execution.profiler.configuration;

import consulo.application.Application;
import consulo.configurable.ConfigurationException;
import consulo.configurable.UnnamedConfigurable;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.ListBox;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionToolbarPosition;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.ex.toolbar.AddAction;
import consulo.ui.ex.toolbar.DownMoveAction;
import consulo.ui.ex.toolbar.EditAction;
import consulo.ui.ex.toolbar.RemoveAction;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilderFactory;
import consulo.ui.ex.toolbar.UpMoveAction;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.util.FormBuilder;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Settings page content which lists and edits the profiler configurations of every type in one language settings group.
 * <p>
 * A plugin registers its page by extending this class as an application configurable whose parent is
 * {@link consulo.execution.profiler.ProfilerConfigurableIds#GROUP}. Configurations are edited as copies and stored with
 * {@link ProfilerRunConfigurationManager} on apply; configurations of other groups are kept where they are.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public class EditProfilerConfigurationComponent implements UnnamedConfigurable {
    private final Application myApplication;
    private final String myLanguageSettingsGroup;
    private final @Nullable String myHelpTopic;
    private final MutableFlatDataModel<ProfilerConfigurationState> myModel = FlatDataModel.of(List.of());
    private final Map<ProfilerConfigurationState, StateEditor> myEditors = new IdentityHashMap<>();

    private @Nullable Map<String, ProfilerConfigurationTypeBase<?>> myTypes;
    private @Nullable ListBox<ProfilerConfigurationState> myList;
    private @Nullable DockLayout myDetails;
    private @Nullable Disposable myUiDisposable;
    private boolean myListModified;

    public EditProfilerConfigurationComponent(String languageSettingsGroup, @Nullable String helpTopic) {
        this(Application.get(), languageSettingsGroup, helpTopic);
    }

    public EditProfilerConfigurationComponent(Application application, String languageSettingsGroup, @Nullable String helpTopic) {
        myApplication = application;
        myLanguageSettingsGroup = languageSettingsGroup;
        myHelpTopic = helpTopic;
    }

    public String getLanguageSettingsGroup() {
        return myLanguageSettingsGroup;
    }

    @Override
    public @Nullable String getHelpTopic() {
        return myHelpTopic;
    }

    @Override
    @RequiredUIAccess
    public Component createUIComponent(Disposable uiDisposable) {
        myUiDisposable = uiDisposable;

        ListBox<ProfilerConfigurationState> list = ListBox.create(myModel);
        list.setRender((presentation, item) -> {
            ProfilerConfigurationState state = item.getValue();
            if (state != null) {
                ProfilerConfigurationTypeBase<?> type = getTypes().get(state.getConfigurationTypeId());
                presentation.withIcon(type == null ? null : type.getIcon());
                presentation.append(getPresentableName(state, type));
            }
        });
        list.addValueListener(event -> showDetails(list.getValue()));
        myList = list;

        Component decorated = myApplication.getInstance(ToolbarDecoratorBuilderFactory.class)
            .create(list)
            .addOrReplaceAction(new AddConfigurationAction())
            .addOrReplaceAction(new RemoveConfigurationAction())
            .disableAction(EditAction.class)
            .disableAction(UpMoveAction.class)
            .disableAction(DownMoveAction.class)
            .withToolbarPosition(ActionToolbarPosition.TOP)
            .build();

        DockLayout details = DockLayout.create();
        myDetails = details;

        TwoComponentSplitLayout layout = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        layout.setProportion(30);
        layout.setFirstComponent(decorated);
        layout.setSecondComponent(details);

        showDetails(list.getValue());
        return layout;
    }

    @Override
    @RequiredUIAccess
    public boolean isModified() {
        if (myListModified) {
            return true;
        }
        for (StateEditor editor : myEditors.values()) {
            if (editor.isModified()) {
                return true;
            }
        }
        return false;
    }

    @Override
    @RequiredUIAccess
    public void apply() throws ConfigurationException {
        for (StateEditor editor : myEditors.values()) {
            editor.apply();
        }

        Map<String, ProfilerConfigurationTypeBase<?>> types = getTypes();
        List<ProfilerConfigurationState> edited = new ArrayList<>(myModel.getSize());
        for (ProfilerConfigurationState state : myModel) {
            ProfilerConfigurationTypeBase<?> type = types.get(state.getConfigurationTypeId());
            ProfilerConfigurationState copy = type == null ? null : type.copyStateFor(state);
            edited.add(copy == null ? state : copy);
        }

        ProfilerRunConfigurationManager manager = myApplication.getInstance(ProfilerRunConfigurationManager.class);
        List<ProfilerConfigurationState> result = new ArrayList<>();
        boolean editedAdded = false;
        for (ProfilerConfigurationState state : manager.getConfigurations()) {
            if (!types.containsKey(state.getConfigurationTypeId())) {
                result.add(state);
            }
            else if (!editedAdded) {
                result.addAll(edited);
                editedAdded = true;
            }
        }
        if (!editedAdded) {
            result.addAll(edited);
        }

        manager.setConfigurations(result);
        myListModified = false;
    }

    @Override
    @RequiredUIAccess
    public void reset() {
        disposeEditors();
        myTypes = null;

        Map<String, ProfilerConfigurationTypeBase<?>> types = getTypes();
        List<ProfilerConfigurationState> states = new ArrayList<>();
        for (ProfilerConfigurationState state : myApplication.getInstance(ProfilerRunConfigurationManager.class).getConfigurations()) {
            ProfilerConfigurationTypeBase<?> type = types.get(state.getConfigurationTypeId());
            ProfilerConfigurationState copy = type == null ? null : type.copyStateFor(state);
            if (copy != null) {
                states.add(copy);
            }
        }

        myModel.replaceAll(states);
        myListModified = false;

        ListBox<ProfilerConfigurationState> list = myList;
        if (list != null) {
            if (!states.isEmpty()) {
                list.setValue(states.get(0));
            }
            showDetails(list.getValue());
        }
    }

    @Override
    @RequiredUIAccess
    public void disposeUIResources() {
        disposeEditors();
        myList = null;
        myDetails = null;
        myUiDisposable = null;
    }

    private Map<String, ProfilerConfigurationTypeBase<?>> getTypes() {
        Map<String, ProfilerConfigurationTypeBase<?>> types = myTypes;
        if (types == null) {
            Map<String, ProfilerConfigurationTypeBase<?>> loaded = new LinkedHashMap<>();
            myApplication.getExtensionPoint(ProfilerConfigurationTypeBase.class).forEach(type -> {
                if (myLanguageSettingsGroup.equals(type.getLanguageSettingsGroup())) {
                    loaded.put(type.getId(), type);
                }
            });
            myTypes = loaded;
            types = loaded;
        }
        return types;
    }

    private static String getPresentableName(ProfilerConfigurationState state, @Nullable ProfilerConfigurationTypeBase<?> type) {
        String displayName = state.getDisplayName();
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        return type == null ? state.getConfigurationTypeId() : type.getDisplayName().get();
    }

    private String getUniqueName(String name) {
        Set<String> usedNames = new HashSet<>();
        for (ProfilerConfigurationState state : myModel) {
            String displayName = state.getDisplayName();
            if (displayName != null) {
                usedNames.add(displayName);
            }
        }
        if (!usedNames.contains(name)) {
            return name;
        }
        int index = 2;
        while (usedNames.contains(name + " (" + index + ")")) {
            index++;
        }
        return name + " (" + index + ")";
    }

    @RequiredUIAccess
    private void addConfiguration(ProfilerConfigurationTypeBase<?> type) {
        ProfilerConfigurationState state = type.getTemplateState();
        state.setDisplayName(getUniqueName(getPresentableName(state, type)));
        myModel.add(state);
        myListModified = true;

        ListBox<ProfilerConfigurationState> list = myList;
        if (list != null) {
            list.setValue(state);
            showDetails(list.getValue());
        }
    }

    @RequiredUIAccess
    private void removeConfiguration(ProfilerConfigurationState state) {
        int index = myModel.indexOf(state);
        myModel.remove(state);
        StateEditor editor = myEditors.remove(state);
        if (editor != null) {
            editor.dispose();
        }
        myListModified = true;

        ListBox<ProfilerConfigurationState> list = myList;
        if (list != null) {
            int size = myModel.getSize();
            if (size > 0) {
                list.setValueByIndex(Math.max(0, Math.min(index, size - 1)));
            }
            else {
                list.setValue(null);
            }
            showDetails(list.getValue());
        }
    }

    @RequiredUIAccess
    private void showDetails(@Nullable ProfilerConfigurationState state) {
        DockLayout details = myDetails;
        Disposable uiDisposable = myUiDisposable;
        if (details == null || uiDisposable == null) {
            return;
        }

        details.removeAll();
        if (state == null || myModel.indexOf(state) < 0) {
            return;
        }

        StateEditor editor = myEditors.get(state);
        if (editor == null) {
            editor = new StateEditor(state, getTypes().get(state.getConfigurationTypeId()));
            myEditors.put(state, editor);
        }
        details.center(editor.getComponent(uiDisposable));
    }

    @RequiredUIAccess
    private void disposeEditors() {
        for (StateEditor editor : myEditors.values()) {
            editor.dispose();
        }
        myEditors.clear();

        DockLayout details = myDetails;
        if (details != null) {
            details.removeAll();
        }
    }

    private final class StateEditor {
        private final ProfilerConfigurationState myState;
        private final @Nullable UnnamedConfigurable myConfigurable;
        private final Disposable myDisposable = Disposable.newDisposable();
        private @Nullable Component myComponent;

        private StateEditor(ProfilerConfigurationState state, @Nullable ProfilerConfigurationTypeBase<?> type) {
            myState = state;
            myConfigurable = type == null ? null : type.createConfigurableFor(state);
        }

        @RequiredUIAccess
        private Component getComponent(Disposable uiDisposable) {
            Component component = myComponent;
            if (component == null) {
                Disposer.register(uiDisposable, myDisposable);
                component = buildComponent();
                myComponent = component;
            }
            return component;
        }

        @RequiredUIAccess
        private Component buildComponent() {
            TextBox nameBox = TextBox.create(Objects.requireNonNullElse(myState.getDisplayName(), ""));
            nameBox.addValueListener(event -> {
                String name = nameBox.getValue();
                myState.setDisplayName(name == null || name.isBlank() ? null : name.trim());
                myModel.update(myState);
                myListModified = true;
            });

            DockLayout layout = DockLayout.create();
            layout.top(FormBuilder.create().addLabeled(LocalizeValue.localizeTODO("Name:"), nameBox).build());

            Component settings = null;
            UnnamedConfigurable configurable = myConfigurable;
            if (configurable != null) {
                settings = configurable.createUIComponent(myDisposable);
                if (settings != null) {
                    configurable.initialize();
                    configurable.reset();
                }
            }
            layout.center(settings != null ? settings : Label.create(LocalizeValue.localizeTODO("No settings")));
            return layout;
        }

        @RequiredUIAccess
        private boolean isModified() {
            UnnamedConfigurable configurable = myConfigurable;
            return myComponent != null && configurable != null && configurable.isModified();
        }

        @RequiredUIAccess
        private void apply() throws ConfigurationException {
            UnnamedConfigurable configurable = myConfigurable;
            if (myComponent != null && configurable != null) {
                configurable.apply();
            }
        }

        @RequiredUIAccess
        private void dispose() {
            UnnamedConfigurable configurable = myConfigurable;
            if (myComponent != null && configurable != null) {
                configurable.disposeUIResources();
            }
            myComponent = null;
            Disposer.dispose(myDisposable);
        }
    }

    private final class AddConfigurationAction extends AddAction<ProfilerConfigurationState> {
        @Override
        @RequiredUIAccess
        protected void doAdd(AnActionEvent e) {
            List<ProfilerConfigurationTypeBase<?>> types = new ArrayList<>(getTypes().values());
            if (types.isEmpty()) {
                return;
            }
            if (types.size() == 1) {
                addConfiguration(types.get(0));
                return;
            }

            String title = LocalizeValue.localizeTODO("Add Profiler Configuration").get();
            BaseListPopupStep<ProfilerConfigurationTypeBase<?>> step =
                new BaseListPopupStep<ProfilerConfigurationTypeBase<?>>(title, types) {
                    @Override
                    public String getTextFor(ProfilerConfigurationTypeBase<?> value) {
                        return value.getDisplayName().get();
                    }

                    @Override
                    public Image getIconFor(ProfilerConfigurationTypeBase<?> value) {
                        return value.getIcon();
                    }

                    @Override
                    public PopupStep<?> onChosen(ProfilerConfigurationTypeBase<?> selectedValue, boolean finalChoice) {
                        return doFinalStep(() -> addConfiguration(selectedValue));
                    }
                };
            myApplication.getInstance(JBPopupFactory.class).createListPopup(step).showUnderneathOf(e);
        }
    }

    private final class RemoveConfigurationAction extends RemoveAction<ProfilerConfigurationState> {
        @Override
        @RequiredUIAccess
        protected void doRemove(ProfilerConfigurationState value, AnActionEvent e) {
            removeConfiguration(value);
        }
    }
}
