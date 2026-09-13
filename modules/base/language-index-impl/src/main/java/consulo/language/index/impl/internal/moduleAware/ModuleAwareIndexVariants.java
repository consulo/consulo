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

import consulo.language.index.impl.internal.stub.SerializedStubTree;
import consulo.language.internal.psi.stub.IndexOptionImpl;
import consulo.language.psi.stub.IndexOption;
import consulo.language.psi.stub.IndexOptionSelector;
import consulo.language.psi.stub.ModuleAwareIndexOptionProvider;
import consulo.language.psi.stub.ModuleAwareIndexOptions;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The variants a file is indexed under and the choice among them at read time. The primary variant is the one every
 * applicable provider's first option (its module-settings default, or the first analysed value) describes; each further
 * value of a provider yields one secondary variant with only that provider's option replaced.
 */
public final class ModuleAwareIndexVariants {
    private ModuleAwareIndexVariants() {
    }

    public static List<VariantDescriptor> descriptorsFor(Project project, VirtualFile file) {
        if (!(file instanceof VirtualFileWithId withId)) {
            return List.of();
        }
        List<ModuleAwareIndexOptionProvider> providers = ModuleAwareIndexOptionRegistry.getApplicableProviders(file.getFileType());
        if (providers.isEmpty()) {
            return List.of();
        }
        Module module = ProjectFileIndex.getInstance(project).getModuleForFile(file);
        if (module == null) {
            return List.of();
        }
        return descriptorsFor(providers, module, file, withId.getId());
    }

    public static List<VariantDescriptor> descriptorsFor(List<ModuleAwareIndexOptionProvider> providers, Module module, VirtualFile file, int fileId) {
        ModuleAwareIndexOptionValueStorage values = ModuleAwareIndexOptionValueStorage.getInstance();
        List<List<VariantDescriptor.ProviderOption>> optionsPerProvider = new ArrayList<>(providers.size());
        List<List<String>> namesPerProvider = new ArrayList<>(providers.size());
        for (ModuleAwareIndexOptionProvider provider : providers) {
            List<VariantDescriptor.ProviderOption> options = new ArrayList<>();
            List<String> names = new ArrayList<>();
            List<ModuleAwareIndexOptionValueStorage.StoredOption> stored = values.getVariants(provider.getId(), fileId);
            if (stored.isEmpty()) {
                IndexOption option = provider.getOptions(module, file);
                options.add(providerOption(provider.getId(), option));
                names.add(displayNameOf(option));
            }
            else {
                for (ModuleAwareIndexOptionValueStorage.StoredOption each : stored) {
                    options.add(new VariantDescriptor.ProviderOption(provider.getId(), each.tag(), each.payload()));
                    names.add(each.displayName());
                }
            }
            optionsPerProvider.add(options);
            namesPerProvider.add(names);
        }

        List<VariantDescriptor.ProviderOption> primaryOptions = new ArrayList<>(providers.size());
        List<String> primaryNames = new ArrayList<>(providers.size());
        for (int i = 0; i < providers.size(); i++) {
            primaryOptions.add(optionsPerProvider.get(i).get(0));
            primaryNames.add(namesPerProvider.get(i).get(0));
        }
        VariantDescriptor primary = new VariantDescriptor(primaryOptions, join(primaryNames));

        Set<VariantDescriptor> result = new LinkedHashSet<>();
        result.add(primary);
        for (int i = 0; i < providers.size(); i++) {
            List<VariantDescriptor.ProviderOption> options = optionsPerProvider.get(i);
            for (int k = 1; k < options.size(); k++) {
                List<String> names = new ArrayList<>(primaryNames);
                names.set(i, namesPerProvider.get(i).get(k));
                result.add(primary.with(options.get(k), join(names)));
            }
        }
        return List.copyOf(result);
    }

    public static VariantDescriptor.ProviderOption providerOption(String providerId, IndexOption option) {
        return new VariantDescriptor.ProviderOption(providerId, OptionsRevalidator.tagOf(option), IndexOptionHasher.payloadOf(option));
    }

    public static String displayNameOf(IndexOption option) {
        return switch (option) {
            case IndexOptionImpl.FullySharable ignored -> "";
            case IndexOptionImpl.UniqueToModule unique -> unique.displayName().get();
            case IndexOptionImpl.SharablePerOption<?> sharable -> sharable.displayName().get();
        };
    }

    private static String join(List<String> names) {
        StringBuilder builder = new StringBuilder();
        for (String name : names) {
            if (name.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(name);
        }
        return builder.toString();
    }

    /**
     * The variant a file is currently viewed under: the one its view options name, else the primary.
     */
    public static int currentVariant(SerializedStubTree tree, VirtualFile file) {
        Map<String, byte[]> view = ModuleAwareIndexOptions.getViewOptions(file);
        if (view != null) {
            int index = tree.findVariant(view);
            if (index >= 0) {
                return index;
            }
        }
        return 0;
    }

    /**
     * The variant a reader asks for: the stored variant whose providers carry the options the selector names (the
     * current variant's options stand in for providers it does not name), else the current variant.
     */
    public static int selectVariant(SerializedStubTree tree, VirtualFile file, @Nullable IndexOptionSelector selector) {
        int current = currentVariant(tree, file);
        if (selector == null || selector == IndexOptionSelector.ALL_VARIANTS || tree.getVariantCount() <= 1) {
            return current;
        }
        VariantDescriptor currentDescriptor = tree.getDescriptor(current);
        if (currentDescriptor == null) {
            return current;
        }
        Map<String, byte[]> wanted = new HashMap<>();
        boolean named = false;
        for (VariantDescriptor.ProviderOption option : currentDescriptor.options()) {
            IndexOption selected = selector.select(option.providerId(), file);
            if (selected != null) {
                wanted.put(option.providerId(), IndexOptionHasher.payloadOf(selected));
                named = true;
            }
            else {
                wanted.put(option.providerId(), option.payload());
            }
        }
        if (!named) {
            return current;
        }
        int index = tree.findVariant(wanted);
        return index >= 0 ? index : current;
    }
}
