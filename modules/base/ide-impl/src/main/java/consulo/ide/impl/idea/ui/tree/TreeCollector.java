// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.tree;

import consulo.util.collection.SmartList;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

import static consulo.util.collection.ArrayUtil.isEmpty;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public final class TreeCollector<T> {
    private final AtomicReference<List<T>> reference = new AtomicReference<>();
    private final BiPredicate<? super T, ? super T> predicate;

    private TreeCollector(BiPredicate<? super T, ? super T> predicate) {
        this.predicate = predicate;
    }

    
    public List<T> get() {
        synchronized (reference) {
            List<T> list = reference.getAndSet(null);
            return list != null ? list : emptyList();
        }
    }

    public boolean add(T object) {
        synchronized (reference) {
            List<T> list = reference.get();
            if (list != null) {
                return add(predicate, list, object);
            }
            reference.set(new SmartList<>(object));
            return true;
        }
    }

    
    @SafeVarargs
    private static <T> List<T> collect(BiPredicate<? super T, ? super T> predicate, T... objects) {
        return isEmpty(objects) ? new ArrayList<>() : collect(predicate, asList(objects));
    }

    
    private static <T> List<T> collect(BiPredicate<? super T, ? super T> predicate, Collection<? extends T> objects) {
        List<T> list = new ArrayList<>(objects.size());
        for (T object : objects) {
            if (object != null) {
                add(predicate, list, object);
            }
        }
        return list;
    }

    private static <T> boolean add(BiPredicate<? super T, ? super T> predicate, List<T> list, T object) {
        for (T each : list) {
            if (predicate.test(each, object)) {
                return false;
            }
        }
        list.removeIf(each -> predicate.test(object, each));
        list.add(object);
        return true;
    }

    public static final class VirtualFileLeafs {
        private static final BiPredicate<VirtualFile, VirtualFile> PREDICATE =
            (child, parent) -> VirtualFileUtil.isAncestor(parent, child, false);

        
        public static TreeCollector<VirtualFile> create() {
            return new TreeCollector<>(PREDICATE);
        }

        
        public static List<VirtualFile> collect(VirtualFile... files) {
            return TreeCollector.collect(PREDICATE, files);
        }

        
        public static List<VirtualFile> collect(Collection<? extends VirtualFile> files) {
            return TreeCollector.collect(PREDICATE, files);
        }
    }

    public static final class VirtualFileRoots {
        private static final BiPredicate<VirtualFile, VirtualFile> PREDICATE =
            (parent, child) -> VirtualFileUtil.isAncestor(parent, child, false);

        
        public static TreeCollector<VirtualFile> create() {
            return new TreeCollector<>(PREDICATE);
        }

        
        public static List<VirtualFile> collect(VirtualFile... files) {
            return TreeCollector.collect(PREDICATE, files);
        }

        
        public static List<VirtualFile> collect(Collection<? extends VirtualFile> files) {
            return TreeCollector.collect(PREDICATE, files);
        }
    }

    public static final class TreePathLeafs {
        private static final BiPredicate<TreePath, TreePath> PREDICATE = (child, parent) -> parent.isDescendant(child);

        
        public static TreeCollector<TreePath> create() {
            return new TreeCollector<>(PREDICATE);
        }

        
        public static List<TreePath> collect(TreePath... paths) {
            return TreeCollector.collect(PREDICATE, paths);
        }

        
        public static List<TreePath> collect(Collection<? extends TreePath> paths) {
            return TreeCollector.collect(PREDICATE, paths);
        }
    }

    public static final class TreePathRoots {
        private static final BiPredicate<TreePath, TreePath> PREDICATE = (parent, child) -> parent.isDescendant(child);

        
        public static TreeCollector<TreePath> create() {
            return new TreeCollector<>(PREDICATE);
        }

        
        public static List<TreePath> collect(TreePath... paths) {
            return TreeCollector.collect(PREDICATE, paths);
        }

        
        public static List<TreePath> collect(Collection<? extends TreePath> paths) {
            return TreeCollector.collect(PREDICATE, paths);
        }
    }
}
