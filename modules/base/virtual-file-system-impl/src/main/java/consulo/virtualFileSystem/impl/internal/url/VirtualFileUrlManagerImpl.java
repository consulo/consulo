// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.virtualFileSystem.impl.internal.url;

import consulo.annotation.component.ServiceImpl;
import consulo.util.io.FileUtil;
import consulo.util.io.URLUtil;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.url.VirtualFileUrl;
import consulo.virtualFileSystem.url.VirtualFileUrlManager;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A lock-free trie of {@link VirtualFileUrlImpl} nodes. Every node is a {@link VirtualFileUrl}, and the nodes share the path prefixes.
 */
@Singleton
@ServiceImpl
public class VirtualFileUrlManagerImpl implements VirtualFileUrlManager {
    private final Provider<VirtualFileManager> myVirtualFileManager;

    private final VirtualFileUrlImpl myRoot = new VirtualFileUrlImpl("/", this, null);

    private final VirtualFileUrlImpl myEmptyUrl = new VirtualFileUrlImpl("", this, null);

    private final ConcurrentMap<String, String> myName2Canonical = new ConcurrentHashMap<>();

    @Inject
    public VirtualFileUrlManagerImpl(Provider<VirtualFileManager> virtualFileManager) {
        myVirtualFileManager = virtualFileManager;
    }

    @Override
    public VirtualFileUrl storeAndGet(String url) {
        if (url.isEmpty()) {
            return myEmptyUrl;
        }
        return insertPath(url, myRoot);
    }

    /**
     * Returns the URL whose '/'-separated segments are [segments].
     * The result is the same as {@code storeAndGet(String.join("/", segments))}, but this method does not build the string.
     */
    public VirtualFileUrl fromUrlSegments(List<String> segments) {
        if (segments.isEmpty() || (segments.size() == 1 && segments.get(0).isEmpty())) {
            return myEmptyUrl;
        }
        VirtualFileUrlImpl current = myRoot;
        for (String segment : segments) {
            current = current.getOrCreateChild(segment);
        }
        current.markRegistered();
        return current;
    }

    @Override
    public @Nullable VirtualFileUrl get(String url) {
        VirtualFileUrlImpl node = findNode(url);
        if (node == null) {
            return null;
        }
        return node.isRegistered() ? node : null;
    }

    /**
     * Returns the trie node for [url], or {@code null} if the trie has no such node.
     * Unlike {@link #get}, the function also returns a node which {@link #storeAndGet} didn't register.
     * Use {@link VirtualFileUrl#findChild} to get the nodes below the result.
     */
    public @Nullable VirtualFileUrlImpl findNode(String url) {
        VirtualFileUrlImpl[] current = {myRoot};
        boolean found = forEachNameSegment(url, nameSegment -> {
            VirtualFileUrlImpl child = current[0].findChild(nameSegment);
            if (child == null) {
                return false;
            }
            current[0] = child;
            return true;
        });
        return found ? current[0] : null;
    }

    /**
     * Processes children of [url] and their children recursively using [processor]. [url] itself isn't processed.
     *
     * @return {@code true} if processing finished normally, or {@code false} if [processor] returned
     * {@link TreeNodeProcessingResult#STOP STOP}.
     */
    public boolean processChildrenRecursively(String url, Function<VirtualFileUrl, TreeNodeProcessingResult> processor) {
        VirtualFileUrlImpl node = findNode(url);
        if (node == null) {
            return true;
        }
        return node.processChildrenRecursively(child -> child.isRegistered() ? processor.apply(child) : TreeNodeProcessingResult.CONTINUE);
    }

    /**
     * Returns {@link VirtualFileUrl} instances which were already created by this manager, without creating new ones.
     * Every trie node is itself a {@link VirtualFileUrl}, so this returns the nodes which were explicitly requested
     * via {@link #storeAndGet} or {@link VirtualFileUrl#append}, skipping the intermediate ones created along the way.
     */
    public List<VirtualFileUrl> getCachedVirtualFileUrls() {
        List<VirtualFileUrl> result = new ArrayList<>();
        myRoot.processChildrenRecursively(child -> {
            if (child.isRegistered()) {
                result.add(child);
            }
            return TreeNodeProcessingResult.CONTINUE;
        });
        return result;
    }

    @Override
    public VirtualFileUrl fromPath(String path) {
        String url = URLUtil.FILE_PROTOCOL + URLUtil.SCHEME_SEPARATOR + FileUtil.toSystemIndependentName(path);
        return storeAndGet(url);
    }

    VirtualFileUrlImpl getRoot() {
        return myRoot;
    }

    VirtualFileManager getVirtualFileManager() {
        return myVirtualFileManager.get();
    }

    VirtualFileUrlImpl createVirtualFileUrl(String name, VirtualFileUrlImpl parent) {
        return new VirtualFileUrlImpl(name, this, parent);
    }

    VirtualFileUrl append(String relativePath, VirtualFileUrlImpl parent) {
        return insertPath(relativePath, parent);
    }

    /**
     * Returns the single instance of [name] shared by every trie node with that segment name,
     * so a segment occurring under many parents (e.g. "src") is stored only once instead of once per node.
     * Populated lazily as new nodes are created; {@link #get} never calls it, so lookups don't pollute it.
     */
    String canonicalizeName(String name) {
        return myName2Canonical.computeIfAbsent(name, it -> it);
    }

    private VirtualFileUrl insertPath(String url, VirtualFileUrlImpl node) {
        VirtualFileUrlImpl[] current = {node};
        forEachNameSegment(url, nameSegment -> {
            current[0] = current[0].getOrCreateChild(nameSegment);
            return true;
        });
        current[0].markRegistered();
        return current[0];
    }

    /**
     * Invokes [action] for each '/' or '\' separated segment of [path] without allocating an intermediate list.
     *
     * @return {@code false} if [action] stopped the iteration
     */
    private static boolean forEachNameSegment(String path, Predicate<String> action) {
        int start = 0;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '/' || c == '\\') {
                if (!action.test(path.substring(start, i))) {
                    return false;
                }
                start = i + 1;
            }
        }
        return action.test(path.substring(start));
    }
}
