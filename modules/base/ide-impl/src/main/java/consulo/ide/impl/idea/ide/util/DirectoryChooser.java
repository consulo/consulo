/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.ide.util;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.AllIcons;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ide.util.gotoByName.ChooseByNamePanel;
import consulo.ide.impl.idea.ide.util.gotoByName.ChooseByNamePopupComponent;
import consulo.ide.impl.idea.ide.util.gotoByName.GotoClassModel2;
import consulo.ide.impl.idea.openapi.project.ProjectUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ide.internal.DirectoryChooserDialog;
import consulo.language.content.ContentFoldersSupportUtil;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.util.ModuleUtilCore;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.ContentFolder;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.TabbedPaneWrapper;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.image.Image;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DirectoryChooser extends DialogWrapper implements DirectoryChooserDialog {
  @NonNls private static final String FILTER_NON_EXISTING = "filter_non_existing";
  private static final String DEFAULT_SELECTION = "last_directory_selection";
  private final DirectoryChooserView myView;
  private boolean myFilterExisting;
  private PsiDirectory myDefaultSelection;
  private List<ItemWrapper> myItems = new ArrayList<>();
  private PsiElement mySelection;
  private final TabbedPaneWrapper myTabbedPaneWrapper;
  private final ChooseByNamePanel myChooseByNamePanel;

  public DirectoryChooser(@Nonnull Project project){
    this(project, new DirectoryChooserModuleTreeView(project));
  }

  public DirectoryChooser(@Nonnull Project project, @Nonnull DirectoryChooserView view){
    super(project, true);
    myView = view;
    final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
    myFilterExisting = propertiesComponent.isValueSet(FILTER_NON_EXISTING) && propertiesComponent.isTrueValue(FILTER_NON_EXISTING);
    myTabbedPaneWrapper = new TabbedPaneWrapper(getDisposable());
    myChooseByNamePanel = new ChooseByNamePanel(project, new GotoClassModel2(project){
      @Nonnull
      @Override
      public String[] getNames(boolean checkBoxState) {
        return super.getNames(false);
      }
    }, "", false, null) {
      @Override
      protected void showTextFieldPanel() {
      }

      @Override
      protected void close(boolean isOk) {
        super.close(isOk);
        if (isOk) {
          final List<Object> elements = getChosenElements();
          if (elements != null && elements.size() > 0) {
            myActionListener.elementChosen(elements.get(0));
          }
          doOKAction();
        }
        else {
          doCancelAction();
        }
      }
    };
    Disposer.register(myDisposable, myChooseByNamePanel);
    init();
  }

  @Override
  protected void doOKAction() {
    PropertiesComponent.getInstance().setValue(FILTER_NON_EXISTING, String.valueOf(myFilterExisting));
    if (myTabbedPaneWrapper.getSelectedIndex() == 1) {
      setSelection(myChooseByNamePanel.getChosenElement());
    }
    final ItemWrapper item = myView.getSelectedItem();
    if (item != null) {
      final PsiDirectory directory = item.getDirectory();
      if (directory != null) {
        PropertiesComponent.getInstance(directory.getProject()).setValue(DEFAULT_SELECTION, directory.getVirtualFile().getPath());
      }
    }
    super.doOKAction();
  }

  @Override
  protected JComponent createCenterPanel(){
    final JPanel panel = new JPanel(new BorderLayout());

    final DefaultActionGroup actionGroup = new DefaultActionGroup();
    actionGroup.add(new FilterExistentAction());
    final JComponent toolbarComponent = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, actionGroup, true).getComponent();
    toolbarComponent.setBorder(null);
    panel.add(toolbarComponent, BorderLayout.NORTH);

    myView.onSelectionChange(this::enableButtons);
    final JComponent component = myView.getComponent();
    final JScrollPane jScrollPane = ScrollPaneFactory.createScrollPane(component);
    //noinspection HardCodedStringLiteral
    int prototypeWidth = component.getFontMetrics(component.getFont()).stringWidth("X:\\1234567890\\1234567890\\com\\company\\system\\subsystem");
    jScrollPane.setPreferredSize(new Dimension(Math.max(300, prototypeWidth),300));

    installEnterAction(component);
    panel.add(jScrollPane, BorderLayout.CENTER);
    myTabbedPaneWrapper.addTab("Directory Structure", panel);

    myChooseByNamePanel.invoke(new ChooseByNamePopupComponent.Callback() {
      @Override
      public void elementChosen(Object element) {
        setSelection(element);
      }
    }, IdeaModalityState.stateForComponent(getRootPane()), false);
    myTabbedPaneWrapper.addTab("Choose By Neighbor Class", myChooseByNamePanel.getPanel());

    return myTabbedPaneWrapper.getComponent();
  }

  private void setSelection(Object element) {
    if (element instanceof PsiElement psiElement) {
      mySelection = psiElement;
    }
  }

  private void installEnterAction(final JComponent component) {
    final KeyStroke enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0);
    final InputMap inputMap = component.getInputMap();
    final ActionMap actionMap = component.getActionMap();
    final Object oldActionKey = inputMap.get(enterKeyStroke);
    final Action oldAction = oldActionKey != null ? actionMap.get(oldActionKey) : null;
    inputMap.put(enterKeyStroke, "clickButton");
    actionMap.put("clickButton", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (isOKActionEnabled()) {
          doOKAction();
        }
        else if (oldAction != null) {
          oldAction.actionPerformed(e);
        }
      }
    });
  }

  @Override
  protected String getDimensionServiceKey() {
    return "chooseDestDirectoryDialog";
  }

  private void buildFragments() {
    ArrayList<String[]> pathes = new ArrayList<>();
    for (int i = 0; i < myView.getItemsSize(); i++) {
      ItemWrapper item = myView.getItemByIndex(i);
      pathes.add(ArrayUtil.toStringArray(FileUtil.splitPath(item.getPresentableUrl())));
    }
    FragmentBuilder headBuilder = new FragmentBuilder(pathes){
        @Override
        protected void append(String fragment, StringBuffer buffer) {
          buffer.append(mySeparator);
          buffer.append(fragment);
        }

        @Override
        protected int getFragmentIndex(String[] path, int index) {
          return path.length > index ? index : -1;
        }
      };
    String commonHead = headBuilder.execute();
    final int headLimit = headBuilder.getIndex();
    FragmentBuilder tailBuilder = new FragmentBuilder(pathes){
        @Override
        protected void append(String fragment, StringBuffer buffer) {
          buffer.insert(0, fragment + mySeparator);
        }

        @Override
        protected int getFragmentIndex(String[] path, int index) {
          int result = path.length - 1 - index;
          return result > headLimit ? result : -1;
        }
      };
    String commonTail = tailBuilder.execute();
    int tailLimit = tailBuilder.getIndex();
    for (int i = 0; i < myView.getItemsSize(); i++) {
      ItemWrapper item = myView.getItemByIndex(i);
      String special = concat(pathes.get(i), headLimit, tailLimit);
      item.setFragments(createFragments(commonHead, special, commonTail));
    }
  }

  @Nullable
  private static String concat(String[] strings, int headLimit, int tailLimit) {
    if (strings.length <= headLimit + tailLimit) return null;
    StringBuilder builder = new StringBuilder();
    String separator = "";
    for (int i = headLimit; i < strings.length - tailLimit; i++) {
      builder.append(separator);
      builder.append(strings[i]);
      separator = File.separator;
    }
    return builder.toString();
  }

  private static PathFragment[] createFragments(String head, String special, String tail) {
    ArrayList<PathFragment> list = new ArrayList<>(3);
    if (head != null) {
      if (special != null || tail != null) list.add(new PathFragment(head + File.separatorChar, true));
      else return new PathFragment[]{new PathFragment(head, true)};
    }
    if (special != null) {
      if (tail != null) list.add(new PathFragment(special + File.separatorChar, false));
      else list.add(new PathFragment(special, false));
    }
    if (tail != null) list.add(new PathFragment(tail, true));
    return list.toArray(new PathFragment[list.size()]);
  }

  private static abstract class FragmentBuilder {
    private final ArrayList<String[]> myPaths;
    private final StringBuffer myBuffer = new StringBuffer();
    private int myIndex;
    protected String mySeparator = "";

    public FragmentBuilder(ArrayList<String[]> pathes) {
      myPaths = pathes;
      myIndex = 0;
    }

    public int getIndex() { return myIndex; }

    @Nullable
    public String execute() {
      while (true) {
        String commonHead = getCommonFragment(myIndex);
        if (commonHead == null) break;
        append(commonHead, myBuffer);
        mySeparator = File.separator;
        myIndex++;
      }
      return myIndex > 0 ? myBuffer.toString() : null;
    }

    protected abstract void append(String fragment, StringBuffer buffer);

    @Nullable
    private String getCommonFragment(int count) {
      String commonFragment = null;
      for (String[] path : myPaths) {
        int index = getFragmentIndex(path, count);
        if (index == -1) return null;
        if (commonFragment == null) {
          commonFragment = path[index];
          continue;
        }
        if (!Comparing.strEqual(commonFragment, path[index], Platform.current().fs().isCaseSensitive())) return null;
      }
      return commonFragment;
    }

    protected abstract int getFragmentIndex(String[] path, int index);
  }

  public static class ItemWrapper {
    final PsiDirectory myDirectory;
    private PathFragment[] myFragments;
    private final String myPostfix;

    private String myRelativeToProjectPath = null;

    public ItemWrapper(PsiDirectory directory, String postfix) {
      myDirectory = directory;
      myPostfix = postfix != null && postfix.length() > 0 ? postfix : null;
    }

    public PathFragment[] getFragments() { return myFragments; }

    public void setFragments(PathFragment[] fragments) {
      myFragments = fragments;
    }

    @RequiredUIAccess
    @Nonnull
    @Deprecated
    @DeprecationInfo(value = "Use #getIcon()")
    public Image getIcon(@Nonnull ProjectFileIndex fileIndex) {
      return getIcon();
    }

    @RequiredUIAccess
    @Nonnull
    public Image getIcon() {
      if (myDirectory != null) {
        VirtualFile virtualFile = myDirectory.getVirtualFile();
        List<ContentFolder> contentFolders = ModuleUtilCore.getContentFolders(myDirectory.getProject());
        for (ContentFolder contentFolder : contentFolders) {
          VirtualFile file = contentFolder.getFile();
          if (file == null) {
            continue;
          }
          if (VfsUtil.isAncestor(file, virtualFile, false)) {
            return ContentFoldersSupportUtil.getContentFolderIcon(contentFolder.getType(), contentFolder.getProperties());
          }
        }
      }
      return AllIcons.Nodes.Folder;
    }

    public String getPresentableUrl() {
      String directoryUrl;
      if (myDirectory != null) {
        directoryUrl = myDirectory.getVirtualFile().getPresentableUrl();
        final VirtualFile baseDir = myDirectory.getProject().getBaseDir();
        if (baseDir != null) {
          final String projectHomeUrl = baseDir.getPresentableUrl();
          if (directoryUrl.startsWith(projectHomeUrl)) {
            directoryUrl = "..." + directoryUrl.substring(projectHomeUrl.length());
          }
        }
      }
      else {
        directoryUrl = "";
      }
      return myPostfix != null ? directoryUrl + myPostfix : directoryUrl;
    }

    public PsiDirectory getDirectory() {
      return myDirectory;
    }

    public String getRelativeToProjectPath() {
      if (myRelativeToProjectPath == null) {
        final PsiDirectory directory = getDirectory();
        final VirtualFile virtualFile = directory != null ? directory.getVirtualFile() : null;
        myRelativeToProjectPath = virtualFile != null
          ? ProjectUtil.calcRelativeToProjectPath(virtualFile, directory.getProject(), true, false, true)
          : getPresentableUrl();
      }
      return myRelativeToProjectPath;
    }
  }

  @Override
  @RequiredUIAccess
  public JComponent getPreferredFocusedComponent(){
    return myView.getComponent();
  }

  @Override
  @RequiredReadAction
  public void fillList(PsiDirectory[] directories, @Nullable PsiDirectory defaultSelection, Project project, String postfixToShow) {
    fillList(directories, defaultSelection, project, postfixToShow, null);
  }

  @Override
  @RequiredReadAction
  public void fillList(PsiDirectory[] directories, @Nullable PsiDirectory defaultSelection, Project project, Map<PsiDirectory,String> postfixes) {
    fillList(directories, defaultSelection, project, null, postfixes);
  }

  @RequiredReadAction
  private void fillList(PsiDirectory[] directories, @Nullable PsiDirectory defaultSelection, Project project, String postfixToShow, Map<PsiDirectory,String> postfixes) {
    if (myView.getItemsSize() > 0){
      myView.clearItems();
    }
    if (defaultSelection == null) {
      defaultSelection = getDefaultSelection(directories, project);
      if (defaultSelection == null && directories.length > 0) {
        defaultSelection = directories[0];
      }
    }
    int selectionIndex = -1;
    for (int i = 0; i < directories.length; i++){
      PsiDirectory directory = directories[i];
      if (directory.equals(defaultSelection)) {
        selectionIndex = i;
        break;
      }
    }
    if (selectionIndex < 0 && directories.length == 1) {
      selectionIndex = 0;
    }

    if (selectionIndex < 0) {
      // find source root corresponding to defaultSelection
      final PsiManager manager = PsiManager.getInstance(project);
      VirtualFile[] sourceRoots = ProjectRootManager.getInstance(project).getContentSourceRoots();
      for (VirtualFile sourceRoot : sourceRoots) {
        if (sourceRoot.isDirectory()) {
          PsiDirectory directory = manager.findDirectory(sourceRoot);
          if (directory != null && isParent(defaultSelection, directory)) {
            defaultSelection = directory;
            break;
          }
        }
      }
    }

    int existingIdx = 0;
    for (int i = 0; i < directories.length; i++){
      PsiDirectory directory = directories[i];
      final String postfixForDirectory;
      if (postfixes == null) {
        postfixForDirectory = postfixToShow;
      }
      else {
        postfixForDirectory = postfixes.get(directory);
      }
      final ItemWrapper itemWrapper = new ItemWrapper(directory, postfixForDirectory);
      myItems.add(itemWrapper);
      if (myFilterExisting) {
        if (selectionIndex == i) selectionIndex = -1;
        if (postfixForDirectory != null && directory.getVirtualFile().findFileByRelativePath(StringUtil.trimStart(postfixForDirectory, File.separator)) == null) {
          if (isParent(directory, defaultSelection)) {
            myDefaultSelection = directory;
          }
          continue;
        }
      }

      myView.addItem(itemWrapper);
      if (selectionIndex < 0 && isParent(directory, defaultSelection)) {
        selectionIndex = existingIdx;
      }
      existingIdx++;
    }
    buildFragments();
    myView.listFilled();
    if (myView.getItemsSize() > 0) {
      if (selectionIndex != -1) {
        myView.selectItemByIndex(selectionIndex);
      } else {
        myView.selectItemByIndex(0);
      }
    }
    else {
      myView.clearSelection();
    }
    enableButtons();
    myView.getComponent().repaint();
  }

  @Nullable
  @RequiredReadAction
  private static PsiDirectory getDefaultSelection(PsiDirectory[] directories, Project project) {
    final String defaultSelectionPath = PropertiesComponent.getInstance(project).getValue(DEFAULT_SELECTION);
    if (defaultSelectionPath != null) {
      final VirtualFile directoryByDefault = LocalFileSystem.getInstance().findFileByPath(defaultSelectionPath);
      if (directoryByDefault != null) {
        final PsiDirectory directory = PsiManager.getInstance(project).findDirectory(directoryByDefault);
        return directory != null && ArrayUtil.find(directories, directory) > -1 ? directory : null;
      }
    }
    return null;
  }

  private static boolean isParent(PsiDirectory directory, PsiDirectory parentCandidate) {
    while (directory != null) {
      if (directory.equals(parentCandidate)) return true;
      directory = directory.getParentDirectory();
    }
    return false;
  }

  private void enableButtons() {
    setOKActionEnabled(myView.getSelectedItem() != null);
  }

  @Override
  @Nullable
  public PsiDirectory getSelectedDirectory() {
    if (mySelection != null) {
      final PsiFile file = mySelection.getContainingFile();
      if (file != null){
        return file.getContainingDirectory();
      }
    }
    ItemWrapper wrapper = myView.getSelectedItem();
    if (wrapper == null) return null;
    return wrapper.myDirectory;
  }


  public static class PathFragment {
    private final String myText;
    private final boolean myCommon;

    public PathFragment(String text, boolean isCommon) {
      myText = text;
      myCommon = isCommon;
    }

    public String getText() {
      return myText;
    }

    public boolean isCommon() {
      return myCommon;
    }
  }


  private class FilterExistentAction extends ToggleAction {
    public FilterExistentAction() {
      super(
        RefactoringLocalize.directoryChooserHideNonExistentCheckboxText().get(),
        UIUtil.removeMnemonic(RefactoringLocalize.directoryChooserHideNonExistentCheckboxText().get()),
        AllIcons.General.Filter
      );
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
      return myFilterExisting;
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
      myFilterExisting = state;
      final ItemWrapper selectedItem = myView.getSelectedItem();
      PsiDirectory directory = selectedItem != null ? selectedItem.getDirectory() : null;
      if (directory == null && myDefaultSelection != null) {
        directory = myDefaultSelection;
      }
      myView.clearItems();
      int idx = 0;
      int selectionId = -1;
      for (ItemWrapper item : myItems) {
        if (myFilterExisting) {
          if (item.myPostfix != null &&
              item.getDirectory().getVirtualFile().findFileByRelativePath(StringUtil.trimStart(item.myPostfix, File.separator)) == null) {
            continue;
          }
        }
        if (item.getDirectory() == directory) {
          selectionId = idx;
        }
        idx++;
        myView.addItem(item);
      }
      buildFragments();
      myView.listFilled();
      if (selectionId < 0) {
        myView.clearSelection();
        if (myView.getItemsSize() > 0) {
          myView.selectItemByIndex(0);
        }
      }
      else {
        myView.selectItemByIndex(selectionId);
      }
      enableButtons();
      myView.getComponent().repaint();
    }
  }
}
