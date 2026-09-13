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
package consulo.language.index.impl.internal.moduleAware;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.document.util.FileContentUtilCore;
import consulo.language.internal.psi.stub.IndexOptionImpl;
import consulo.language.psi.stub.IndexOption;
import consulo.language.psi.stub.ModuleAwareIndexOptionProvider;
import consulo.language.psi.stub.ModuleAwareIndexOptions;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runs {@link ModuleAwareIndexOptionProvider#analyze} for the providers of a project and turns its answers into
 * indexing work: values that moved are stored, the files are scanned again so the index catches up, and their trees
 * are rebuilt. Passes are serialized per project; a trigger that lands while a pass runs asks for exactly one more
 * pass after it, so a pass never races another and the last pass always saw the latest content.
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public final class ModuleAwareIndexOptionsAnalyzer {
    private static final Logger LOG = Logger.getInstance(ModuleAwareIndexOptionsAnalyzer.class);

    private static final int IDLE = 0;
    private static final int RUNNING = 1;
    private static final int RERUN_REQUESTED = 2;

    public static ModuleAwareIndexOptionsAnalyzer getInstance(Project project) {
        return project.getInstance(ModuleAwareIndexOptionsAnalyzer.class);
    }

    private final Project myProject;
    private final Object myLock = new Object();
    private int myState = IDLE;
    private boolean myFullPassRequested;
    private final Set<VirtualFile> myChangedFiles = new HashSet<>();

    @Inject
    public ModuleAwareIndexOptionsAnalyzer(Project project) {
        myProject = project;
    }

    /**
     * @param changedFiles the files that changed, or {@code null} for a full pass over the project
     */
    public void schedule(@Nullable Collection<VirtualFile> changedFiles) {
        if (myProject.isDisposed()) {
            return;
        }
        synchronized (myLock) {
            if (changedFiles == null) {
                myFullPassRequested = true;
            }
            else {
                myChangedFiles.addAll(changedFiles);
            }
            if (myState == RUNNING) {
                myState = RERUN_REQUESTED;
                return;
            }
            if (myState == RERUN_REQUESTED) {
                return;
            }
            myState = RUNNING;
        }
        Application.get().executeOnPooledThread(this::runPasses);
    }

    private void runPasses() {
        while (true) {
            if (myProject.isDisposed()) {
                synchronized (myLock) {
                    myState = IDLE;
                }
                return;
            }
            Collection<VirtualFile> changed;
            synchronized (myLock) {
                changed = myFullPassRequested ? null : new ArrayList<>(myChangedFiles);
                myFullPassRequested = false;
                myChangedFiles.clear();
            }
            try {
                runPass(changed);
            }
            catch (Throwable t) {
                LOG.error("Module-aware option analysis failed", t);
            }
            synchronized (myLock) {
                if (myState == RUNNING) {
                    myState = IDLE;
                    return;
                }
                myState = RUNNING;
            }
        }
    }

    private void runPass(@Nullable Collection<VirtualFile> changed) {
        List<VirtualFile> moved = ReadAction.compute(() -> {
            if (myProject.isDisposed()) {
                return List.of();
            }
            ModuleAwareIndexOptionValueStorage storage = ModuleAwareIndexOptionValueStorage.getInstance();
            List<VirtualFile> result = new ArrayList<>();
            for (ModuleAwareIndexOptionProvider provider : Application.get().getExtensionPoint(ModuleAwareIndexOptionProvider.class).getExtensionList()) {
                Map<VirtualFile, List<IndexOption>> values = provider.analyze(myProject, changed);
                for (Map.Entry<VirtualFile, List<IndexOption>> entry : values.entrySet()) {
                    VirtualFile file = entry.getKey();
                    if (!(file instanceof VirtualFileWithId withId) || !file.isValid() || entry.getValue().isEmpty()) {
                        continue;
                    }
                    List<ModuleAwareIndexOptionValueStorage.StoredOption> fresh = toStored(entry.getValue());
                    List<ModuleAwareIndexOptionValueStorage.StoredOption> stored = storage.getVariants(provider.getId(), withId.getId());
                    if (sameVariants(stored, fresh)) {
                        continue;
                    }
                    if (stored.isEmpty() && !differsFromDefault(provider, file, fresh)) {
                        storage.putVariants(provider.getId(), withId.getId(), fresh);
                        continue;
                    }
                    storage.putVariants(provider.getId(), withId.getId(), fresh);
                    result.add(file);
                }
            }
            storage.flush();
            return result;
        });
        if (moved.isEmpty()) {
            return;
        }
        ModuleAwareIndexOptions.optionsChanged(myProject, moved, "module-aware options analysed");
        Application application = Application.get();
        application.invokeLater(() -> {
            List<VirtualFile> valid = new ArrayList<>(moved);
            valid.removeIf(file -> !file.isValid());
            if (!valid.isEmpty() && !myProject.isDisposed()) {
                application.runWriteAction(() -> FileContentUtilCore.reparseFiles(valid));
            }
        });
    }

    private static List<ModuleAwareIndexOptionValueStorage.StoredOption> toStored(List<IndexOption> options) {
        List<ModuleAwareIndexOptionValueStorage.StoredOption> stored = new ArrayList<>(options.size());
        for (IndexOption option : options) {
            ModuleAwareIndexOptionValueStorage.StoredOption candidate = new ModuleAwareIndexOptionValueStorage.StoredOption(
                OptionsRevalidator.tagOf(option),
                IndexOptionHasher.payloadOf(option),
                ModuleAwareIndexVariants.displayNameOf(option)
            );
            boolean duplicate = false;
            for (ModuleAwareIndexOptionValueStorage.StoredOption existing : stored) {
                if (existing.sameValue(candidate)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                stored.add(candidate);
            }
        }
        return stored;
    }

    private static boolean sameVariants(List<ModuleAwareIndexOptionValueStorage.StoredOption> stored,
                                        List<ModuleAwareIndexOptionValueStorage.StoredOption> fresh) {
        if (stored.size() != fresh.size() || stored.isEmpty()) {
            return stored.size() == fresh.size();
        }
        if (!stored.get(0).sameValue(fresh.get(0))) {
            return false;
        }
        for (int i = 1; i < fresh.size(); i++) {
            boolean found = false;
            for (int j = 1; j < stored.size(); j++) {
                if (stored.get(j).sameValue(fresh.get(i))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /**
     * A file that was never analysed was indexed and parsed under {@link ModuleAwareIndexOptionProvider#getOptions};
     * its first analysed value is a change only if it differs from that single default variant.
     */
    private boolean differsFromDefault(ModuleAwareIndexOptionProvider provider, VirtualFile file, List<ModuleAwareIndexOptionValueStorage.StoredOption> fresh) {
        consulo.module.Module module = consulo.module.content.ProjectFileIndex.getInstance(myProject).getModuleForFile(file);
        if (module == null || fresh.size() != 1) {
            return true;
        }
        IndexOption option = provider.getOptions(module, file);
        return OptionsRevalidator.tagOf(option) != fresh.get(0).tag() || !Arrays.equals(IndexOptionHasher.payloadOf(option), fresh.get(0).payload());
    }
}
