// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.virtualFileSystem.impl.internal.url;

import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.url.VirtualFileUrl;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Represents a URL (in VFS format) of a file or directory. Every instance is a node of the trie in {@link VirtualFileUrlManagerImpl}.
 */
public class VirtualFileUrlImpl implements VirtualFileUrl {
    private final String myNodeName;
    private final VirtualFileUrlManagerImpl myManager;
    private final @Nullable VirtualFileUrlImpl myParent;

    private final AtomicReference<@Nullable ConcurrentMap<String, VirtualFileUrlImpl>> myChildrenRef = new AtomicReference<>();

    private final CachedVirtualFileFinder myFileFinder = new CachedVirtualFileFinder();

    private volatile @Nullable String myUrl;

    private volatile boolean myRegistered;

    public VirtualFileUrlImpl(String nodeName, VirtualFileUrlManagerImpl manager, @Nullable VirtualFileUrlImpl parent) {
        myNodeName = nodeName;
        myManager = manager;
        myParent = parent;
    }

    void markRegistered() {
        if (!myRegistered) {
            myRegistered = true;
        }
    }

    public boolean isRegistered() {
        return myRegistered;
    }

    @Nullable VirtualFileUrlImpl getParentNode() {
        return myParent;
    }

    @Override
    public @Nullable VirtualFileUrlImpl findChild(String nameSegment) {
        ConcurrentMap<String, VirtualFileUrlImpl> children = myChildrenRef.get();
        return children == null ? null : children.get(nameSegment);
    }

    /**
     * Returns the existing child for [nameSegment] or atomically creates it, allocating this node's children map on first use.
     */
    VirtualFileUrlImpl getOrCreateChild(String nameSegment) {
        VirtualFileUrlImpl existing = findChild(nameSegment);
        if (existing != null) {
            return existing;
        }
        String canonicalName = myManager.canonicalizeName(nameSegment);
        return getOrCreateChildren().computeIfAbsent(canonicalName, name -> myManager.createVirtualFileUrl(name, this));
    }

    private ConcurrentMap<String, VirtualFileUrlImpl> getOrCreateChildren() {
        ConcurrentMap<String, VirtualFileUrlImpl> children = myChildrenRef.get();
        if (children != null) {
            return children;
        }
        ConcurrentMap<String, VirtualFileUrlImpl> created = new ConcurrentHashMap<>();
        if (myChildrenRef.compareAndSet(null, created)) {
            return created;
        }
        ConcurrentMap<String, VirtualFileUrlImpl> raced = myChildrenRef.get();
        return raced == null ? created : raced;
    }

    @Override
    public String getUrl() {
        String url = myUrl;
        if (url == null) {
            url = computeUrlInternal();
            myUrl = url;
        }
        return url;
    }

    /**
     * Returns the segments of {@link #getUrl()} split at '/', without building the URL string.
     * The result is the same as {@code url.split('/', '\\')}. The empty URL has one empty segment.
     */
    public List<String> getUrlSegments() {
        if (myParent == null) {
            List<String> segments = new ArrayList<>();
            segments.add(myNodeName);
            return segments;
        }
        List<String> segments = new ArrayList<>();
        VirtualFileUrlImpl node = this;
        while (true) {
            VirtualFileUrlImpl parentNode = node.myParent;
            if (parentNode == null) {
                break;
            }
            segments.add(node.myNodeName);
            node = parentNode;
        }
        Collections.reverse(segments);
        return segments;
    }

    @Override
    public String getFileName() {
        return myNodeName;
    }

    @Override
    public @Nullable VirtualFileUrl getParent() {
        VirtualFileUrlImpl parentNode = myParent;
        if (parentNode == null || parentNode == myManager.getRoot()) {
            return null;
        }
        return parentNode;
    }

    @Override
    public String getPresentableUrl() {
        String calculatedUrl = getUrl();
        if (calculatedUrl.startsWith("file://")) {
            return calculatedUrl.substring("file://".length());
        }
        else if (calculatedUrl.startsWith("jar://")) {
            String removedSuffix = removeSuffix(removeSuffix(calculatedUrl, "!/"), "!");
            return removedSuffix.substring("jar://".length());
        }
        return calculatedUrl;
    }

    @Override
    public List<VirtualFileUrl> getSubTreeFileUrls() {
        if (myChildrenRef.get() == null) {
            return List.of();
        }
        List<VirtualFileUrl> result = new ArrayList<>();
        computeSubtree(result);
        return result;
    }

    @Override
    public VirtualFileUrl append(String relativePath) {
        return myManager.append(relativePath.startsWith("/") ? relativePath.substring(1) : relativePath, this);
    }

    @Override
    public @Nullable VirtualFile getFile() {
        return myFileFinder.findVirtualFile(myManager.getVirtualFileManager(), getUrl());
    }

    public void cacheVirtualFile(VirtualFile file) {
        myFileFinder.cacheVirtualFile(myManager.getVirtualFileManager(), file);
    }

    /**
     * Returns the file cached by {@link #cacheVirtualFile} or by an earlier search, if VFS didn't change after that.
     * Otherwise, returns {@code null}. The function doesn't search VFS.
     */
    public @Nullable VirtualFile getCachedVirtualFile() {
        return myFileFinder.getCachedVirtualFile(myManager.getVirtualFileManager());
    }

    @Override
    public String toString() {
        return getUrl();
    }

    private String computeUrlInternal() {
        VirtualFileUrlImpl parentNode = myParent;
        if (parentNode == null) {
            return myNodeName;
        }
        if (parentNode == myManager.getRoot() && myNodeName.isEmpty()) {
            return "/";
        }
        StringBuilder builder = new StringBuilder();
        buildPathRecursive(builder);
        return builder.toString();
    }

    private void buildPathRecursive(StringBuilder builder) {
        VirtualFileUrlImpl parentNode = myParent;
        if (parentNode == null) {
            return;
        }
        parentNode.buildPathRecursive(builder);
        if (parentNode.myParent != null) {
            builder.append('/');
        }
        builder.append(myNodeName);
    }

    private void computeSubtree(List<VirtualFileUrl> result) {
        ConcurrentMap<String, VirtualFileUrlImpl> children = myChildrenRef.get();
        if (children == null) {
            return;
        }
        for (VirtualFileUrlImpl child : children.values()) {
            result.add(child);
            child.computeSubtree(result);
        }
    }

    boolean processChildrenRecursively(Function<VirtualFileUrlImpl, TreeNodeProcessingResult> processor) {
        ConcurrentMap<String, VirtualFileUrlImpl> children = myChildrenRef.get();
        if (children == null) {
            return true;
        }
        for (VirtualFileUrlImpl child : children.values()) {
            switch (processor.apply(child)) {
                case CONTINUE -> {
                    if (!child.processChildrenRecursively(processor)) {
                        return false;
                    }
                }
                case SKIP_CHILDREN -> {
                }
                case SKIP_TO_PARENT -> {
                    return true;
                }
                case STOP -> {
                    return false;
                }
            }
        }
        return true;
    }

    private static String removeSuffix(String value, String suffix) {
        return value.endsWith(suffix) ? value.substring(0, value.length() - suffix.length()) : value;
    }
}
