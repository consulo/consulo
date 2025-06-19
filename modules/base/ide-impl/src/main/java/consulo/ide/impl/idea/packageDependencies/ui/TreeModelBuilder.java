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
package consulo.ide.impl.idea.packageDependencies.ui;

import consulo.application.AllIcons;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.content.ContentIterator;
import consulo.ide.impl.idea.openapi.roots.libraries.LibraryUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.language.editor.scope.AnalysisScopeBundle;
import consulo.language.editor.scope.localize.AnalysisScopeLocalize;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiPackage;
import consulo.language.psi.PsiPackageManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.project.ui.view.tree.ModuleGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TreeModelBuilder {
    private final ProjectFileIndex myFileIndex;
    private final Project myProject;
    private static final Logger LOG = Logger.getInstance(TreeModelBuilder.class);
    private final boolean myShowModuleGroups;
    protected final PsiPackageManager myPackageManager;
    private static final Key<Integer> FILE_COUNT = Key.create("packages.FILE_COUNT");

    private static enum ScopeType {
        TEST,
        SOURCE,
        LIB
    }

    private final boolean myShowModules;
    private final boolean myGroupByScopeType;
    private final boolean myFlattenPackages;
    private boolean myShowFiles;
    private final boolean myShowIndividualLibs;
    private final Marker myMarker;
    private final boolean myAddUnmarkedFiles;
    private final PackageDependenciesNode myRoot;
    private final Map<ScopeType, Map<Pair<Module, PsiPackage>, PackageNode>> myModulePackageNodes = new HashMap<>();
    private final Map<ScopeType, Map<Pair<OrderEntry, PsiPackage>, PackageNode>> myLibraryPackageNodes = new HashMap<>();
    private final Map<ScopeType, Map<Module, ModuleNode>> myModuleNodes = new HashMap<>();
    private final Map<ScopeType, Map<String, ModuleGroupNode>> myModuleGroupNodes = new HashMap<>();
    private final Map<ScopeType, Map<OrderEntry, LibraryNode>> myLibraryNodes = new HashMap<>();
    private int myScannedFileCount = 0;
    private int myTotalFileCount = 0;
    private int myMarkedFileCount = 0;
    private GeneralGroupNode myAllLibsNode = null;

    private GeneralGroupNode mySourceRoot = null;
    private GeneralGroupNode myTestRoot = null;
    private GeneralGroupNode myLibsRoot = null;

    public static final String PRODUCTION_NAME = AnalysisScopeBundle.message("package.dependencies.production.node.text");
    public static final String TEST_NAME = AnalysisScopeBundle.message("package.dependencies.test.node.text");
    public static final String LIBRARY_NAME = AnalysisScopeBundle.message("package.dependencies.library.node.text");

    @RequiredUIAccess
    public TreeModelBuilder(Project project, boolean showIndividualLibs, Marker marker, DependenciesPanel.DependencyPanelSettings settings) {
        myProject = project;
        final boolean multiModuleProject = ModuleManager.getInstance(project).getModules().length > 1;
        myShowModules = settings.UI_SHOW_MODULES && multiModuleProject;
        myGroupByScopeType = settings.UI_GROUP_BY_SCOPE_TYPE;
        myFlattenPackages = settings.UI_FLATTEN_PACKAGES;
        myShowFiles = settings.UI_SHOW_FILES;
        myShowIndividualLibs = showIndividualLibs;
        myShowModuleGroups = settings.UI_SHOW_MODULE_GROUPS && multiModuleProject;
        myMarker = marker;
        myAddUnmarkedFiles = !settings.UI_FILTER_LEGALS;
        myRoot = new RootNode(project);
        myFileIndex = ProjectRootManager.getInstance(project).getFileIndex();

        createMaps(ScopeType.LIB);
        createMaps(ScopeType.SOURCE);
        createMaps(ScopeType.TEST);

        if (myGroupByScopeType) {
            mySourceRoot = new GeneralGroupNode(PRODUCTION_NAME, AllIcons.Nodes.Package, project);
            myTestRoot = new GeneralGroupNode(TEST_NAME, AllIcons.Nodes.TestPackage, project);
            myLibsRoot = new GeneralGroupNode(LIBRARY_NAME, AllIcons.Nodes.PpLibFolder, project);
            myRoot.add(mySourceRoot);
            myRoot.add(myTestRoot);
            myRoot.add(myLibsRoot);
        }
        myPackageManager = PsiPackageManager.getInstance(myProject);
    }

    private void createMaps(ScopeType scopeType) {
        myModulePackageNodes.put(scopeType, new HashMap<>());
        myLibraryPackageNodes.put(scopeType, new HashMap<>());
        myModuleGroupNodes.put(scopeType, new HashMap<>());
        myModuleNodes.put(scopeType, new HashMap<>());
        myLibraryNodes.put(scopeType, new HashMap<>());
    }

    @RequiredUIAccess
    public static synchronized TreeModel createTreeModel(
        Project project,
        boolean showProgress,
        Set<PsiFile> files,
        Marker marker,
        DependenciesPanel.DependencyPanelSettings settings
    ) {
        return new TreeModelBuilder(project, true, marker, settings).build(files, showProgress);
    }

    @RequiredUIAccess
    public static synchronized TreeModel createTreeModel(Project project, Marker marker, DependenciesPanel.DependencyPanelSettings settings) {
        return new TreeModelBuilder(project, true, marker, settings).build(project);
    }

    @RequiredUIAccess
    public static synchronized TreeModel createTreeModel(
        Project project,
        boolean showIndividualLibs,
        Marker marker
    ) {
        return new TreeModelBuilder(project, showIndividualLibs, marker, new DependenciesPanel.DependencyPanelSettings()).build(project);
    }

    private void countFiles(Project project) {
        final Integer fileCount = project.getUserData(FILE_COUNT);
        if (fileCount == null) {
            myFileIndex.iterateContent(fileOrDir -> {
                if (!fileOrDir.isDirectory()) {
                    counting();
                }
                return true;
            });

            for (VirtualFile root : LibraryUtil.getLibraryRoots(project)) {
                countFilesRecursively(root);
            }

            project.putUserData(FILE_COUNT, myTotalFileCount);
        }
        else {
            myTotalFileCount = fileCount;
        }
    }

    public static void clearCaches(Project project) {
        project.putUserData(FILE_COUNT, null);
    }

    public TreeModel build(final Project project) {
        Runnable buildingRunnable = () -> {
            countFiles(project);
            myFileIndex.iterateContent(new ContentIterator() {
                PackageDependenciesNode lastParent = null;
                VirtualFile dir = null;

                public boolean processFile(VirtualFile fileOrDir) {
                    if (!fileOrDir.isDirectory()) {
                        if (lastParent != null && !Comparing.equal(dir, fileOrDir.getParent())) {
                            lastParent = null;
                        }
                        lastParent = buildFileNode(fileOrDir, lastParent);
                        dir = fileOrDir.getParent();
                    }
                    else {
                        lastParent = null;
                    }
                    return true;
                }
            });

            for (VirtualFile root : LibraryUtil.getLibraryRoots(project)) {
                processFilesRecursively(root);
            }
        };

        buildingRunnable.run();

        return new TreeModel(myRoot, myTotalFileCount, myMarkedFileCount);
    }

    private void processFilesRecursively(@Nonnull VirtualFile file) {
        VfsUtilCore.visitChildrenRecursively(file, new VirtualFileVisitor() {
            private PackageDependenciesNode parent = null;

            @Override
            public boolean visitFile(@Nonnull VirtualFile file) {
                if (file.isDirectory()) {
                    parent = null;
                }
                else {
                    parent = buildFileNode(file, parent);
                }
                return true;
            }

            @Override
            public void afterChildrenVisited(@Nonnull VirtualFile file) {
                if (file.isDirectory()) {
                    parent = null;
                }
            }
        });
    }

    private void countFilesRecursively(VirtualFile file) {
        VfsUtilCore.visitChildrenRecursively(file, new VirtualFileVisitor() {
            @Override
            public boolean visitFile(@Nonnull VirtualFile file) {
                if (!file.isDirectory()) {
                    counting();
                }
                return true;
            }
        });
    }

    private void counting() {
        myTotalFileCount++;
        final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
        if (indicator != null) {
            ((PanelProgressIndicator) indicator).update(
                AnalysisScopeLocalize.packageDependenciesBuildProgressText().get(),
                true,
                0
            );
        }
    }

    private TreeModel build(final Set<PsiFile> files, boolean showProgress) {
        if (files.size() == 1) {
            myShowFiles = true;
        }

        Runnable buildingRunnable = () -> {
            for (final PsiFile file : files) {
                if (file != null) {
                    buildFileNode(file.getVirtualFile(), null);
                }
            }
        };

        if (showProgress) {
            final LocalizeValue title = AnalysisScopeLocalize.packageDependenciesBuildProcessTitle();
            ProgressManager.getInstance().runProcessWithProgressSynchronously(buildingRunnable, title, false, myProject);
        }
        else {
            buildingRunnable.run();
        }

        TreeUtil.sort(myRoot, new DependencyNodeComparator());
        return new TreeModel(myRoot, myTotalFileCount, myMarkedFileCount);
    }

    @Nullable
    private PackageDependenciesNode buildFileNode(final VirtualFile file, @Nullable PackageDependenciesNode parent) {
        final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
        if (indicator != null) {
            ((PanelProgressIndicator) indicator).update(
                AnalysisScopeLocalize.packageDependenciesBuildProgressText().get(),
                false,
                ((double) myScannedFileCount++) / myTotalFileCount
            );
        }

        boolean isMarked = myMarker != null && myMarker.isMarked(file);
        if (isMarked) myMarkedFileCount++;
        if (isMarked || myAddUnmarkedFiles) {
            PackageDependenciesNode dirNode = parent != null ? parent : getFileParentNode(file);
            if (dirNode == null) return null;

            if (myShowFiles) {
                FileNode fileNode = new FileNode(file, myProject, isMarked);
                dirNode.add(fileNode);
            }
            else {
                dirNode.addFile(file, isMarked);
            }
            return dirNode;
        }
        return null;
    }

    @Nullable
    @RequiredUIAccess
    public PackageDependenciesNode getFileParentNode(VirtualFile vFile) {
        LOG.assertTrue(vFile != null);
        final VirtualFile containingDirectory = vFile.getParent();
        LOG.assertTrue(containingDirectory != null);
        PsiPackage aPackage = null;
        final String packageName = myFileIndex.getPackageNameByDirectory(containingDirectory);
        if (packageName != null) {
            aPackage = myPackageManager.findAnyPackage(packageName);
        }
        if (aPackage != null) {
            if (myFileIndex.isInLibrarySource(vFile) || myFileIndex.isInLibraryClasses(vFile)) {
                return getLibraryDirNode(aPackage, getLibraryForFile(vFile));
            }
            else {
                return getModuleDirNode(aPackage, myFileIndex.getModuleForFile(vFile), getFileScopeType(vFile));
            }
        }
        return myFileIndex.isInLibrarySource(vFile) ? null : getModuleNode(myFileIndex.getModuleForFile(vFile), getFileScopeType(vFile));

    }

    private ScopeType getFileScopeType(VirtualFile file) {
        if (myFileIndex.isLibraryClassFile(file) || myFileIndex.isInLibrarySource(file)) return ScopeType.LIB;
        if (myFileIndex.isInTestSourceContent(file)) return ScopeType.TEST;
        return ScopeType.SOURCE;
    }

    @Nullable
    private OrderEntry getLibraryForFile(VirtualFile virtualFile) {
        if (virtualFile == null) return null;
        List<OrderEntry> orders = myFileIndex.getOrderEntriesForFile(virtualFile);
        for (OrderEntry order : orders) {
            if (order instanceof LibraryOrderEntry || order instanceof ModuleExtensionWithSdkOrderEntry) return order;
        }
        return null;
    }

    private <T> T getMap(Map<ScopeType, T> map, ScopeType scopeType) {
        return map.get(myGroupByScopeType ? scopeType : ScopeType.SOURCE);
    }

    private PackageDependenciesNode getLibraryDirNode(PsiPackage aPackage, OrderEntry libraryOrJdk) {
        if (aPackage == null || aPackage.getName() == null) {
            return getLibraryOrJDKNode(libraryOrJdk);
        }

        if (!myShowModules && !myGroupByScopeType) {
            return getModuleDirNode(aPackage, null, ScopeType.LIB);
        }

        Pair<OrderEntry, PsiPackage> descriptor = Pair.create(myShowIndividualLibs ? libraryOrJdk : null, aPackage);
        PackageNode node = getMap(myLibraryPackageNodes, ScopeType.LIB).get(descriptor);
        if (node != null) return node;

        node = new PackageNode(aPackage, myFlattenPackages);
        getMap(myLibraryPackageNodes, ScopeType.LIB).put(descriptor, node);

        if (myFlattenPackages) {
            getLibraryOrJDKNode(libraryOrJdk).add(node);
        }
        else {
            getLibraryDirNode(aPackage.getParentPackage(), libraryOrJdk).add(node);
        }

        return node;
    }

    @RequiredUIAccess
    private PackageDependenciesNode getModuleDirNode(PsiPackage aPackage, Module module, ScopeType scopeType) {
        if (aPackage == null) {
            return getModuleNode(module, scopeType);
        }

        Pair<Module, PsiPackage> descriptor = Pair.create(myShowModules ? module : null, aPackage);
        PackageNode node = getMap(myModulePackageNodes, scopeType).get(descriptor);

        if (node != null) return node;

        node = new PackageNode(aPackage, myFlattenPackages);
        getMap(myModulePackageNodes, scopeType).put(descriptor, node);

        if (myFlattenPackages) {
            getModuleNode(module, scopeType).add(node);
        }
        else {
            getModuleDirNode(aPackage.getParentPackage(), module, scopeType).add(node);
        }

        return node;
    }

    @Nullable
    @RequiredUIAccess
    private PackageDependenciesNode getModuleNode(Module module, ScopeType scopeType) {
        if (module == null || !myShowModules) {
            return getRootNode(scopeType);
        }
        ModuleNode node = getMap(myModuleNodes, scopeType).get(module);
        if (node != null) return node;
        node = new ModuleNode(module);
        final ModuleManager moduleManager = ModuleManager.getInstance(myProject);
        final String[] groupPath = moduleManager.getModuleGroupPath(module);
        if (groupPath == null) {
            getMap(myModuleNodes, scopeType).put(module, node);
            getRootNode(scopeType).add(node);
            return node;
        }
        getMap(myModuleNodes, scopeType).put(module, node);
        if (myShowModuleGroups) {
            getParentModuleGroup(groupPath, scopeType).add(node);
        }
        else {
            getRootNode(scopeType).add(node);
        }
        return node;
    }

    private PackageDependenciesNode getParentModuleGroup(String[] groupPath, ScopeType scopeType) {
        final String key = StringUtil.join(groupPath, ",");
        ModuleGroupNode groupNode = getMap(myModuleGroupNodes, scopeType).get(key);
        if (groupNode == null) {
            groupNode = new ModuleGroupNode(new ModuleGroup(groupPath), myProject);
            getMap(myModuleGroupNodes, scopeType).put(key, groupNode);
            getRootNode(scopeType).add(groupNode);
        }
        if (groupPath.length > 1) {
            String[] path = new String[groupPath.length - 1];
            System.arraycopy(groupPath, 0, path, 0, groupPath.length - 1);
            final PackageDependenciesNode node = getParentModuleGroup(path, scopeType);
            node.add(groupNode);
        }
        return groupNode;
    }

    private PackageDependenciesNode getLibraryOrJDKNode(OrderEntry libraryOrJdk) {
        if (libraryOrJdk == null || !myShowModules) {
            return getRootNode(ScopeType.LIB);
        }

        if (!myShowIndividualLibs) {
            if (myGroupByScopeType) return getRootNode(ScopeType.LIB);
            if (myAllLibsNode == null) {
                myAllLibsNode = new GeneralGroupNode(
                    AnalysisScopeLocalize.dependenciesLibrariesNodeText().get(),
                    AllIcons.Nodes.PpLibFolder,
                    myProject
                );
                getRootNode(ScopeType.LIB).add(myAllLibsNode);
            }
            return myAllLibsNode;
        }

        LibraryNode node = getMap(myLibraryNodes, ScopeType.LIB).get(libraryOrJdk);
        if (node != null) return node;
        node = new LibraryNode(libraryOrJdk, myProject);
        getMap(myLibraryNodes, ScopeType.LIB).put(libraryOrJdk, node);

        getRootNode(ScopeType.LIB).add(node);
        return node;
    }


    @Nonnull
    private PackageDependenciesNode getRootNode(ScopeType scopeType) {
        if (!myGroupByScopeType) {
            return myRoot;
        }
        else {
            if (scopeType == ScopeType.TEST) {
                return myTestRoot;
            }
            else if (scopeType == ScopeType.SOURCE) {
                return mySourceRoot;
            }
            else {
                return myLibsRoot;
            }
        }
    }
}
