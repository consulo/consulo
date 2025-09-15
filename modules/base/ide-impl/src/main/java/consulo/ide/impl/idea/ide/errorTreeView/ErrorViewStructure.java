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
package consulo.ide.impl.idea.ide.errorTreeView;

import consulo.application.Application;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.ide.impl.idea.ide.errorTreeView.impl.ErrorTreeViewConfiguration;
import consulo.ide.impl.idea.ui.CustomizeColoredTreeCellRenderer;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.errorTreeView.ErrorTreeElementKind;
import consulo.ui.ex.errorTreeView.HotfixData;
import consulo.ui.ex.errorTreeView.MutableErrorTreeView;
import consulo.ui.ex.errorTreeView.SimpleErrorData;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.*;

/**
 * @author Eugene Zhuravlev
 * @since 2004-11-12
 */
public class ErrorViewStructure extends AbstractTreeStructure {
    private final ErrorTreeElement myRoot = new MyRootElement();

    private final List<String> myGroupNames = new ArrayList<>();
    private final Map<String, GroupingElement> myGroupNameToElementMap = new HashMap<>();
    private final Map<String, List<NavigatableMessageElement>> myGroupNameToMessagesMap = new HashMap<>();
    private final Map<ErrorTreeElementKind, List<ErrorTreeElement>> mySimpleMessages = new EnumMap<>(ErrorTreeElementKind.class);
    private final Object myLock = new Object();

    private static final ErrorTreeElementKind[] ourMessagesOrder =
        {ErrorTreeElementKind.INFO, ErrorTreeElementKind.ERROR, ErrorTreeElementKind.WARNING, ErrorTreeElementKind.NOTE, ErrorTreeElementKind.GENERIC};
    private final Project myProject;
    private final ErrorTreeViewConfiguration myConfiguration;

    public ErrorViewStructure(Project project, boolean canHideInfosOrWarnings) {
        myProject = project;
        myConfiguration = canHideInfosOrWarnings ? ErrorTreeViewConfiguration.getInstance(project) : null;
    }

    @Nonnull
    @Override
    public Object getRootElement() {
        return myRoot;
    }

    @Nonnull
    @Override
    public ErrorTreeElement[] getChildElements(@Nonnull Object element) {
        if (element == myRoot) {
            List<ErrorTreeElement> children = new ArrayList<>();
            // simple messages
            synchronized (myLock) {
                for (ErrorTreeElementKind kind : ourMessagesOrder) {
                    if (!canShowKind(kind)) {
                        continue;
                    }
                    List<ErrorTreeElement> elems = mySimpleMessages.get(kind);
                    if (elems != null) {
                        children.addAll(elems);
                    }
                }
                // files
                for (String myGroupName : myGroupNames) {
                    GroupingElement groupingElement = myGroupNameToElementMap.get(myGroupName);
                    if (shouldShowFileElement(groupingElement)) {
                        children.add(groupingElement);
                    }
                }
            }
            return ArrayUtil.toObjectArray(children, ErrorTreeElement.class);
        }

        if (element instanceof GroupingElement) {
            synchronized (myLock) {
                List<NavigatableMessageElement> children = myGroupNameToMessagesMap.get(((GroupingElement) element).getName());
                if (children != null && !children.isEmpty()) {
                    if (myConfiguration != null) {
                        List<ErrorTreeElement> filtered = new ArrayList<>(children.size());
                        for (NavigatableMessageElement navigatableMessageElement : children) {
                            ErrorTreeElementKind kind = navigatableMessageElement.getKind();
                            if (!canShowKind(kind)) {
                                continue;
                            }
                            filtered.add(navigatableMessageElement);
                        }
                        return ArrayUtil.toObjectArray(filtered, ErrorTreeElement.class);
                    }
                    return ArrayUtil.toObjectArray(children, NavigatableMessageElement.class);
                }
            }
        }

        return ErrorTreeElement.EMPTY_ARRAY;
    }

    private boolean shouldShowFileElement(GroupingElement groupingElement) {
        if (myConfiguration == null) {
            return getChildCount(groupingElement) > 0;
        }
        synchronized (myLock) {
            List<NavigatableMessageElement> children = myGroupNameToMessagesMap.get(groupingElement.getName());
            if (children != null) {
                for (NavigatableMessageElement child : children) {
                    ErrorTreeElementKind kind = child.getKind();
                    if (canShowKind(kind)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public Object getParentElement(@Nonnull Object element) {
        if (element instanceof GroupingElement || element instanceof SimpleMessageElement) {
            return myRoot;
        }
        if (element instanceof NavigatableMessageElement) {
            GroupingElement result = ((NavigatableMessageElement) element).getParent();
            return result == null ? myRoot : result;
        }
        return null;
    }

    @Override
    @Nonnull
    public NodeDescriptor createDescriptor(@Nonnull Object element, NodeDescriptor parentDescriptor) {
        return new ErrorTreeNodeDescriptor(myProject, parentDescriptor, (ErrorTreeElement) element);
    }

    @Override
    public final void commit() {
    }

    @Override
    public final boolean hasSomethingToCommit() {
        return false;
    }

    public void addMessage(
        @Nonnull ErrorTreeElementKind kind,
        @Nonnull String[] text,
        @Nullable VirtualFile underFileGroup,
        @Nullable VirtualFile file,
        int line,
        int column,
        @Nullable Object data
    ) {
        if (underFileGroup != null || file != null) {
            if (file == null) {
                line = column = -1;
            }

            int guiline = line < 0 ? -1 : line + 1;
            int guicolumn = column < 0 ? -1 : column + 1;

            VirtualFile group = underFileGroup != null ? underFileGroup : file;
            VirtualFile nav = file != null ? file : underFileGroup;

            addNavigatableMessage(
                group.getPresentableUrl(),
                new OpenFileDescriptorImpl(myProject, nav, line, column),
                kind,
                text,
                data,
                NewErrorTreeViewPanelImpl.createExportPrefix(guiline),
                NewErrorTreeViewPanelImpl.createRendererPrefix(guiline, guicolumn),
                group
            );
        }
        else {
            addSimpleMessage(kind, text, data);
        }
    }

    public List<Object> getGroupChildrenData(String groupName) {
        synchronized (myLock) {
            List<NavigatableMessageElement> children = myGroupNameToMessagesMap.get(groupName);
            if (children == null || children.isEmpty()) {
                return Collections.emptyList();
            }
            List<Object> result = new ArrayList<>();
            for (NavigatableMessageElement child : children) {
                Object data = child.getData();
                if (data != null) {
                    result.add(data);
                }
            }
            return result;
        }
    }

    public void addFixedHotfixGroup(String text, List<SimpleErrorData> children) {
        FixedHotfixGroupElement group = new FixedHotfixGroupElement(text, null, null);

        addGroupPlusElements(text, group, children);
    }

    public void addHotfixGroup(HotfixData hotfixData, List<SimpleErrorData> children, MutableErrorTreeView view) {
        String text = hotfixData.getErrorText();
        HotfixGroupElement group = new HotfixGroupElement(text, null, null, hotfixData.getFix(), hotfixData.getFixComment(), view);

        addGroupPlusElements(text, group, children);
    }

    private void addGroupPlusElements(String text, GroupingElement group, List<SimpleErrorData> children) {
        List<NavigatableMessageElement> elements = new ArrayList<>();
        for (SimpleErrorData child : children) {
            elements.add(new MyNavigatableWithDataElement(
                myProject,
                child.getKind(),
                group,
                child.getMessages(),
                child.getVf(),
                NewErrorTreeViewPanelImpl.createExportPrefix(-1),
                NewErrorTreeViewPanelImpl.createRendererPrefix(-1, -1)
            ));
        }

        synchronized (myLock) {
            myGroupNames.add(text);
            myGroupNameToElementMap.put(text, group);
            myGroupNameToMessagesMap.put(text, elements);
        }
    }

    public void addMessage(@Nonnull ErrorTreeElementKind kind, String[] text, Object data) {
        addSimpleMessage(kind, text, data);
    }

    public void addNavigatableMessage(
        @Nullable String groupName,
        Navigatable navigatable,
        @Nonnull ErrorTreeElementKind kind,
        String[] message,
        Object data,
        String exportText,
        String rendererTextPrefix,
        VirtualFile file
    ) {
        if (groupName == null) {
            addSimpleMessageElement(new NavigatableMessageElement(kind, null, message, navigatable, exportText, rendererTextPrefix));
        }
        else {
            synchronized (myLock) {
                List<NavigatableMessageElement> elements = myGroupNameToMessagesMap.get(groupName);
                if (elements == null) {
                    elements = new ArrayList<>();
                    myGroupNameToMessagesMap.put(groupName, elements);
                }
                elements.add(new NavigatableMessageElement(
                    kind,
                    getGroupingElement(groupName, data, file),
                    message,
                    navigatable,
                    exportText,
                    rendererTextPrefix
                ));
            }
        }
    }

    public void addNavigatableMessage(@Nonnull String groupName, @Nonnull NavigatableMessageElement navigatableMessageElement) {
        synchronized (myLock) {
            List<NavigatableMessageElement> elements = myGroupNameToMessagesMap.get(groupName);
            if (elements == null) {
                elements = new ArrayList<>();
                myGroupNameToMessagesMap.put(groupName, elements);
            }
            if (!myGroupNameToElementMap.containsKey(groupName)) {
                myGroupNames.add(groupName);
                myGroupNameToElementMap.put(groupName, navigatableMessageElement.getParent());
            }
            elements.add(navigatableMessageElement);
        }
    }

    private void addSimpleMessage(@Nonnull ErrorTreeElementKind kind, String[] text, Object data) {
        addSimpleMessageElement(new SimpleMessageElement(kind, text, data));
    }

    private void addSimpleMessageElement(ErrorTreeElement element) {
        synchronized (myLock) {
            List<ErrorTreeElement> elements = mySimpleMessages.get(element.getKind());
            if (elements == null) {
                elements = new ArrayList<>();
                mySimpleMessages.put(element.getKind(), elements);
            }
            elements.add(element);
        }
    }

    @Nullable
    public GroupingElement lookupGroupingElement(String groupName) {
        synchronized (myLock) {
            return myGroupNameToElementMap.get(groupName);
        }
    }

    public GroupingElement getGroupingElement(String groupName, Object data, VirtualFile file) {
        synchronized (myLock) {
            GroupingElement element = myGroupNameToElementMap.get(groupName);
            if (element != null) {
                return element;
            }
            element = new GroupingElement(groupName, data, file);
            myGroupNames.add(groupName);
            myGroupNameToElementMap.put(groupName, element);
            return element;
        }
    }

    public int getChildCount(GroupingElement groupingElement) {
        synchronized (myLock) {
            List<NavigatableMessageElement> children = myGroupNameToMessagesMap.get(groupingElement.getName());
            return children == null ? 0 : children.size();
        }
    }

    public void clear() {
        synchronized (myLock) {
            myGroupNames.clear();
            myGroupNameToElementMap.clear();
            myGroupNameToMessagesMap.clear();
            mySimpleMessages.clear();
        }
    }

    @Nullable
    public ErrorTreeElement getFirstMessage(@Nonnull ErrorTreeElementKind kind) {
        if (!canShowKind(kind)) {
            return null; // no warnings are available
        }
        synchronized (myLock) {
            List<ErrorTreeElement> simpleMessages = mySimpleMessages.get(kind);
            if (simpleMessages != null && !simpleMessages.isEmpty()) {
                return simpleMessages.get(0);
            }
            for (String path : myGroupNames) {
                List<NavigatableMessageElement> messages = myGroupNameToMessagesMap.get(path);
                if (messages != null) {
                    for (NavigatableMessageElement navigatableMessageElement : messages) {
                        if (kind.equals(navigatableMessageElement.getKind())) {
                            return navigatableMessageElement;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean canShowKind(@Nonnull ErrorTreeElementKind kind) {
        if (myConfiguration == null) {
            return true;
        }
        if (ErrorTreeElementKind.WARNING.equals(kind) || ErrorTreeElementKind.NOTE.equals(kind)) {
            return myConfiguration.SHOW_WARNINGS;
        }
        if (ErrorTreeElementKind.INFO.equals(kind)) {
            return myConfiguration.SHOW_INFOS;
        }
        return true;
    }

    private static class MyRootElement extends ErrorTreeElement {
        @Override
        public String[] getText() {
            return null;
        }

        @Override
        public Object getData() {
            return null;
        }

        @Override
        public String getExportTextPrefix() {
            return "";
        }
    }

    public void removeGroup(String name) {
        synchronized (myLock) {
            myGroupNames.remove(name);
            myGroupNameToElementMap.remove(name);
            myGroupNameToMessagesMap.remove(name);
        }
    }

    public void removeElement(ErrorTreeElement element) {
        if (element == myRoot) {
            return;
        }
        if (element instanceof GroupingElement groupingElement) {
            removeGroup(groupingElement.getName());
            VirtualFile virtualFile = groupingElement.getFile();
            if (virtualFile != null) {
                Application.get().runReadAction(() -> {
                    PsiFile psiFile = virtualFile.isValid() ? PsiManager.getInstance(myProject).findFile(virtualFile) : null;
                    if (psiFile != null) {
                        // urge the daemon to re-highlight the file despite no modification has been made
                        DaemonCodeAnalyzer.getInstance(myProject).restart(psiFile);
                    }
                });
            }
        }
        else if (element instanceof NavigatableMessageElement) {
            NavigatableMessageElement navElement = (NavigatableMessageElement) element;
            GroupingElement parent = navElement.getParent();
            if (parent != null) {
                synchronized (myLock) {
                    List<NavigatableMessageElement> groupMessages = myGroupNameToMessagesMap.get(parent.getName());
                    if (groupMessages != null) {
                        groupMessages.remove(navElement);
                    }
                }
            }
        }
        else {
            synchronized (myLock) {
                List<ErrorTreeElement> simples = mySimpleMessages.get(element.getKind());
                if (simples != null) {
                    simples.remove(element);
                }
            }
        }
    }

    private static class MyNavigatableWithDataElement extends NavigatableMessageElement {
        private final VirtualFile myVf;
        private final CustomizeColoredTreeCellRenderer myCustomizeColoredTreeCellRenderer;

        private MyNavigatableWithDataElement(
            Project project,
            @Nonnull ErrorTreeElementKind kind,
            GroupingElement parent,
            String[] message,
            @Nonnull final VirtualFile vf,
            String exportText,
            String rendererTextPrefix
        ) {
            super(kind, parent, message, new OpenFileDescriptorImpl(project, vf, -1, -1), exportText, rendererTextPrefix);
            myVf = vf;
            myCustomizeColoredTreeCellRenderer = new CustomizeColoredTreeCellRenderer() {
                @Override
                public void customizeCellRenderer(
                    SimpleColoredComponent renderer,
                    JTree tree,
                    Object value,
                    boolean selected,
                    boolean expanded,
                    boolean leaf,
                    int row,
                    boolean hasFocus
                ) {
                    Image icon = myVf.getFileType().getIcon();
                    renderer.setIcon(icon);
                    String[] messages = getText();
                    String text = messages == null || messages.length == 0 ? vf.getPath() : messages[0];
                    renderer.append(text);
                }
            };
        }

        @Override
        public Object getData() {
            return myVf;
        }

        @Override
        public CustomizeColoredTreeCellRenderer getLeftSelfRenderer() {
            return myCustomizeColoredTreeCellRenderer;
        }
    }
}
