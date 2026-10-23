/*
 * Copyright 2013-2026 consulo.io
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
package consulo.application.content.impl.internal.bundle;

import consulo.content.OrderRootType;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.url.VirtualFileUrl;
import consulo.virtualFileSystem.url.VirtualFileUrlManager;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SdkRoots {
    private static final Logger LOG = Logger.getInstance(SdkRoots.class);

    private static final String ROOT_ELEMENT = "root";
    private static final String URL_ATTRIBUTE = "url";
    private static final String TYPE_ATTRIBUTE = "type";

    private final VirtualFileUrlManager myUrlManager;

    private final Map<String, List<VirtualFileUrl>> myRoots = new ConcurrentHashMap<>();

    public SdkRoots(VirtualFileUrlManager urlManager) {
        myUrlManager = urlManager;
    }

    public VirtualFile[] getFiles(String rootTypeId) {
        List<VirtualFileUrl> roots = myRoots.getOrDefault(rootTypeId, List.of());
        if (roots.isEmpty()) {
            return VirtualFile.EMPTY_ARRAY;
        }

        List<VirtualFile> files = new ArrayList<>(roots.size());
        for (VirtualFileUrl root : roots) {
            VirtualFile file = root.getFile();
            if (file != null) {
                files.add(file);
            }
        }
        return files.toArray(VirtualFile.EMPTY_ARRAY);
    }

    public String[] getUrls(String rootTypeId) {
        List<VirtualFileUrl> roots = myRoots.getOrDefault(rootTypeId, List.of());
        if (roots.isEmpty()) {
            return ArrayUtil.EMPTY_STRING_ARRAY;
        }

        String[] urls = new String[roots.size()];
        for (int i = 0; i < urls.length; i++) {
            urls[i] = roots.get(i).getUrl();
        }
        return urls;
    }

    public void addRoot(VirtualFile file, String rootTypeId) {
        addRoot(file.getUrl(), rootTypeId);
    }

    public void addRoot(String url, String rootTypeId) {
        VirtualFileUrl root = myUrlManager.storeAndGet(url);
        myRoots.compute(rootTypeId, (type, roots) -> {
            if (roots == null) {
                return List.of(root);
            }
            if (roots.contains(root)) {
                return roots;
            }
            List<VirtualFileUrl> newRoots = new ArrayList<>(roots.size() + 1);
            newRoots.addAll(roots);
            newRoots.add(root);
            return List.copyOf(newRoots);
        });
    }

    public void removeRoot(VirtualFile file, String rootTypeId) {
        removeRoot(file.getUrl(), rootTypeId);
    }

    public void removeRoot(String url, String rootTypeId) {
        myRoots.computeIfPresent(rootTypeId, (type, roots) -> {
            List<VirtualFileUrl> newRoots = new ArrayList<>(roots.size());
            for (VirtualFileUrl root : roots) {
                if (!root.getUrl().equals(url)) {
                    newRoots.add(root);
                }
            }
            return List.copyOf(newRoots);
        });
    }

    public void removeAllRoots(String rootTypeId) {
        myRoots.remove(rootTypeId);
    }

    public void removeAllRoots() {
        myRoots.clear();
    }

    public void copyRootsFrom(SdkRoots roots) {
        myRoots.clear();
        myRoots.putAll(roots.myRoots);
    }

    /**
     * <roots>
     * <sourcePath>
     * <root type="composite">
     * <root type="simple" url="jar://I:/Java/jdk1.8/src.zip!/" />
     * <root type="simple" url="jar://I:/Java/jdk1.8/javafx-src.zip!/" />
     * </root>
     * </sourcePath>
     * </roots>
     */
    public void readExternal(Element element) {
        removeAllRoots();

        for (OrderRootType type : OrderRootType.getAllTypes()) {
            String rootTypeId = type.getId();
            Element child = element.getChild(rootTypeId);
            if (child == null) {
                continue;
            }

            List<Element> composites = child.getChildren();
            if (composites.size() != 1) {
                LOG.error(composites);
            }
            if (composites.isEmpty()) {
                continue;
            }

            for (Element root : composites.get(0).getChildren(ROOT_ELEMENT)) {
                String url = root.getAttributeValue(URL_ATTRIBUTE);
                if (url != null) {
                    addRoot(url, rootTypeId);
                    setNoCopyJars(url);
                }
            }
        }
    }

    public void writeExternal(Element element) {
        for (OrderRootType type : OrderRootType.getSortedRootTypes()) {
            String rootTypeId = type.getId();

            Element typeElement = new Element(rootTypeId);
            element.addContent(typeElement);

            Element composite = new Element(ROOT_ELEMENT);
            composite.setAttribute(TYPE_ATTRIBUTE, "composite");
            typeElement.addContent(composite);

            for (VirtualFileUrl root : myRoots.getOrDefault(rootTypeId, List.of())) {
                Element rootElement = new Element(ROOT_ELEMENT);
                rootElement.setAttribute(URL_ATTRIBUTE, root.getUrl());
                rootElement.setAttribute(TYPE_ATTRIBUTE, "simple");
                composite.addContent(rootElement);
            }
        }
    }

    private static void setNoCopyJars(String url) {
        String protocol = VirtualFileManager.extractProtocol(url);
        if (protocol == null) {
            return;
        }

        VirtualFileSystem fs = VirtualFileManager.getInstance().getFileSystem(protocol);
        if (fs instanceof ArchiveFileSystem archiveFileSystem) {
            archiveFileSystem.addNoCopyArchiveForPath(url);
        }
    }
}
