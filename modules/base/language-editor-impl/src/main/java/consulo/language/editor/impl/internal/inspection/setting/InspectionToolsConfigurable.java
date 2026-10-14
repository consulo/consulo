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
package consulo.language.editor.impl.internal.inspection.setting;

import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.impl.internal.inspection.InspectionManagerBase;
import consulo.application.Application;
import consulo.codeEditor.CodeInsightColors;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.language.Language;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.impl.internal.inspection.InspectionProjectProfileManager;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionToolRegistrar;
import consulo.language.editor.impl.internal.rawHighlight.SeverityRegistrarImpl;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProfileManager;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.inspection.scheme.ModifiableModel;
import consulo.language.editor.inspection.scheme.Profile;
import consulo.language.editor.inspection.scheme.ProfileManager;
import consulo.language.editor.rawHighlight.HighlightInfoTypeImpl;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.InputBoxBuilder;
import consulo.ui.InputProblem;
import consulo.ui.Label;
import consulo.ui.MessageBoxes;
import consulo.ui.Space;
import consulo.ui.TextAttribute;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.io.FileUtil;
import consulo.util.jdom.JDOMUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.util.lang.SystemProperties;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.virtualFileSystem.VirtualFile;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Anna.Kozlova
 * @since 2006-07-31
 */
public abstract class InspectionToolsConfigurable implements SearchableConfigurable, Configurable.NoMargin, Configurable.NoScroll {
    private static final Logger LOG = Logger.getInstance(InspectionToolsConfigurable.class);

    public static final String ID = "editor.code.inspections";

    protected final Application myApplication;
    protected final InspectionProfileManager myProfileManager;
    protected final InspectionProjectProfileManager myProjectProfileManager;

    private final List<InspectionProfilePanel> myPanels = new ArrayList<>();
    private final Set<InspectionProfilePanel> myDeletedPanels = new HashSet<>();
    private final Set<Language> myFilterLanguages = new LinkedHashSet<>();

    private @Nullable Disposable myUIDisposable;
    private @Nullable MutableFlatDataModel<InspectionProfilePanel> myProfilesModel;
    private @Nullable ComboBox<InspectionProfilePanel> myProfiles;
    private @Nullable Label myDescriptionLabel;
    private @Nullable DockLayout myContent;
    private @Nullable DockLayout myRoot;

    private @Nullable Profile myPendingProfile;
    private @Nullable String myPendingTool;

    protected InspectionToolsConfigurable(
        Application application,
        InspectionProjectProfileManager projectProfileManager,
        InspectionProfileManager profileManager
    ) {
        myApplication = application;
        myProjectProfileManager = projectProfileManager;
        myProfileManager = profileManager;
    }

    protected abstract InspectionProfileImpl getCurrentProfile();

    protected abstract void applyRootProfile(String name, boolean isShared);

    protected boolean acceptTool(InspectionToolWrapper entry) {
        return true;
    }

    private Project getProject() {
        return myProjectProfileManager.getProject();
    }

    @Override
    public LocalizeValue getDisplayName() {
        return InspectionLocalize.inspectionSettingsIdeDisplayName();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public @Nullable Runnable enableSearch(String option) {
        return () -> {
            InspectionProfilePanel panel = getSelectedPanel();
            if (panel != null) {
                panel.setFilter(option);
            }
        };
    }

    @RequiredUIAccess
    @Override
    public Component createUIComponent(Disposable parentDisposable) {
        DockLayout root = myRoot;
        if (root != null) {
            return root;
        }

        if (InspectionManager.getInstance(getProject()) instanceof InspectionManagerBase manager) {
            manager.buildInspectionSearchIndexIfNecessary();
        }

        Disposable uiDisposable = Disposable.newDisposable();
        Disposer.register(parentDisposable, uiDisposable);
        myUIDisposable = uiDisposable;

        MutableFlatDataModel<InspectionProfilePanel> profilesModel = FlatDataModel.of(List.of());
        myProfilesModel = profilesModel;

        ComboBox<InspectionProfilePanel> profiles = ComboBox.create(profilesModel);
        profiles.setRenderer((presentation, item) -> {
            InspectionProfilePanel panel = item.getValue();
            if (panel == null) {
                return;
            }
            presentation.append(LocalizeValue.of(panel.getCurrentProfileName()), panel.isProfileShared() ? TextAttribute.REGULAR_BOLD : TextAttribute.REGULAR);
            if (panel.isProfileShared()) {
                presentation.append(LocalizeValue.of(" "), TextAttribute.GRAYED);
                presentation.append(InspectionLocalize.inspectionSettingsProfileShared(), TextAttribute.GRAYED);
            }
        });
        profiles.addValueListener(event -> onProfileChosen(event.getValue()));
        myProfiles = profiles;

        Button manage = Button.create(LocalizeValue.empty());
        manage.setIcon(PlatformIconGroup.generalGearplain());
        manage.addStyle(ButtonStyle.INPLACE);
        manage.setToolTipText(InspectionLocalize.inspectionSettingsManage());
        manage.addClickListener(this::showManagePopup);
        profiles.setSuffixComponent(manage);

        HorizontalLayout profileRow = HorizontalLayout.create(Space.SMALL);
        profileRow.add(Label.create(InspectionLocalize.inspectionSettingsProfileLabel()));
        profileRow.add(profiles);

        Label descriptionLabel = Label.create();
        myDescriptionLabel = descriptionLabel;

        DockLayout header = DockLayout.create(Space.MEDIUM);
        header.left(profileRow);
        header.center(descriptionLabel);
        header.paddingBuilder().leftSet(Space.MEDIUM).rightSet(Space.MEDIUM).topSet(Space.MEDIUM).apply();

        DockLayout content = DockLayout.create(Space.NONE);
        myContent = content;

        root = DockLayout.create(Space.NONE);
        root.top(header);
        root.center(content);
        myRoot = root;
        return root;
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component getPreferredFocusedUIComponent() {
        InspectionProfilePanel panel = getSelectedPanel();
        return panel == null ? null : panel.getPreferredFocusedComponent();
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        if (myRoot == null) {
            return false;
        }

        InspectionProfilePanel selectedPanel = getSelectedPanel();
        if (selectedPanel != null && !Comparing.equal(selectedPanel.getSelectedProfile(), getCurrentProfile())) {
            return true;
        }
        for (InspectionProfilePanel panel : myPanels) {
            if (!myDeletedPanels.contains(panel) && panel.isModified()) {
                return true;
            }
        }
        return getProfiles().size() != myPanels.size() || !myDeletedPanels.isEmpty();
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        if (myRoot == null) {
            return;
        }

        InspectionProfilePanel selectedPanel = getSelectedPanel();
        for (InspectionProfilePanel panel : myPanels) {
            if (myDeletedPanels.contains(panel)) {
                deleteProfile(panel.getSelectedProfile());
            }
            else {
                panel.apply();
                if (panel == selectedPanel) {
                    applyRootProfile(panel.getCurrentProfileName(), panel.isProfileShared());
                }
            }
        }
        doReset();
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        if (myRoot == null) {
            return;
        }
        doReset();
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        disposePanels();

        Disposable uiDisposable = myUIDisposable;
        if (uiDisposable != null) {
            Disposer.dispose(uiDisposable);
        }

        myUIDisposable = null;
        myProfilesModel = null;
        myProfiles = null;
        myDescriptionLabel = null;
        myContent = null;
        myRoot = null;
    }

    @RequiredUIAccess
    public void selectProfile(Profile profile) {
        InspectionProfilePanel panel = findPanel(profile);
        ComboBox<InspectionProfilePanel> profiles = myProfiles;
        if (panel != null && profiles != null) {
            profiles.setValue(panel);
        }
        else {
            myPendingProfile = profile;
        }
    }

    @RequiredUIAccess
    public void selectInspectionTool(String selectedToolShortName) {
        InspectionProfilePanel panel = getSelectedPanel();
        if (panel != null) {
            panel.selectInspectionTool(selectedToolShortName);
        }
        else {
            myPendingTool = selectedToolShortName;
        }
    }

    public void setFilterLanguages(Collection<Language> languages) {
        myFilterLanguages.clear();
        myFilterLanguages.addAll(languages);
        for (InspectionProfilePanel panel : myPanels) {
            panel.getInspectionsFilter().addLanguages(myFilterLanguages);
        }
    }

    public @Nullable InspectionProfileImpl getSelectedObject() {
        InspectionProfilePanel panel = getSelectedPanel();
        return panel == null ? null : panel.getSelectedProfile();
    }

    protected void deleteProfile(Profile profile) {
        String name = profile.getName();
        if (profile.getProfileManager() == myProfileManager) {
            if (myProfileManager.getProfile(name, false) != null) {
                myProfileManager.deleteProfile(name);
            }
            return;
        }
        if (profile.getProfileManager() == myProjectProfileManager) {
            if (myProjectProfileManager.getProfile(name, false) != null) {
                myProjectProfileManager.deleteProfile(name);
            }
        }
    }

    protected Collection<InspectionProfile> getProfiles() {
        Collection<InspectionProfile> result = new ArrayList<>();
        for (Profile profile : new TreeSet<>(myProfileManager.getProfiles())) {
            result.add((InspectionProfile) profile);
        }
        for (Profile profile : myProjectProfileManager.getProfiles()) {
            result.add((InspectionProfile) profile);
        }
        return result;
    }

    @RequiredUIAccess
    private void doReset() {
        disposePanels();

        for (InspectionProfile profile : getProfiles()) {
            InspectionProfileImpl modifiableProfile = (InspectionProfileImpl) profile.getModifiableModel();
            myPanels.add(createPanel(modifiableProfile, profile.getName()));
        }

        MutableFlatDataModel<InspectionProfilePanel> profilesModel = Objects.requireNonNull(myProfilesModel);
        profilesModel.replaceAll(new ArrayList<>(myPanels));

        Profile toSelect = myPendingProfile != null ? myPendingProfile : getCurrentProfile();
        myPendingProfile = null;

        InspectionProfilePanel panel = findPanel(toSelect);
        if (panel == null && !myPanels.isEmpty()) {
            panel = myPanels.get(0);
        }

        ComboBox<InspectionProfilePanel> profiles = Objects.requireNonNull(myProfiles);
        if (panel != null && profiles.getValue() != panel) {
            profiles.setValue(panel);
        }
        onProfileChosen(panel);
    }

    @RequiredUIAccess
    private void disposePanels() {
        for (InspectionProfilePanel panel : myPanels) {
            Disposer.dispose(panel);
        }
        myPanels.clear();
        myDeletedPanels.clear();

        DockLayout content = myContent;
        if (content != null) {
            content.removeAll();
        }
    }

    @RequiredUIAccess
    private void onProfileChosen(@Nullable InspectionProfilePanel panel) {
        DockLayout content = myContent;
        Label descriptionLabel = myDescriptionLabel;
        if (content == null || descriptionLabel == null) {
            return;
        }

        content.removeAll();
        if (panel == null) {
            descriptionLabel.setText(LocalizeValue.empty());
            return;
        }

        content.center(panel.getComponent());
        descriptionLabel.setText(LocalizeValue.of(StringUtil.notNullize(panel.getSelectedProfile().getDescription())));

        String tool = myPendingTool;
        if (tool != null) {
            myPendingTool = null;
            panel.selectInspectionTool(tool);
        }
    }

    private InspectionProfilePanel createPanel(InspectionProfileImpl profile, String profileName) {
        InspectionProfilePanel panel = new InspectionProfilePanel(
            myApplication,
            myProjectProfileManager,
            profileName,
            profile,
            this::acceptTool
        );
        panel.getInspectionsFilter().addLanguages(myFilterLanguages);

        Disposable uiDisposable = myUIDisposable;
        if (uiDisposable != null) {
            Disposer.register(uiDisposable, panel);
        }
        return panel;
    }

    private @Nullable InspectionProfilePanel getSelectedPanel() {
        ComboBox<InspectionProfilePanel> profiles = myProfiles;
        return profiles == null ? null : profiles.getValue();
    }

    private @Nullable InspectionProfilePanel findPanel(Profile profile) {
        for (InspectionProfilePanel panel : myPanels) {
            if (!myDeletedPanels.contains(panel) && Comparing.equal(panel.getSelectedProfile(), profile)) {
                return panel;
            }
        }
        return null;
    }

    private boolean hasName(String name, boolean shared) {
        for (InspectionProfilePanel panel : myPanels) {
            if (!myDeletedPanels.contains(panel) && name.equals(panel.getCurrentProfileName()) && shared == panel.isProfileShared()) {
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    private void addProfile(InspectionProfileImpl model) {
        InspectionProfilePanel panel = createPanel(model, model.getName());
        myPanels.add(panel);
        refreshProfiles();
        Objects.requireNonNull(myProfiles).setValue(panel);
    }

    @RequiredUIAccess
    private void refreshProfiles() {
        MutableFlatDataModel<InspectionProfilePanel> profilesModel = myProfilesModel;
        ComboBox<InspectionProfilePanel> profiles = myProfiles;
        if (profilesModel == null || profiles == null) {
            return;
        }

        InspectionProfilePanel selected = profiles.getValue();
        List<InspectionProfilePanel> visible = new ArrayList<>();
        for (InspectionProfilePanel panel : myPanels) {
            if (!myDeletedPanels.contains(panel)) {
                visible.add(panel);
            }
        }
        profilesModel.replaceAll(visible);
        if (selected != null && visible.contains(selected)) {
            profiles.setValue(selected);
        }
        else if (!visible.isEmpty()) {
            profiles.setValue(visible.get(0));
        }
    }

    @RequiredUIAccess
    private void showManagePopup(ClickEvent event) {
        InspectionProfilePanel panel = getSelectedPanel();
        DockLayout root = myRoot;
        if (panel == null || root == null) {
            return;
        }

        ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
        group.add(new ToggleAction(InspectionLocalize.inspectionSettingsShareToTeam()) {
            @Override
            public boolean isSelected(AnActionEvent e) {
                return panel.isProfileShared();
            }

            @RequiredUIAccess
            @Override
            public void setSelected(AnActionEvent e, boolean state) {
                setShareToTeamMembers(panel, state);
            }

            @Override
            public boolean isDumbAware() {
                return true;
            }
        });
        group.addSeparator();
        group.add(DumbAwareAction.create(CommonLocalize.buttonCopy(), e -> copy(panel)));
        if (!panel.getSelectedProfile().isProfileLocked()) {
            group.add(DumbAwareAction.create(InspectionLocalize.inspectionSettingsRename(), e -> rename(panel)));
        }
        if (isDeleteEnabled(panel)) {
            group.add(DumbAwareAction.create(CommonLocalize.buttonDelete(), e -> delete(panel)));
        }
        boolean hasDescription = !StringUtil.isEmpty(panel.getSelectedProfile().getDescription());
        group.add(DumbAwareAction.create(
            hasDescription ? InspectionLocalize.inspectionSettingsEditDescription() : InspectionLocalize.inspectionSettingsAddDescription(),
            e -> editDescription(panel)
        ));
        group.add(DumbAwareAction.create(InspectionLocalize.inspectionSettingsExport(), e -> export(panel)));
        group.addSeparator();
        group.add(DumbAwareAction.create(InspectionLocalize.inspectionSettingsImport(), e -> doImport()));

        JBPopupFactory.getInstance().createActionGroupPopup(
            null,
            group.build(),
            DataManager.getInstance().getDataContext(event.getComponent()),
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            false
        ).showBy(event);
    }

    @RequiredUIAccess
    private void setShareToTeamMembers(InspectionProfilePanel selectedPanel, boolean shared) {
        String name = selectedPanel.getCurrentProfileName();
        for (InspectionProfilePanel panel : myPanels) {
            if (panel != selectedPanel && !myDeletedPanels.contains(panel) && Comparing.equal(panel.getCurrentProfileName(), name)) {
                if (panel.isProfileShared() == shared) {
                    MessageBoxes.okError(shared ? InspectionLocalize.inspectionSettingsSharedProfileExists() : InspectionLocalize.inspectionSettingsApplicationProfileExists())
                        .title(InspectionLocalize.inspectionSettingsIdeDisplayName())
                        .showAsync(myRoot);
                    return;
                }
            }
        }

        selectedPanel.setProfileShared(shared);
        refreshProfiles();
    }

    @RequiredUIAccess
    private void copy(InspectionProfilePanel panel) {
        InspectionProfileImpl selectedProfile = panel.getSelectedProfile();
        String profileName = selectedProfile.getName();
        do {
            profileName = InspectionLocalize.inspectionSettingsCopyName(profileName).get();
        }
        while (hasName(profileName, panel.isProfileShared()));

        ProfileManager profileManager = selectedProfile.getProfileManager();
        InspectionProfileImpl inspectionProfile =
            new InspectionProfileImpl(profileName, InspectionToolRegistrar.fromApplication(myApplication), profileManager);
        inspectionProfile.copyFrom(selectedProfile);
        inspectionProfile.setName(profileName);
        inspectionProfile.initInspectionTools(getProject());
        inspectionProfile.setModified(true);

        InspectionProfileImpl modifiableModel = (InspectionProfileImpl) inspectionProfile.getModifiableModel();
        modifiableModel.setModified(true);
        modifiableModel.setProjectLevel(false);
        addProfile(modifiableModel);

        InspectionProfilePanel copyPanel = Objects.requireNonNull(getSelectedPanel());
        rename(copyPanel);
    }

    @RequiredUIAccess
    private void rename(InspectionProfilePanel panel) {
        String initialName = panel.getCurrentProfileName();
        UIAccess uiAccess = UIAccess.current();
        InputBoxBuilder.text()
            .title(InspectionLocalize.inspectionSettingsRenameTitle())
            .text(InspectionLocalize.inspectionSettingsRenameText())
            .value(initialName)
            .validator(value -> {
                String name = StringUtil.notNullize(value).trim();
                if (name.isEmpty()) {
                    return InputProblem.error(InspectionLocalize.inspectionSettingsNameEmpty());
                }
                if (!name.equals(initialName) && hasName(name, panel.isProfileShared())) {
                    return InputProblem.error(InspectionLocalize.inspectionSettingsNameInUse());
                }
                return null;
            })
            .showAsync(myRoot)
            .whenCompleteAsync((value, error) -> {
                if (value == null) {
                    return;
                }
                String name = value.trim();
                if (!name.isEmpty() && !name.equals(initialName)) {
                    panel.setCurrentProfileName(name);
                    refreshProfiles();
                }
            }, uiAccess);
    }

    private boolean isDeleteEnabled(InspectionProfilePanel selectedPanel) {
        boolean selectedIsProject = selectedPanel.getSelectedProfile().getProfileManager() == myProjectProfileManager;

        boolean projectProfileFound = false;
        boolean ideProfileFound = false;
        for (InspectionProfilePanel panel : myPanels) {
            if (panel == selectedPanel || myDeletedPanels.contains(panel)) {
                continue;
            }
            boolean isProjectProfile = panel.getSelectedProfile().getProfileManager() == myProjectProfileManager;
            projectProfileFound |= isProjectProfile;
            ideProfileFound |= !isProjectProfile;
        }
        return selectedIsProject ? projectProfileFound : ideProfileFound;
    }

    @RequiredUIAccess
    private void delete(InspectionProfilePanel panel) {
        myDeletedPanels.add(panel);
        refreshProfiles();
    }

    @RequiredUIAccess
    private void editDescription(InspectionProfilePanel panel) {
        InspectionProfileImpl profile = panel.getSelectedProfile();
        UIAccess uiAccess = UIAccess.current();
        InputBoxBuilder.text()
            .title(InspectionLocalize.inspectionSettingsDescriptionTitle())
            .text(InspectionLocalize.inspectionSettingsDescriptionText())
            .value(StringUtil.notNullize(profile.getDescription()))
            .showAsync(myRoot)
            .whenCompleteAsync((description, error) -> {
                if (description != null && !Comparing.strEqual(description, profile.getDescription())) {
                    profile.setDescription(description);
                    profile.setModified(true);
                }
                Label descriptionLabel = myDescriptionLabel;
                if (descriptionLabel != null && getSelectedPanel() == panel) {
                    descriptionLabel.setText(LocalizeValue.of(StringUtil.notNullize(profile.getDescription())));
                }
            }, uiAccess);
    }

    @RequiredUIAccess
    private void export(InspectionProfilePanel panel) {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            .withDescription(InspectionLocalize.inspectionSettingsExportDescription());

        UIAccess uiAccess = UIAccess.current();
        FileChooser.chooseFile(descriptor, getProject(), (VirtualFile) null).whenCompleteAsync((file, error) -> {
            if (file == null) {
                return;
            }

            InspectionProfileImpl profile = panel.getSelectedProfile();
            String filePath = FileUtil.toSystemDependentName(file.getPath()) + File.separator + FileUtil.sanitizeFileName(profile.getName()) + ".xml";
            if (new File(filePath).isFile()) {
                MessageBoxes.okCancel()
                    .asWarning()
                    .text(InspectionLocalize.inspectionSettingsExportOverwrite(filePath))
                    .showAsync(myRoot)
                    .whenCompleteAsync((ok, e) -> {
                        if (Boolean.TRUE.equals(ok)) {
                            writeProfile(profile, filePath);
                        }
                    }, uiAccess);
            }
            else {
                writeProfile(profile, filePath);
            }
        }, uiAccess);
    }

    private static void writeProfile(InspectionProfileImpl profile, String filePath) {
        Element element = new Element("inspections");
        try {
            profile.writeExternal(element);
            JDOMUtil.writeDocument(new Document(element), filePath, SystemProperties.getLineSeparator());
        }
        catch (WriteExternalException | IOException e) {
            LOG.error(e);
        }
    }

    @RequiredUIAccess
    private void doImport() {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
            .withFileFilter(file -> "xml".equals(file.getExtension()))
            .withDescription(InspectionLocalize.inspectionSettingsImportDescription());

        UIAccess uiAccess = UIAccess.current();
        FileChooser.chooseFile(descriptor, getProject(), (VirtualFile) null).whenCompleteAsync((file, error) -> {
            if (file != null) {
                importProfile(file, uiAccess);
            }
        }, uiAccess);
    }

    @RequiredUIAccess
    private void importProfile(VirtualFile file, UIAccess uiAccess) {
        Element rootElement;
        try (InputStream stream = file.getInputStream()) {
            rootElement = JDOMUtil.loadDocument(stream).getRootElement();
        }
        catch (IOException | JDOMException e) {
            LOG.error(e);
            return;
        }

        if (Comparing.strEqual(rootElement.getName(), "component")) {
            rootElement = rootElement.getChildren().get(0);
        }

        Set<String> levels = new LinkedHashSet<>();
        for (Element inspectElement : rootElement.getChildren("inspection_tool")) {
            levels.add(inspectElement.getAttributeValue("level"));
            for (Element scope : inspectElement.getChildren("scope")) {
                levels.add(scope.getAttributeValue("level"));
            }
        }
        levels.remove(null);
        for (Iterator<String> iterator = levels.iterator(); iterator.hasNext(); ) {
            String level = iterator.next();
            if (myProfileManager.getOwnSeverityRegistrar().getSeverity(level) != null) {
                iterator.remove();
            }
        }

        Element profileElement = rootElement;
        if (levels.isEmpty()) {
            readProfile(profileElement, uiAccess);
            return;
        }

        MessageBoxes.yesNo()
            .asWarning()
            .text(InspectionLocalize.inspectionSettingsImportUndefinedSeverities(StringUtil.join(levels, ", ")))
            .showAsync(myRoot)
            .whenCompleteAsync((yes, error) -> {
                if (Boolean.TRUE.equals(yes)) {
                    TextAttributes textAttributes = Objects.requireNonNull(CodeInsightColors.WARNINGS_ATTRIBUTES.getDefaultAttributes());
                    for (String level : levels) {
                        HighlightInfoTypeImpl info = new HighlightInfoTypeImpl(new HighlightSeverity(level, 50), TextAttributesKey.of(level));
                        ((SeverityRegistrarImpl) myProfileManager.getOwnSeverityRegistrar()).registerSeverity(
                            new SeverityRegistrarImpl.SeverityBasedTextAttributes(textAttributes.clone(), info),
                            textAttributes.getErrorStripeColor()
                        );
                    }
                }
                readProfile(profileElement, uiAccess);
            }, uiAccess);
    }

    @RequiredUIAccess
    private void readProfile(Element rootElement, UIAccess uiAccess) {
        InspectionProfileImpl profile =
            new InspectionProfileImpl("TempProfile", InspectionToolRegistrar.fromApplication(myApplication), myProfileManager);
        try {
            profile.readExternal(rootElement);
        }
        catch (InvalidDataException e) {
            LOG.error(e);
            return;
        }
        profile.setProjectLevel(false);
        profile.initInspectionTools(getProject());

        if (findPanel(profile) == null) {
            addImportedProfile(profile);
            return;
        }

        MessageBoxes.okCancel()
            .asInfo()
            .text(InspectionLocalize.inspectionSettingsImportOverwrite(profile.getName()))
            .showAsync(myRoot)
            .whenCompleteAsync((ok, error) -> {
                if (Boolean.TRUE.equals(ok)) {
                    addImportedProfile(profile);
                }
            }, uiAccess);
    }

    @RequiredUIAccess
    private void addImportedProfile(InspectionProfileImpl profile) {
        ModifiableModel model = profile.getModifiableModel();
        model.setModified(true);
        addProfile((InspectionProfileImpl) model);
    }
}
