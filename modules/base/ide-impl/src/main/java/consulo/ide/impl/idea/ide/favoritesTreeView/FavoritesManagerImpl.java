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

package consulo.ide.impl.idea.ide.favoritesTreeView;

import consulo.annotation.component.ServiceImpl;
import consulo.bookmark.ui.view.*;
import consulo.bookmark.ui.view.event.FavoritesListener;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.content.ContentIterator;
import consulo.content.base.BinariesOrderRootType;
import consulo.disposer.Disposable;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.ide.favoritesTreeView.actions.AddToFavoritesAction;
import consulo.ide.impl.idea.ide.projectView.impl.*;
import consulo.ide.impl.idea.util.EventDispatcher;
import consulo.ide.impl.idea.util.containers.Convertor;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.*;
import consulo.language.psi.event.PsiTreeChangeAdapter;
import consulo.language.psi.event.PsiTreeChangeEvent;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.project.ui.view.internal.AbstractUrl;
import consulo.project.ui.view.internal.node.LibraryGroupElement;
import consulo.project.ui.view.internal.node.NamedLibraryElement;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.view.tree.ModuleGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.InputValidator;
import consulo.ui.ex.awt.Messages;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.TreeItem;
import consulo.util.xml.serializer.DefaultJDOMExternalizer;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import java.util.*;
import java.util.function.Consumer;

@Singleton
@ServiceImpl
@State(name = "FavoritesManager", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class FavoritesManagerImpl implements FavoritesManager, PersistentStateComponent<Element> {
    private final ArrayList<String> myListOrder = new ArrayList<>();
    // fav list name -> list of (root: root url, root class)
    private final Map<String, List<TreeItem<Pair<AbstractUrl, String>>>> myName2FavoritesRoots =
        new TreeMap<>((o1, o2) -> myListOrder.indexOf(o1) - myListOrder.indexOf(o2));
    private final Map<String, String> myDescriptions = new HashMap<>();
    private final Project myProject;
    private final EventDispatcher<FavoritesListener> myListeners = EventDispatcher.create(FavoritesListener.class);
    private final FavoritesViewSettingsImpl myViewSettings = new FavoritesViewSettingsImpl();

    @Nullable
    private Map<String, FavoritesListProvider> myProviders;

    private void rootsChanged() {
        myListeners.getMulticaster().rootsChanged();
    }

    private void listAdded(String listName) {
        myListeners.getMulticaster().listAdded(listName);
    }

    private void listRemoved(String listName) {
        myListeners.getMulticaster().listRemoved(listName);
    }

    public void renameList(final Project project, @Nonnull String listName) {
        final String newName = Messages.showInputDialog(
            project,
            IdeBundle.message("prompt.input.favorites.list.new.name", listName),
            IdeBundle.message("title.rename.favorites.list"),
            Messages.getInformationIcon(),
            listName,
            new InputValidator() {
                @RequiredUIAccess
                @Override
                public boolean checkInput(String inputString) {
                    return inputString != null && inputString.trim().length() > 0;
                }

                @RequiredUIAccess
                @Override
                public boolean canClose(String inputString) {
                    inputString = inputString.trim();
                    if (myName2FavoritesRoots.keySet().contains(inputString) || getProviders().keySet()
                        .contains(inputString)) {
                        Messages.showErrorDialog(
                            project,
                            IdeBundle.message("error.favorites.list.already.exists", inputString.trim()),
                            IdeBundle.message("title.unable.to.add.favorites.list")
                        );
                        return false;
                    }
                    return !inputString.isEmpty();
                }
            }
        );

        if (newName != null && renameFavoritesList(listName, newName)) {
            rootsChanged();
        }
    }

    @Override
    @Deprecated
    public void addFavoritesListener(FavoritesListener listener) {
        myListeners.addListener(listener);
        listener.rootsChanged();
    }

    @Override
    public void addFavoritesListener(final FavoritesListener listener, @Nonnull Disposable parent) {
        myListeners.addListener(listener, parent);
        listener.rootsChanged();
    }

    @Override
    @Deprecated
    public void removeFavoritesListener(FavoritesListener listener) {
        myListeners.removeListener(listener);
    }

    List<AbstractTreeNode> createRootNodes() {
        List<AbstractTreeNode> result = new ArrayList<>();
        for (String listName : myName2FavoritesRoots.keySet()) {
            result.add(new FavoritesListNode(myProject, listName, myDescriptions.get(listName)));
        }
        ArrayList<FavoritesListProvider> providers = new ArrayList<>(getProviders().values());
        Collections.sort(providers);
        for (FavoritesListProvider provider : providers) {
            FavoritesListNode node = provider.createFavoriteListNode(myProject);
            if (node != null) {
                result.add(node);
            }
        }
        return result;
    }

    public static FavoritesManagerImpl getInstance(Project project) {
        return (FavoritesManagerImpl)project.getInstance(FavoritesManager.class);
    }

    @Inject
    public FavoritesManagerImpl(Project project) {
        myProject = project;
    }

    @Nonnull
    private Map<String, FavoritesListProvider> getProviders() {
        if (myProviders != null) {
            return myProviders;
        }

        myProviders = new HashMap<>();
        for (FavoritesListProvider provider : FavoritesListProvider.EP_NAME.getExtensionList(myProject)) {
            myProviders.put(provider.getListName(myProject), provider);
        }
        final MyRootsChangeAdapter myPsiTreeChangeAdapter = new MyRootsChangeAdapter();

        PsiManager.getInstance(myProject).addPsiTreeChangeListener(myPsiTreeChangeAdapter, myProject);
        if (myName2FavoritesRoots.isEmpty()) {
            myDescriptions.put(myProject.getName(), "auto-added");
            createNewList(myProject.getName());
        }
        return myProviders;
    }

    @Nonnull
    public List<String> getAvailableFavoritesListNames() {
        return new ArrayList<>(myName2FavoritesRoots.keySet());
    }

    public synchronized void createNewList(@Nonnull String listName) {
        myListOrder.add(listName);
        myName2FavoritesRoots.put(listName, new ArrayList<>());
        listAdded(listName);
    }

    public synchronized void fireListeners(@Nonnull final String listName) {
        rootsChanged();
    }

    @Override
    public FavoritesViewSettingsImpl getViewSettings() {
        return myViewSettings;
    }

    public synchronized boolean removeFavoritesList(@Nonnull String name) {
        boolean result = myName2FavoritesRoots.remove(name) != null;
        myListOrder.remove(name);
        myDescriptions.remove(name);
        listRemoved(name);
        return result;
    }

    @Override
    @Nonnull
    public List<TreeItem<Pair<AbstractUrl, String>>> getFavoritesListRootUrls(@Nonnull String name) {
        final List<TreeItem<Pair<AbstractUrl, String>>> pairs = myName2FavoritesRoots.get(name);
        return pairs == null ? new ArrayList<>() : pairs;
    }

    public synchronized boolean addRoots(@Nonnull String name, Module moduleContext, @Nonnull Object elements) {
        Collection<AbstractTreeNode> nodes = AddToFavoritesAction.createNodes(myProject, moduleContext, elements, true, getViewSettings());
        return !nodes.isEmpty() && addRoots(name, nodes);
    }

    public synchronized Comparator<FavoritesTreeNodeDescriptor> getCustomComparator(@Nonnull final String name) {
        return getProviders().get(name);
    }

    private Pair<AbstractUrl, String> createPairForNode(AbstractTreeNode node) {
        final String className = node.getClass().getName();
        final Object value = node.getValue();
        final AbstractUrl url = createUrlByElement(value, myProject);
        if (url == null) {
            return null;
        }
        return Pair.create(url, className);
    }

    public boolean addRoots(final String name, final Collection<AbstractTreeNode> nodes) {
        final Collection<TreeItem<Pair<AbstractUrl, String>>> list = getFavoritesListRootUrls(name);

        final HashSet<AbstractUrl> set = new HashSet<>(ContainerUtil.map(list, item -> item.getData().getFirst()));
        for (AbstractTreeNode node : nodes) {
            final Pair<AbstractUrl, String> pair = createPairForNode(node);
            if (pair != null) {
                if (set.contains(pair.getFirst())) {
                    continue;
                }
                final TreeItem<Pair<AbstractUrl, String>> treeItem = new TreeItem<>(pair);
                list.add(treeItem);
                set.add(pair.getFirst());
                appendChildNodes(node, treeItem);
            }
        }
        rootsChanged();
        return true;
    }

    private void appendChildNodes(AbstractTreeNode node, TreeItem<Pair<AbstractUrl, String>> treeItem) {
        final Collection<? extends AbstractTreeNode> children = node.getChildren();
        for (AbstractTreeNode child : children) {
            final TreeItem<Pair<AbstractUrl, String>> childTreeItem = new TreeItem<>(createPairForNode(child));
            treeItem.addChild(childTreeItem);
            appendChildNodes(child, childTreeItem);
        }
    }

    public synchronized boolean addRoot(
        @Nonnull String name,
        @Nonnull List<AbstractTreeNode> parentElements,
        final AbstractTreeNode newElement,
        @Nullable AbstractTreeNode sibling
    ) {
        final List<TreeItem<Pair<AbstractUrl, String>>> items = myName2FavoritesRoots.get(name);
        if (items == null) {
            return false;
        }
        AbstractUrl url = createUrlByElement(newElement.getValue(), myProject);
        if (url == null) {
            return false;
        }
        final TreeItem<Pair<AbstractUrl, String>> newItem = new TreeItem<>(Pair.create(url, newElement.getClass().getName()));

        if (parentElements.isEmpty()) {
            // directly to list
            if (sibling != null) {
                TreeItem<Pair<AbstractUrl, String>> after = null;
                AbstractUrl siblingUrl = createUrlByElement(sibling.getValue(), myProject);
                int idx = -1;
                for (int i = 0; i < items.size(); i++) {
                    TreeItem<Pair<AbstractUrl, String>> item = items.get(i);
                    if (item.getData().getFirst().equals(siblingUrl)) {
                        idx = i;
                        break;
                    }
                }
                if (idx != -1) {
                    items.add(idx, newItem);
                }
                else {
                    items.add(newItem);
                }
            }
            else {
                items.add(newItem);
            }

            rootsChanged();
            return true;
        }

        Collection<TreeItem<Pair<AbstractUrl, String>>> list = items;
        TreeItem<Pair<AbstractUrl, String>> item = null;
        for (AbstractTreeNode obj : parentElements) {
            AbstractUrl objUrl = createUrlByElement(obj.getValue(), myProject);
            item = findNextItem(objUrl, list);
            if (item == null) {
                return false;
            }
            list = item.getChildren();
        }

        if (sibling != null) {
            TreeItem<Pair<AbstractUrl, String>> after = null;
            AbstractUrl siblingUrl = createUrlByElement(sibling.getValue(), myProject);
            for (TreeItem<Pair<AbstractUrl, String>> treeItem : list) {
                if (treeItem.getData().getFirst().equals(siblingUrl)) {
                    after = treeItem;
                    break;
                }
            }
            if (after == null) {
                item.addChild(newItem);
            }
            else {
                item.addChildAfter(newItem, after);
            }
        }
        else {
            item.addChild(newItem);
        }
        rootsChanged();
        return true;
    }

    private <T> boolean findListToRemoveFrom(
        @Nonnull String name,
        @Nonnull final List<T> elements,
        final Convertor<T, AbstractUrl> convertor
    ) {
        Collection<TreeItem<Pair<AbstractUrl, String>>> list = getFavoritesListRootUrls(name);
        if (elements.size() > 1) {
            final List<T> sublist = elements.subList(0, elements.size() - 1);
            for (T obj : sublist) {
                AbstractUrl objUrl = convertor.convert(obj);
                final TreeItem<Pair<AbstractUrl, String>> item = findNextItem(objUrl, list);
                if (item == null || item.getChildren() == null) {
                    return false;
                }
                list = item.getChildren();
            }
        }

        TreeItem<Pair<AbstractUrl, String>> found = null;
        AbstractUrl url = convertor.convert(elements.get(elements.size() - 1));
        if (url == null) {
            return false;
        }
        for (TreeItem<Pair<AbstractUrl, String>> pair : list) {
            if (url.equals(pair.getData().getFirst())) {
                found = pair;
                break;
            }
        }

        if (found != null) {
            list.remove(found);
            rootsChanged();
            return true;
        }
        return false;
    }

    public synchronized boolean removeRoot(@Nonnull String name, @Nonnull List<AbstractTreeNode> elements) {
        final Convertor<AbstractTreeNode, AbstractUrl> convertor = obj -> createUrlByElement(obj.getValue(), myProject);
        boolean result = true;
        for (AbstractTreeNode element : elements) {
            final List<AbstractTreeNode> path = TaskDefaultFavoriteListProvider.getPathToUsualNode(element);
            result &= findListToRemoveFrom(name, path.subList(1, path.size()), convertor);
        }
        return result;
    }

    private TreeItem<Pair<AbstractUrl, String>> findNextItem(AbstractUrl url, Collection<TreeItem<Pair<AbstractUrl, String>>> list) {
        for (TreeItem<Pair<AbstractUrl, String>> pair : list) {
            if (url.equals(pair.getData().getFirst())) {
                return pair;
            }
        }
        return null;
    }

    private boolean renameFavoritesList(@Nonnull String oldName, @Nonnull String newName) {
        List<TreeItem<Pair<AbstractUrl, String>>> list = myName2FavoritesRoots.remove(oldName);
        if (list != null && newName.length() > 0) {
            int index = myListOrder.indexOf(oldName);
            if (index == -1) {
                index = myListOrder.size();
            }
            myListOrder.set(index, newName);
            myName2FavoritesRoots.put(newName, list);
            String description = myDescriptions.remove(oldName);
            if (description != null) {
                myDescriptions.put(newName, description);
            }
            rootsChanged();
            return true;
        }
        return false;
    }

    @Nullable
    public FavoritesListProvider getListProvider(@Nullable String name) {
        return getProviders().get(name);
    }

    @Override
    public void loadState(Element element) {
        myName2FavoritesRoots.clear();
        for (Object list : element.getChildren(ELEMENT_FAVORITES_LIST)) {
            final String name = ((Element)list).getAttributeValue(ATTRIBUTE_NAME);
            List<TreeItem<Pair<AbstractUrl, String>>> roots = readRoots((Element)list, myProject);
            myListOrder.add(name);
            myName2FavoritesRoots.put(name, roots);
        }
        DefaultJDOMExternalizer.readExternal(this, element);
    }

    @NonNls
    private static final String CLASS_NAME = "klass";
    private static final String FAVORITES_ROOT = "favorite_root";
    private static final String ELEMENT_FAVORITES_LIST = "favorites_list";
    private static final String ATTRIBUTE_NAME = "name";

    private static List<TreeItem<Pair<AbstractUrl, String>>> readRoots(final Element list, Project project) {
        List<TreeItem<Pair<AbstractUrl, String>>> result = new ArrayList<>();
        readFavoritesOneLevel(list, project, result);
        return result;
    }

    private static void readFavoritesOneLevel(Element list, Project project, Collection<TreeItem<Pair<AbstractUrl, String>>> result) {
        final List listChildren = list.getChildren(FAVORITES_ROOT);
        if (listChildren == null || listChildren.isEmpty()) {
            return;
        }

        for (Object favorite : listChildren) {
            final Element favoriteElement = (Element)favorite;
            final String className = favoriteElement.getAttributeValue(CLASS_NAME);
            final AbstractUrl abstractUrl = readUrlFromElement(favoriteElement, project);
            if (abstractUrl != null) {
                final TreeItem<Pair<AbstractUrl, String>> treeItem = new TreeItem<>(Pair.create(abstractUrl, className));
                result.add(treeItem);
                readFavoritesOneLevel(favoriteElement, project, treeItem.getChildren());
            }
        }
    }

    private static final ArrayList<AbstractUrl> ourAbstractUrlProviders = new ArrayList<>();

    static {
        ourAbstractUrlProviders.add(new ModuleUrl(null, null));
        ourAbstractUrlProviders.add(new DirectoryUrl(null, null));

        ourAbstractUrlProviders.add(new ModuleGroupUrl(null));

        ourAbstractUrlProviders.add(new PsiFileUrl(null));
        ourAbstractUrlProviders.add(new LibraryModuleGroupUrl(null));
        ourAbstractUrlProviders.add(new NamedLibraryUrl(null, null));
    }

    private static final String ATTRIBUTE_TYPE = "type";
    private static final String ATTRIBUTE_URL = "url";
    private static final String ATTRIBUTE_MODULE = "module";

    @Nullable
    private static AbstractUrl readUrlFromElement(Element element, Project project) {
        final String type = element.getAttributeValue(ATTRIBUTE_TYPE);
        final String urlValue = element.getAttributeValue(ATTRIBUTE_URL);
        final String moduleName = element.getAttributeValue(ATTRIBUTE_MODULE);

        for (BookmarkNodeProvider nodeProvider : project.getExtensionList(BookmarkNodeProvider.class)) {
            if (nodeProvider.getFavoriteTypeId().equals(type)) {
                return new AbstractUrlFavoriteAdapter(urlValue, moduleName, nodeProvider);
            }
        }

        for (AbstractUrl urlProvider : ourAbstractUrlProviders) {
            AbstractUrl url = urlProvider.createUrl(type, moduleName, urlValue);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public Element getState() {
        Element state = new Element("state");
        for (final String name : myName2FavoritesRoots.keySet()) {
            Element list = new Element(ELEMENT_FAVORITES_LIST);
            list.setAttribute(ATTRIBUTE_NAME, name);
            writeRoots(list, myName2FavoritesRoots.get(name));
            state.addContent(list);
        }
        DefaultJDOMExternalizer.writeExternal(this, state);
        return state;
    }

    @Nullable
    public static AbstractUrl createUrlByElement(Object element, final Project project) {
        if (element instanceof SmartPsiElementPointer) {
            element = ((SmartPsiElementPointer)element).getElement();
        }

        for (BookmarkNodeProvider nodeProvider : project.getExtensionList(BookmarkNodeProvider.class)) {
            String url = nodeProvider.getElementUrl(element);
            if (url != null) {
                return new AbstractUrlFavoriteAdapter(url, nodeProvider.getElementModuleName(element), nodeProvider);
            }
        }

        for (AbstractUrl urlProvider : ourAbstractUrlProviders) {
            AbstractUrl url = urlProvider.createUrlByElement(element);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    private static void writeRoots(Element element, Collection<TreeItem<Pair<AbstractUrl, String>>> roots) {
        for (TreeItem<Pair<AbstractUrl, String>> root : roots) {
            final AbstractUrl url = root.getData().getFirst();
            if (url == null) {
                continue;
            }
            final Element list = new Element(FAVORITES_ROOT);
            url.write(list);
            list.setAttribute(CLASS_NAME, root.getData().getSecond());
            element.addContent(list);
            final List<TreeItem<Pair<AbstractUrl, String>>> children = root.getChildren();
            if (children != null && !children.isEmpty()) {
                writeRoots(list, children);
            }
        }
    }

    public String getFavoriteListName(@Nullable final String currentSubId, @Nonnull final VirtualFile vFile) {
        if (currentSubId != null && contains(currentSubId, vFile)) {
            return currentSubId;
        }
        for (String listName : myName2FavoritesRoots.keySet()) {
            if (contains(listName, vFile)) {
                return listName;
            }
        }
        return null;
    }

    // currently only one level here..
    public boolean contains(@Nonnull String name, @Nonnull final VirtualFile vFile) {
        final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
        final Set<Boolean> find = new HashSet<>();
        final ContentIterator contentIterator = new ContentIterator() {
            @Override
            public boolean processFile(VirtualFile fileOrDir) {
                if (fileOrDir != null && fileOrDir.getPath().equals(vFile.getPath())) {
                    find.add(Boolean.TRUE);
                }
                return true;
            }
        };

        Collection<TreeItem<Pair<AbstractUrl, String>>> urls = getFavoritesListRootUrls(name);
        for (TreeItem<Pair<AbstractUrl, String>> pair : urls) {
            AbstractUrl abstractUrl = pair.getData().getFirst();
            if (abstractUrl == null) {
                continue;
            }
            final Object[] path = abstractUrl.createPath(myProject);
            if (path == null || path.length < 1 || path[0] == null) {
                continue;
            }
            Object element = path[path.length - 1];
            if (element instanceof SmartPsiElementPointer) {
                final VirtualFile virtualFile = PsiUtilBase.getVirtualFile(((SmartPsiElementPointer)element).getElement());
                if (virtualFile == null) {
                    continue;
                }
                if (vFile.getPath().equals(virtualFile.getPath())) {
                    return true;
                }
                if (!virtualFile.isDirectory()) {
                    continue;
                }
                projectFileIndex.iterateContentUnderDirectory(virtualFile, contentIterator);
            }

            if (element instanceof PsiElement) {
                final VirtualFile virtualFile = PsiUtilBase.getVirtualFile((PsiElement)element);
                if (virtualFile == null) {
                    continue;
                }
                if (vFile.getPath().equals(virtualFile.getPath())) {
                    return true;
                }
                if (!virtualFile.isDirectory()) {
                    continue;
                }
                projectFileIndex.iterateContentUnderDirectory(virtualFile, contentIterator);
            }
            if (element instanceof Module) {
                ModuleRootManager.getInstance((Module)element).getFileIndex().iterateContent(contentIterator);
            }
            if (element instanceof LibraryGroupElement) {
                final boolean inLibrary = ModuleRootManager.getInstance(((LibraryGroupElement)element).getModule())
                    .getFileIndex()
                    .isInContent(vFile) && projectFileIndex.isInLibraryClasses(vFile);
                if (inLibrary) {
                    return true;
                }
            }
            if (element instanceof NamedLibraryElement) {
                NamedLibraryElement namedLibraryElement = (NamedLibraryElement)element;
                final VirtualFile[] files = namedLibraryElement.getOrderEntry().getFiles(BinariesOrderRootType.getInstance());
                if (ArrayUtil.find(files, vFile) > -1) {
                    return true;
                }
            }
            if (element instanceof ModuleGroup) {
                ModuleGroup group = (ModuleGroup)element;
                final Collection<Module> modules = group.modulesInGroup(myProject, true);
                for (Module module : modules) {
                    ModuleRootManager.getInstance(module).getFileIndex().iterateContent(contentIterator);
                }
            }


            for (BookmarkNodeProvider provider : myProject.getExtensionList(BookmarkNodeProvider.class)) {
                if (provider.elementContainsFile(element, vFile)) {
                    return true;
                }
            }

            if (!find.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void iterateTreeItems(
        final Collection<TreeItem<Pair<AbstractUrl, String>>> coll,
        Consumer<TreeItem<Pair<AbstractUrl, String>>> consumer
    ) {
        final ArrayDeque<TreeItem<Pair<AbstractUrl, String>>> queue = new ArrayDeque<>();
        queue.addAll(coll);
        while (!queue.isEmpty()) {
            final TreeItem<Pair<AbstractUrl, String>> item = queue.removeFirst();
            consumer.accept(item);
            final List<TreeItem<Pair<AbstractUrl, String>>> children = item.getChildren();
            if (children != null && !children.isEmpty()) {
                queue.addAll(children);
            }
        }
    }

    private class MyRootsChangeAdapter extends PsiTreeChangeAdapter {
        @Override
        public void beforeChildMovement(@Nonnull final PsiTreeChangeEvent event) {
            final PsiElement oldParent = event.getOldParent();
            final PsiElement newParent = event.getNewParent();
            final PsiElement child = event.getChild();
            if (newParent instanceof PsiDirectory) {
                final Module module = newParent.getModule();
                if (module == null) {
                    return;
                }
                AbstractUrl childUrl = null;
                if (child instanceof PsiFile) {
                    childUrl = new PsiFileUrl(((PsiDirectory)newParent).getVirtualFile().getUrl() + "/" + ((PsiFile)child).getName());
                }
                else if (child instanceof PsiDirectory) {
                    childUrl = new DirectoryUrl(
                        ((PsiDirectory)newParent).getVirtualFile().getUrl() + "/" + ((PsiDirectory)child).getName(),
                        module.getName()
                    );
                }

                for (String listName : myName2FavoritesRoots.keySet()) {
                    final List<TreeItem<Pair<AbstractUrl, String>>> roots = myName2FavoritesRoots.get(listName);
                    final AbstractUrl finalChildUrl = childUrl;
                    iterateTreeItems(
                        roots,
                        new Consumer<TreeItem<Pair<AbstractUrl, String>>>() {
                            @Override
                            public void accept(TreeItem<Pair<AbstractUrl, String>> item) {
                                final Pair<AbstractUrl, String> root = item.getData();
                                final Object[] path = root.first.createPath(myProject);
                                if (path == null || path.length < 1 || path[0] == null) {
                                    return;
                                }
                                final Object element = path[path.length - 1];
                                if (element == child && finalChildUrl != null) {
                                    item.setData(Pair.create(finalChildUrl, root.second));
                                }
                                else {
                                    if (element == oldParent) {
                                        item.setData(Pair.create(root.first.createUrlByElement(newParent), root.second));
                                    }
                                }
                            }
                        }
                    );
                }
            }
        }

        @Override
        public void beforePropertyChange(@Nonnull final PsiTreeChangeEvent event) {
            if (event.getPropertyName().equals(PsiTreeChangeEvent.PROP_FILE_NAME) || event.getPropertyName()
                .equals(PsiTreeChangeEvent.PROP_DIRECTORY_NAME)) {
                final PsiElement psiElement = event.getChild();
                if (psiElement instanceof PsiFile || psiElement instanceof PsiDirectory) {
                    final Module module = psiElement.getModule();
                    if (module == null) {
                        return;
                    }
                    final String url = ((PsiDirectory)psiElement.getParent()).getVirtualFile().getUrl() + "/" + event.getNewValue();
                    final AbstractUrl childUrl =
                        psiElement instanceof PsiFile ? new PsiFileUrl(url) : new DirectoryUrl(url, module.getName());

                    for (String listName : myName2FavoritesRoots.keySet()) {
                        final List<TreeItem<Pair<AbstractUrl, String>>> roots = myName2FavoritesRoots.get(listName);
                        iterateTreeItems(roots, item -> {
                            final Pair<AbstractUrl, String> root = item.getData();
                            final Object[] path = root.first.createPath(myProject);
                            if (path == null || path.length < 1 || path[0] == null) {
                                return;
                            }
                            final Object element = path[path.length - 1];
                            if (element == psiElement && psiElement instanceof PsiFile) {
                                item.setData(Pair.create(childUrl, root.second));
                            }
                            else {
                                item.setData(root);
                            }
                        });
                    }
                }
            }
        }
    }
}
