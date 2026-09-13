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

import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.IndexOption;
import consulo.language.psi.stub.ModuleAwareIndexOptions;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.SandFileType;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The seed a sand file is parsed under: the module flags plus, for an included file, the union of every includer's
 * environment at its include site. The include part needs the whole project, so it is produced by
 * {@link #analyze} when the platform asks and read back through the value the platform recorded; the parser never
 * computes it.
 */
public final class SandSeedEnv {
    private SandSeedEnv() {
    }

    public static Set<String> seedFor(Project project, @Nullable VirtualFile file) {
        if (file == null) {
            return Set.of();
        }
        SandOptions options = ModuleAwareIndexOptions.getOptions(project, file, SandModuleAwareIndexOptionProvider.ID, SandOptionsExternalizer.INSTANCE);
        return options == null ? Set.of() : options.symbols();
    }

    public static Set<String> seedFor(PsiElement context) {
        SandOptions options = ModuleAwareIndexOptions.getOptions(context, SandModuleAwareIndexOptionProvider.ID, SandOptionsExternalizer.INSTANCE);
        return options == null ? Set.of() : options.symbols();
    }

    public static Map<VirtualFile, List<IndexOption>> analyze(Project project, @Nullable Collection<VirtualFile> changedFiles) {
        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
        List<VirtualFile> sandFiles = new ArrayList<>();
        fileIndex.iterateContent(file -> {
            if (!file.isDirectory() && file.getFileType() == SandFileType.INSTANCE) {
                sandFiles.add(file);
            }
            return true;
        });

        Map<VirtualFile, Set<Set<String>>> includeSeeds = new HashMap<>();
        for (VirtualFile includer : sandFiles) {
            SandIncludeSimulator.Walk walk = SandIncludeSimulator.walk(includer, SandFlagEnv.moduleFlags(project, includer));
            for (Map.Entry<VirtualFile, Set<Set<String>>> entry : walk.includeSiteEnvs().entrySet()) {
                includeSeeds.computeIfAbsent(entry.getKey(), key -> new LinkedHashSet<>()).addAll(entry.getValue());
            }
        }

        Map<VirtualFile, List<IndexOption>> result = new HashMap<>();
        for (VirtualFile file : sandFiles) {
            Module module = fileIndex.getModuleForFile(file);
            if (module == null) {
                continue;
            }
            Set<String> moduleFlags = new TreeSet<>(SandFlagEnv.moduleFlags(project, file));
            List<IndexOption> variants = new ArrayList<>();
            Set<Set<String>> seen = new LinkedHashSet<>();
            seen.add(moduleFlags);
            variants.add(optionsOf(module, moduleFlags));
            for (Set<String> environment : includeSeeds.getOrDefault(file, Set.of())) {
                Set<String> symbols = new TreeSet<>(moduleFlags);
                symbols.addAll(environment);
                if (seen.add(symbols)) {
                    variants.add(optionsOf(module, symbols));
                }
            }
            result.put(file, variants);
        }
        return result;
    }

    /**
     * The option under which {@code includer} sees {@code included}: the included file's module flags plus the
     * environment at the include site, or {@code null} when the includer does not include it.
     */
    public static @Nullable IndexOption optionsSeenFrom(Project project, VirtualFile includer, VirtualFile included) {
        Module module = ProjectFileIndex.getInstance(project).getModuleForFile(included);
        if (module == null) {
            return null;
        }
        SandIncludeSimulator.Walk walk = SandIncludeSimulator.walk(includer, SandFlagEnv.moduleFlags(project, includer));
        Set<Set<String>> environments = walk.includeSiteEnvs().get(included);
        if (environments == null || environments.isEmpty()) {
            return null;
        }
        Set<String> symbols = new TreeSet<>(SandFlagEnv.moduleFlags(project, included));
        symbols.addAll(environments.iterator().next());
        return optionsOf(module, symbols);
    }

    static IndexOption optionsOf(Module module, Set<String> symbols) {
        SandOptions options = new SandOptions(new TreeSet<>(symbols), "sandbox");
        LocalizeValue label = LocalizeValue.of("sandbox / " + module.getName());
        return IndexOption.sharablePerOption(options, SandOptionsExternalizer.INSTANCE, label);
    }
}
