/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.AccessRule;
import consulo.application.progress.ProgressManager;
import consulo.application.util.DateFormatUtil;
import consulo.application.util.function.Processor;
import consulo.application.util.function.ThrowableComputable;
import consulo.application.util.registry.Registry;
import consulo.component.ProcessCanceledException;
import consulo.component.messagebus.MessageBusConnection;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.TextConsoleBuilderFactory;
import consulo.language.content.FileIndexFacade;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.ContentUtilEx;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.versionControlSystem.change.ContentRevisionCache;
import consulo.versionControlSystem.change.VcsAnnotationLocalChangesListener;
import consulo.versionControlSystem.checkout.CheckoutProvider;
import consulo.versionControlSystem.history.VcsHistoryCache;
import consulo.versionControlSystem.impl.internal.change.VcsAnnotationLocalChangesListenerImpl;
import consulo.versionControlSystem.impl.internal.checkout.CompositeCheckoutListener;
import consulo.versionControlSystem.impl.internal.update.UpdateInfoTreeImpl;
import consulo.versionControlSystem.internal.*;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.versionControlSystem.root.VcsRootSettings;
import consulo.versionControlSystem.ui.ViewUpdateInfoNotification;
import consulo.versionControlSystem.update.ActionInfo;
import consulo.versionControlSystem.update.UpdatedFiles;
import consulo.versionControlSystem.util.VcsRootIterator;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatusManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Element;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@State(name = "ProjectLevelVcsManager", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@Singleton
@ServiceImpl
public class ProjectLevelVcsManagerImpl extends ProjectLevelVcsManagerEx implements PersistentStateComponent<Element>, Disposable {

    private static final Logger LOG = Logger.getInstance(ProjectLevelVcsManagerImpl.class);
    private static final String SETTINGS_EDITED_MANUALLY = "settingsEditedManually";

    private final ProjectLevelVcsManagerSerialization mySerialization;
    private final OptionsAndConfirmations myOptionsAndConfirmations;

    private final NewMappings myMappings;
    private final Project myProject;
    private final MappingsToRoots myMappingsToRoots;

    private ContentManager myContentManager;
    private ConsoleView myConsole;
    private final Disposable myConsoleDisposer = new Disposable() {
        @Override
        public void dispose() {
            if (myConsole != null) {
                Disposer.dispose(myConsole);
                myConsole = null;
            }
        }
    };

    private static final String ELEMENT_MAPPING = "mapping";
    private static final String ATTRIBUTE_DIRECTORY = "directory";
    private static final String ATTRIBUTE_VCS = "vcs";
    private static final String ATTRIBUTE_DEFAULT_PROJECT = "defaultProject";
    private static final String ELEMENT_ROOT_SETTINGS = "rootSettings";
    private static final String ATTRIBUTE_CLASS = "class";

    private boolean myMappingsLoaded;
    private boolean myHaveLegacyVcsConfiguration;
    private final DefaultVcsRootPolicy myDefaultVcsRootPolicy;

    private volatile int myBackgroundOperationCounter;

    private final Set<ActionKey> myBackgroundRunningTasks = ConcurrentHashMap.newKeySet();

    private final List<VcsConsoleLine> myPendingOutput = new ArrayList<>();

    private final VcsHistoryCache myVcsHistoryCache;
    private final ContentRevisionCache myContentRevisionCache;
    private final FileIndexFacade myExcludedIndex;
    private final VcsAnnotationLocalChangesListenerImpl myAnnotationLocalChangesListener;

    @Inject
    public ProjectLevelVcsManagerImpl(Project project,
                                      FileStatusManager manager,
                                      FileIndexFacade excludedFileIndex,
                                      DefaultVcsRootPolicy defaultVcsRootPolicy) {
        myProject = project;
        mySerialization = new ProjectLevelVcsManagerSerialization();
        myOptionsAndConfirmations = new OptionsAndConfirmations();

        myDefaultVcsRootPolicy = defaultVcsRootPolicy;
        myMappings = new NewMappings(myProject, this, manager);
        myMappingsToRoots = new MappingsToRoots(myMappings, myProject);

        myVcsHistoryCache = new VcsHistoryCache();
        myContentRevisionCache = new ContentRevisionCache();
        VcsListener vcsListener = () -> {
            myVcsHistoryCache.clear();
            myContentRevisionCache.clearAll();
        };
        myExcludedIndex = excludedFileIndex;
        MessageBusConnection connection = myProject.getMessageBus().connect();
        connection.subscribe(VcsMappingListener.class, vcsListener);
        connection.subscribe(PluginVcsMappingListener.class, vcsListener);
        myAnnotationLocalChangesListener = new VcsAnnotationLocalChangesListenerImpl(myProject, this);

        Disposer.register(this, myMappings);
    }

    @Override
    public void afterLoadState() {
        myOptionsAndConfirmations.init(mySerialization::getInitOptionValue);
    }

    @Override
    @Nullable
    public AbstractVcs findVcsByName(String name) {
        if (name == null) {
            return null;
        }
        AbstractVcs result = myProject.isDisposed() ? null : AllVcses.getInstance(myProject).getByName(name);
        ProgressManager.checkCanceled();
        return result;
    }

    @Override
    @Nullable
    public VcsDescriptor getDescriptor(String name) {
        if (name == null) {
            return null;
        }
        if (myProject.isDisposed()) {
            return null;
        }
        return AllVcses.getInstance(myProject).getDescriptor(name);
    }

    @Override
    public void iterateVfUnderVcsRoot(VirtualFile file, Processor<VirtualFile> processor) {
        VcsRootIterator.iterateVfUnderVcsRoot(myProject, file, processor);
    }

    @Override
    public VcsDescriptor[] getAllVcss() {
        return AllVcses.getInstance(myProject).getAll();
    }

    public boolean haveVcses() {
        return !AllVcses.getInstance(myProject).isEmpty();
    }

    @Override
    public void dispose() {
        releaseConsole();
        myMappings.disposeMe();
        Disposer.dispose(myAnnotationLocalChangesListener);
        myContentManager = null;
    }

    @Nonnull
    @Override
    public VcsAnnotationLocalChangesListener getAnnotationLocalChangesListener() {
        return myAnnotationLocalChangesListener;
    }

    @Override
    public boolean checkAllFilesAreUnder(AbstractVcs abstractVcs, VirtualFile[] files) {
        if (files == null) {
            return false;
        }
        for (VirtualFile file : files) {
            if (getVcsFor(file) != abstractVcs) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Nullable
    public AbstractVcs getVcsFor(@Nonnull VirtualFile file) {
        String vcsName = myMappings.getVcsFor(file);
        if (vcsName == null || vcsName.isEmpty()) {
            return null;
        }
        return AllVcses.getInstance(myProject).getByName(vcsName);
    }

    @Override
    @Nullable
    public AbstractVcs getVcsFor(FilePath file) {
        VirtualFile vFile = ChangesUtil.findValidParentAccurately(file);
        ThrowableComputable<AbstractVcs, RuntimeException> action = () -> {
            if (!myProject.getApplication().isUnitTestMode() && !myProject.isInitialized()) {
                return null;
            }
            if (myProject.isDisposed()) {
                throw new ProcessCanceledException();
            }
            if (vFile != null) {
                return getVcsFor(vFile);
            }
            return null;
        };
        return AccessRule.read(action);
    }

    @Override
    @Nullable
    public VirtualFile getVcsRootFor(@Nullable VirtualFile file) {
        if (file == null) {
            return null;
        }
        VcsDirectoryMapping mapping = myMappings.getMappingFor(file);
        if (mapping == null) {
            return null;
        }
        String directory = mapping.getDirectory();
        if (directory.isEmpty()) {
            return myDefaultVcsRootPolicy.getVcsRootFor(file);
        }
        return LocalFileSystem.getInstance().findFileByPath(directory);
    }

    @Override
    @Nullable
    public VcsRoot getVcsRootObjectFor(VirtualFile file) {
        VcsDirectoryMapping mapping = myMappings.getMappingFor(file);
        if (mapping == null) {
            return null;
        }
        String directory = mapping.getDirectory();
        AbstractVcs vcs = findVcsByName(mapping.getVcs());
        if (directory.isEmpty()) {
            VirtualFile root = myDefaultVcsRootPolicy.getVcsRootFor(file);
            return  root != null ? new VcsRoot(vcs, root) : null;
        }
        VirtualFile path = LocalFileSystem.getInstance().findFileByPath(directory);
        return path != null ? new VcsRoot(vcs, path) : null;
    }

    @Override
    @Nullable
    public VirtualFile getVcsRootFor(FilePath file) {
        if (myProject.isDisposed()) {
            return null;
        }
        VirtualFile vFile = ChangesUtil.findValidParentAccurately(file);
        if (vFile != null) {
            return getVcsRootFor(vFile);
        }
        return null;
    }

    @Override
    public VcsRoot getVcsRootObjectFor(FilePath file) {
        if (myProject.isDisposed()) {
            return null;
        }
        VirtualFile vFile = ChangesUtil.findValidParentAccurately(file);
        if (vFile != null) {
            return getVcsRootObjectFor(vFile);
        }
        return null;
    }

    @Nullable
    @Override
    public ContentManager getContentManager() {
        if (myContentManager == null) {
            ToolWindow changes = ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.VCS);
            myContentManager = changes == null ? null : changes.getContentManager();
        }
        return myContentManager;
    }

    @Override
    public boolean checkVcsIsActive(AbstractVcs vcs) {
        return checkVcsIsActive(vcs.getId());
    }

    @Override
    public boolean checkVcsIsActive(@Nonnull String vcsId) {
        return myMappings.haveActiveVcs(vcsId);
    }

    @Nonnull
    @Override
    public Collection<AbstractVcs> getAllSupportedVcss() {
        return AllVcses.getInstance(myProject).getSupportedVcses();
    }

    @Override
    public AbstractVcs[] getAllActiveVcss() {
        return myMappings.getActiveVcses();
    }

    @Override
    @Nullable
    public AbstractVcs getSingleVCS() {
        AbstractVcs[] vcses = getAllActiveVcss();
        return vcses.length == 1 ? vcses[0] : null;
    }

    @Override
    public boolean hasActiveVcss() {
        return myMappings.hasActiveVcss();
    }

    @Override
    public boolean hasAnyMappings() {
        return !myMappings.isEmpty();
    }

    @Override
    public void addMessageToConsoleWindow(@Nullable VcsConsoleLine line) {
        if (line == null) {
            return;
        }

        myProject.getApplication().invokeLater(
            () -> {
                // for default and disposed projects the ContentManager is not available.
                if (myProject.isDisposed() || myProject.isDefault()) {
                    return;
                }
                ContentManager contentManager = getContentManager();
                if (contentManager == null) {
                    myPendingOutput.add(line);
                }
                else {
                    getOrCreateConsoleContent(contentManager);
                    line.print(myConsole);
                }
            },
            myProject.getApplication().getDefaultModalityState()
        );
    }

    private Content getOrCreateConsoleContent(ContentManager contentManager) {
        String displayName = VcsLocalize.vcsConsoleToolwindowDisplayName().get();
        Content content = contentManager.findContent(displayName);
        if (content == null) {
            releaseConsole();

            myConsole = TextConsoleBuilderFactory.getInstance().createBuilder(myProject).getConsole();

            JPanel panel = new JPanel(new BorderLayout());
            panel.add(myConsole.getComponent(), BorderLayout.CENTER);

            ActionToolbar toolbar = ActionManager.getInstance()
                .createActionToolbar("VcsManager", new DefaultActionGroup(myConsole.createConsoleActions()), false);
            panel.add(toolbar.getComponent(), BorderLayout.WEST);

            content = ContentFactory.getInstance().createContent(panel, displayName, true);
            content.setDisposer(myConsoleDisposer);
            contentManager.addContent(content);

            for (VcsConsoleLine pair : myPendingOutput) {
                pair.print(myConsole);
            }
            myPendingOutput.clear();
        }
        return content;
    }

    private void releaseConsole() {
        Disposer.dispose(myConsoleDisposer);
    }

    @Override
    @Nonnull
    public VcsShowSettingOption getOptions(VcsConfiguration.StandardOption option) {
        return myOptionsAndConfirmations.getOptions(option);
    }

    @Override
    public List<VcsShowOptionsSettingImpl> getAllOptions() {
        return myOptionsAndConfirmations.getAllOptions();
    }

    @Override
    @Nonnull
    public VcsShowSettingOption getStandardOption(@Nonnull VcsConfiguration.StandardOption option, @Nonnull AbstractVcs vcs) {
        VcsShowOptionsSettingImpl options = (VcsShowOptionsSettingImpl) getOptions(option);
        options.addApplicableVcs(vcs);
        return options;
    }

    @Override
    @Nonnull
    public VcsShowSettingOption getOrCreateCustomOption(@Nonnull String vcsActionName, @Nonnull AbstractVcs vcs) {
        return myOptionsAndConfirmations.getOrCreateCustomOption(vcsActionName, vcs);
    }

    @RequiredUIAccess
    @Override
    public void showProjectOperationInfo(UpdatedFiles updatedFiles, String displayActionName) {
        UpdateInfoTreeImpl tree = showUpdateProjectInfo(updatedFiles, displayActionName, ActionInfo.STATUS, false);
        if (tree != null) {
            ViewUpdateInfoNotification.focusUpdateInfoTree(myProject, tree);
        }
    }

    @Override
    @RequiredUIAccess
    @Nullable
    public UpdateInfoTreeImpl showUpdateProjectInfo(UpdatedFiles updatedFiles, String displayActionName, ActionInfo actionInfo, boolean canceled) {
        if (!myProject.isOpen() || myProject.isDisposed()) {
            return null;
        }
        ContentManager contentManager = getContentManager();
        if (contentManager == null) {
            return null;  // content manager is made null during dispose; flag is set later
        }
        UpdateInfoTreeImpl updateInfoTree = new UpdateInfoTreeImpl(contentManager, myProject, updatedFiles, displayActionName, actionInfo);
        ContentUtilEx.addTabbedContent(contentManager, updateInfoTree, "Update Info", DateFormatUtil.formatDateTime(System.currentTimeMillis()), false, updateInfoTree);
        updateInfoTree.expandRootChildren();
        return updateInfoTree;
    }

    public void cleanupMappings() {
        myMappings.cleanupMappings();
    }

    @Override
    public List<VcsDirectoryMapping> getDirectoryMappings() {
        return myMappings.getDirectoryMappings();
    }

    @Override
    public List<VcsDirectoryMapping> getDirectoryMappings(AbstractVcs vcs) {
        return myMappings.getDirectoryMappings(vcs.getName());
    }

    @Override
    @Nullable
    public VcsDirectoryMapping getDirectoryMappingFor(FilePath path) {
        VirtualFile vFile = ChangesUtil.findValidParentAccurately(path);
        if (vFile != null) {
            return myMappings.getMappingFor(vFile);
        }
        return null;
    }

    private boolean hasExplicitMapping(VirtualFile vFile) {
        VcsDirectoryMapping mapping = myMappings.getMappingFor(vFile);
        return mapping != null && !mapping.isDefaultMapping();
    }

    @Override
    public void setDirectoryMapping(String path, String activeVcsName) {
        if (myMappingsLoaded) {
            return;            // ignore per-module VCS settings if the mapping table was loaded from .ipr
        }
        myHaveLegacyVcsConfiguration = true;
        myMappings.setMapping(FileUtil.toSystemIndependentName(path), activeVcsName);
    }

    public void setAutoDirectoryMapping(String path, String activeVcsName) {
        List<VirtualFile> defaultRoots = myMappings.getDefaultRoots();
        if (defaultRoots.size() == 1 && StringUtil.isEmpty(myMappings.haveDefaultMapping())) {
            myMappings.removeDirectoryMapping(new VcsDirectoryMapping("", ""));
        }
        myMappings.setMapping(path, activeVcsName);
    }

    public void removeDirectoryMapping(VcsDirectoryMapping mapping) {
        myMappings.removeDirectoryMapping(mapping);
    }

    @Override
    public void setDirectoryMappings(List<VcsDirectoryMapping> items) {
        myHaveLegacyVcsConfiguration = true;
        myMappings.setDirectoryMappings(items);
    }

    @Override
    public void iterateVcsRoot(VirtualFile root, Processor<FilePath> iterator) {
        VcsRootIterator.iterateVcsRoot(myProject, root, iterator);
    }

    @Override
    public void iterateVcsRoot(VirtualFile root, Processor<FilePath> iterator, @Nullable VirtualFileFilter directoryFilter) {
        VcsRootIterator.iterateVcsRoot(myProject, root, iterator, directoryFilter);
    }

    @Nullable
    @Override
    public Element getState() {
        Element element = new Element("state");
        mySerialization.writeExternalUtil(element, myOptionsAndConfirmations);
        if (myHaveLegacyVcsConfiguration) {
            element.setAttribute(SETTINGS_EDITED_MANUALLY, "true");
        }
        return element;
    }

    @Override
    public void loadState(Element state) {
        mySerialization.readExternalUtil(state, myOptionsAndConfirmations);
        Attribute attribute = state.getAttribute(SETTINGS_EDITED_MANUALLY);
        if (attribute != null) {
            try {
                myHaveLegacyVcsConfiguration = attribute.getBooleanValue();
            }
            catch (DataConversionException ignored) {
            }
        }
    }

    @Override
    @Nonnull
    public VcsShowConfirmationOption getStandardConfirmation(@Nonnull VcsConfiguration.StandardConfirmation option, AbstractVcs vcs) {
        VcsShowConfirmationOptionImpl result = getConfirmation(option);
        if (vcs != null) {
            result.addApplicableVcs(vcs);
        }
        return result;
    }

    @Override
    public List<VcsShowConfirmationOptionImpl> getAllConfirmations() {
        return myOptionsAndConfirmations.getAllConfirmations();
    }

    @Override
    @Nonnull
    public VcsShowConfirmationOptionImpl getConfirmation(VcsConfiguration.StandardConfirmation option) {
        return myOptionsAndConfirmations.getConfirmation(option);
    }

    private final Map<VcsListener, MessageBusConnection> myAdapters = new HashMap<>();

    @Override
    public void addVcsListener(VcsListener listener) {
        MessageBusConnection connection = myProject.getMessageBus().connect();
        connection.subscribe(VcsMappingListener.class, listener);
        myAdapters.put(listener, connection);
    }

    @Override
    public void removeVcsListener(VcsListener listener) {
        MessageBusConnection connection = myAdapters.remove(listener);
        if (connection != null) {
            connection.disconnect();
        }
    }

    @Override
    public void startBackgroundVcsOperation() {
        myBackgroundOperationCounter++;
    }

    @Override
    public void stopBackgroundVcsOperation() {
        // in fact, the condition is "should not be called under ApplicationManager.invokeLater() and similar"
        assert !myProject.getApplication().isDispatchThread() || myProject.getApplication().isUnitTestMode();
        LOG.assertTrue(myBackgroundOperationCounter > 0, "myBackgroundOperationCounter > 0");
        myBackgroundOperationCounter--;
    }

    @Override
    public boolean isBackgroundVcsOperationRunning() {
        return myBackgroundOperationCounter > 0;
    }

    @Override
    public List<VirtualFile> getRootsUnderVcsWithoutFiltering(AbstractVcs vcs) {
        return myMappings.getMappingsAsFilesUnderVcs(vcs);
    }

    @Override
    @Nonnull
    public VirtualFile[] getRootsUnderVcs(@Nonnull AbstractVcs vcs) {
        return myMappingsToRoots.getRootsUnderVcs(vcs);
    }

    @Override
    public List<VirtualFile> getDetailedVcsMappings(AbstractVcs vcs) {
        return myMappingsToRoots.getDetailedVcsMappings(vcs);
    }

    @Override
    public VirtualFile[] getAllVersionedRoots() {
        List<VirtualFile> vFiles = new ArrayList<>();
        AbstractVcs[] vcses = myMappings.getActiveVcses();
        for (AbstractVcs vcs : vcses) {
            Collections.addAll(vFiles, getRootsUnderVcs(vcs));
        }
        return VirtualFileUtil.toVirtualFileArray(vFiles);
    }

    @Override
    @Nonnull
    public VcsRoot[] getAllVcsRoots() {
        List<VcsRoot> vcsRoots = new ArrayList<>();
        AbstractVcs[] vcses = myMappings.getActiveVcses();
        for (AbstractVcs vcs : vcses) {
            VirtualFile[] roots = getRootsUnderVcs(vcs);
            for (VirtualFile root : roots) {
                vcsRoots.add(new VcsRoot(vcs, root));
            }
        }
        return vcsRoots.toArray(new VcsRoot[vcsRoots.size()]);
    }

    @Nonnull
    @Override
    public LocalizeValue getConsolidatedVcsName() {
        AbstractVcs singleVcs = getSingleVCS();
        return singleVcs != null ? singleVcs.getShortNameWithMnemonic() : VcsLocalize.vcsGenericNameWithMnemonic();
    }

    @Override
    public void notifyDirectoryMappingChanged() {
        myProject.getMessageBus().syncPublisher(VcsMappingListener.class).directoryMappingChanged();
    }

    protected void activateActiveVcses() {
        myMappings.activateActiveVcses();
    }

    void readDirectoryMappings(Element element) {
        List<VcsDirectoryMapping> mappingsList = new ArrayList<>();
        boolean haveNonEmptyMappings = false;
        for (Element child : element.getChildren(ELEMENT_MAPPING)) {
            String vcs = child.getAttributeValue(ATTRIBUTE_VCS);
            if (vcs != null && !vcs.isEmpty()) {
                haveNonEmptyMappings = true;
            }
            VcsDirectoryMapping mapping = new VcsDirectoryMapping(child.getAttributeValue(ATTRIBUTE_DIRECTORY), vcs);
            mappingsList.add(mapping);

            Element rootSettingsElement = child.getChild(ELEMENT_ROOT_SETTINGS);
            if (rootSettingsElement != null) {
                String className = rootSettingsElement.getAttributeValue(ATTRIBUTE_CLASS);
                AbstractVcs vcsInstance = findVcsByName(mapping.getVcs());
                if (vcsInstance != null && className != null) {
                    VcsRootSettings rootSettings = vcsInstance.createEmptyVcsRootSettings();
                    if (rootSettings != null) {
                        try {
                            rootSettings.readExternal(rootSettingsElement);
                            mapping.setRootSettings(rootSettings);
                        }
                        catch (InvalidDataException e) {
                            LOG.error("Failed to load VCS root settings class " + className + " for VCS " + vcsInstance.getClass().getName(), e);
                        }
                    }
                }
            }
        }
        boolean defaultProject = Boolean.TRUE.toString().equals(element.getAttributeValue(ATTRIBUTE_DEFAULT_PROJECT));
        // run autodetection if there's no VCS in default project and
        if (haveNonEmptyMappings || !defaultProject) {
            myMappingsLoaded = true;
        }
        myMappings.setDirectoryMappings(mappingsList);
    }

    void writeDirectoryMappings(@Nonnull Element element) {
        if (myProject.isDefault()) {
            element.setAttribute(ATTRIBUTE_DEFAULT_PROJECT, Boolean.TRUE.toString());
        }
        for (VcsDirectoryMapping mapping : getDirectoryMappings()) {
            VcsRootSettings rootSettings = mapping.getRootSettings();
            if (rootSettings == null && StringUtil.isEmpty(mapping.getDirectory()) && StringUtil.isEmpty(mapping.getVcs())) {
                continue;
            }

            Element child = new Element(ELEMENT_MAPPING);
            child.setAttribute(ATTRIBUTE_DIRECTORY, mapping.getDirectory());
            child.setAttribute(ATTRIBUTE_VCS, mapping.getVcs());
            if (rootSettings != null) {
                Element rootSettingsElement = new Element(ELEMENT_ROOT_SETTINGS);
                rootSettingsElement.setAttribute(ATTRIBUTE_CLASS, rootSettings.getClass().getName());
                try {
                    rootSettings.writeExternal(rootSettingsElement);
                    child.addContent(rootSettingsElement);
                }
                catch (WriteExternalException e) {
                    // don't add element
                }
            }
            element.addContent(child);
        }
    }

    public boolean needAutodetectMappings() {
        return !myHaveLegacyVcsConfiguration && !myMappingsLoaded;
    }

    /**
     * Used to guess VCS for automatic mapping through a look into a working copy
     */
    @Override
    @Nullable
    public AbstractVcs findVersioningVcs(VirtualFile file) {
        VcsDescriptor[] vcsDescriptors = getAllVcss();
        VcsDescriptor probableVcs = null;
        for (VcsDescriptor vcsDescriptor : vcsDescriptors) {
            if (vcsDescriptor.probablyUnderVcs(file)) {
                if (probableVcs != null) {
                    return null;
                }
                probableVcs = vcsDescriptor;
            }
        }
        return probableVcs == null ? null : findVcsByName(probableVcs.getId());
    }

    @Override
    public CheckoutProvider.Listener getCompositeCheckoutListener() {
        return new CompositeCheckoutListener(myProject);
    }

    @Override
    public void fireDirectoryMappingsChanged() {
        if (myProject.isOpen() && !myProject.isDisposed()) {
            myMappings.mappingsChanged();
        }
    }

    @Override
    public String haveDefaultMapping() {
        return myMappings.haveDefaultMapping();
    }

    /**
     * @deprecated {@link BackgroundableActionLock}
     */
    @Deprecated
    public BackgroundableActionEnabledHandler getBackgroundableActionHandler(VcsBackgroundableActions action) {
        return new BackgroundableActionEnabledHandler(myProject, action);
    }

    @Override
    public boolean isBackgroundTaskRunning(@Nonnull Object... keys) {
        return myBackgroundRunningTasks.contains(new ActionKey(keys));
    }

    @Override
    public void startBackgroundTask(@Nonnull Object... keys) {
        LOG.assertTrue(myBackgroundRunningTasks.add(new ActionKey(keys)));
    }

    @Override
    public void stopBackgroundTask(@Nonnull Object... keys) {
        LOG.assertTrue(myBackgroundRunningTasks.remove(new ActionKey(keys)));
    }

    @Override
    public void addInitializationRequest(VcsInitObject vcsInitObject, Runnable runnable) {
        VcsInitialization.getInstance(myProject).add(vcsInitObject, runnable);
    }

    @Override
    public boolean isFileInContent(@Nullable VirtualFile vf) {
        ThrowableComputable<Boolean, RuntimeException> action = () -> vf != null && (
            myExcludedIndex.isInContent(vf) || isFileInBaseDir(vf) || vf.equals(myProject.getBaseDir()) || hasExplicitMapping(vf)
                || isInDirectoryBasedRoot(vf) || !Registry.is("ide.hide.excluded.files") && myExcludedIndex.isExcludedFile(vf)
        ) && !isIgnored(vf);
        return AccessRule.read(action);
    }

    @Override
    public boolean isIgnored(VirtualFile vf) {
        return Registry.is("ide.hide.excluded.files") ? myExcludedIndex.isExcludedFile(vf) : myExcludedIndex.isUnderIgnored(vf);
    }

    private boolean isInDirectoryBasedRoot(VirtualFile file) {
        if (file == null) {
            return false;
        }
        VirtualFile baseDir = myProject.getBaseDir();
        if (baseDir == null) {
            return false;
        }
        VirtualFile ideaDir = baseDir.findChild(Project.DIRECTORY_STORE_FOLDER);
        return ideaDir != null && ideaDir.isValid() && ideaDir.isDirectory() && VirtualFileUtil.isAncestor(ideaDir, file, false);
    }

    private boolean isFileInBaseDir(VirtualFile file) {
        VirtualFile parent = file.getParent();
        return !file.isDirectory() && parent != null && parent.equals(myProject.getBaseDir());
    }

    @Override
    public VcsHistoryCache getVcsHistoryCache() {
        return myVcsHistoryCache;
    }

    @Override
    public ContentRevisionCache getContentRevisionCache() {
        return myContentRevisionCache;
    }

    @TestOnly
    public void waitForInitialized() {
        VcsInitialization.getInstance(myProject).waitFinished();
    }

    private static class ActionKey {
        private final Object[] myObjects;

        ActionKey(@Nonnull Object... objects) {
            myObjects = objects;
        }

        @Override
        public final boolean equals(Object o) {
            return !(o == null || getClass() != o.getClass()) && Arrays.equals(myObjects, ((ActionKey) o).myObjects);
        }

        @Override
        public final int hashCode() {
            return Arrays.hashCode(myObjects);
        }

        @Override
        public String toString() {
            return getClass() + " - " + Arrays.toString(myObjects);
        }
    }
}
