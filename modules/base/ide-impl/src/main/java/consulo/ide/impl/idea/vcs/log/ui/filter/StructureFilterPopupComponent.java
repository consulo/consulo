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
package consulo.ide.impl.idea.vcs.log.ui.filter;

import consulo.application.AllIcons;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.ui.SizedIcon;
import consulo.ide.impl.idea.ui.popup.KeepingPopupOpenAction;
import consulo.ide.impl.idea.util.NotNullFunction;
import consulo.ide.impl.idea.util.ui.ColorIcon;
import consulo.ide.impl.idea.vcs.log.VcsLogRootFilterImpl;
import consulo.ide.impl.idea.vcs.log.data.VcsLogStructureFilterImpl;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogColorManager;
import consulo.ide.impl.idea.vcs.log.ui.frame.VcsLogGraphTable;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.log.VcsLogDataPack;
import consulo.versionControlSystem.log.VcsLogRootFilter;
import consulo.versionControlSystem.log.VcsLogStructureFilter;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.JdkConstants;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;

class StructureFilterPopupComponent extends FilterPopupComponent<VcsLogFileFilter> {
  private static final int FILTER_LABEL_LENGTH = 30;
  private static final int CHECKBOX_ICON_SIZE = 15;
  public static final FileByNameComparator FILE_BY_NAME_COMPARATOR = new FileByNameComparator();
  public static final FileByPathComparator FILE_BY_PATH_COMPARATOR = new FileByPathComparator();
  public static final FilePathByNameComparator FILE_PATH_BY_NAME_COMPARATOR = new FilePathByNameComparator();
  public static final FilePathByPathComparator FILE_PATH_BY_PATH_COMPARATOR = new FilePathByPathComparator();
  @Nonnull
  private final VcsLogColorManager myColorManager;
  private final FixedSizeQueue<VcsLogStructureFilter> myHistory = new FixedSizeQueue<>(5);

  public StructureFilterPopupComponent(@Nonnull FilterModel<VcsLogFileFilter> filterModel, @Nonnull VcsLogColorManager colorManager) {
    super("Paths", filterModel);
    myColorManager = colorManager;
  }

  @Nonnull
  @Override
  protected String getText(@Nonnull VcsLogFileFilter filter) {
    Collection<VirtualFile> roots = filter.getRootFilter() == null ? getAllRoots() : filter.getRootFilter().getRoots();
    Collection<FilePath> files =
      filter.getStructureFilter() == null ? Collections.<FilePath>emptySet() : filter.getStructureFilter().getFiles();
    Collection<VirtualFile> visibleRoots =
      VcsLogUtil.getAllVisibleRoots(getAllRoots(), filter.getRootFilter(), filter.getStructureFilter());

    if (files.isEmpty()) {
      return getTextFromRoots(roots, "roots", true, visibleRoots.size() == getAllRoots().size());
    }
    else {
      return getTextFromFilePaths(files, "folders", false, files.isEmpty());
    }
  }

  private static String getTextFromRoots(@Nonnull Collection<VirtualFile> files,
                                         @Nonnull String category,
                                         final boolean shorten,
                                         boolean full) {
    return getText(files, category, shorten ? FILE_BY_NAME_COMPARATOR : FILE_BY_PATH_COMPARATOR,
                   file -> shorten ? file.getName() : StringUtil.shortenPathWithEllipsis(file.getPresentableUrl(), FILTER_LABEL_LENGTH),
                   full);
  }

  private static String getTextFromFilePaths(@Nonnull Collection<FilePath> files,
                                             @Nonnull String category,
                                             final boolean shorten,
                                             boolean full) {
    return getText(files, category, shorten ? FILE_PATH_BY_NAME_COMPARATOR : FILE_PATH_BY_PATH_COMPARATOR,
                   file -> shorten ? file.getName() : StringUtil.shortenPathWithEllipsis(file.getPresentableUrl(), FILTER_LABEL_LENGTH),
                   full);
  }

  private static <F> String getText(@Nonnull Collection<F> files,
                                    @Nonnull String category,
                                    @Nonnull Comparator<F> comparator,
                                    @Nonnull NotNullFunction<F, String> getText,
                                    boolean full) {
    if (full) {
      return ALL;
    }
    else if (files.isEmpty()) {
      return "No " + category;
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

  @Nullable
  @Override
  protected String getToolTip(@Nonnull VcsLogFileFilter filter) {
    return getToolTip(filter.getRootFilter() == null ? getAllRoots() : filter.getRootFilter().getRoots(),
                      filter.getStructureFilter() == null ? Collections.<FilePath>emptySet() : filter.getStructureFilter().getFiles());
  }

  @Nonnull
  private String getToolTip(@Nonnull Collection<VirtualFile> roots, @Nonnull Collection<FilePath> files) {
    String tooltip = "";
    if (roots.isEmpty()) {
      tooltip += "No Roots Selected";
    }
    else if (roots.size() != getAllRoots().size()) {
      tooltip += "Roots:\n" + getTooltipTextForRoots(roots, true);
    }
    if (!files.isEmpty()) {
      if (!tooltip.isEmpty()) tooltip += "\n";
      tooltip += "Folders:\n" + getTooltipTextForFilePaths(files, false);
    }
    return tooltip;
  }

  private static String getTooltipTextForRoots(Collection<VirtualFile> files, final boolean shorten) {
    return getTooltipTextForFiles(files, shorten ? FILE_BY_NAME_COMPARATOR : FILE_BY_PATH_COMPARATOR,
                                  file -> shorten ? file.getName() : file.getPresentableUrl());
  }

  private static String getTooltipTextForFilePaths(Collection<FilePath> files, final boolean shorten) {
    return getTooltipTextForFiles(files, shorten ? FILE_PATH_BY_NAME_COMPARATOR : FILE_PATH_BY_PATH_COMPARATOR,
                                  file -> shorten ? file.getName() : file.getPresentableUrl());
  }

  private static <F> String getTooltipTextForFiles(@Nonnull Collection<F> files,
                                                   @Nonnull Comparator<F> comparator,
                                                   @Nonnull NotNullFunction<F, String> getText) {
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
  protected ActionGroup createActionGroup() {
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

    if (roots.size() > 15) {
      return new DefaultActionGroup(createAllAction(), new SelectFoldersAction(),
                                    new AnSeparator("Recent"), new DefaultActionGroup(structureActions),
                                    new AnSeparator("Roots"), new DefaultActionGroup(rootActions));
    }
    else {
      return new DefaultActionGroup(createAllAction(), new SelectFoldersAction(),
                                    new AnSeparator("Roots"), new DefaultActionGroup(rootActions),
                                    new AnSeparator("Recent"), new DefaultActionGroup(structureActions));
    }
  }

  private Set<VirtualFile> getAllRoots() {
    return myFilterModel.getDataPack().getLogProviders().keySet();
  }

  private boolean isVisible(@Nonnull VirtualFile root) {
    VcsLogFileFilter filter = myFilterModel.getFilter();
    if (filter != null && filter.getRootFilter() != null) {
      return filter.getRootFilter().getRoots().contains(root);
    }
    else {
      return true;
    }
  }

  private void setVisible(@Nonnull VirtualFile root, boolean visible) {
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

  private void setVisibleOnly(@Nonnull VirtualFile root) {
    myFilterModel.setFilter(new VcsLogFileFilter(null, new VcsLogRootFilterImpl(Collections.singleton(root))));
  }

  private static String getStructureActionText(@Nonnull VcsLogStructureFilter filter) {
    return getTextFromFilePaths(filter.getFiles(), "items", false, filter.getFiles().isEmpty());
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
    @Nonnull
    private final CheckboxColorIcon myIcon;
    @Nonnull
    private final VirtualFile myRoot;

    private SelectVisibleRootAction(@Nonnull VirtualFile root) {
      super(root.getName(), root.getPresentableUrl(), null);
      myRoot = root;
      myIcon = new CheckboxColorIcon(CHECKBOX_ICON_SIZE, VcsLogGraphTable.getRootBackgroundColor(myRoot, myColorManager));
      getTemplatePresentation().setIcon(Image.empty(CHECKBOX_ICON_SIZE)); // see PopupFactoryImpl.calcMaxIconSize
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      return isVisible(myRoot);
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      if (!isEnabled()) {
        setVisibleOnly(myRoot);
      }
      else {
        if ((e.getModifiers() & getMask()) != 0) {
          setVisibleOnly(myRoot);
        }
        else {
          setVisible(myRoot, state);
        }
      }
    }

    @JdkConstants.InputEventMask
    private int getMask() {
      return Platform.current().os().isMac() ? InputEvent.META_MASK : InputEvent.CTRL_MASK;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      super.update(e);

      updateIcon();
      e.getPresentation().setIcon(myIcon);
      e.getPresentation().putClientProperty(UIUtil.TOOL_TIP_TEXT_KEY, KeyEvent.getKeyModifiersText(getMask()) +
                                                                      "+Click to see only \"" +
                                                                      e.getPresentation().getText() +
                                                                      "\"");
    }

    private void updateIcon() {
      myIcon.prepare(isVisible(myRoot) && isEnabled());
    }

    private boolean isEnabled() {
      return myFilterModel.getFilter() == null || (myFilterModel.getFilter().getStructureFilter() == null);
    }
  }

  private static class CheckboxColorIcon extends ColorIcon implements Image {
    private final int mySize;
    private boolean mySelected = false;
    private SizedIcon mySizedIcon;

    public CheckboxColorIcon(int size, @Nonnull Color color) {
      super(size, color);
      mySize = size;
      mySizedIcon = new SizedIcon(TargetAWT.to(AllIcons.Actions.Checked), mySize, mySize);
    }

    public void prepare(boolean selected) {
      mySelected = selected;
    }

    @Nonnull
    @Override
    public CheckboxColorIcon withIconPreScaled(boolean preScaled) {
      mySizedIcon = (SizedIcon)mySizedIcon.withIconPreScaled(preScaled);
      return (CheckboxColorIcon)super.withIconPreScaled(preScaled);
    }

    @Override
    public void paintIcon(Component component, Graphics g, int i, int j) {
      super.paintIcon(component, g, i, j);
      if (mySelected) {
        mySizedIcon.paintIcon(component, g, i, j);
      }
    }

    @Override
    public int getHeight() {
      return getIconHeight();
    }

    @Override
    public int getWidth() {
      return getIconWidth();
    }
  }

  private class SelectFoldersAction extends DumbAwareAction {
    public static final String STRUCTURE_FILTER_TEXT = "Select Folders...";

    SelectFoldersAction() {
      super(STRUCTURE_FILTER_TEXT);
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      Project project = e.getRequiredData(Project.KEY);
      VcsLogDataPack dataPack = myFilterModel.getDataPack();
      VcsLogFileFilter filter = myFilterModel.getFilter();

      Collection<VirtualFile> files;
      if (filter == null || filter.getStructureFilter() == null) {
        files = Collections.emptySet();
      }
      else {
        files = ContainerUtil.mapNotNull(filter.getStructureFilter().getFiles(), filePath -> {
          // for now, ignoring non-existing paths
          return filePath.getVirtualFile();
        });
      }

      VcsStructureChooser chooser = new VcsStructureChooser(project, "Select Files or Folders to Filter by", files,
                                                            new ArrayList<>(dataPack.getLogProviders().keySet()));
      if (chooser.showAndGet()) {
        VcsLogStructureFilterImpl structureFilter = new VcsLogStructureFilterImpl(new HashSet<VirtualFile>(chooser.getSelectedFiles()));
        myFilterModel.setFilter(new VcsLogFileFilter(structureFilter, null));
        myHistory.add(structureFilter);
      }
    }

    @Override
    public void update(AnActionEvent e) {
      e.getPresentation().setEnabledAndVisible(e.getData(Project.KEY) != null);
    }
  }

  private class SelectFromHistoryAction extends ToggleAction {
    @Nonnull
    private final VcsLogStructureFilter myFilter;
    @Nonnull
    private final Image myIcon;
    @Nonnull
    private final Image myEmptyIcon;

    private SelectFromHistoryAction(@Nonnull VcsLogStructureFilter filter) {
      super(getStructureActionText(filter), getTooltipTextForFilePaths(filter.getFiles(), false).replace("\n", " "), null);
      myFilter = filter;
      myIcon = ImageEffects.resize(AllIcons.Actions.Checked, CHECKBOX_ICON_SIZE, CHECKBOX_ICON_SIZE);
      myEmptyIcon = Image.empty(CHECKBOX_ICON_SIZE);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      return myFilterModel.getFilter() != null && myFilterModel.getFilter().getStructureFilter() == myFilter;
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      myFilterModel.setFilter(new VcsLogFileFilter(myFilter, null));
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      super.update(e);

      Presentation presentation = e.getPresentation();
      if (isSelected(e)) {
        presentation.setIcon(myIcon);
      }
      else {
        presentation.setIcon(myEmptyIcon);
      }
    }
  }

  private static class FixedSizeQueue<T> implements Iterable<T> {
    private final LinkedList<T> myQueue = new LinkedList<>();
    private final int maxSize;

    public FixedSizeQueue(int maxSize) {
      this.maxSize = maxSize;
    }

    @Nonnull
    @Override
    public Iterator<T> iterator() {
      return ContainerUtil.reverse(myQueue).iterator();
    }

    public void add(T t) {
      myQueue.add(t);
      if (myQueue.size() > maxSize) {
        myQueue.poll();
      }
    }
  }
}
