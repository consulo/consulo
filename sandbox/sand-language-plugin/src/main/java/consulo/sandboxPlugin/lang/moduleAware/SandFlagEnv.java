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
package consulo.sandboxPlugin.lang.moduleAware;

import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.project.content.scope.ProjectScopes;
import consulo.sandboxPlugin.ide.module.extension.SandModuleExtension;
import consulo.sandboxPlugin.lang.psi.stub.SandIncludeIndex;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.util.collection.Maps;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The merge point for a file's effective flag environments. Every file has its standalone
 * context (its own module options), one context per inclusion site (the includer's full
 * environment at that point, found through the reverse {@link SandIncludeIndex} — never a
 * project walk), and its end-of-file self context under each of those. Environments filter
 * condition-annotated stubs at query and resolve time — they are never an indexing input.
 *
 * <p>Memory shape: contexts are computed per file on demand, recursing only through the
 * file's includer chain, and memoized in a <b>soft-value</b> map — big projects pay only
 * for the files actually queried, and the GC may reclaim everything under pressure. The
 * memo is dropped when the VFS or the project roots change.</p>
 */
public final class SandFlagEnv {
    private record Memo(long stamp, ConcurrentMap<VirtualFile, Set<Set<String>>> byFile) {
    }

    private static final Key<AtomicReference<Memo>> MEMO = Key.create("sand.flag.env.memo");

    private SandFlagEnv() {
    }

    public static Set<Set<String>> allContexts(Project project, VirtualFile file) {
        ConcurrentMap<VirtualFile, Set<Set<String>>> memo = memo(project);
        Set<Set<String>> cached = memo.get(file);
        if (cached != null) {
            return cached;
        }
        Set<Set<String>> contexts = computeContexts(project, file, memo, new HashSet<>());
        memo.put(file, contexts);
        return contexts;
    }

    /**
     * The environment references in this file resolve under: the union of all the file's
     * contexts — everything definable in any of its worlds. A position-insensitive
     * approximation; a precise implementation would track the environment at the
     * reference's offset.
     */
    public static Set<String> resolutionEnv(Project project, VirtualFile file) {
        Set<String> union = new HashSet<>();
        for (Set<String> context : allContexts(project, file)) {
            union.addAll(context);
        }
        return union;
    }

    public static Set<String> moduleFlags(Project project, VirtualFile file) {
        Module module = ProjectFileIndex.getInstance(project).getModuleForFile(file);
        if (module == null) {
            return Set.of();
        }
        SandModuleExtension extension = module.getExtension(SandModuleExtension.class);
        return extension == null ? Set.of() : extension.getFlags();
    }

    private static Set<Set<String>> computeContexts(Project project,
                                                    VirtualFile file,
                                                    ConcurrentMap<VirtualFile, Set<Set<String>>> memo,
                                                    Set<VirtualFile> visiting) {
        Set<String> own = moduleFlags(project, file);
        if (!visiting.add(file)) {
            return Set.of(Set.copyOf(own));
        }

        Set<Set<String>> baseEnvs = new HashSet<>();
        baseEnvs.add(Set.copyOf(own));

        for (VirtualFile includer : FileBasedIndex.getInstance()
            .getContainingFiles(SandIncludeIndex.INDEX_ID, file.getName(), ProjectScopes.getContentScope(project))) {
            if (file.equals(includer)) {
                continue;
            }
            Set<Set<String>> includerContexts = memo.get(includer);
            if (includerContexts == null) {
                includerContexts = computeContexts(project, includer, memo, visiting);
                memo.put(includer, includerContexts);
            }
            for (Set<String> includerContext : includerContexts) {
                Set<Set<String>> siteEnvs = SandIncludeSimulator.walk(includer, includerContext).includeSiteEnvs().get(file);
                if (siteEnvs == null) {
                    continue;
                }
                for (Set<String> siteEnv : siteEnvs) {
                    Set<String> merged = new HashSet<>(own);
                    merged.addAll(siteEnv);
                    baseEnvs.add(Set.copyOf(merged));
                }
            }
        }

        Set<Set<String>> contexts = new HashSet<>();
        for (Set<String> baseEnv : baseEnvs) {
            contexts.add(baseEnv);
            contexts.add(SandIncludeSimulator.walk(file, baseEnv).endEnv());
        }

        visiting.remove(file);
        return Set.copyOf(contexts);
    }

    private static ConcurrentMap<VirtualFile, Set<Set<String>>> memo(Project project) {
        long stamp = VirtualFileManager.getInstance().getModificationCount()
            + 31 * ProjectRootManager.getInstance(project).getModificationCount();
        AtomicReference<Memo> reference = project.getUserData(MEMO);
        if (reference == null) {
            reference = project.putUserDataIfAbsent(MEMO, new AtomicReference<>());
        }
        while (true) {
            Memo memo = reference.get();
            if (memo != null && memo.stamp() == stamp) {
                return memo.byFile();
            }
            Memo fresh = new Memo(stamp, Maps.newConcurrentSoftValueHashMap());
            if (reference.compareAndSet(memo, fresh)) {
                return fresh.byFile();
            }
        }
    }
}
