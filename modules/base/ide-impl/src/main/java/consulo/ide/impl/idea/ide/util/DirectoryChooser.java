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
import consulo.application.ApplicationPropertiesComponent;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ide.util.gotoByName.ChooseByNamePanel;
import consulo.ide.impl.idea.ide.util.gotoByName.ChooseByNamePopupComponent;
import consulo.ide.impl.idea.ide.util.gotoByName.GotoClassModel2;
import consulo.ide.impl.idea.openapi.project.ProjectUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
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
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.TabbedPaneWrapper;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DirectoryChooser extends DialogWrapper implements DirectoryChooserDialog {
    private static final String FILTER_NON_EXISTING = "filter_non_existing";
    private static final String DEFAULT_SELECTION = "last_directory_selection";
    private final DirectoryChooserView myView;
    private boolean myFilterExisting;
    private PsiDirectory myDefaultSelection;
    private List<ItemWrapper> myItems = new ArrayList<>();
    private PsiElement mySelection;
    private final TabbedPaneWrapper myTabbedPaneWrapper;
    private final ChooseByNamePanel myChooseByNamePanel;

    public DirectoryChooser(@Nonnull Project project) {
        this(project, new DirectoryChooserModuleTreeView(project));
    }

    public DirectoryChooser(@Nonnull Project project, @Nonnull DirectoryChooserView view) {
        super(project, true);
        myView = view;
        ApplicationPropertiesComponent propertiesComponent = ApplicationPropertiesComponent.getInstance();
        myFilterExisting = propertiesComponent.isValueSet(FILTER_NON_EXISTING) && propertiesComponent.isTrueValue(FILTER_NON_EXISTING);
        myTabbedPaneWrapper = new TabbedPaneWrapper(getDisposable());
        myChooseByNamePanel = new ChooseByNamePanel(project, new GotoClassModel2(project) {
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
                    List<Object> elements = getChosenElements();
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
        ApplicationPropertiesComponent.getInstance().setValue(FILTER_NON_EXISTING, String.valueOf(myFilterExisting));
        if (myTabbedPaneWrapper.getSelectedIndex() == 1) {
            setSelection(myChooseByNamePanel.getChosenElement());
        }
        ItemWrapper item = myView.getSelectedItem();
        if (item != null) {
            PsiDirectory directory = item.getDirectory();
            if (directory != null) {
                directory.getProject().getInstance(ProjectPropertiesComponent.class)
                    .setValue(DEFAULT_SELECTION, directory.getVirtualFile().getPath());
            }
        }
        super.doOKAction();
    }

    @Override
    @RequiredUIAccess
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new FilterExistentAction());
        JComponent toolbarComponent =
            ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, actionGroup, true).getComponent();
        toolbarComponent.setBorder(null);
        panel.add(toolbarComponent, BorderLayout.NORTH);

        myView.onSelectionChange(this::enableButtons);
        JComponent component = myView.getComponent();
        JScrollPane jScrollPane = ScrollPaneFactory.createScrollPane(component);
        //noinspection HardCodedStringLiteral
        int prototypeWidth =
            component.getFontMetrics(component.getFont()).stringWidth("X:\\1234567890\\1234567890\\com\\company\\system\\subsystem");
        jScrollPane.setPreferredSize(new Dimension(Math.max(300, prototypeWidth), 300));

        installEnterAction(component);
        panel.add(jScrollPane, BorderLayout.CENTER);
        myTabbedPaneWrapper.addTab("Directory Structure", panel);

        myChooseByNamePanel.invoke(
            new ChooseByNamePopupComponent.Callback() {
                @Override
                public void elementChosen(Object element) {
                    setSelection(element);
                }
            },
            IdeaModalityState.stateForComponent(getRootPane()),
            false
        );
        myTabbedPaneWrapper.addTab("Choose By Neighbor Class", myChooseByNamePanel.getPanel());

        return myTabbedPaneWrapper.getComponent();
    }

    private void setSelection(Object element) {
        if (element instanceof PsiElement psiElement) {
            mySelection = psiElement;
        }
    }

    private void installEnterAction(JComponent component) {
        KeyStroke enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        InputMap inputMap = component.getInputMap();
        ActionMap actionMap = component.getActionMap();
        Object oldActionKey = inputMap.get(enterKeyStroke);
        final Action oldAction = oldActionKey != null ? actionMap.get(oldActionKey) : null;
        inputMap.put(enterKeyStroke, "clickButton");
        actionMap.put(
            "clickButton",
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (isOKActionEnabled()) {
                        doOKAction();
                    }
                    else if (oldAction != null) {
                        oldAction.actionPerformed(e);
                    }
                }
            }
        );
    }

    @Override
    protected String getDimensionServiceKey() {
        return "chooseDestDirectoryDialog";
    }

    private void buildFragments() {
        List<String[]> pathes = new ArrayList<>();
        for (int i = 0; i < myView.getItemsSize(); i++) {
            ItemWrapper item = myView.getItemByIndex(i);
            pathes.add(ArrayUtil.toStringArray(FileUtil.splitPath(item.getPresentableUrl())));
        }
        FragmentBuilder headBuilder = new FragmentBuilder(pathes) {
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
        FragmentBuilder tailBuilder = new FragmentBuilder(pathes) {
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
        if (strings.length <= headLimit + tailLimit) {
            return null;
        }
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
            if (special != null || tail != null) {
                list.add(new PathFragment(head + File.separatorChar, true));
            }
            else {
                return new PathFragment[]{new PathFragment(head, true)};
            }
        }
        if (special != null) {
            if (tail != null) {
                list.add(new PathFragment(special + File.separatorChar, false));
            }
            else {
                list.add(new PathFragment(special, false));
            }
        }
        if (tail != null) {
            list.add(new PathFragment(tail, true));
        }
        return list.toArray(new PathFragment[list.size()]);
    }

    private static abstract class FragmentBuilder {
        private final List<String[]> myPaths;
        private final StringBuffer myBuffer = new StringBuffer();
        private int myIndex;
        protected String mySeparator = "";

        public FragmentBuilder(List<String[]> pathes) {
            myPaths = pathes;
            myIndex = 0;
        }

        public int getIndex() {
            return myIndex;
        }

        @Nullable
        public String execute() {
            while (true) {
                String commonHead = getCommonFragment(myIndex);
                if (commonHead == null) {
                    break;
                }
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
                if (index == -1) {
                    return null;
                }
                if (commonFragment == null) {
                    commonFragment = path[index];
                    continue;
                }
                if (!Comparing.strEqual(commonFragment, path[index], Platform.current().fs().isCaseSensitive())) {
                    return null;
                }
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

        public PathFragment[] getFragments() {
            return myFragments;
        }

        public void setFragments(PathFragment[] fragments) {
            myFragments = fragments;
        }

        @Deprecated
        @DeprecationInfo(value = "Use #getIcon()")
        @Nonnull
        @RequiredUIAccess
        public Image getIcon(@Nonnull ProjectFileIndex fileIndex) {
            return getIcon();
        }

        @Nonnull
        @RequiredUIAccess
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
            return PlatformIconGroup.nodesFolder();
        }

        public String getPresentableUrl() {
            String directoryUrl;
            if (myDirectory != null) {
                directoryUrl = myDirectory.getVirtualFile().getPresentableUrl();
                VirtualFile baseDir = myDirectory.getProject().getBaseDir();
                if (baseDir != null) {
                    String projectHomeUrl = baseDir.getPresentableUrl();
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
                PsiDirectory directory = getDirectory();
                VirtualFile virtualFile = directory != null ? directory.getVirtualFile() : null;
                myRelativeToProjectPath = virtualFile != null
                    ? ProjectUtil.calcRelativeToProjectPath(virtualFile, directory.getProject(), true, false, true)
                    : getPresentableUrl();
            }
            return myRelativeToProjectPath;
        }
    }

    @Override
    @RequiredUIAccess
    public JComponent getPreferredFocusedComponent() {
        return myView.getComponent();
    }

    @Override
    @RequiredReadAction
    public void fillList(PsiDirectory[] directories, @Nullable PsiDirectory defaultSelection, Project project, String postfixToShow) {
        fillList(directories, defaultSelection, project, postfixToShow, null);
    }

    @Override
    @RequiredReadAction
    public void fillList(
        PsiDirectory[] directories,
        @Nullable PsiDirectory defaultSelection,
        Project project,
        Map<PsiDirectory, String> postfixes
    ) {
        fillList(directories, defaultSelection, project, null, postfixes);
    }

    @RequiredReadAction
    private void fillList(
        PsiDirectory[] directories,
        @Nullable PsiDirectory defaultSelection,
        Project project,
        String postfixToShow,
        Map<PsiDirectory, String> postfixes
    ) {
        if (myView.getItemsSize() > 0) {
            myView.clearItems();
        }
        if (defaultSelection == null) {
            defaultSelection = getDefaultSelection(directories, project);
            if (defaultSelection == null && directories.length > 0) {
                defaultSelection = directories[0];
            }
        }
        int selectionIndex = -1;
        for (int i = 0; i < directories.length; i++) {
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
            PsiManager manager = PsiManager.getInstance(project);
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
        for (int i = 0; i < directories.length; i++) {
            PsiDirectory directory = directories[i];
            String postfixForDirectory;
            if (postfixes == null) {
                postfixForDirectory = postfixToShow;
            }
            else {
                postfixForDirectory = postfixes.get(directory);
            }
            ItemWrapper itemWrapper = new ItemWrapper(directory, postfixForDirectory);
            myItems.add(itemWrapper);
            if (myFilterExisting) {
                if (selectionIndex == i) {
                    selectionIndex = -1;
                }
                if (postfixForDirectory != null && directory.getVirtualFile()
                    .findFileByRelativePath(StringUtil.trimStart(postfixForDirectory, File.separator)) == null) {
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
            }
            else {
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
        String defaultSelectionPath = project.getInstance(ProjectPropertiesComponent.class).getValue(DEFAULT_SELECTION);
        if (defaultSelectionPath != null) {
            VirtualFile directoryByDefault = LocalFileSystem.getInstance().findFileByPath(defaultSelectionPath);
            if (directoryByDefault != null) {
                PsiDirectory directory = PsiManager.getInstance(project).findDirectory(directoryByDefault);
                return directory != null && ArrayUtil.find(directories, directory) > -1 ? directory : null;
            }
        }
        return null;
    }

    private static boolean isParent(PsiDirectory directory, PsiDirectory parentCandidate) {
        while (directory != null) {
            if (directory.equals(parentCandidate)) {
                return true;
            }
            directory = directory.getParentDirectory();
        }
        return false;
    }

    private void enableButtons() {
        setOKActionEnabled(myView.getSelectedItem() != null);
    }

    @Nullable
    @Override
    public PsiDirectory getSelectedDirectory() {
        if (mySelection != null) {
            PsiFile file = mySelection.getContainingFile();
            if (file != null) {
                return file.getContainingDirectory();
            }
        }
        ItemWrapper wrapper = myView.getSelectedItem();
        if (wrapper == null) {
            return null;
        }
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
                RefactoringLocalize.directoryChooserHideNonExistentCheckboxText(),
                RefactoringLocalize.directoryChooserHideNonExistentCheckboxText().map(Presentation.NO_MNEMONIC),
                PlatformIconGroup.generalFilter()
            );
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return myFilterExisting;
        }

        @Override
        @RequiredUIAccess
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            myFilterExisting = state;
            ItemWrapper selectedItem = myView.getSelectedItem();
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
                        item.getDirectory()
                            .getVirtualFile()
                            .findFileByRelativePath(StringUtil.trimStart(item.myPostfix, File.separator)) == null) {
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
