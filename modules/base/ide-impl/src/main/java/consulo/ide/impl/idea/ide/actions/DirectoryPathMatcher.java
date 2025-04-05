/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.matcher.MinusculeMatcher;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.ide.impl.idea.ide.util.gotoByName.GotoFileModel;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScopesCore;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntryWithTracking;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author peter
 */
class DirectoryPathMatcher {
    @Nonnull
    private final GotoFileModel myModel;
    @Nullable
    private final List<Pair<VirtualFile, String>> myFiles;
    @Nonnull
    final String dirPattern;

    private DirectoryPathMatcher(@Nonnull GotoFileModel model, @Nullable List<Pair<VirtualFile, String>> files, @Nonnull String pattern) {
        myModel = model;
        myFiles = files;
        dirPattern = pattern;
    }

    @Nullable
    @RequiredReadAction
    static DirectoryPathMatcher root(@Nonnull GotoFileModel model, @Nonnull String pattern) {
        DirectoryPathMatcher matcher = new DirectoryPathMatcher(model, null, "");
        for (int i = 0; i < pattern.length(); i++) {
            matcher = matcher.appendChar(pattern.charAt(i));
            if (matcher == null) {
                return null;
            }
        }
        return matcher;
    }

    @Nullable
    @RequiredReadAction
    DirectoryPathMatcher appendChar(char c) {
        String nextPattern = dirPattern + c;
        if (c == '*' || c == '/' || c == ' ') {
            return new DirectoryPathMatcher(myModel, myFiles, nextPattern);
        }

        List<Pair<VirtualFile, String>> files = getMatchingRoots();

        List<Pair<VirtualFile, String>> nextRoots = new ArrayList<>();
        MinusculeMatcher matcher = GotoFileItemProvider.getQualifiedNameMatcher(nextPattern);
        List<VirtualFile> nonMatchingRoots = new ArrayList<>();
        for (Pair<VirtualFile, String> pair : files) {
            if (containsChar(pair.second, c) && matcher.matches(pair.second)) {
                nextRoots.add(pair);
            }
            else {
                nonMatchingRoots.add(pair.first);
            }
        }
        processProjectFilesUnder(nonMatchingRoots, sub -> {
            if (!sub.isDirectory()) {
                return false;
            }
            if (!containsChar(sub.getNameSequence(), c)) {
                return true; //go deeper
            }

            String fullName = myModel.getFullName(sub);
            if (fullName == null) {
                return true;
            }
            fullName = FileUtil.toSystemIndependentName(fullName);
            if (matcher.matches(fullName)) {
                nextRoots.add(Pair.create(sub, fullName));
                return false;
            }
            return true;
        });

        return nextRoots.isEmpty() ? null : new DirectoryPathMatcher(myModel, nextRoots, nextPattern);
    }

    /**
     * return null if not cheap
     */
    @Nullable
    @RequiredReadAction
    Set<String> findFileNamesMatchingIfCheap(char nextLetter, MinusculeMatcher matcher) {
        List<Pair<VirtualFile, String>> files = getMatchingRoots();
        Set<String> names = new HashSet<>();
        AtomicInteger counter = new AtomicInteger();
        BooleanSupplier tooMany = () -> counter.get() > 1000;
        List<VirtualFile> nonMatchingRoots = new ArrayList<>();
        for (Pair<VirtualFile, String> pair : files) {
            if (containsChar(pair.second, nextLetter) && matcher.matches(pair.second)) {
                names.add(pair.first.getName());
            }
            else {
                nonMatchingRoots.add(pair.first);
            }
        }
        processProjectFilesUnder(nonMatchingRoots, sub -> {
            counter.incrementAndGet();
            if (tooMany.getAsBoolean()) {
                return false;
            }

            String name = sub.getName();
            if (containsChar(name, nextLetter) && matcher.matches(name)) {
                names.add(name);
            }
            return true;
        });
        return tooMany.getAsBoolean() ? null : names;
    }

    @Nonnull
    @RequiredReadAction
    private List<Pair<VirtualFile, String>> getMatchingRoots() {
        return myFiles != null ? myFiles : getProjectRoots(myModel);
    }

    @Nonnull
    GlobalSearchScope narrowDown(@Nonnull GlobalSearchScope fileSearchScope) {
        if (myFiles == null) {
            return fileSearchScope;
        }

        VirtualFile[] array = ContainerUtil.map2Array(myFiles, VirtualFile.class, p -> p.first);
        return GlobalSearchScopesCore.directoriesScope(myModel.getProject(), true, array).intersectWith(fileSearchScope);

    }

    private void processProjectFilesUnder(List<VirtualFile> roots, Predicate<? super VirtualFile> predicate) {
        Set<VirtualFile> visited = new HashSet<>(roots.size());
        GlobalSearchScope scope = GlobalSearchScope.allScope(myModel.getProject());
        for (VirtualFile root : roots) {
            VfsUtilCore.visitChildrenRecursively(root, new VirtualFileVisitor<Void>() {
                @Override
                public boolean visitFile(@Nonnull VirtualFile file) {
                    return visited.add(file) && scope.contains(file) && predicate.test(file);
                }

                @Nullable
                @Override
                public Iterable<VirtualFile> getChildrenIterable(@Nonnull VirtualFile file) {
                    return file instanceof NewVirtualFile newVirtualFile ? newVirtualFile.getCachedChildren() : null;
                }
            });
        }
    }

    private static boolean containsChar(CharSequence name, char c) {
        return StringUtil.indexOf(name, c, 0, name.length(), false) >= 0;
    }

    @Nonnull
    @RequiredReadAction
    private static List<Pair<VirtualFile, String>> getProjectRoots(GotoFileModel model) {
        Set<VirtualFile> roots = new HashSet<>();
        for (Module module : ModuleManager.getInstance(model.getProject()).getModules()) {
            Collections.addAll(roots, ModuleRootManager.getInstance(module).getContentRoots());
            for (OrderEntry entry : ModuleRootManager.getInstance(module).getOrderEntries()) {
                if (entry instanceof OrderEntryWithTracking) {
                    Collections.addAll(roots, entry.getFiles(BinariesOrderRootType.getInstance()));
                    Collections.addAll(roots, entry.getFiles(SourcesOrderRootType.getInstance()));
                }
            }
        }
        return roots.stream().map(root -> {
            VirtualFile top = model.getTopLevelRoot(root);
            return top != null ? top : root;
        }).distinct().map(r -> Pair.create(r, StringUtil.notNullize(model.getFullName(r)))).collect(Collectors.toList());
    }
}
