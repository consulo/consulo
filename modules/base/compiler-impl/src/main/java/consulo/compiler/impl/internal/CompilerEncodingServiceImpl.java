/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.compiler.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.compiler.CompilerEncodingService;
import consulo.compiler.CompilerManager;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.encoding.EncodingProjectManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.nio.charset.Charset;
import java.util.*;

/**
 * @author nik
 */
@Singleton
@ServiceImpl
public class CompilerEncodingServiceImpl extends CompilerEncodingService {
    @Nonnull
    private final Project myProject;
    private final CachedValue<Map<Module, Set<Charset>>> myModuleFileEncodings;

    @Inject
    public CompilerEncodingServiceImpl(@Nonnull Project project) {
        myProject = project;
        myModuleFileEncodings = CachedValuesManager.getManager(project).createCachedValue(
            () -> {
                Map<Module, Set<Charset>> result = computeModuleCharsetMap();
                return CachedValueProvider.Result.create(result, ProjectRootManager.getInstance(myProject),
                    EncodingProjectManager.getInstance(myProject).getModificationTracker()
                );
            },
            false
        );
    }

    @RequiredReadAction
    private Map<Module, Set<Charset>> computeModuleCharsetMap() {
        Map<Module, Set<Charset>> map = new HashMap<>();
        Map<? extends VirtualFile, ? extends Charset> mappings = EncodingProjectManager.getInstance(myProject).getAllMappings();
        ProjectFileIndex index = ProjectRootManager.getInstance(myProject).getFileIndex();
        CompilerManager compilerManager = CompilerManager.getInstance(myProject);
        for (Map.Entry<? extends VirtualFile, ? extends Charset> entry : mappings.entrySet()) {
            VirtualFile file = entry.getKey();
            Charset charset = entry.getValue();
            if (file == null || charset == null
                || (!file.isDirectory() && !compilerManager.isCompilableFileType(file.getFileType()))
                || !index.isInSourceContent(file)) {
                continue;
            }

            Module module = index.getModuleForFile(file);
            if (module == null) {
                continue;
            }

            Set<Charset> set = map.get(module);
            if (set == null) {
                set = new LinkedHashSet<>();
                map.put(module, set);

                VirtualFile sourceRoot = index.getSourceRootForFile(file);
                VirtualFile current = file.getParent();
                Charset parentCharset = null;
                while (current != null) {
                    Charset currentCharset = mappings.get(current);
                    if (currentCharset != null) {
                        parentCharset = currentCharset;
                    }
                    if (current.equals(sourceRoot)) {
                        break;
                    }
                    current = current.getParent();
                }
                if (parentCharset != null) {
                    set.add(parentCharset);
                }
            }
            set.add(charset);
        }
        //todo[nik,jeka] perhaps we should take into account encodings of source roots only not individual files
        for (Module module : ModuleManager.getInstance(myProject).getModules()) {
            for (VirtualFile file : ModuleRootManager.getInstance(module).getSourceRoots(true)) {
                Charset encoding = EncodingProjectManager.getInstance(myProject).getEncoding(file, true);
                if (encoding != null) {
                    Set<Charset> charsets = map.get(module);
                    if (charsets == null) {
                        charsets = new LinkedHashSet<>();
                        map.put(module, charsets);
                    }
                    charsets.add(encoding);
                }
            }
        }

        return map;
    }

    @Nullable
    @Override
    public Charset getPreferredModuleEncoding(@Nonnull Module module) {
        Set<Charset> encodings = myModuleFileEncodings.getValue().get(module);
        return ContainerUtil.getFirstItem(encodings, EncodingProjectManager.getInstance(myProject).getDefaultCharset());
    }

    @Nonnull
    @Override
    public Collection<Charset> getAllModuleEncodings(@Nonnull Module module) {
        Set<Charset> encodings = myModuleFileEncodings.getValue().get(module);
        if (encodings != null) {
            return encodings;
        }
        return ContainerUtil.createMaybeSingletonList(EncodingProjectManager.getInstance(myProject).getDefaultCharset());
    }
}
