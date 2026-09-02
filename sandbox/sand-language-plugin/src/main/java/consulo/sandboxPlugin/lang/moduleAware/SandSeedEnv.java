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

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.dumb.IndexNotReadyException;
import consulo.document.util.FileContentUtilCore;
import consulo.language.psi.search.FileTypeIndex;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.content.scope.ProjectScopes;
import consulo.sandboxPlugin.lang.SandFileType;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderEx;
import consulo.virtualFileSystem.FileAttribute;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The seed a sand file is parsed under — the C#-preprocessor model's
 * {@code getStableDefines}: module flags plus the union of every includer's entry
 * environment at its include site (your original {@code #define A} + {@code #include}
 * requirement). The include contributions are pre-computed outside indexing (smart mode)
 * and stored per project; a contribution change re-parses and re-indexes the affected
 * included files.
 */
public final class SandSeedEnv {
    private static final Logger LOG = Logger.getInstance(SandSeedEnv.class);
    private static final Key<ConcurrentMap<VirtualFile, Set<String>>> INCLUDE_SEEDS = Key.create("sand.include.seeds");
    private static final Key<AtomicBoolean> RECOMPUTE_PENDING = Key.create("sand.include.seeds.pending");
    private static final FileAttribute SEED_ATTRIBUTE = new FileAttribute("sand-include-seed", 1, false);

    private SandSeedEnv() {
    }

    public static Set<String> seedFor(Project project, @Nullable VirtualFile file) {
        if (file == null) {
            return Set.of();
        }
        Set<String> seed = new HashSet<>(SandFlagEnv.moduleFlags(project, file));
        seed.addAll(includeSeed(project, file));
        return seed;
    }

    private static Set<String> includeSeed(Project project, VirtualFile file) {
        ConcurrentMap<VirtualFile, Set<String>> store = store(project);
        Set<String> fromIncluders = store.get(file);
        if (fromIncluders == null) {
            fromIncluders = readAttribute(file);
            Set<String> previous = store.putIfAbsent(file, fromIncluders);
            if (previous != null) {
                fromIncluders = previous;
            }
        }
        return fromIncluders;
    }

    public static void scheduleRecompute(Project project) {
        if (project.isDisposed()) {
            return;
        }
        AtomicBoolean pending = pending(project);
        if (!pending.compareAndSet(false, true)) {
            return;
        }
        DumbService.getInstance(project).runWhenSmart(() -> Application.get().executeOnPooledThread(() -> {
            pending.set(false);
            if (project.isDisposed()) {
                return;
            }
            List<VirtualFile> changed;
            try {
                changed = ReadAction.compute(() -> recompute(project));
            }
            catch (IndexNotReadyException e) {
                scheduleRecompute(project);
                return;
            }
            catch (Throwable t) {
                LOG.error("sand-seed recompute failed", t);
                return;
            }
            if (changed.isEmpty()) {
                return;
            }
            FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
            for (VirtualFile file : changed) {
                fileBasedIndex.requestReindex(file);
            }
            Application application = Application.get();
            application.invokeLater(() -> application.runWriteAction(() -> FileContentUtilCore.reparseFiles(changed)));
        }));
    }

    private static List<VirtualFile> recompute(Project project) {
        Map<VirtualFile, Set<String>> newSeeds = new HashMap<>();
        Collection<VirtualFile> sandFiles =
            FileTypeIndex.getFiles(SandFileType.INSTANCE, ProjectScopes.getContentScope(project));
        for (VirtualFile includer : sandFiles) {
            SandIncludeSimulator.Walk walk = SandIncludeSimulator.walk(includer, SandFlagEnv.moduleFlags(project, includer));
            for (Map.Entry<VirtualFile, Set<Set<String>>> entry : walk.includeSiteEnvs().entrySet()) {
                Set<String> union = newSeeds.computeIfAbsent(entry.getKey(), key -> new HashSet<>());
                for (Set<String> environment : entry.getValue()) {
                    union.addAll(environment);
                }
            }
        }

        ConcurrentMap<VirtualFile, Set<String>> store = store(project);
        List<VirtualFile> changed = new ArrayList<>();
        for (Map.Entry<VirtualFile, Set<String>> entry : newSeeds.entrySet()) {
            if (!Objects.equals(store.get(entry.getKey()), entry.getValue())) {
                changed.add(entry.getKey());
            }
        }
        for (VirtualFile stale : store.keySet()) {
            if (!newSeeds.containsKey(stale)) {
                changed.add(stale);
            }
        }

        store.clear();
        store.putAll(newSeeds);
        for (VirtualFile file : changed) {
            writeAttribute(file, newSeeds.getOrDefault(file, Set.of()));
        }
        return changed;
    }

    private static Set<String> readAttribute(VirtualFile file) {
        try (DataInputStream stream = SEED_ATTRIBUTE.readAttribute(file)) {
            if (stream == null) {
                return Set.of();
            }
            int count = stream.readInt();
            Set<String> seed = new HashSet<>(count);
            for (int i = 0; i < count; i++) {
                seed.add(stream.readUTF());
            }
            return seed;
        }
        catch (IOException e) {
            LOG.warn("sand-include-seed attribute read failed for " + file, e);
            return Set.of();
        }
    }

    private static void writeAttribute(VirtualFile file, Set<String> seed) {
        List<String> sorted = new ArrayList<>(seed);
        sorted.sort(null);
        try (DataOutputStream stream = SEED_ATTRIBUTE.writeAttribute(file)) {
            stream.writeInt(sorted.size());
            for (String flag : sorted) {
                stream.writeUTF(flag);
            }
        }
        catch (IOException e) {
            LOG.warn("sand-include-seed attribute write failed for " + file, e);
        }
    }

    private static ConcurrentMap<VirtualFile, Set<String>> store(Project project) {
        ConcurrentMap<VirtualFile, Set<String>> store = project.getUserData(INCLUDE_SEEDS);
        if (store == null) {
            store = ((UserDataHolderEx) project).putUserDataIfAbsent(INCLUDE_SEEDS, new ConcurrentHashMap<>());
        }
        return store;
    }

    private static AtomicBoolean pending(Project project) {
        AtomicBoolean pending = project.getUserData(RECOMPUTE_PENDING);
        if (pending == null) {
            pending = ((UserDataHolderEx) project).putUserDataIfAbsent(RECOMPUTE_PENDING, new AtomicBoolean());
        }
        return pending;
    }
}
