/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.ui.filter;

import consulo.application.dumb.DumbAware;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.details.ModifiedInputDetails;
import consulo.ui.ex.action.*;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.image.ImageState;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.log.VcsLogRootFilter;
import consulo.versionControlSystem.log.VcsLogStructureFilter;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogStructureFilterImpl;
import consulo.versionControlSystem.log.impl.internal.ui.VcsLogColorManagerImpl;
import consulo.versionControlSystem.log.impl.internal.ui.VcsLogRootFilterImpl;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
import consulo.versionControlSystem.log.ui.VcsLogColorManager;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

class StructureFilterPopup extends FilterPopup<VcsLogFileFilter> {
    private static final int FILTER_LABEL_LENGTH = 30;
    public static final FileByNameComparator FILE_BY_NAME_COMPARATOR = new FileByNameComparator();
    public static final FileByPathComparator FILE_BY_PATH_COMPARATOR = new FileByPathComparator();
    public static final FilePathByNameComparator FILE_PATH_BY_NAME_COMPARATOR = new FilePathByNameComparator();
    public static final FilePathByPathComparator FILE_PATH_BY_PATH_COMPARATOR = new FilePathByPathComparator();

    private final VcsLogColorManager myColorManager;
    private final FixedSizeQueue<VcsLogStructureFilter> myHistory = new FixedSizeQueue<>(5);

    StructureFilterPopup(FilterModel<VcsLogFileFilter> filterModel, VcsLogColorManager colorManager) {
        super(VersionControlSystemLogLocalize.filterPaths(), filterModel);
        myColorManager = colorManager;
    }

    @Override
    protected String getText(VcsLogFileFilter filter) {
        Collection<VirtualFile> roots = filter.getRootFilter() == null ? getAllRoots() : filter.getRootFilter().getRoots();
        Collection<FilePath> files =
            filter.getStructureFilter() == null ? Collections.<FilePath>emptySet() : filter.getStructureFilter().getFiles();
        Collection<VirtualFile> visibleRoots =
            VcsLogUtil.getAllVisibleRoots(getAllRoots(), filter.getRootFilter(), filter.getStructureFilter());

        if (files.isEmpty()) {
            return getTextFromRoots(roots, VersionControlSystemLogLocalize.filterPathsNoRoots(), true,
                visibleRoots.size() == getAllRoots().size());
        }
        else {
            return getTextFromFilePaths(files, VersionControlSystemLogLocalize.filterPathsNoFolders(), false, files.isEmpty());
        }
    }

    private static String getTextFromRoots(Collection<VirtualFile> files, LocalizeValue emptyText, boolean shorten, boolean full) {
        return getText(files, emptyText, shorten ? FILE_BY_NAME_COMPARATOR : FILE_BY_PATH_COMPARATOR,
            file -> shorten ? file.getName() : StringUtil.shortenPathWithEllipsis(file.getPresentableUrl(), FILTER_LABEL_LENGTH),
            full);
    }

    private static String getTextFromFilePaths(Collection<FilePath> files, LocalizeValue emptyText, boolean shorten, boolean full) {
        return getText(files, emptyText, shorten ? FILE_PATH_BY_NAME_COMPARATOR : FILE_PATH_BY_PATH_COMPARATOR,
            file -> shorten ? file.getName() : StringUtil.shortenPathWithEllipsis(file.getPresentableUrl(), FILTER_LABEL_LENGTH),
            full);
    }

    private static <F> String getText(
        Collection<F> files,
        LocalizeValue emptyText,
        Comparator<F> comparator,
        Function<F, String> getText,
        boolean full
    ) {
        if (full) {
            return VersionControlSystemLogLocalize.filterAll().get();
        }
        else if (files.isEmpty()) {
            return emptyText.get();
        }
        else {
            F firstFile = Collections.min(files, comparator);
            String firstFileName = getText.apply(firstFile);
            if (files.size() == 1) {
                return firstFileName;
            }
            else {
                return firstFileName + " + " + (files.size() - 1);
            }
        }
    }

    @Override
    protected @Nullable String getToolTip(VcsLogFileFilter filter) {
        return getToolTip(
            filter.getRootFilter() == null ? getAllRoots() : filter.getRootFilter().getRoots(),
            filter.getStructureFilter() == null ? Collections.<FilePath>emptySet() : filter.getStructureFilter().getFiles()
        );
    }

    private String getToolTip(Collection<VirtualFile> roots, Collection<FilePath> files) {
        String tooltip = "";
        if (roots.isEmpty()) {
            tooltip += VersionControlSystemLogLocalize.filterPathsNoRootsSelected().get();
        }
        else if (roots.size() != getAllRoots().size()) {
            tooltip += VersionControlSystemLogLocalize.filterPathsTooltipRoots().get() + "\n" + getTooltipTextForRoots(roots, true);
        }
        if (!files.isEmpty()) {
            if (!tooltip.isEmpty()) {
                tooltip += "\n";
            }
            tooltip += VersionControlSystemLogLocalize.filterPathsTooltipFolders().get() + "\n" + getTooltipTextForFilePaths(files, false);
        }
        return tooltip;
    }

    private static String getTooltipTextForRoots(Collection<VirtualFile> files, boolean shorten) {
        return getTooltipTextForFiles(files, shorten ? FILE_BY_NAME_COMPARATOR : FILE_BY_PATH_COMPARATOR,
            file -> shorten ? file.getName() : file.getPresentableUrl());
    }

    private static String getTooltipTextForFilePaths(Collection<FilePath> files, boolean shorten) {
        return getTooltipTextForFiles(files, shorten ? FILE_PATH_BY_NAME_COMPARATOR : FILE_PATH_BY_PATH_COMPARATOR,
            file -> shorten ? file.getName() : file.getPresentableUrl());
    }

    private static <F> String getTooltipTextForFiles(Collection<F> files, Comparator<F> comparator, Function<F, String> getText) {
        List<F> filesToDisplay = ContainerUtil.sorted(files, comparator);
        if (files.size() > 10) {
            filesToDisplay = filesToDisplay.subList(0, 10);
        }
        String tooltip = StringUtil.join(filesToDisplay, getText, "\n");
        if (files.size() > 10) {
            tooltip += "\n...";
        }
        return tooltip;
    }

    @Override
    public ActionGroup createActionGroup() {
        Set<VirtualFile> roots = getAllRoots();

        List<AnAction> rootActions = new ArrayList<>();
        if (myColorManager.isMultipleRoots()) {
            for (VirtualFile root : ContainerUtil.sorted(roots, FILE_BY_NAME_COMPARATOR)) {
                rootActions.add(new SelectVisibleRootAction(root));
            }
        }
        List<AnAction> structureActions = new ArrayList<>();
        for (VcsLogStructureFilter filter : myHistory) {
            structureActions.add(new SelectFromHistoryAction(filter));
        }

        AnSeparator recent = AnSeparator.create(VersionControlSystemLogLocalize.filterRecent());
        AnSeparator rootsSeparator = AnSeparator.create(VersionControlSystemLogLocalize.filterPathsRoots());
        if (roots.size() > 15) {
            return new DefaultActionGroup(createAllAction(), new SelectFoldersAction(),
                recent, new DefaultActionGroup(structureActions),
                rootsSeparator, new DefaultActionGroup(rootActions));
        }
        else {
            return new DefaultActionGroup(createAllAction(), new SelectFoldersAction(),
                rootsSeparator, new DefaultActionGroup(rootActions),
                recent, new DefaultActionGroup(structureActions));
        }
    }

    private Set<VirtualFile> getAllRoots() {
        return myFilterModel.getDataPack().getLogProviders().keySet();
    }

    private boolean isVisible(VirtualFile root) {
        VcsLogFileFilter filter = myFilterModel.getFilter();
        if (filter != null && filter.getRootFilter() != null) {
            return filter.getRootFilter().getRoots().contains(root);
        }
        else {
            return true;
        }
    }

    private void setVisible(VirtualFile root, boolean visible) {
        Set<VirtualFile> roots = getAllRoots();

        VcsLogFileFilter previousFilter = myFilterModel.getFilter();
        VcsLogRootFilter rootFilter = previousFilter != null ? previousFilter.getRootFilter() : null;

        Collection<VirtualFile> visibleRoots;
        if (rootFilter == null) {
            if (visible) {
                visibleRoots = roots;
            }
            else {
                visibleRoots = ContainerUtil.subtract(roots, Collections.singleton(root));
            }
        }
        else {
            if (visible) {
                visibleRoots = ContainerUtil.union(new HashSet<>(rootFilter.getRoots()), Collections.singleton(root));
            }
            else {
                visibleRoots = ContainerUtil.subtract(rootFilter.getRoots(), Collections.singleton(root));
            }
        }
        myFilterModel.setFilter(new VcsLogFileFilter(null, new VcsLogRootFilterImpl(visibleRoots)));
    }

    private void setVisibleOnly(VirtualFile root) {
        myFilterModel.setFilter(new VcsLogFileFilter(null, new VcsLogRootFilterImpl(Collections.singleton(root))));
    }

    private static String getStructureActionText(VcsLogStructureFilter filter) {
        return getTextFromFilePaths(filter.getFiles(), VersionControlSystemLogLocalize.filterPathsNoItems(), false,
            filter.getFiles().isEmpty());
    }

    private static boolean isOnlyModifierDown(AnActionEvent e) {
        InputDetails details = e.getInputDetails();
        if (details instanceof ModifiedInputDetails modified) {
            return Platform.current().os().isMac() ? modified.withMeta() : modified.withCtrl();
        }
        return false;
    }

    private static class FileByNameComparator implements Comparator<VirtualFile> {
        @Override
        public int compare(VirtualFile o1, VirtualFile o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    private static class FileByPathComparator implements Comparator<VirtualFile> {
        @Override
        public int compare(VirtualFile o1, VirtualFile o2) {
            return o1.getPresentableUrl().compareTo(o2.getPresentableUrl());
        }
    }

    private static class FilePathByNameComparator implements Comparator<FilePath> {
        @Override
        public int compare(FilePath o1, FilePath o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    private static class FilePathByPathComparator implements Comparator<FilePath> {
        @Override
        public int compare(FilePath o1, FilePath o2) {
            return o1.getPresentableUrl().compareTo(o2.getPresentableUrl());
        }
    }

    private class SelectVisibleRootAction extends ToggleAction implements DumbAware, KeepingPopupOpenAction {
        private final ImageState<Boolean> myImageState;
        private final VirtualFile myRoot;
        private final Image myIcon;

        private SelectVisibleRootAction(VirtualFile root) {
            super(LocalizeValue.of(root.getName()), LocalizeValue.of(root.getPresentableUrl()), null);
            myRoot = root;
            myImageState = new ImageState<>(Boolean.FALSE);
            myIcon = Image.stated(myImageState, selected -> {
                Image backgroundImage = ImageEffects.colorFilled(
                    Image.DEFAULT_ICON_SIZE,
                    Image.DEFAULT_ICON_SIZE,
                    VcsLogColorManagerImpl.getBackgroundColor(myColorManager.getRootColor(myRoot))
                );
                if (selected) {
                    return ImageEffects.layered(backgroundImage, PlatformIconGroup.actionsChecked());
                }
                return backgroundImage;
            });
            getTemplatePresentation().setIcon(myIcon);
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return isVisible(myRoot);
        }

        @RequiredUIAccess
        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            if (!isEnabled() || isOnlyModifierDown(e)) {
                setVisibleOnly(myRoot);
            }
            else {
                setVisible(myRoot, state);
            }
        }

        @Override
        public void update(AnActionEvent e) {
            super.update(e);

            myImageState.setState(isVisible(myRoot) && isEnabled());
            e.getPresentation().setIcon(myIcon);
            e.getPresentation().setDescription(VersionControlSystemLogLocalize.filterPathsRootOnlyTooltip(
                Platform.current().os().isMac() ? "Cmd" : "Ctrl",
                e.getPresentation().getText()
            ));
        }

        private boolean isEnabled() {
            return myFilterModel.getFilter() == null || (myFilterModel.getFilter().getStructureFilter() == null);
        }
    }

    private class SelectFoldersAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        SelectFoldersAction() {
            super(VersionControlSystemLogLocalize.filterPathsSelectFolders());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            Project project = e.getRequiredData(Project.KEY);
            FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, false, false, false, true)
                .withTitle(VersionControlSystemLogLocalize.filterPathsSelectFoldersTitle())
                .withRoots(new ArrayList<>(getAllRoots()));

            UIAccess uiAccess = UIAccess.current();
            FileChooser.chooseFiles(descriptor, project, null).whenCompleteAsync((files, error) -> {
                if (files == null || files.length == 0) {
                    return;
                }
                VcsLogStructureFilterImpl structureFilter = new VcsLogStructureFilterImpl(new HashSet<VirtualFile>(Arrays.asList(files)));
                myFilterModel.setFilter(new VcsLogFileFilter(structureFilter, null));
                myHistory.add(structureFilter);
            }, uiAccess);
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabledAndVisible(e.hasData(Project.KEY));
        }
    }

    private class SelectFromHistoryAction extends ToggleAction {
        private final VcsLogStructureFilter myFilter;

        private SelectFromHistoryAction(VcsLogStructureFilter filter) {
            super(
                LocalizeValue.of(getStructureActionText(filter)),
                LocalizeValue.of(getTooltipTextForFilePaths(filter.getFiles(), false).replace("\n", " ")),
                null
            );
            myFilter = filter;
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return myFilterModel.getFilter() != null && myFilterModel.getFilter().getStructureFilter() == myFilter;
        }

        @RequiredUIAccess
        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            myFilterModel.setFilter(new VcsLogFileFilter(myFilter, null));
        }
    }

    private static class FixedSizeQueue<T> implements Iterable<T> {
        private final LinkedList<T> myQueue = new LinkedList<>();
        private final int myMaxSize;

        public FixedSizeQueue(int maxSize) {
            myMaxSize = maxSize;
        }

        @Override
        public Iterator<T> iterator() {
            return ContainerUtil.reverse(myQueue).iterator();
        }

        public void add(T t) {
            myQueue.add(t);
            if (myQueue.size() > myMaxSize) {
                myQueue.poll();
            }
        }
    }
}
