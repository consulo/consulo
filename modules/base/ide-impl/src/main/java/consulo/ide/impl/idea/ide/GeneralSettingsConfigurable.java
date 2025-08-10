/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.configurable.*;
import consulo.disposer.Disposable;
import consulo.execution.ProcessCloseConfirmation;
import consulo.fileChooser.provider.FileChooseDialogProvider;
import consulo.fileChooser.provider.FileOperateDialogProvider;
import consulo.fileChooser.provider.FileSaveDialogProvider;
import consulo.ide.impl.fileChooser.FileOperateDialogSettings;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;
import consulo.project.internal.RecentProjectsManager;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.LabeledBuilder;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

@ExtensionImpl
public class GeneralSettingsConfigurable extends SimpleConfigurable<GeneralSettingsConfigurable.MyComponent>
    implements SearchableConfigurable, ApplicationConfigurable {

    private record LocaleInfo(@Nonnull Locale locale, boolean isDefault) {
        public Locale setValue() {
            if (isDefault()) {
                return null;
            }
            return locale();
        }
    }

    protected static class MyComponent implements Supplier<Component> {
        private CheckBox myChkReopenLastProject;
        private CheckBox myConfirmExit;

        private RadioButton myOpenProjectInNewWindow;
        private RadioButton myOpenProjectInSameWindow;
        private RadioButton myConfirmWindowToOpenProject;

        private IntBox myRecentProjectsLimit;

        private CheckBox myChkSyncOnFrameActivation;
        private CheckBox myChkSaveOnFrameDeactivation;
        private CheckBox myChkAutoSaveIfInactive;
        private TextBox myTfInactiveTimeout;
        private CheckBox myChkUseSafeWrite;

        private CheckBox myChkSupportScreenReaders;

        private ComboBox<Locale> myLocaleBox;

        private RadioButton myTerminateProcessRadioButton;
        private RadioButton myDisconnectRadioButton;
        private RadioButton myAskRadioButton;

        private ComboBox<FileOperateDialogProvider> myFileChooseDialogBox;
        private ComboBox<FileOperateDialogProvider> myFileSaveDialogBox;

        private VerticalLayout myRootLayout;

        private boolean myApplied;
        private LocaleInfo myInitialLocale;
        private boolean myLocaleChanged;

        private AtomicInteger myEventBlocker = new AtomicInteger();

        @RequiredUIAccess
        public MyComponent(@Nonnull Application application) {
            myRootLayout = VerticalLayout.create(0);

            VerticalLayout startupOrShutdownLayout = VerticalLayout.create();
            startupOrShutdownLayout.add(myChkReopenLastProject = CheckBox.create(IdeLocalize.checkboxReopenLastProjectOnStartup()));
            startupOrShutdownLayout.add(myConfirmExit = CheckBox.create(IdeLocalize.checkboxConfirmApplicationExit()));
            myRootLayout.add(LabeledLayout.create(LocalizeValue.localizeTODO("Startup/Shutdown"), startupOrShutdownLayout));

            VerticalLayout projectReopeningLayout = VerticalLayout.create();
            ValueGroup<Boolean> projectOpenGroup = ValueGroup.createBool();
            projectReopeningLayout.add(
                myOpenProjectInNewWindow = RadioButton.create(LocalizeValue.localizeTODO("Open project in new window"))
                    .toGroup(projectOpenGroup)
            );
            projectReopeningLayout.add(
                myOpenProjectInSameWindow = RadioButton.create(LocalizeValue.localizeTODO("Open project in the same window"))
                    .toGroup(projectOpenGroup)
            );
            projectReopeningLayout.add(
                myConfirmWindowToOpenProject = RadioButton.create(LocalizeValue.localizeTODO("Confirm window to open project in"))
                    .toGroup(projectOpenGroup)
            );

            myRecentProjectsLimit = IntBox.create(RecentProjectsManager.DEFAULT_RECENT_PROJECTS_LIMIT);
            myRecentProjectsLimit.setRange(0, Integer.MAX_VALUE);

            projectReopeningLayout.add(LabeledBuilder.sided(LocalizeValue.localizeTODO("Recent Projects Limit"), myRecentProjectsLimit));

            myRootLayout.add(LabeledLayout.create(LocalizeValue.localizeTODO("Project Opening"), projectReopeningLayout));

            VerticalLayout syncLayout = VerticalLayout.create();
            syncLayout.add(myChkSyncOnFrameActivation = CheckBox.create(IdeLocalize.checkboxSynchronizeFilesOnFrameActivation()));
            syncLayout.add(
                myChkSaveOnFrameDeactivation = CheckBox.create(IdeLocalize.checkboxSaveFilesOnFrameDeactivation())
            );

            HorizontalLayout syncWithIde = HorizontalLayout.create();
            syncLayout.add(syncWithIde);
            syncWithIde.add(myChkAutoSaveIfInactive = CheckBox.create(IdeLocalize.checkboxSaveFilesAutomatically()));
            syncWithIde.add(myTfInactiveTimeout = TextBox.create());
            myTfInactiveTimeout.setEnabled(false);
            syncWithIde.add(Label.create(IdeLocalize.labelInactiveTimeoutSec()));
            myChkAutoSaveIfInactive.addValueListener(event -> myTfInactiveTimeout.setEnabled(event.getValue()));

            syncLayout.add(
                myChkUseSafeWrite = CheckBox.create(LocalizeValue.localizeTODO("Use \"safe write\" (save changes to a temporary file first)"))
            );
            myRootLayout.add(LabeledLayout.create(LocalizeValue.localizeTODO("Synchronization"), syncLayout));

            VerticalLayout screenLayout = VerticalLayout.create();
            screenLayout.add(myChkSupportScreenReaders = CheckBox.create(IdeLocalize.checkboxSupportScreenReaders()));
            myRootLayout.add(LabeledLayout.create(LocalizeValue.localizeTODO("Accessibility"), screenLayout));

            VerticalLayout localizeLayout = VerticalLayout.create();
            Set<Locale> avaliableLocales = LocalizeManager.get().getAvaliableLocales();

            List<Locale> locales = new ArrayList<>(avaliableLocales);
            locales.sort(Comparator.comparing(Locale::getDisplayName));
            locales.addFirst(Locale.ROOT);

            ComboBox<Locale> localeBox = ComboBox.create(locales);
            localeBox.setValue(Locale.ROOT);
            localeBox.setTextRender(locale -> {
                if (locale == null || locale == Locale.ROOT) {
                    Locale detectedLocale = LocalizeManager.get().getAutoDetectedLocale();
                    return IdeLocalize.optionsLocaleAutoLocale(detectedLocale.getDisplayName());
                }

                return LocalizeValue.of(locale.getDisplayName());
            });
            localeBox.addValueListener(event -> {
                myLocaleChanged = true;

                if (myEventBlocker.get() == 0) {
                    LocalizeManager.get().setLocale(event.getValue());
                }
            });

            localizeLayout.add(LabeledBuilder.sided(LocalizeValue.localizeTODO("Locale"), myLocaleBox = localeBox));
            myRootLayout.add(LabeledLayout.create(LocalizeValue.localizeTODO("Localization"), localizeLayout));

            VerticalLayout processLayout = VerticalLayout.create();
            ValueGroup<Boolean> processGroup = ValueGroup.createBool();
            processLayout.add(myTerminateProcessRadioButton = RadioButton.create(IdeLocalize.radioProcessCloseTerminate()).toGroup(processGroup));
            processLayout.add(myDisconnectRadioButton = RadioButton.create(IdeLocalize.radioProcessCloseDisconnect()).toGroup(processGroup));
            processLayout.add(myAskRadioButton = RadioButton.create(IdeLocalize.radioProcessCloseAsk()).toGroup(processGroup));

            myRootLayout.add(LabeledLayout.create(IdeLocalize.groupSettingsProcessTabClose(), processLayout));

            VerticalLayout fileDialogsLayout = VerticalLayout.create();

            ComboBox.Builder<FileOperateDialogProvider> fileChooseDialogBox = ComboBox.<FileOperateDialogProvider>builder();
            application.getExtensionPoint(FileChooseDialogProvider.class).forEach(fileChooseDialogProvider -> {
                if (fileChooseDialogProvider.isAvailable()) {
                    fileChooseDialogBox.add(fileChooseDialogProvider, fileChooseDialogProvider.getName());
                }
            });

            fileDialogsLayout.add(LabeledBuilder.sided(
                LocalizeValue.localizeTODO("File/Path Choose Dialog Type"),
                myFileChooseDialogBox = fileChooseDialogBox.build()
            ));

            ComboBox.Builder<FileOperateDialogProvider> fileSaveDialogBox = ComboBox.<FileOperateDialogProvider>builder();
            application.getExtensionPoint(FileSaveDialogProvider.class).forEach(fileSaveDialogProvider -> {
                if (fileSaveDialogProvider.isAvailable()) {
                    fileSaveDialogBox.add(fileSaveDialogProvider, fileSaveDialogProvider.getName());
                }
            });

            fileDialogsLayout.add(LabeledBuilder.sided(
                LocalizeValue.localizeTODO("File Save Dialog Type"),
                myFileSaveDialogBox = fileSaveDialogBox.build()
            ));

            myRootLayout.add(LabeledLayout.create(LocalizeValue.localizeTODO("File Dialogs"), fileDialogsLayout));
        }

        @Nonnull
        @Override
        public Component get() {
            return myRootLayout;
        }
    }

    private final Application myApplication;
    private final Provider<GeneralSettings> myGeneralSettings;
    private final Provider<FileOperateDialogSettings> myFileOperateDialogSettings;

    @Inject
    public GeneralSettingsConfigurable(
        Application application,
        Provider<GeneralSettings> generalSettings,
        Provider<FileOperateDialogSettings> fileOperateDialogSettings
    ) {
        myGeneralSettings = generalSettings;
        myFileOperateDialogSettings = fileOperateDialogSettings;
        myApplication = application;
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    protected MyComponent createPanel(@Nonnull Disposable uiDisposable) {
        return new MyComponent(myApplication);
    }

    @RequiredUIAccess
    @Override
    protected boolean isModified(@Nonnull MyComponent component) {
        GeneralSettings generalSettings = myGeneralSettings.get();
        RecentProjectsManager recentProjectsManager = RecentProjectsManager.getInstance();

        boolean isModified = false;
        isModified |= generalSettings.isReopenLastProject() != component.myChkReopenLastProject.getValue();
        isModified |= generalSettings.isConfirmExit() != component.myConfirmExit.getValue();
        isModified |= generalSettings.isSupportScreenReaders() != component.myChkSupportScreenReaders.getValue();
        isModified |= generalSettings.isSyncOnFrameActivation() != component.myChkSyncOnFrameActivation.getValue();
        isModified |= generalSettings.isSaveOnFrameDeactivation() != component.myChkSaveOnFrameDeactivation.getValue();
        isModified |= generalSettings.isAutoSaveIfInactive() != component.myChkAutoSaveIfInactive.getValue();
        isModified |= generalSettings.getProcessCloseConfirmation() != getProcessCloseConfirmation(component);
        isModified |= generalSettings.getConfirmOpenNewProject() != getConfirmOpenNewProject(component);

        isModified |= !Objects.equals(recentProjectsManager.getRecentProjectsLimit(), component.myRecentProjectsLimit.getValueOrError());

        FileOperateDialogSettings dialogSettings = myFileOperateDialogSettings.get();
        isModified |= isModified(dialogSettings.getFileChooseDialogId(), component.myFileChooseDialogBox);
        isModified |= isModified(dialogSettings.getFileSaveDialogId(), component.myFileSaveDialogBox);

        isModified |= isLocaleChanged(component.myLocaleBox.getValueOrError()) || component.myLocaleChanged;

        int inactiveTimeout = -1;
        try {
            inactiveTimeout = Integer.parseInt(component.myTfInactiveTimeout.getValue());
        }
        catch (NumberFormatException ignored) {
        }
        isModified |= inactiveTimeout > 0 && generalSettings.getInactiveTimeout() != inactiveTimeout;

        isModified |= generalSettings.isUseSafeWrite() != component.myChkUseSafeWrite.getValue();

        return isModified;
    }

    private boolean isLocaleChanged(@Nonnull Locale locale) {
        LocalizeManager localizeManager = LocalizeManager.get();

        if (locale == Locale.ROOT && localizeManager.isDefaultLocale()) {
            return false;
        }

        return !Objects.equals(locale, localizeManager.getLocale());
    }

    private boolean isModified(@Nullable String id, ComboBox<FileOperateDialogProvider> comboBox) {
        FileOperateDialogProvider value = comboBox.getValueOrError();

        if (id == null && FileOperateDialogProvider.APPLICATION_ID.equals(value.getId())) {
            return false;
        }

        return !Objects.equals(id, value.getId());
    }

    @RequiredUIAccess
    @Override
    protected void apply(@Nonnull MyComponent component) throws ConfigurationException {
        component.myApplied = true;

        GeneralSettings generalSettings = myGeneralSettings.get();
        RecentProjectsManager recentProjectsManager = RecentProjectsManager.getInstance();

        generalSettings.setReopenLastProject(component.myChkReopenLastProject.getValue());
        generalSettings.setSupportScreenReaders(component.myChkSupportScreenReaders.getValue());
        generalSettings.setSyncOnFrameActivation(component.myChkSyncOnFrameActivation.getValue());
        generalSettings.setSaveOnFrameDeactivation(component.myChkSaveOnFrameDeactivation.getValue());
        generalSettings.setConfirmExit(component.myConfirmExit.getValue());
        generalSettings.setConfirmOpenNewProject(getConfirmOpenNewProject(component));
        generalSettings.setProcessCloseConfirmation(getProcessCloseConfirmation(component));
        recentProjectsManager.setRecentProjectsLimit(component.myRecentProjectsLimit.getValueOrError());

        LocalizeManager localizeManager = LocalizeManager.get();
        Locale locale = component.myLocaleBox.getValueOrError();
        localizeManager.setLocale(locale == Locale.ROOT ? null : locale);

        generalSettings.setAutoSaveIfInactive(component.myChkAutoSaveIfInactive.getValue());
        try {
            int newInactiveTimeout = Integer.parseInt(component.myTfInactiveTimeout.getValue());
            if (newInactiveTimeout > 0) {
                generalSettings.setInactiveTimeout(newInactiveTimeout);
            }
        }
        catch (NumberFormatException ignored) {
        }
        generalSettings.setUseSafeWrite(component.myChkUseSafeWrite.getValue());

        FileOperateDialogSettings dialogSettings = myFileOperateDialogSettings.get();

        apply(component.myFileChooseDialogBox, dialogSettings::setFileChooseDialogId);
        apply(component.myFileSaveDialogBox, dialogSettings::setFileSaveDialogId);
    }

    private void apply(ComboBox<FileOperateDialogProvider> comboBox, Consumer<String> func) {
        FileOperateDialogProvider value = comboBox.getValueOrError();

        String id = value.getId();
        if (FileOperateDialogProvider.APPLICATION_ID.equals(id)) {
            func.accept(null);
            return;
        }

        func.accept(id);
    }

    @RequiredUIAccess
    @Override
    protected void disposeUIResources(@Nonnull MyComponent component) {
        super.disposeUIResources(component);

        LocalizeManager localizeManager = LocalizeManager.get();

        if (!component.myApplied
            && component.myLocaleChanged
            && component.myInitialLocale != null
            && !Objects.equals(component.myInitialLocale.locale(), localizeManager.getLocale())) {
            localizeManager.setLocale(component.myInitialLocale.setValue());
        }
    }

    @RequiredUIAccess
    @Override
    protected void reset(@Nonnull MyComponent component) {
        LocalizeManager localizeManager = LocalizeManager.get();

        component.myInitialLocale = new LocaleInfo(localizeManager.getLocale(), localizeManager.isDefaultLocale());

        RecentProjectsManager recentProjectsManager = RecentProjectsManager.getInstance();
        GeneralSettings settings = GeneralSettings.getInstance();
        component.myChkSupportScreenReaders.setValue(settings.isSupportScreenReaders());
        component.myChkReopenLastProject.setValue(settings.isReopenLastProject());
        component.myChkSyncOnFrameActivation.setValue(settings.isSyncOnFrameActivation());
        component.myChkSaveOnFrameDeactivation.setValue(settings.isSaveOnFrameDeactivation());
        component.myChkAutoSaveIfInactive.setValue(settings.isAutoSaveIfInactive());
        component.myTfInactiveTimeout.setValue(Integer.toString(settings.getInactiveTimeout()));
        component.myTfInactiveTimeout.setEnabled(settings.isAutoSaveIfInactive());
        component.myChkUseSafeWrite.setValue(settings.isUseSafeWrite());
        component.myConfirmExit.setValue(settings.isConfirmExit());
        component.myRecentProjectsLimit.setValue(recentProjectsManager.getRecentProjectsLimit());

        switch (settings.getProcessCloseConfirmation()) {
            case TERMINATE:
                component.myTerminateProcessRadioButton.setValue(true);
                break;
            case DISCONNECT:
                component.myDisconnectRadioButton.setValue(true);
                break;
            case ASK:
                component.myAskRadioButton.setValue(true);
                break;
        }
        switch (settings.getConfirmOpenNewProject()) {
            case GeneralSettings.OPEN_PROJECT_ASK:
                component.myConfirmWindowToOpenProject.setValue(true);
                break;
            case GeneralSettings.OPEN_PROJECT_NEW_WINDOW:
                component.myOpenProjectInNewWindow.setValue(true);
                break;
            case GeneralSettings.OPEN_PROJECT_SAME_WINDOW:
                component.myOpenProjectInSameWindow.setValue(true);
                break;
        }

        FileOperateDialogSettings dialogSettings = myFileOperateDialogSettings.get();

        reset(component.myFileChooseDialogBox, dialogSettings::getFileChooseDialogId);
        reset(component.myFileSaveDialogBox, dialogSettings::getFileSaveDialogId);

        try {
            component.myEventBlocker.incrementAndGet();

            component.myLocaleBox.setValue(localizeManager.isDefaultLocale() ? Locale.ROOT : localizeManager.getLocale());
        }
        finally {
            component.myEventBlocker.decrementAndGet();
        }
    }

    @RequiredUIAccess
    private void reset(ComboBox<FileOperateDialogProvider> comboBox, Supplier<String> idGetter) {
        String id = ObjectUtil.notNull(idGetter.get(), FileOperateDialogProvider.APPLICATION_ID);

        comboBox.setValueByCondition(it -> it.getId().equals(id));
    }

    private static int getConfirmOpenNewProject(MyComponent component) {
        if (component.myConfirmWindowToOpenProject.getValue()) {
            return GeneralSettings.OPEN_PROJECT_ASK;
        }
        else if (component.myOpenProjectInNewWindow.getValue()) {
            return GeneralSettings.OPEN_PROJECT_NEW_WINDOW;
        }
        else {
            return GeneralSettings.OPEN_PROJECT_SAME_WINDOW;
        }
    }

    private static ProcessCloseConfirmation getProcessCloseConfirmation(MyComponent component) {
        if (component.myTerminateProcessRadioButton.getValue()) {
            return ProcessCloseConfirmation.TERMINATE;
        }
        else if (component.myDisconnectRadioButton.getValue()) {
            return ProcessCloseConfirmation.DISCONNECT;
        }
        else {
            return ProcessCloseConfirmation.ASK;
        }
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("System");
    }

    @Nullable
    @Override
    public String getParentId() {
        return StandardConfigurableIds.GENERAL_GROUP;
    }

    @Override
    @Nonnull
    public String getId() {
        return "preferences.general";
    }
}
