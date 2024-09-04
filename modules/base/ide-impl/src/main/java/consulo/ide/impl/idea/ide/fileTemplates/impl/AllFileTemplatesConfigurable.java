/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.ide.impl.idea.ide.fileTemplates.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.configurable.*;
import consulo.disposer.Disposable;
import consulo.fileTemplate.*;
import consulo.fileTemplate.impl.internal.*;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.ide.impl.idea.openapi.actionSystem.DefaultCompactActionGroup;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.language.plain.PlainTextFileType;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.*;
import java.text.MessageFormat;
import java.util.List;
import java.util.*;

import static consulo.fileTemplate.FileTemplateManager.*;

/*
 * @author: MYakovlev
 * Date: Jul 26, 2002
 * Time: 12:44:56 PM
 */
@ExtensionImpl
public class AllFileTemplatesConfigurable implements SearchableConfigurable, Configurable.NoMargin, Configurable.NoScroll, ProjectConfigurable {
    private static final Logger LOG = Logger.getInstance(AllFileTemplatesConfigurable.class);

    private final Project myProject;
    private final FileTemplateManager myManager;
    private JPanel myMainPanel;
    private FileTemplateTab myCurrentTab;
    private FileTemplateTab myTemplatesList;
    private FileTemplateTab myIncludesList;
    private FileTemplateTab myCodeTemplatesList;
    @Nullable
    private FileTemplateTab myOtherTemplatesList;
    private JComponent myToolBar;
    private TabbedPaneWrapper myTabbedPane;
    private FileTemplateConfigurable myEditor;
    private boolean myModified = false;
    private JComponent myEditorComponent;
    private JPanel myLeftPanel;
    private FileTemplateTab[] myTabs;
    private final Set<String> myInternalTemplateNames;

    private FileTemplatesScheme myScheme;
    private final Map<FileTemplatesScheme, Map<String, FileTemplate[]>> myChangesCache = new HashMap<>();

    private static final String CURRENT_TAB = "FileTemplates.CurrentTab";
    private static final String SELECTED_TEMPLATE = "FileTemplates.SelectedTemplate";

    @Inject
    public AllFileTemplatesConfigurable(Project project) {
        myProject = project;
        myManager = getInstance(project);
        myScheme = myManager.getCurrentScheme();
        myInternalTemplateNames = ContainerUtil.map2Set(myManager.getInternalTemplates(), template -> template.getName());
    }

    private void onRemove() {
        myCurrentTab.removeSelected();
        myModified = true;
    }

    private void onAdd() {
        createTemplate(IdeLocalize.templateUnnamed().get(), PlainTextFileType.INSTANCE.getDefaultExtension(), "");
    }

    private FileTemplate createTemplate(final @Nonnull String prefName, final @Nonnull String extension, final @Nonnull String content) {
        final FileTemplate[] templates = myCurrentTab.getTemplates();
        final FileTemplate newTemplate = FileTemplateImplUtil.createTemplate(prefName, extension, content, templates);
        myCurrentTab.addTemplate(newTemplate);
        myModified = true;
        myCurrentTab.selectTemplate(newTemplate);
        fireListChanged();
        myEditor.focusToNameField();
        return newTemplate;
    }

    private void onClone() {
        try {
            myEditor.apply();
        }
        catch (ConfigurationException ignore) {
        }

        final FileTemplate selected = myCurrentTab.getSelectedTemplate();
        if (selected == null) {
            return;
        }

        final FileTemplate[] templates = myCurrentTab.getTemplates();
        final Set<String> names = new HashSet<>();
        for (FileTemplate template : templates) {
            names.add(template.getName());
        }
        @SuppressWarnings({"UnresolvedPropertyKey"}) final String nameTemplate = IdeBundle.message("template.copy.N.of.T");
        String name = MessageFormat.format(nameTemplate, "", selected.getName());
        int i = 0;
        while (names.contains(name)) {
            name = MessageFormat.format(nameTemplate, ++i + " ", selected.getName());
        }
        final FileTemplate newTemplate = new CustomFileTemplate(name, selected.getExtension());
        newTemplate.setText(selected.getText());
        newTemplate.setReformatCode(selected.isReformatCode());
        newTemplate.setLiveTemplateEnabled(selected.isLiveTemplateEnabled());
        myCurrentTab.addTemplate(newTemplate);
        myModified = true;
        myCurrentTab.selectTemplate(newTemplate);
        fireListChanged();
    }

    @Override
    public String getDisplayName() {
        return IdeLocalize.titleFileTemplates().get();
    }

    @Override
    public String getHelpTopic() {
        int index = myTabbedPane.getSelectedIndex();
        switch (index) {
            case 0:
                return "fileTemplates.templates";
            case 1:
                return "fileTemplates.includes";
            case 2:
                return "fileTemplates.code";
            case 3:
                return "fileTemplates.j2ee";
            default:
                throw new IllegalStateException("wrong index: " + index);
        }
    }

    @RequiredUIAccess
    @Override
    public JComponent createComponent(@Nonnull Disposable parentUIDisposable) {
        myTemplatesList = new FileTemplateTabAsList(IdeLocalize.tabFiletemplatesTemplates().get()) {
            @Override
            public void onTemplateSelected() {
                onListSelectionChanged();
            }
        };
        myIncludesList = new FileTemplateTabAsList(IdeLocalize.tabFiletemplatesIncludes().get()) {
            @Override
            public void onTemplateSelected() {
                onListSelectionChanged();
            }
        };
        myCodeTemplatesList = new FileTemplateTabAsList(IdeLocalize.tabFiletemplatesCode().get()) {
            @Override
            public void onTemplateSelected() {
                onListSelectionChanged();
            }
        };
        myCurrentTab = myTemplatesList;

        final List<FileTemplateTab> allTabs = new ArrayList<>(Arrays.asList(myTemplatesList, myIncludesList, myCodeTemplatesList));

        List<FileTemplateGroupDescriptorFactory> factories = FileTemplateGroupDescriptorFactory.EP_NAME.getExtensionList();
        if (!factories.isEmpty()) {
            myOtherTemplatesList = new FileTemplateTabAsTree(IdeLocalize.tabFiletemplatesJ2ee().get()) {
                @Override
                public void onTemplateSelected() {
                    onListSelectionChanged();
                }

                @Override
                protected FileTemplateNode initModel() {
                    SortedSet<FileTemplateGroupDescriptor> categories = new TreeSet<>((o1, o2) -> o1.getTitle().compareTo(o2.getTitle()));

                    for (FileTemplateGroupDescriptorFactory templateGroupFactory : factories) {
                        ContainerUtil.addIfNotNull(categories, templateGroupFactory.getFileTemplatesDescriptor());
                    }

                    //noinspection HardCodedStringLiteral
                    return new FileTemplateNode("ROOT", null, ContainerUtil.map2List(categories, FileTemplateNode::new));
                }
            };
            allTabs.add(myOtherTemplatesList);
        }

        myEditor = new FileTemplateConfigurable(myProject);
        myEditor.addChangeListener(e -> onEditorChanged());
        myEditorComponent = myEditor.createComponent();
        myEditorComponent.setBorder(JBUI.Borders.empty(10, 0, 10, 10));

        myTabs = allTabs.toArray(new FileTemplateTab[allTabs.size()]);
        myTabbedPane = new TabbedPaneWrapper(parentUIDisposable);
        myTabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        myLeftPanel = new JPanel(new CardLayout());
        myLeftPanel.setBorder(JBUI.Borders.empty(10, 10, 10, 0));
        for (FileTemplateTab tab : myTabs) {
            myLeftPanel.add(ScrollPaneFactory.createScrollPane(tab.getComponent()), tab.getTitle());
            JPanel fakePanel = new JPanel();
            fakePanel.setPreferredSize(new Dimension(0, 0));
            myTabbedPane.addTab(tab.getTitle(), fakePanel);
        }

        myTabbedPane.addChangeListener(e -> onTabChanged());

        DefaultActionGroup group = new DefaultActionGroup();
        AnAction removeAction = new AnAction(IdeLocalize.actionRemoveTemplate(), LocalizeValue.empty(), AllIcons.General.Remove) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                onRemove();
            }

            @RequiredUIAccess
            @Override
            public void update(@Nonnull AnActionEvent e) {
                super.update(e);
                FileTemplate selectedItem = myCurrentTab.getSelectedTemplate();
                e.getPresentation().setEnabled(selectedItem != null && !isInternalTemplate(selectedItem.getName(), myCurrentTab.getTitle()));
            }
        };
        AnAction addAction = new AnAction(IdeLocalize.actionCreateTemplate(), LocalizeValue.empty(), AllIcons.General.Add) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                onAdd();
            }

            @RequiredUIAccess
            @Override
            public void update(@Nonnull AnActionEvent e) {
                super.update(e);
                e.getPresentation().setEnabled(!(myCurrentTab == myCodeTemplatesList || myCurrentTab == myOtherTemplatesList));
            }
        };
        AnAction cloneAction = new AnAction(IdeLocalize.actionCopyTemplate(), LocalizeValue.empty(), PlatformIconGroup.actionsCopy()) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                onClone();
            }

            @RequiredUIAccess
            @Override
            public void update(@Nonnull AnActionEvent e) {
                super.update(e);
                e.getPresentation().setEnabled(
                    myCurrentTab != myCodeTemplatesList && myCurrentTab != myOtherTemplatesList && myCurrentTab.getSelectedTemplate() != null
                );
            }
        };
        AnAction resetAction = new AnAction(IdeLocalize.actionResetToDefault(), LocalizeValue.empty(), AllIcons.General.Reset) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                onReset();
            }

            @RequiredUIAccess
            @Override
            public void update(@Nonnull AnActionEvent e) {
                super.update(e);
                final FileTemplate selectedItem = myCurrentTab.getSelectedTemplate();
                e.getPresentation().setEnabled(selectedItem instanceof BundledFileTemplate && !selectedItem.isDefault());
            }
        };
        group.add(addAction);
        group.add(removeAction);
        group.add(cloneAction);
        group.add(resetAction);

        addAction.registerCustomShortcutSet(CommonShortcuts.INSERT, myCurrentTab.getComponent());
        removeAction.registerCustomShortcutSet(CommonShortcuts.getDelete(), myCurrentTab.getComponent());

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true);
        toolbar.setTargetComponent(myMainPanel);
        myToolBar = toolbar.getComponent();
        myToolBar.setBorder(IdeBorderFactory.createEmptyBorder());

        JPanel toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.add(myToolBar, BorderLayout.WEST);
        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, new DefaultCompactActionGroup(new ChangeSchemaCombo(this)), true);
        JComponent schemaComponent = actionToolbar.getComponent();
        JPanel schemaPanel = new JPanel(new BorderLayout());
        schemaPanel.add(schemaComponent, BorderLayout.EAST);
        schemaPanel.add(new JLabel("Schema:"), BorderLayout.WEST);
        toolbarPanel.add(schemaPanel, BorderLayout.EAST);


        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(myTabbedPane.getComponent(), BorderLayout.NORTH);
        Splitter splitter = new Splitter(false, 0.3f);
        splitter.setDividerWidth(JBUI.scale(10));
        splitter.setFirstComponent(myLeftPanel);
        splitter.setSecondComponent(myEditorComponent);
        centerPanel.add(splitter, BorderLayout.CENTER);

        myMainPanel = new JPanel(new BorderLayout());
        myMainPanel.add(toolbarPanel, BorderLayout.NORTH);
        myMainPanel.add(centerPanel, BorderLayout.CENTER);

        final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
        final String tabName = propertiesComponent.getValue(CURRENT_TAB);
        if (selectTab(tabName)) {
            //final String selectedTemplateName = propertiesComponent.getValue(SELECTED_TEMPLATE);
            //for (FileTemplate template : myCurrentTab.getTemplates()) {
            //  if (Comparing.strEqual(template.getName(), selectedTemplateName)) {
            //    myCurrentTab.selectTemplate(template);
            //    break;
            //  }
            //}
        }
        
        actionToolbar.setTargetComponent(myMainPanel);
        return myMainPanel;
    }

    private void onReset() {
        FileTemplate selected = myCurrentTab.getSelectedTemplate();
        if (selected instanceof BundledFileTemplate bundledFileTemplate) {
            if (Messages.showOkCancelDialog(
                IdeLocalize.promptResetToOriginalTemplate().get(),
                IdeLocalize.titleResetTemplate().get(),
                Messages.getQuestionIcon()
            ) != Messages.OK) {
                return;
            }
            bundledFileTemplate.revertToDefaults();
            myEditor.reset();
            myModified = true;
        }
    }

    private void onEditorChanged() {
        fireListChanged();
    }

    private void onTabChanged() {
        applyEditor(myCurrentTab.getSelectedTemplate());

        final int selectedIndex = myTabbedPane.getSelectedIndex();
        if (0 <= selectedIndex && selectedIndex < myTabs.length) {
            myCurrentTab = myTabs[selectedIndex];
        }
        ((CardLayout) myLeftPanel.getLayout()).show(myLeftPanel, myCurrentTab.getTitle());
        onListSelectionChanged();
    }

    @RequiredUIAccess
    private void onListSelectionChanged() {
        FileTemplate selectedValue = myCurrentTab.getSelectedTemplate();
        FileTemplate prevTemplate = myEditor == null ? null : myEditor.getTemplate();
        if (prevTemplate != selectedValue) {
            LOG.assertTrue(myEditor != null, "selected:" + selectedValue + "; prev:" + prevTemplate);
            //selection has changed
            if (Arrays.asList(myCurrentTab.getTemplates()).contains(prevTemplate) && !applyEditor(prevTemplate)) {
                return;
            }
            if (selectedValue == null) {
                myEditor.setTemplate(null, FileTemplateManagerImpl.getInstanceImpl(myProject).getDefaultTemplateDescription());
                myEditorComponent.repaint();
            }
            else {
                selectTemplate(selectedValue);
            }
        }
    }

    @RequiredUIAccess
    private boolean applyEditor(FileTemplate prevTemplate) {
        if (myEditor.isModified()) {
            try {
                myModified = true;
                myEditor.apply();
                fireListChanged();
            }
            catch (ConfigurationException e) {
                if (Arrays.asList(myCurrentTab.getTemplates()).contains(prevTemplate)) {
                    myCurrentTab.selectTemplate(prevTemplate);
                }
                Messages.showErrorDialog(myMainPanel, e.getMessage(), IdeLocalize.titleCannotSaveCurrentTemplate().get());
                return false;
            }
        }
        return true;
    }

    @RequiredUIAccess
    private void selectTemplate(FileTemplate template) {
        FileTemplateStreamProvider defDesc = null;
        if (myCurrentTab == myTemplatesList) {
            defDesc = FileTemplateManagerImpl.getInstanceImpl(myProject).getDefaultTemplateDescription();
        }
        else if (myCurrentTab == myIncludesList) {
            defDesc = FileTemplateManagerImpl.getInstanceImpl(myProject).getDefaultIncludeDescription();
        }
        if (myEditor.getTemplate() != template) {
            myEditor.setTemplate(template, defDesc);
            final boolean isInternal = template != null && isInternalTemplate(template.getName(), myCurrentTab.getTitle());
            myEditor.setShowInternalMessage(isInternal ? " " : null);
            myEditor.setShowAdjustCheckBox(myTemplatesList == myCurrentTab);
        }
    }

    // internal template could not be removed and should be rendered bold
    public static boolean isInternalTemplate(String templateName, String templateTabTitle) {
        if (templateName == null) {
            return false;
        }
        if (Comparing.strEqual(templateTabTitle, IdeLocalize.tabFiletemplatesTemplates().get())) {
            return isInternalTemplateName(templateName);
        }
        if (Comparing.strEqual(templateTabTitle, IdeLocalize.tabFiletemplatesCode().get())) {
            return true;
        }
        if (Comparing.strEqual(templateTabTitle, IdeLocalize.tabFiletemplatesJ2ee().get())) {
            return true;
        }
        if (Comparing.strEqual(templateTabTitle, IdeLocalize.tabFiletemplatesIncludes().get())) {
            return Comparing.strEqual(templateName, FILE_HEADER_TEMPLATE_NAME);
        }
        return false;
    }

    private static boolean isInternalTemplateName(final String templateName) {
        return FileTemplateRegistratorImpl.last().getInternalTemplates().containsKey(templateName);
    }

    private void initLists() {
        FileTemplatesScheme scheme = myManager.getCurrentScheme();
        myManager.setCurrentScheme(myScheme);
        myTemplatesList.init(getTemplates(DEFAULT_TEMPLATES_CATEGORY));
        myIncludesList.init(getTemplates(INCLUDES_TEMPLATES_CATEGORY));
        myCodeTemplatesList.init(getTemplates(CODE_TEMPLATES_CATEGORY));
        if (myOtherTemplatesList != null) {
            myOtherTemplatesList.init(getTemplates(J2EE_TEMPLATES_CATEGORY));
        }
        myManager.setCurrentScheme(scheme);
    }

    private FileTemplate[] getTemplates(String category) {
        Map<String, FileTemplate[]> templates = myChangesCache.get(myScheme);
        if (templates == null) {
            return myManager.getTemplates(category);
        }
        else {
            return templates.get(category);
        }
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        return myScheme != myManager.getCurrentScheme() || !myChangesCache.isEmpty() || isSchemeModified();
    }

    private boolean isSchemeModified() {
        return myModified || myEditor != null && myEditor.isModified();
    }

    private void checkCanApply(FileTemplateTab list) throws ConfigurationException {
        final FileTemplate[] templates = myCurrentTab.getTemplates();
        final List<String> allNames = new ArrayList<>();
        FileTemplate itemWithError = null;
        boolean errorInName = true;
        String errorString = null;
        for (FileTemplate template : templates) {
            final String currName = template.getName();
            if (currName.length() == 0) {
                itemWithError = template;
                errorString = IdeLocalize.errorPleaseSpecifyTemplateName().get();
                break;
            }
            if (allNames.contains(currName)) {
                itemWithError = template;
                errorString = "Template with name \'" + currName + "\' already exists. Please specify a different template name";
                break;
            }
            allNames.add(currName);
        }

        if (itemWithError != null) {
            final boolean _errorInName = errorInName;
            myTabbedPane.setSelectedIndex(Arrays.asList(myTabs).indexOf(list));
            selectTemplate(itemWithError);
            list.selectTemplate(itemWithError);
            ApplicationManager.getApplication().invokeLater(() -> {
                if (_errorInName) {
                    myEditor.focusToNameField();
                }
                else {
                    myEditor.focusToExtensionField();
                }
            });
            throw new ConfigurationException(errorString);
        }
    }

    private void fireListChanged() {
        if (myCurrentTab != null) {
            myCurrentTab.fireDataChanged();
        }
        if (myMainPanel != null) {
            myMainPanel.revalidate();
        }
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        if (myEditor != null && myEditor.isModified()) {
            myModified = true;
            myEditor.apply();
        }

        for (FileTemplateTab list : myTabs) {
            checkCanApply(list);
        }
        updateCache();
        for (Map.Entry<FileTemplatesScheme, Map<String, FileTemplate[]>> entry : myChangesCache.entrySet()) {
            myManager.setCurrentScheme(entry.getKey());
            myManager.setTemplates(DEFAULT_TEMPLATES_CATEGORY, Arrays.asList(entry.getValue().get(DEFAULT_TEMPLATES_CATEGORY)));
            myManager.setTemplates(INTERNAL_TEMPLATES_CATEGORY, Arrays.asList(entry.getValue().get(INTERNAL_TEMPLATES_CATEGORY)));
            myManager.setTemplates(INCLUDES_TEMPLATES_CATEGORY, Arrays.asList(entry.getValue().get(INCLUDES_TEMPLATES_CATEGORY)));
            myManager.setTemplates(CODE_TEMPLATES_CATEGORY, Arrays.asList(entry.getValue().get(CODE_TEMPLATES_CATEGORY)));
            myManager.setTemplates(J2EE_TEMPLATES_CATEGORY, Arrays.asList(entry.getValue().get(J2EE_TEMPLATES_CATEGORY)));
        }
        myChangesCache.clear();

        myManager.setCurrentScheme(myScheme);

        if (myEditor != null) {
            myModified = false;
            fireListChanged();
        }
    }

    public void selectTemplatesTab() {
        selectTab(IdeLocalize.tabFiletemplatesTemplates().get());
    }

    private boolean selectTab(String tabName) {
        int idx = 0;
        for (FileTemplateTab tab : myTabs) {
            if (Comparing.strEqual(tab.getTitle(), tabName)) {
                myCurrentTab = tab;
                myTabbedPane.setSelectedIndex(idx);
                return true;
            }
            idx++;
        }
        return false;
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        myEditor.reset();
        changeScheme(myManager.getCurrentScheme());
        myChangesCache.clear();
        myModified = false;
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        if (myCurrentTab != null) {
            final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
            propertiesComponent.setValue(CURRENT_TAB, myCurrentTab.getTitle(), IdeLocalize.tabFiletemplatesTemplates().get());
            final FileTemplate template = myCurrentTab.getSelectedTemplate();
            if (template != null) {
                propertiesComponent.setValue(SELECTED_TEMPLATE, template.getName());
            }
        }

        if (myEditor != null) {
            myEditor.disposeUIResources();
            myEditor = null;
            myEditorComponent = null;
        }
        myMainPanel = null;
        myTabbedPane = null;
        myToolBar = null;
        myTabs = null;
        myCurrentTab = null;
        myTemplatesList = null;
        myCodeTemplatesList = null;
        myIncludesList = null;
        myOtherTemplatesList = null;
    }

    public FileTemplate createNewTemplate(@Nonnull String preferredName, @Nonnull String extension, @Nonnull String text) {
        return createTemplate(preferredName, extension, text);
    }

    @Override
    @Nonnull
    public String getId() {
        return "fileTemplates";
    }

    @Nullable
    @Override
    public String getParentId() {
        return StandardConfigurableIds.EDITOR_GROUP;
    }

    public static void editCodeTemplate(@Nonnull final String templateId, Project project) {
        final ShowSettingsUtil util = ShowSettingsUtil.getInstance();
        final AllFileTemplatesConfigurable configurable = new AllFileTemplatesConfigurable(project);
        util.editConfigurable(project, configurable, () -> {
            configurable.myTabbedPane.setSelectedIndex(ArrayUtil.indexOf(configurable.myTabs, configurable.myCodeTemplatesList));
            for (FileTemplate template : configurable.myCodeTemplatesList.getTemplates()) {
                if (Comparing.equal(templateId, template.getName())) {
                    configurable.myCodeTemplatesList.selectTemplate(template);
                    break;
                }
            }
        });
    }

    public void changeScheme(FileTemplatesScheme scheme) {
        if (myEditor != null && myEditor.isModified()) {
            myModified = true;
            try {
                myEditor.apply();
            }
            catch (ConfigurationException e) {
                Messages.showErrorDialog(myEditorComponent, e.getMessage(), e.getTitle());
                return;
            }
        }
        updateCache();
        myScheme = scheme;
        initLists();
    }

    @SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
    private void updateCache() {
        if (isSchemeModified()) {
            if (!myChangesCache.containsKey(myScheme)) {
                Map<String, FileTemplate[]> templates = new HashMap<>();
                FileTemplate[] allTemplates = myTemplatesList.getTemplates();
                templates.put(
                    DEFAULT_TEMPLATES_CATEGORY,
                    ContainerUtil.filter(allTemplates, template -> !myInternalTemplateNames.contains(template.getName()))
                        .toArray(FileTemplate.EMPTY_ARRAY)
                );
                templates.put(
                    INTERNAL_TEMPLATES_CATEGORY,
                    ContainerUtil.filter(allTemplates, template -> myInternalTemplateNames.contains(template.getName()))
                        .toArray(FileTemplate.EMPTY_ARRAY)
                );
                templates.put(INCLUDES_TEMPLATES_CATEGORY, myIncludesList.getTemplates());
                templates.put(CODE_TEMPLATES_CATEGORY, myCodeTemplatesList.getTemplates());
                templates.put(J2EE_TEMPLATES_CATEGORY, myOtherTemplatesList == null ? FileTemplate.EMPTY_ARRAY : myOtherTemplatesList.getTemplates());
                myChangesCache.put(myScheme, templates);
            }
        }
    }

    public FileTemplateManager getManager() {
        return myManager;
    }

    public FileTemplatesScheme getCurrentScheme() {
        return myScheme;
    }

    @TestOnly
    FileTemplateConfigurable getEditor() {
        return myEditor;
    }

    @TestOnly
    FileTemplateTab[] getTabs() {
        return myTabs;
    }
}
