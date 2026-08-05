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

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.stub.IndexOption;
import consulo.language.psi.stub.ModuleAwareIndexOptionProvider;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.sandboxPlugin.lang.Sand2FileType;
import consulo.sandboxPlugin.lang.SandFileType;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import java.util.Set;

/**
 * Sample module-aware options provider for the Sand sandbox language. Demonstrates the
 * intended plugin-side integration pattern: implement
 * {@link ModuleAwareIndexOptionProvider}, return a {@link IndexOption#sharablePerOption}
 * whose payload is a {@code record} with a matching {@link consulo.index.io.data.DataExternalizer}.
 *
 * <p>Per-module options are synthesised from the module name for the sandbox — a real
 * plugin would read from a {@code ModuleExtension} or external build-system config.</p>
 */
@ExtensionImpl
public final class SandModuleAwareIndexOptionProvider implements ModuleAwareIndexOptionProvider {
    public static final String ID = "sand-module-aware";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public Set<FileType> getInputFileTypes() {
        return Set.of(SandFileType.INSTANCE, Sand2FileType.INSTANCE);
    }

    @Override
    public IndexOption getOptions(Module module, VirtualFile file) {
        String moduleName = module.getName();
        SandOptions options = new SandOptions(
            Set.of(moduleName.toUpperCase() + "_DEBUG"),
            "sandbox");
        LocalizeValue label = LocalizeValue.of("sandbox / " + moduleName);
        return IndexOption.sharablePerOption(options, SandOptionsExternalizer.INSTANCE, label);
    }
}
