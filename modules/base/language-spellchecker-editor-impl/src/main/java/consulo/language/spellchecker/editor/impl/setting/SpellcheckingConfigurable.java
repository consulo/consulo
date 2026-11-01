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
package consulo.language.spellchecker.editor.impl.setting;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.configurable.ConfigurationException;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.configurable.UnnamedConfigurable;
import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.language.spellchecker.editor.SpellCheckerManager;
import consulo.language.spellchecker.editor.SpellcheckerEngine;
import consulo.language.spellchecker.editor.SpellcheckerEngineManager;
import consulo.language.spellchecker.editor.SpellcheckerFeature;
import consulo.language.spellchecker.editor.impl.SpellCheckerManagerImpl;
import consulo.language.spellchecker.editor.localize.SpellCheckerLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.InputBoxBuilder;
import consulo.ui.Label;
import consulo.ui.ListBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ActionToolbarPosition;
import consulo.ui.ex.toolbar.AddAction;
import consulo.ui.ex.toolbar.DownMoveAction;
import consulo.ui.ex.toolbar.EditAction;
import consulo.ui.ex.toolbar.RemoveAction;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilderFactory;
import consulo.ui.ex.toolbar.UpMoveAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * <em>Settings | Editor | Spellchecking</em>: engine selector with the options of the selected engine, and the dictionaries shared
 * by all engines.
 *
 * @author VISTALL
 * @since 2026-11-01
 */
@ExtensionImpl
public class SpellcheckingConfigurable implements ProjectConfigurable {
    /**
     * Value of the engine selector for automatic selection.
     */
    private static final String AUTO_ENGINE = "";

    private final Project myProject;
    private final Application myApplication;
    private final SpellcheckerEngineManager myEngineManager;
    private final SpellCheckerManager mySpellCheckerManager;
    private final SpellCheckerSettings mySettings;

    private final Map<String, UnnamedConfigurable> myEngineConfigurables = new HashMap<>();
    private final Map<String, Component> myEngineComponents = new HashMap<>();

    private @Nullable ComboBox<String> myEngineBox;
    private @Nullable DockLayout myEnginePanel;
    private @Nullable Label myEngineInfo;
    private @Nullable Disposable myUiDisposable;

    private final MutableFlatDataModel<String> myWordsModel = FlatDataModel.of(List.of());
    private final MutableFlatDataModel<String> myFoldersModel = FlatDataModel.of(List.of());
    private final Map<String, CheckBox> myBundledBoxes = new LinkedHashMap<>();

    @Inject
    public SpellcheckingConfigurable(
        Project project,
        Application application,
        SpellcheckerEngineManager engineManager,
        SpellCheckerManager spellCheckerManager,
        SpellCheckerSettings settings
    ) {
        myProject = project;
        myApplication = application;
        myEngineManager = engineManager;
        mySpellCheckerManager = spellCheckerManager;
        mySettings = settings;
    }

    @Override
    public String getId() {
        return "reference.settings.ide.settings.spelling";
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.EDITOR_GROUP;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return SpellCheckerLocalize.spellchecking();
    }

    @RequiredUIAccess
    @Override
    public Component createUIComponent(Disposable parentDisposable) {
        myUiDisposable = parentDisposable;

        VerticalLayout root = VerticalLayout.create();

        List<String> engineIds = new ArrayList<>();
        engineIds.add(AUTO_ENGINE);
        Map<String, LocalizeValue> engineNames = new HashMap<>();
        myApplication.getExtensionPoint(SpellcheckerEngine.class).forEach(engine -> {
            engineIds.add(engine.getId());
            engineNames.put(engine.getId(), engine.getDisplayName());
        });

        ComboBox<String> engineBox = ComboBox.create(engineIds);
        SpellcheckerEngine autoEngine = myEngineManager.getAutoSelectedEngine();
        engineBox.setTextRenderer(id -> {
            if (id == null || AUTO_ENGINE.equals(id)) {
                return autoEngine == null ? SpellCheckerLocalize.engineAuto() : SpellCheckerLocalize.engineAutoResolved(autoEngine.getDisplayName());
            }
            return engineNames.getOrDefault(id, LocalizeValue.of(id));
        });
        engineBox.addValueListener(event -> showEngineOptions(event.getValue()));
        myEngineBox = engineBox;
        root.add(LabeledLayout.create(SpellCheckerLocalize.engineLabel(), engineBox));

        Label engineInfo = Label.create(LocalizeValue.empty());
        myEngineInfo = engineInfo;
        root.add(engineInfo);

        DockLayout enginePanel = DockLayout.create();
        myEnginePanel = enginePanel;
        root.add(enginePanel);

        root.add(LabeledLayout.create(SpellCheckerLocalize.acceptedWords(), createWordsList()));
        root.add(LabeledLayout.create(SpellCheckerLocalize.customDictionariesFolders(), createFoldersList()));
        root.add(LabeledLayout.create(SpellCheckerLocalize.bundledDictionaries(), createBundledList()));

        reset();
        return root;
    }

    @RequiredUIAccess
    private Component createWordsList() {
        ListBox<String> wordsBox = ListBox.create(myWordsModel);
        return myApplication.getInstance(ToolbarDecoratorBuilderFactory.class)
            .create(wordsBox)
            .addOrReplaceAction(new AddAction<String>() {
                @Override
                @RequiredUIAccess
                protected void doAdd(AnActionEvent e) {
                    InputBoxBuilder.text()
                        .title(SpellCheckerLocalize.addNewWord())
                        .text(SpellCheckerLocalize.enterSimpleWord())
                        .showAsync(wordsBox)
                        .thenAccept(word -> {
                            if (word != null && !StringUtil.isEmptyOrSpaces(word)) {
                                myApplication.getLastUIAccess().give(() -> addWord(word.trim()));
                            }
                        });
                }
            })
            .addOrReplaceAction(new RemoveAction<String>() {
                @Override
                @RequiredUIAccess
                protected void doRemove(String value, AnActionEvent e) {
                    myWordsModel.remove(value);
                }
            })
            .disableAction(EditAction.class)
            .disableAction(UpMoveAction.class)
            .disableAction(DownMoveAction.class)
            .withToolbarPosition(ActionToolbarPosition.TOP)
            .build();
    }

    @RequiredUIAccess
    private void addWord(String word) {
        for (String existing : myWordsModel) {
            if (existing.equals(word)) {
                return;
            }
        }
        myWordsModel.add(word);
    }

    @RequiredUIAccess
    private Component createFoldersList() {
        ListBox<String> foldersBox = ListBox.create(myFoldersModel);
        return myApplication.getInstance(ToolbarDecoratorBuilderFactory.class)
            .create(foldersBox)
            .addOrReplaceAction(new AddAction<String>() {
                @Override
                @RequiredUIAccess
                protected void doAdd(AnActionEvent e) {
                    FileChooser.chooseFile(FileChooserDescriptorFactory.createSingleFolderDescriptor(), myProject, null)
                        .thenAccept(file -> {
                            if (file != null) {
                                myApplication.getLastUIAccess().give(() -> addFolder(file));
                            }
                        });
                }
            })
            .addOrReplaceAction(new RemoveAction<String>() {
                @Override
                @RequiredUIAccess
                protected void doRemove(String value, AnActionEvent e) {
                    myFoldersModel.remove(value);
                }
            })
            .disableAction(EditAction.class)
            .disableAction(UpMoveAction.class)
            .disableAction(DownMoveAction.class)
            .withToolbarPosition(ActionToolbarPosition.TOP)
            .build();
    }

    @RequiredUIAccess
    private void addFolder(VirtualFile file) {
        String path = file.getPath();
        for (String existing : myFoldersModel) {
            if (existing.equals(path)) {
                return;
            }
        }
        myFoldersModel.add(path);
    }

    @RequiredUIAccess
    private Component createBundledList() {
        VerticalLayout layout = VerticalLayout.create();
        for (String dictionary : SpellCheckerManagerImpl.getBundledDictionaryNames(myApplication)) {
            CheckBox box = CheckBox.create(LocalizeValue.of(dictionary));
            myBundledBoxes.put(dictionary, box);
            layout.add(box);
        }
        if (myBundledBoxes.isEmpty()) {
            layout.add(Label.create(SpellCheckerLocalize.noDictionaries()));
        }
        return layout;
    }

    @RequiredUIAccess
    private void showEngineOptions(@Nullable String engineId) {
        DockLayout enginePanel = myEnginePanel;
        if (enginePanel == null) {
            return;
        }

        SpellcheckerEngine engine = findEngine(engineId);
        Label engineInfo = myEngineInfo;
        if (engineInfo != null) {
            engineInfo.setText(engine == null ? LocalizeValue.empty() : getEngineInfo(engine));
        }
        UnnamedConfigurable configurable = engine == null ? null : getEngineConfigurable(engine);
        Component component = null;
        if (engine != null && configurable != null && myUiDisposable != null) {
            component = myEngineComponents.get(engine.getId());
            if (component == null) {
                component = configurable.createUIComponent(myUiDisposable);
                if (component != null) {
                    myEngineComponents.put(engine.getId(), component);
                    configurable.reset();
                }
            }
        }
        enginePanel.removeAll();
        if (component != null) {
            enginePanel.center(component);
        }
    }

    private @Nullable SpellcheckerEngine findEngine(@Nullable String engineId) {
        if (engineId == null || AUTO_ENGINE.equals(engineId)) {
            return myEngineManager.getAutoSelectedEngine();
        }
        return myApplication.getExtensionPoint(SpellcheckerEngine.class).findFirstSafe(it -> engineId.equals(it.getId()));
    }

    private static LocalizeValue getEngineInfo(SpellcheckerEngine engine) {
        List<LocalizeValue> features = new ArrayList<>();
        for (SpellcheckerFeature feature : engine.getFeatures()) {
            features.add(feature.getName());
        }

        Set<String> locales = new TreeSet<>();
        for (Locale locale : engine.getLocales()) {
            locales.add(locale.getDisplayName());
        }
        LocalizeValue languages = locales.isEmpty() ? SpellCheckerLocalize.engineInfoNoLanguages() : LocalizeValue.of(String.join(", ", locales));
        return SpellCheckerLocalize.engineInfo(LocalizeValue.join(", ", features.toArray(LocalizeValue[]::new)), languages);
    }

    private @Nullable UnnamedConfigurable getEngineConfigurable(SpellcheckerEngine engine) {
        if (myEngineConfigurables.containsKey(engine.getId())) {
            return myEngineConfigurables.get(engine.getId());
        }
        UnnamedConfigurable configurable = engine.createConfigurable();
        myEngineConfigurables.put(engine.getId(), configurable);
        return configurable;
    }

    private @Nullable String getSelectedEngineIdFromUI() {
        ComboBox<String> engineBox = myEngineBox;
        String value = engineBox == null ? null : engineBox.getValue();
        return value == null || AUTO_ENGINE.equals(value) ? null : value;
    }

    private Set<String> getWordsFromUI() {
        Set<String> words = new HashSet<>();
        for (String word : myWordsModel) {
            words.add(word);
        }
        return words;
    }

    private List<String> getFoldersFromUI() {
        List<String> folders = new ArrayList<>();
        for (String folder : myFoldersModel) {
            folders.add(folder);
        }
        return folders;
    }

    private Set<String> getDisabledBundledFromUI() {
        Set<String> disabled = new HashSet<>();
        for (Map.Entry<String, CheckBox> entry : myBundledBoxes.entrySet()) {
            if (!entry.getValue().getValueOrError()) {
                disabled.add(entry.getKey());
            }
        }
        return disabled;
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        if (myEngineBox == null) {
            return false;
        }
        if (!Objects.equals(getSelectedEngineIdFromUI(), myEngineManager.getSelectedEngineId())) {
            return true;
        }
        for (UnnamedConfigurable configurable : myEngineConfigurables.values()) {
            if (configurable != null && configurable.isModified()) {
                return true;
            }
        }
        return !getWordsFromUI().equals(mySpellCheckerManager.getUserDictionaryWords())
            || !getFoldersFromUI().equals(mySettings.getDictionaryFoldersPaths())
            || !getDisabledBundledFromUI().equals(mySettings.getBundledDisabledDictionariesPaths());
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        if (myEngineBox == null) {
            return;
        }
        for (UnnamedConfigurable configurable : myEngineConfigurables.values()) {
            if (configurable != null && configurable.isModified()) {
                configurable.apply();
            }
        }
        myEngineManager.setSelectedEngineId(getSelectedEngineIdFromUI());

        mySettings.setDictionaryFoldersPaths(getFoldersFromUI());
        mySettings.setBundledDisabledDictionariesPaths(getDisabledBundledFromUI());
        mySpellCheckerManager.updateUserDictionary(getWordsFromUI());
        mySpellCheckerManager.reloadDictionaries();
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        ComboBox<String> engineBox = myEngineBox;
        if (engineBox == null) {
            return;
        }
        String selectedEngineId = myEngineManager.getSelectedEngineId();
        engineBox.setValue(selectedEngineId == null || findEngine(selectedEngineId) == null ? AUTO_ENGINE : selectedEngineId);
        showEngineOptions(engineBox.getValue());

        for (UnnamedConfigurable configurable : myEngineConfigurables.values()) {
            if (configurable != null) {
                configurable.reset();
            }
        }

        myWordsModel.removeAll();
        for (String word : new TreeSet<>(mySpellCheckerManager.getUserDictionaryWords())) {
            myWordsModel.add(word);
        }

        myFoldersModel.removeAll();
        for (String folder : mySettings.getDictionaryFoldersPaths()) {
            myFoldersModel.add(folder);
        }

        Set<String> disabledBundled = mySettings.getBundledDisabledDictionariesPaths();
        for (Map.Entry<String, CheckBox> entry : myBundledBoxes.entrySet()) {
            entry.getValue().setValue(!disabledBundled.contains(entry.getKey()));
        }
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        for (UnnamedConfigurable configurable : myEngineConfigurables.values()) {
            if (configurable != null) {
                configurable.disposeUIResources();
            }
        }
        myEngineConfigurables.clear();
        myEngineComponents.clear();
        myBundledBoxes.clear();
        myEngineBox = null;
        myEnginePanel = null;
        myEngineInfo = null;
        myUiDisposable = null;
    }
}
