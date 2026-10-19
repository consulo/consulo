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

import consulo.language.psi.stub.IndexOption;
import consulo.language.psi.stub.ModuleAwareIndexOptionProvider;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * The contexts a file is compiled in, which its module-aware options come from.
 * <p>
 * A file of a module's content is compiled as part of that module: one context. A file of a library or an SDK belongs
 * to no module - it is compiled as part of every module that reaches it, so it has one context per order entry reaching
 * it: the same jar or header directory used by several modules, a library listed by a module twice, a library a
 * dependency exports. The contexts come in a stable order - owner module name, then the entry's position among that
 * module's entries - so the same roots always give the same primary context, the first one.
 *
 * @author VISTALL
 * @since 2026-10-19
 */
public final class ModuleAwareIndexContexts {
    /**
     * How many variants the contexts of one file may produce at most; a library shared by more differently configured
     * modules is indexed under the first ones only, the rest are served through the primary variant.
     */
    static final int MAX_VARIANTS = 8;

    /**
     * @param orderEntry the entry the module reaches the file through; {@code null} for a file of the module's content
     */
    public record Context(Module module, @Nullable OrderEntry orderEntry) {
        public IndexOption optionsOf(ModuleAwareIndexOptionProvider provider, VirtualFile file) {
            return orderEntry == null ? provider.getOptions(module, file) : provider.getOptions(orderEntry, file);
        }
    }

    private static final Comparator<OrderEntry> ORDER = Comparator
        .comparing((OrderEntry entry) -> entry.getOwnerModule().getName())
        .thenComparingInt(ModuleAwareIndexContexts::positionOf);

    private ModuleAwareIndexContexts() {
    }

    /**
     * The contexts of {@code file}, primary first; empty when no module reaches it.
     */
    public static List<Context> contextsOf(Project project, VirtualFile file) {
        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
        Module module = fileIndex.getModuleForFile(file);
        if (module != null) {
            return List.of(new Context(module, null));
        }
        if (!fileIndex.isInLibrary(file)) {
            return List.of();
        }

        List<OrderEntry> entries = new ArrayList<>(fileIndex.getOrderEntriesForFile(file));
        entries.removeIf(entry -> entry.getOwnerModule().isDisposed());
        entries.sort(ORDER);
        List<Context> contexts = new ArrayList<>(entries.size());
        for (OrderEntry entry : entries) {
            contexts.add(new Context(entry.getOwnerModule(), entry));
        }
        return contexts;
    }

    /**
     * The options {@code provider} gives {@code file} in each of its contexts, primary first and without repeats:
     * contexts compiling the file alike share one variant.
     */
    public static List<IndexOption> optionsOf(ModuleAwareIndexOptionProvider provider, List<Context> contexts, VirtualFile file) {
        List<IndexOption> options = new ArrayList<>(Math.min(contexts.size(), MAX_VARIANTS));
        List<byte[]> payloads = new ArrayList<>(options.size());
        for (Context context : contexts) {
            IndexOption option = context.optionsOf(provider, file);
            byte[] payload = IndexOptionHasher.payloadOf(option);
            if (contains(options, payloads, option, payload)) {
                continue;
            }
            options.add(option);
            payloads.add(payload);
            if (options.size() == MAX_VARIANTS) {
                break;
            }
        }
        return options;
    }

    private static boolean contains(List<IndexOption> options, List<byte[]> payloads, IndexOption option, byte[] payload) {
        OptionsMeta.VariantTag tag = OptionsRevalidator.tagOf(option);
        for (int i = 0; i < options.size(); i++) {
            if (OptionsRevalidator.tagOf(options.get(i)) == tag && Arrays.equals(payloads.get(i), payload)) {
                return true;
            }
        }
        return false;
    }

    private static int positionOf(OrderEntry entry) {
        OrderEntry[] entries = ModuleRootManager.getInstance(entry.getOwnerModule()).getOrderEntries();
        for (int i = 0; i < entries.length; i++) {
            if (entries[i].equals(entry)) {
                return i;
            }
        }
        return entries.length;
    }
}
