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
package com.intellij.openapi.vcs.impl;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.changes.ChangesUtil;
import com.intellij.openapi.vcs.changes.VcsAnnotationLocalChangesListener;
import com.intellij.openapi.vcs.changes.VcsAnnotationLocalChangesListenerImpl;
import com.intellij.openapi.vcs.checkout.CompositeCheckoutListener;
import com.intellij.openapi.vcs.ex.ProjectLevelVcsManagerEx;
import com.intellij.openapi.vcs.history.VcsHistoryCache;
import com.intellij.openapi.vcs.impl.projectlevelman.*;
import com.intellij.openapi.vcs.update.ActionInfo;
import com.intellij.openapi.vcs.update.UpdateInfoTree;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import com.intellij.openapi.vcs.update.UpdatedFilesListener;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.ContentUtilEx;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.vcs.ViewUpdateInfoNotification;
import consulo.annotation.DeprecationInfo;
import consulo.application.AccessRule;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.vcs.api.localize.VcsApiLocalize;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

@State(name = "ProjectLevelVcsManager", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@Singleton
public class ProjectLevelVcsManagerImpl extends ProjectLevelVcsManagerEx implements PersistentStateComponent<Element>, Disposable {
  static final class ActivateVcsesStartupActivity implements VcsStartupActivity {
    @Override
    public void runActivity(@Nonnull Project project) {
      ProjectLevelVcsManagerImpl projectLevelVcsManager = (ProjectLevelVcsManagerImpl)getInstance(project);
      projectLevelVcsManager.activateActiveVcses();
    }

    @Override
    public int getOrder() {
      return VcsInitObject.MAPPINGS.getOrder();
    }
  }

  private static final Logger LOG = Logger.getInstance(ProjectLevelVcsManagerImpl.class);
  @NonNls
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

  @NonNls
  private static final String ELEMENT_MAPPING = "mapping";
  @NonNls
  private static final String ATTRIBUTE_DIRECTORY = "directory";
  @NonNls
  private static final String ATTRIBUTE_VCS = "vcs";
  @NonNls
  private static final String ATTRIBUTE_DEFAULT_PROJECT = "defaultProject";
  @NonNls
  private static final String ELEMENT_ROOT_SETTINGS = "rootSettings";
  @NonNls
  private static final String ATTRIBUTE_CLASS = "class";

  private boolean myMappingsLoaded;
  private boolean myHaveLegacyVcsConfiguration;
  private final DefaultVcsRootPolicy myDefaultVcsRootPolicy;

  private volatile int myBackgroundOperationCounter;

  private final Set<ActionKey> myBackgroundRunningTasks = ContainerUtil.newHashSet();

  private final List<VcsConsoleLine> myPendingOutput = new ArrayList<>();

  private final VcsHistoryCache myVcsHistoryCache;
  private final ContentRevisionCache myContentRevisionCache;
  private final FileIndexFacade myExcludedIndex;
  private final VcsFileListenerContextHelper myVcsFileListenerContextHelper;
  private final VcsAnnotationLocalChangesListenerImpl myAnnotationLocalChangesListener;

  @Inject
  public ProjectLevelVcsManagerImpl(Project project,
                                    final FileStatusManager manager,
                                    final FileIndexFacade excludedFileIndex,
                                    DefaultVcsRootPolicy defaultVcsRootPolicy,
                                    VcsFileListenerContextHelper vcsFileListenerContextHelper) {
    myProject = project;
    mySerialization = new ProjectLevelVcsManagerSerialization();
    myOptionsAndConfirmations = new OptionsAndConfirmations();

    myDefaultVcsRootPolicy = defaultVcsRootPolicy;
    myMappings = new NewMappings(myProject, this, manager);
    myMappingsToRoots = new MappingsToRoots(myMappings, myProject);

    myVcsHistoryCache = new VcsHistoryCache();
    myContentRevisionCache = new ContentRevisionCache();
    myVcsFileListenerContextHelper = vcsFileListenerContextHelper;
    VcsListener vcsListener = () -> {
      myVcsHistoryCache.clear();
      myVcsFileListenerContextHelper.possiblySwitchActivation(hasActiveVcss());
    };
    myExcludedIndex = excludedFileIndex;
    MessageBusConnection connection = myProject.getMessageBus().connect();
    connection.subscribe(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED, vcsListener);
    connection.subscribe(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED_IN_PLUGIN, vcsListener);
    connection.subscribe(UpdatedFilesListener.UPDATED_FILES, myContentRevisionCache::clearCurrent);
    myAnnotationLocalChangesListener = new VcsAnnotationLocalChangesListenerImpl(myProject, this);

    Disposer.register(this, myMappings);
  }

  @Override
  public void afterLoadState() {
    myOptionsAndConfirmations.init(mySerialization::getInitOptionValue);
  }

  @Deprecated
  @DeprecationInfo("Use own AllVcsesI impl, or use extension extender")
  public void registerVcs(AbstractVcs vcs) {
    // FIXME [VISTALL] nothing since this impl used only for tests
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @DeprecationInfo("Use own AllVcsesI impl, or use extension extender")
  public void unregisterVcs(@Nonnull AbstractVcs vcs) {
    // FIXME [VISTALL] nothing since this impl used only for tests
    throw new UnsupportedOperationException();
  }

  @Override
  @Nullable
  public AbstractVcs findVcsByName(String name) {
    if (name == null) return null;
    AbstractVcs result = myProject.isDisposed() ? null : AllVcses.getInstance(myProject).getByName(name);
    ProgressManager.checkCanceled();
    return result;
  }

  @Override
  @Nullable
  public VcsDescriptor getDescriptor(final String name) {
    if (name == null) return null;
    if (myProject.isDisposed()) return null;
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
    if (files == null) return false;
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
    final String vcsName = myMappings.getVcsFor(file);
    if (vcsName == null || vcsName.isEmpty()) {
      return null;
    }
    return AllVcses.getInstance(myProject).getByName(vcsName);
  }

  @Override
  @Nullable
  public AbstractVcs getVcsFor(final FilePath file) {
    final VirtualFile vFile = ChangesUtil.findValidParentAccurately(file);
    ThrowableComputable<AbstractVcs, RuntimeException> action = () -> {
      if (!ApplicationManager.getApplication().isUnitTestMode() && !myProject.isInitialized()) return null;
      if (myProject.isDisposed()) throw new ProcessCanceledException();
      if (vFile != null) {
        return getVcsFor(vFile);
      }
      return null;
    };
    return AccessRule.read(action);
  }

  @Override
  @Nullable
  public VirtualFile getVcsRootFor(@Nullable final VirtualFile file) {
    if (file == null) return null;
    final VcsDirectoryMapping mapping = myMappings.getMappingFor(file);
    if (mapping == null) {
      return null;
    }
    final String directory = mapping.getDirectory();
    if (directory.isEmpty()) {
      return myDefaultVcsRootPolicy.getVcsRootFor(file);
    }
    return LocalFileSystem.getInstance().findFileByPath(directory);
  }

  @Override
  @Nullable
  public VcsRoot getVcsRootObjectFor(final VirtualFile file) {
    final VcsDirectoryMapping mapping = myMappings.getMappingFor(file);
    if (mapping == null) {
      return null;
    }
    final String directory = mapping.getDirectory();
    final AbstractVcs vcs = findVcsByName(mapping.getVcs());
    if (directory.isEmpty()) {
      return new VcsRoot(vcs, myDefaultVcsRootPolicy.getVcsRootFor(file));
    }
    return new VcsRoot(vcs, LocalFileSystem.getInstance().findFileByPath(directory));
  }

  @Override
  @Nullable
  public VirtualFile getVcsRootFor(final FilePath file) {
    if (myProject.isDisposed()) return null;
    VirtualFile vFile = ChangesUtil.findValidParentAccurately(file);
    if (vFile != null) {
      return getVcsRootFor(vFile);
    }
    return null;
  }

  @Override
  public VcsRoot getVcsRootObjectFor(final FilePath file) {
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
    return checkVcsIsActive(vcs.getName());
  }

  @Override
  public boolean checkVcsIsActive(final String vcsName) {
    return myMappings.haveActiveVcs(vcsName);
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
    if (line == null) return;

    ApplicationManager.getApplication().invokeLater(() -> {
      // for default and disposed projects the ContentManager is not available.
      if (myProject.isDisposed() || myProject.isDefault()) return;
      final ContentManager contentManager = getContentManager();
      if (contentManager == null) {
        myPendingOutput.add(line);
      }
      else {
        getOrCreateConsoleContent(contentManager);
        line.print(myConsole);
      }
    }, ModalityState.defaultModalityState());
  }

  private Content getOrCreateConsoleContent(final ContentManager contentManager) {
    final String displayName = VcsBundle.message("vcs.console.toolwindow.display.name");
    Content content = contentManager.findContent(displayName);
    if (content == null) {
      releaseConsole();

      myConsole = TextConsoleBuilderFactory.getInstance().createBuilder(myProject).getConsole();

      JPanel panel = new JPanel(new BorderLayout());
      panel.add(myConsole.getComponent(), BorderLayout.CENTER);

      ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("VcsManager", new DefaultActionGroup(myConsole.createConsoleActions()), false);
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
    final VcsShowOptionsSettingImpl options = (VcsShowOptionsSettingImpl)getOptions(option);
    options.addApplicableVcs(vcs);
    return options;
  }

  @Override
  @Nonnull
  public VcsShowSettingOption getOrCreateCustomOption(@Nonnull String vcsActionName, @Nonnull AbstractVcs vcs) {
    return myOptionsAndConfirmations.getOrCreateCustomOption(vcsActionName, vcs);
  }

  @CalledInAwt
  @Override
  public void showProjectOperationInfo(final UpdatedFiles updatedFiles, String displayActionName) {
    UpdateInfoTree tree = showUpdateProjectInfo(updatedFiles, displayActionName, ActionInfo.STATUS, false);
    if (tree != null) ViewUpdateInfoNotification.focusUpdateInfoTree(myProject, tree);
  }

  @CalledInAwt
  @Nullable
  @Override
  public UpdateInfoTree showUpdateProjectInfo(UpdatedFiles updatedFiles, String displayActionName, ActionInfo actionInfo, boolean canceled) {
    if (!myProject.isOpen() || myProject.isDisposed()) return null;
    ContentManager contentManager = getContentManager();
    if (contentManager == null) {
      return null;  // content manager is made null during dispose; flag is set later
    }
    final UpdateInfoTree updateInfoTree = new UpdateInfoTree(contentManager, myProject, updatedFiles, displayActionName, actionInfo);
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
  public List<VcsDirectoryMapping> getDirectoryMappings(final AbstractVcs vcs) {
    return myMappings.getDirectoryMappings(vcs.getName());
  }

  @Override
  @Nullable
  public VcsDirectoryMapping getDirectoryMappingFor(final FilePath path) {
    VirtualFile vFile = ChangesUtil.findValidParentAccurately(path);
    if (vFile != null) {
      return myMappings.getMappingFor(vFile);
    }
    return null;
  }

  private boolean hasExplicitMapping(final VirtualFile vFile) {
    final VcsDirectoryMapping mapping = myMappings.getMappingFor(vFile);
    return mapping != null && !mapping.isDefaultMapping();
  }

  @Override
  public void setDirectoryMapping(final String path, final String activeVcsName) {
    if (myMappingsLoaded) return;            // ignore per-module VCS settings if the mapping table was loaded from .ipr
    myHaveLegacyVcsConfiguration = true;
    myMappings.setMapping(FileUtil.toSystemIndependentName(path), activeVcsName);
  }

  public void setAutoDirectoryMapping(String path, String activeVcsName) {
    final List<VirtualFile> defaultRoots = myMappings.getDefaultRoots();
    if (defaultRoots.size() == 1 && StringUtil.isEmpty(myMappings.haveDefaultMapping())) {
      myMappings.removeDirectoryMapping(new VcsDirectoryMapping("", ""));
    }
    myMappings.setMapping(path, activeVcsName);
  }

  public void removeDirectoryMapping(VcsDirectoryMapping mapping) {
    myMappings.removeDirectoryMapping(mapping);
  }

  @Override
  public void setDirectoryMappings(final List<VcsDirectoryMapping> items) {
    myHaveLegacyVcsConfiguration = true;
    myMappings.setDirectoryMappings(items);
  }

  @Override
  public void iterateVcsRoot(final VirtualFile root, final Processor<FilePath> iterator) {
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
    final Attribute attribute = state.getAttribute(SETTINGS_EDITED_MANUALLY);
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
    final VcsShowConfirmationOptionImpl result = getConfirmation(option);
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
    connection.subscribe(VCS_CONFIGURATION_CHANGED, listener);
    myAdapters.put(listener, connection);
  }

  @Override
  public void removeVcsListener(VcsListener listener) {
    final MessageBusConnection connection = myAdapters.remove(listener);
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
    assert !ApplicationManager.getApplication().isDispatchThread() || ApplicationManager.getApplication().isUnitTestMode();
    LOG.assertTrue(myBackgroundOperationCounter > 0, "myBackgroundOperationCounter > 0");
    myBackgroundOperationCounter--;
  }

  @Override
  public boolean isBackgroundVcsOperationRunning() {
    return myBackgroundOperationCounter > 0;
  }

  @Override
  public List<VirtualFile> getRootsUnderVcsWithoutFiltering(final AbstractVcs vcs) {
    return myMappings.getMappingsAsFilesUnderVcs(vcs);
  }

  @Override
  @Nonnull
  public VirtualFile[] getRootsUnderVcs(@Nonnull AbstractVcs vcs) {
    return myMappingsToRoots.getRootsUnderVcs(vcs);
  }

  @Override
  public List<VirtualFile> getDetailedVcsMappings(final AbstractVcs vcs) {
    return myMappingsToRoots.getDetailedVcsMappings(vcs);
  }

  @Override
  public VirtualFile[] getAllVersionedRoots() {
    List<VirtualFile> vFiles = new ArrayList<>();
    final AbstractVcs[] vcses = myMappings.getActiveVcses();
    for (AbstractVcs vcs : vcses) {
      Collections.addAll(vFiles, getRootsUnderVcs(vcs));
    }
    return VfsUtilCore.toVirtualFileArray(vFiles);
  }

  @Override
  @Nonnull
  public VcsRoot[] getAllVcsRoots() {
    List<VcsRoot> vcsRoots = new ArrayList<>();
    final AbstractVcs[] vcses = myMappings.getActiveVcses();
    for (AbstractVcs vcs : vcses) {
      final VirtualFile[] roots = getRootsUnderVcs(vcs);
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
    return singleVcs != null ? singleVcs.getShortNameWithMnemonic() : VcsApiLocalize.vcsGenericNameWithMnemonic();
  }

  @Override
  public void notifyDirectoryMappingChanged() {
    myProject.getMessageBus().syncPublisher(VCS_CONFIGURATION_CHANGED).directoryMappingChanged();
  }

  private void activateActiveVcses() {
    myMappings.activateActiveVcses();
  }

  void readDirectoryMappings(final Element element) {
    final List<VcsDirectoryMapping> mappingsList = new ArrayList<>();
    boolean haveNonEmptyMappings = false;
    for (Element child : element.getChildren(ELEMENT_MAPPING)) {
      final String vcs = child.getAttributeValue(ATTRIBUTE_VCS);
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
          final VcsRootSettings rootSettings = vcsInstance.createEmptyVcsRootSettings();
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
    final VcsDescriptor[] vcsDescriptors = getAllVcss();
    VcsDescriptor probableVcs = null;
    for (VcsDescriptor vcsDescriptor : vcsDescriptors) {
      if (vcsDescriptor.probablyUnderVcs(file)) {
        if (probableVcs != null) {
          return null;
        }
        probableVcs = vcsDescriptor;
      }
    }
    return probableVcs == null ? null : findVcsByName(probableVcs.getName());
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
  public BackgroundableActionEnabledHandler getBackgroundableActionHandler(final VcsBackgroundableActions action) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    return new BackgroundableActionEnabledHandler(myProject, action);
  }

  @CalledInAwt
  boolean isBackgroundTaskRunning(@Nonnull Object... keys) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    return myBackgroundRunningTasks.contains(new ActionKey(keys));
  }

  @CalledInAwt
  void startBackgroundTask(@Nonnull Object... keys) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    LOG.assertTrue(myBackgroundRunningTasks.add(new ActionKey(keys)));
  }

  @CalledInAwt
  void stopBackgroundTask(@Nonnull Object... keys) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    LOG.assertTrue(myBackgroundRunningTasks.remove(new ActionKey(keys)));
  }

  public void addInitializationRequest(final VcsInitObject vcsInitObject, final Runnable runnable) {
    VcsInitialization.getInstance(myProject).add(vcsInitObject, runnable);
  }

  @Override
  public boolean isFileInContent(@Nullable final VirtualFile vf) {
    ThrowableComputable<Boolean, RuntimeException> action = () -> vf != null &&
                                                                  (myExcludedIndex.isInContent(vf) ||
                                                                   isFileInBaseDir(vf) ||
                                                                   vf.equals(myProject.getBaseDir()) ||
                                                                   hasExplicitMapping(vf) ||
                                                                   isInDirectoryBasedRoot(vf) ||
                                                                   !Registry.is("ide.hide.excluded.files") && myExcludedIndex.isExcludedFile(vf)) &&
                                                                  !isIgnored(vf);
    return AccessRule.read(action);
  }

  @Override
  public boolean isIgnored(VirtualFile vf) {
    if (Registry.is("ide.hide.excluded.files")) {
      return myExcludedIndex.isExcludedFile(vf);
    }
    else {
      return myExcludedIndex.isUnderIgnored(vf);
    }
  }

  private boolean isInDirectoryBasedRoot(final VirtualFile file) {
    if (file == null) return false;
    final VirtualFile baseDir = myProject.getBaseDir();
    if (baseDir == null) return false;
    final VirtualFile ideaDir = baseDir.findChild(Project.DIRECTORY_STORE_FOLDER);
    return ideaDir != null && ideaDir.isValid() && ideaDir.isDirectory() && VfsUtilCore.isAncestor(ideaDir, file, false);
  }

  private boolean isFileInBaseDir(final VirtualFile file) {
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
      if (o == null || getClass() != o.getClass()) return false;
      return Arrays.equals(myObjects, ((ActionKey)o).myObjects);
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
