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

package consulo.module.impl.internal.layer;

import consulo.content.ContentFolderTypeProvider;
import consulo.content.base.ExcludedContentFolderTypeProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.layer.ContentFolder;
import consulo.module.content.layer.DirectoryIndexExcludePolicy;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.FileUtil;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.pointer.VirtualFilePointer;
import consulo.virtualFileSystem.pointer.VirtualFilePointerManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jdom.Element;

import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.function.Predicate;

/**
 * @author dsl
 */
public class ContentEntryImpl extends BaseModuleRootLayerChild implements ContentEntryEx {
    public static String getUrlFrom(@Nonnull Element e) {
        LOG.assertTrue(ELEMENT_NAME.equals(e.getName()));

        String url = e.getAttributeValue(URL_ATTRIBUTE);
        if (url == null) {
            throw new InvalidDataException();
        }
        return url;
    }

    private static final Logger LOG = Logger.getInstance(ContentEntryImpl.class);

    public static final String ELEMENT_NAME = "content";
    public static final String URL_ATTRIBUTE = "url";

    @Nonnull
    private final VirtualFilePointer myRoot;

    private final Set<ContentFolder> myContentFolders = new TreeSet<>(ContentFolderComparator.INSTANCE);

    public ContentEntryImpl(@Nonnull VirtualFile file, @Nonnull ModuleRootLayerImpl m) {
        this(file.getUrl(), m);
    }

    public ContentEntryImpl(@Nonnull String url, @Nonnull ModuleRootLayerImpl m) {
        super(m);
        myRoot = VirtualFilePointerManager.getInstance().create(url, this, null);
    }

    public ContentEntryImpl(@Nonnull Element e, @Nonnull ModuleRootLayerImpl m) {
        this(getUrlFrom(e), m);

        for (Element child : e.getChildren(ContentFolderImpl.ELEMENT_NAME)) {
            addFolderInternal(new ContentFolderImpl(child, this));
        }
    }

    @Override
    public VirtualFile getFile() {
        return myRoot.getFile();
    }

    @Override
    @Nonnull
    public String getUrl() {
        return myRoot.getUrl();
    }

    @Nonnull
    @Override
    public ContentFolder[] getFolders(@Nonnull Predicate<ContentFolderTypeProvider> predicate) {
        List<ContentFolder> list = new ArrayList<>();
        for (ContentFolder contentFolder : getFolders0(predicate)) {
            list.add(contentFolder);
        }
        return list.isEmpty() ? ContentFolder.EMPTY_ARRAY : list.toArray(new ContentFolder[list.size()]);
    }


    @Nonnull
    @Override
    public VirtualFile[] getFolderFiles(@Nonnull Predicate<ContentFolderTypeProvider> predicate) {
        List<VirtualFile> list = new ArrayList<>();
        for (ContentFolder contentFolder : getFolders0(predicate)) {
            ContainerUtil.addIfNotNull(list, contentFolder.getFile());
        }
        return VirtualFileUtil.toVirtualFileArray(list);
    }

    @Nonnull
    @Override
    public String[] getFolderUrls(@Nonnull Predicate<ContentFolderTypeProvider> predicate) {
        List<String> list = new ArrayList<>();
        for (ContentFolder contentFolder : getFolders0(predicate)) {
            list.add(contentFolder.getUrl());
        }
        return ArrayUtil.toStringArray(list);
    }

    private List<ContentFolder> getFolders0(Predicate<ContentFolderTypeProvider> predicate) {
        List<ContentFolder> list = new ArrayList<>(myContentFolders.size());
        for (ContentFolder contentFolder : myContentFolders) {
            if (predicate.test(contentFolder.getType())) {
                list.add(contentFolder);
            }
        }

        Module module = getModuleRootLayer().getModule();
        if (module.getModuleDirUrl() == null) {
            return list;
        }

        if (predicate.test(ExcludedContentFolderTypeProvider.getInstance())) {
            for (DirectoryIndexExcludePolicy excludePolicy : DirectoryIndexExcludePolicy.EP_NAME.getExtensionList(getRootModel().getProject())) {
                VirtualFilePointer[] files = excludePolicy.getExcludeRootsForModule(myModuleRootLayer);
                for (VirtualFilePointer file : files) {
                    list.add(new LightContentFolderImpl(file, ExcludedContentFolderTypeProvider.getInstance(), this));
                }
            }
        }
        return list;
    }

    @Nonnull
    @Override
    public ContentFolder addFolder(@Nonnull VirtualFile file, @Nonnull ContentFolderTypeProvider contentFolderType) {
        assertCanAddFolder(file);
        return addFolderInternal(new ContentFolderImpl(file, contentFolderType, this));
    }

    @Nonnull
    @Override
    public ContentFolder addFolder(@Nonnull String url, @Nonnull ContentFolderTypeProvider contentFolderType) {
        assertFolderUnderMe(url);
        return addFolderInternal(new ContentFolderImpl(url, contentFolderType, null, this));
    }

    private ContentFolder addFolderInternal(ContentFolderImpl f) {
        myContentFolders.add(f);
        Disposer.register(this, f);
        return f;
    }

    @Nonnull
    @Override
    public Collection<ContentFolder> getContentFolders() {
        return myContentFolders;
    }

    @Override
    public void removeFolder(@Nonnull ContentFolder contentFolder) {
        assert !isDisposed();
        assertCanRemoveFrom(contentFolder, myContentFolders);
        myContentFolders.remove(contentFolder);
        if (contentFolder instanceof Disposable disposable) {
            Disposer.dispose(disposable);
        }
    }

    private void assertCanAddFolder(@Nonnull VirtualFile file) {
        assertCanAddFolder(file.getUrl());
    }

    private void assertCanAddFolder(@Nonnull String url) {
        getRootModel().assertWritable();
        assertFolderUnderMe(url);
    }

    private <T extends ContentFolder> void assertCanRemoveFrom(T f, @Nonnull Set<T> ff) {
        getRootModel().assertWritable();
        LOG.assertTrue(ff.contains(f));
    }

    private void assertFolderUnderMe(@Nonnull String url) {
        String path = VirtualFileUtil.urlToPath(url);
        String rootPath = VirtualFileUtil.urlToPath(getUrl());
        if (!FileUtil.isAncestor(rootPath, path, false)) {
            LOG.error("The file '" + path + "' is not under content entry root '" + rootPath + "'");
        }
    }

    @Override
    @Nonnull
    public ContentEntryEx cloneEntry(@Nonnull ModuleRootLayerImpl rootModel) {
        assert !isDisposed();

        ContentEntryImpl cloned = new ContentEntryImpl(myRoot.getUrl(), rootModel);
        for (ContentFolder contentFolder : myContentFolders) {
            if (contentFolder instanceof ClonableContentFolder clonableContentFolder) {
                ContentFolderImpl folder = (ContentFolderImpl)clonableContentFolder.cloneFolder(cloned);
                cloned.addFolderInternal(folder);
            }
        }

        return cloned;
    }

    public void writeExternal(@Nonnull Element element) {
        assert !isDisposed();
        LOG.assertTrue(ELEMENT_NAME.equals(element.getName()));
        element.setAttribute(URL_ATTRIBUTE, myRoot.getUrl());
        for (ContentFolder contentFolder : myContentFolders) {
            Element subElement = new Element(ContentFolderImpl.ELEMENT_NAME);
            ((ContentFolderImpl)contentFolder).writeExternal(subElement);
            element.addContent(subElement);
        }
    }

    private static final class ContentFolderComparator implements Comparator<ContentFolder> {
        public static final ContentFolderComparator INSTANCE = new ContentFolderComparator();

        @Override
        public int compare(@Nonnull ContentFolder o1, @Nonnull ContentFolder o2) {
            return o1.getUrl().compareTo(o2.getUrl());
        }
    }
}
