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
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The variants a file is indexed under and the choice among them at read time. The primary variant is the one every
 * applicable provider's first option (its default in the file's primary context, or the first analysed value)
 * describes; each further value of a provider - the default in another context of a library file, or a further
 * analysed value - yields one secondary variant with only that provider's option replaced.
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
        List<ModuleAwareIndexContexts.Context> contexts = ModuleAwareIndexContexts.contextsOf(project, file);
        if (contexts.isEmpty()) {
            return List.of();
        }
        return descriptorsFor(providers, contexts, file, withId.getId());
    }

    /**
     * The variants to index {@code file} under in {@code project}: its own, primary first, and for a file of a library -
     * which other projects may reach too - the variants other projects indexed it under before, after them, so
     * that indexing it here does not throw theirs away. Capped at {@link ModuleAwareIndexContexts#MAX_VARIANTS}.
     */
    public static List<VariantDescriptor> descriptorsToIndex(Project project, VirtualFile file) {
        if (!(file instanceof VirtualFileWithId withId)) {
            return List.of();
        }
        List<ModuleAwareIndexOptionProvider> providers = ModuleAwareIndexOptionRegistry.getApplicableProviders(file.getFileType());
        if (providers.isEmpty()) {
            return List.of();
        }
        List<ModuleAwareIndexContexts.Context> contexts = ModuleAwareIndexContexts.contextsOf(project, file);
        if (contexts.isEmpty()) {
            return List.of();
        }
        List<VariantDescriptor> own = descriptorsFor(providers, contexts, file, withId.getId());
        if (own.isEmpty() || !isShared(contexts)) {
            return own;
        }

        Set<String> providerIds = new HashSet<>();
        for (ModuleAwareIndexOptionProvider provider : providers) {
            providerIds.add(provider.getId());
        }
        List<VariantDescriptor> result = new ArrayList<>(own);
        for (VariantDescriptor known : ModuleAwareIndexKnownVariants.getInstance().get(withId.getId())) {
            if (result.size() >= ModuleAwareIndexContexts.MAX_VARIANTS) {
                break;
            }
            // a variant of providers which are no longer the file's - a plugin was removed - cannot be built again
            if (providerIdsOf(known).equals(providerIds) && !containsOptions(result, known)) {
                result.add(known);
            }
        }
        return List.copyOf(result);
    }

    /**
     * Remembers the variants {@code file} was indexed under, so that the next project indexing it keeps them.
     */
    public static void rememberIndexed(Project project, VirtualFile file, List<VariantDescriptor> descriptors) {
        if (!(file instanceof VirtualFileWithId withId) || descriptors.isEmpty()) {
            return;
        }
        if (isShared(ModuleAwareIndexContexts.contextsOf(project, file))) {
            ModuleAwareIndexKnownVariants.getInstance().put(withId.getId(), descriptors);
        }
    }

    /**
     * @return the variants remembered for {@code fileId} - what it was last indexed under when it is shared
     */
    public static List<VariantDescriptor> remembered(int fileId) {
        return ModuleAwareIndexKnownVariants.getInstance().get(fileId);
    }

    /**
     * Only a file which no module owns - one of a library or an SDK - is reached by other projects too; a project's own
     * files keep only their own variants, so editing a header, which moves its includers' options, leaves no stale
     * variant behind.
     */
    private static boolean isShared(List<ModuleAwareIndexContexts.Context> contexts) {
        return !contexts.isEmpty() && contexts.get(0).orderEntry() != null;
    }

    private static Set<String> providerIdsOf(VariantDescriptor descriptor) {
        Set<String> ids = new HashSet<>();
        for (VariantDescriptor.ProviderOption option : descriptor.options()) {
            ids.add(option.providerId());
        }
        return ids;
    }

    private static boolean containsOptions(List<VariantDescriptor> descriptors, VariantDescriptor wanted) {
        for (VariantDescriptor descriptor : descriptors) {
            if (descriptor.options().equals(wanted.options())) {
                return true;
            }
        }
        return false;
    }

    public static List<VariantDescriptor> descriptorsFor(List<ModuleAwareIndexOptionProvider> providers,
                                                         List<ModuleAwareIndexContexts.Context> contexts,
                                                         VirtualFile file,
                                                         int fileId) {
        ModuleAwareIndexOptionValueStorage values = ModuleAwareIndexOptionValueStorage.getInstance();
        List<List<VariantDescriptor.ProviderOption>> optionsPerProvider = new ArrayList<>(providers.size());
        List<List<String>> namesPerProvider = new ArrayList<>(providers.size());
        for (ModuleAwareIndexOptionProvider provider : providers) {
            List<VariantDescriptor.ProviderOption> options = new ArrayList<>();
            List<String> names = new ArrayList<>();
            List<ModuleAwareIndexOptionValueStorage.StoredOption> stored = values.getVariants(provider.getId(), fileId);
            if (stored.isEmpty()) {
                for (IndexOption option : ModuleAwareIndexContexts.optionsOf(provider, contexts, file)) {
                    options.add(providerOption(provider.getId(), option));
                    names.add(displayNameOf(option));
                }
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
        return currentVariant(tree, file, null);
    }

    /**
     * The variant a file is currently viewed under in {@code project}: the one its view options name, else the one of
     * the project's own primary context. The stored primary is the variant of the project which indexed the file last,
     * which for a file shared by several projects need not be this one.
     */
    public static int currentVariant(SerializedStubTree tree, VirtualFile file, @Nullable Project project) {
        Map<String, byte[]> view = ModuleAwareIndexOptions.getViewOptions(file);
        if (view != null) {
            int index = tree.findVariant(view);
            if (index >= 0) {
                return index;
            }
        }
        if (project == null || project.isDisposed() || tree.getVariantCount() <= 1) {
            return 0;
        }
        Map<String, OptionsMeta.PerProviderMeta> own = ModuleAwareIndexMetaRecorder.ownPrimaryState(project, file);
        if (own == null || own.isEmpty()) {
            return 0;
        }
        Map<String, Integer> versions = new HashMap<>();
        for (ModuleAwareIndexOptionProvider provider : ModuleAwareIndexOptionRegistry.getApplicableProviders(file.getFileType())) {
            versions.put(provider.getId(), provider.getVersion());
        }
        for (int i = 0; i < tree.getVariantCount(); i++) {
            VariantDescriptor descriptor = tree.getDescriptor(i);
            if (descriptor != null && own.equals(descriptor.toMeta(versions))) {
                return i;
            }
        }
        return 0;
    }

    /**
     * The variant a reader asks for: the stored variant whose providers carry the options the selector names (the
     * current variant's options stand in for providers it does not name), else the current variant.
     */
    public static int selectVariant(SerializedStubTree tree, VirtualFile file, @Nullable IndexOptionSelector selector) {
        return selectVariant(tree, file, selector, null);
    }

    public static int selectVariant(SerializedStubTree tree,
                                    VirtualFile file,
                                    @Nullable IndexOptionSelector selector,
                                    @Nullable Project project) {
        int current = currentVariant(tree, file, project);
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
