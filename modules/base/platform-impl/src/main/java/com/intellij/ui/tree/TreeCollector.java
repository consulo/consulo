// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.tree;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.SmartList;
import javax.annotation.Nonnull;

import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

import static com.intellij.openapi.vfs.VfsUtilCore.isAncestor;
import static com.intellij.util.ArrayUtil.isEmpty;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public final class TreeCollector<T> {
  private final AtomicReference<List<T>> reference = new AtomicReference<>();
  private final BiPredicate<? super T, ? super T> predicate;

  private TreeCollector(@Nonnull BiPredicate<? super T, ? super T> predicate) {
    this.predicate = predicate;
  }

  @Nonnull
  public List<T> get() {
    synchronized (reference) {
      List<T> list = reference.getAndSet(null);
      return list != null ? list : emptyList();
    }
  }

  public boolean add(@Nonnull T object) {
    synchronized (reference) {
      List<T> list = reference.get();
      if (list != null) return add(predicate, list, object);
      reference.set(new SmartList<>(object));
      return true;
    }
  }

  @Nonnull
  private static <T> List<T> collect(@Nonnull BiPredicate<? super T, ? super T> predicate, T... objects) {
    return isEmpty(objects) ? new ArrayList<>() : collect(predicate, asList(objects));
  }

  @Nonnull
  private static <T> List<T> collect(@Nonnull BiPredicate<? super T, ? super T> predicate, @Nonnull Collection<? extends T> objects) {
    List<T> list = new ArrayList<>(objects.size());
    for (T object : objects) if (object != null) add(predicate, list, object);
    return list;
  }

  private static <T> boolean add(@Nonnull BiPredicate<? super T, ? super T> predicate, @Nonnull List<T> list, @Nonnull T object) {
    for (T each : list) if (predicate.test(each, object)) return false;
    list.removeIf(each -> predicate.test(object, each));
    list.add(object);
    return true;
  }


  public static final class VirtualFileLeafs {
    private static final BiPredicate<VirtualFile, VirtualFile> PREDICATE = (child, parent) -> isAncestor(parent, child, false);

    @Nonnull
    public static TreeCollector<VirtualFile> create() {
      return new TreeCollector<>(PREDICATE);
    }

    @Nonnull
    public static List<VirtualFile> collect(VirtualFile... files) {
      return TreeCollector.collect(PREDICATE, files);
    }

    @Nonnull
    public static List<VirtualFile> collect(@Nonnull Collection<? extends VirtualFile> files) {
      return TreeCollector.collect(PREDICATE, files);
    }
  }


  public static final class VirtualFileRoots {
    private static final BiPredicate<VirtualFile, VirtualFile> PREDICATE = (parent, child) -> isAncestor(parent, child, false);

    @Nonnull
    public static TreeCollector<VirtualFile> create() {
      return new TreeCollector<>(PREDICATE);
    }

    @Nonnull
    public static List<VirtualFile> collect(VirtualFile... files) {
      return TreeCollector.collect(PREDICATE, files);
    }

    @Nonnull
    public static List<VirtualFile> collect(@Nonnull Collection<? extends VirtualFile> files) {
      return TreeCollector.collect(PREDICATE, files);
    }
  }


  public static final class TreePathLeafs {
    private static final BiPredicate<TreePath, TreePath> PREDICATE = (child, parent) -> parent.isDescendant(child);

    @Nonnull
    public static TreeCollector<TreePath> create() {
      return new TreeCollector<>(PREDICATE);
    }

    @Nonnull
    public static List<TreePath> collect(TreePath... paths) {
      return TreeCollector.collect(PREDICATE, paths);
    }

    @Nonnull
    public static List<TreePath> collect(@Nonnull Collection<? extends TreePath> paths) {
      return TreeCollector.collect(PREDICATE, paths);
    }
  }


  public static final class TreePathRoots {
    private static final BiPredicate<TreePath, TreePath> PREDICATE = (parent, child) -> parent.isDescendant(child);

    @Nonnull
    public static TreeCollector<TreePath> create() {
      return new TreeCollector<>(PREDICATE);
    }

    @Nonnull
    public static List<TreePath> collect(TreePath... paths) {
      return TreeCollector.collect(PREDICATE, paths);
    }

    @Nonnull
    public static List<TreePath> collect(@Nonnull Collection<? extends TreePath> paths) {
      return TreeCollector.collect(PREDICATE, paths);
    }
  }
}
