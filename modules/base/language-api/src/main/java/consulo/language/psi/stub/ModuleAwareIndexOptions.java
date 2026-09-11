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
package consulo.language.psi.stub;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.index.io.data.DataExternalizer;
import consulo.language.internal.psi.stub.IndexOptionImpl;
import consulo.language.internal.psi.stub.ModuleAwareIndexOptionsRescanner;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.light.LightVirtualFileBase;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * How a {@link ModuleAwareIndexOptionProvider} says that the options it reports have moved.
 * <p>
 * The named files are scanned again, and the scan reindexes only those whose recorded options really differ, so a
 * provider does not have to work out which files are affected. Use this rather than
 * {@link FileBasedIndex#requestReindex}: that one means the file itself changed and puts it in the queue of files
 * dirtied on disk, which is carried across sessions and drives the decision to skip a full scan on the next open.
 */
public final class ModuleAwareIndexOptions {
    /**
     * The option payloads, by provider id, a physical file is currently viewed under. Set by navigation with a
     * context and cleared by "view original"; parsing and highlighting of the file follow it, and stub index
     * queries take the matching stored variant as the file's current one.
     */
    public static final Key<Map<String, byte[]>> VIEW_OPTIONS = Key.create("module.aware.index.view.options");

    private static final ThreadLocal<IndexOptionSelector> SELECTOR = new ThreadLocal<>();

    private ModuleAwareIndexOptions() {
    }

    /**
     * The options of {@code file} for the given provider: the value recorded for the file's primary variant by the
     * platform, or the provider's own {@link ModuleAwareIndexOptionProvider#getOptions} default when nothing was
     * analysed yet. {@code null} when the provider is unknown, the file belongs to no module, or the option carries
     * no payload. Safe to call while indexing or parsing the file.
     */
    public static <T extends Record> @Nullable T getOptions(Project project, VirtualFile file, String providerId, DataExternalizer<T> externalizer) {
        ModuleAwareIndexOptionsRescanner service = Application.get().getInstance(ModuleAwareIndexOptionsRescanner.class);
        byte[] payload = service.getRecordedOptionPayload(file, providerId);
        if (payload != null) {
            return decode(payload, externalizer);
        }
        IndexOption option = service.getDefaultOptions(project, file, providerId);
        if (option instanceof IndexOptionImpl.SharablePerOption<?> sharable) {
            @SuppressWarnings("unchecked") T value = (T) sharable.value();
            return value;
        }
        return null;
    }

    /**
     * The same, for the file a parser or indexer is working on: the payloads attached to the PSI being built (a
     * secondary stub variant, or a copy that serves one), then the physical file's view options, then its recorded
     * primary variant. Light copies resolve to the physical file behind them.
     */
    public static <T extends Record> @Nullable T getOptions(PsiElement context, String providerId, DataExternalizer<T> externalizer) {
        PsiFile psiFile = context.getContainingFile();
        Map<String, byte[]> attached = psiFile == null ? null : psiFile.getUserData(IndexingDataKeys.INDEX_OPTIONS);
        if (attached != null) {
            byte[] payload = attached.get(providerId);
            return payload == null ? null : decode(payload, externalizer);
        }
        VirtualFile file = physicalFileOf(context);
        if (file == null) {
            return null;
        }
        Map<String, byte[]> view = file.getUserData(VIEW_OPTIONS);
        if (view != null) {
            byte[] payload = view.get(providerId);
            if (payload != null) {
                return decode(payload, externalizer);
            }
        }
        return getOptions(context.getProject(), file, providerId, externalizer);
    }

    /**
     * Runs {@code computation} with {@code selector} deciding which stored variant of each file stub index queries
     * return.
     */
    public static <T> T withSelector(IndexOptionSelector selector, Supplier<T> computation) {
        IndexOptionSelector previous = SELECTOR.get();
        SELECTOR.set(selector);
        try {
            return computation.get();
        }
        finally {
            if (previous == null) {
                SELECTOR.remove();
            }
            else {
                SELECTOR.set(previous);
            }
        }
    }

    public static @Nullable IndexOptionSelector currentSelector() {
        return SELECTOR.get();
    }

    /**
     * Makes {@code file} be viewed under the given options, one per provider, or under its recorded primary variant
     * when {@code optionsByProvider} is {@code null}; the file is reparsed so its tree follows. Options that name no
     * stored variant of the file are refused, since the file's tree and its stubs must describe the same variant.
     *
     * @return whether the file is now viewed under the requested options
     */
    public static boolean setViewOptions(Project project, VirtualFile file, @Nullable Map<String, IndexOption> optionsByProvider) {
        ModuleAwareIndexOptionsRescanner service = Application.get().getInstance(ModuleAwareIndexOptionsRescanner.class);
        Map<String, byte[]> payloads = null;
        if (optionsByProvider != null) {
            payloads = new HashMap<>();
            for (Map.Entry<String, IndexOption> entry : optionsByProvider.entrySet()) {
                payloads.put(entry.getKey(), service.payloadOf(entry.getValue()));
            }
            payloads = Map.copyOf(payloads);
        }
        Map<String, byte[]> previous = file.getUserData(VIEW_OPTIONS);
        if (samePayloads(previous, payloads)) {
            return true;
        }
        return service.applyViewOptions(project, file, payloads);
    }

    /**
     * The same as {@link #setViewOptions(Project, VirtualFile, Map)} for options already in their stored payload form,
     * such as the ones a PSI copy of another variant carries under {@link IndexingDataKeys#INDEX_OPTIONS}.
     */
    public static boolean setViewOptionPayloads(Project project, VirtualFile file, @Nullable Map<String, byte[]> payloads) {
        Map<String, byte[]> previous = file.getUserData(VIEW_OPTIONS);
        if (samePayloads(previous, payloads)) {
            return true;
        }
        return Application.get().getInstance(ModuleAwareIndexOptionsRescanner.class).applyViewOptions(project, file, payloads);
    }

    /**
     * For an element of a PSI copy that serves another stored variant of a file: the physical file, switched to be
     * viewed under that variant, so that navigation into the copy lands in the real file showing the same variant.
     * {@code null} for elements of ordinary files.
     */
    @RequiredReadAction
    public static @Nullable VirtualFile physicalFileOfVariantCopy(PsiElement element) {
        PsiFile containingFile = element.getContainingFile();
        if (containingFile == null) {
            return null;
        }
        Map<String, byte[]> options = containingFile.getUserData(IndexingDataKeys.INDEX_OPTIONS);
        VirtualFile physical = containingFile.getUserData(IndexingDataKeys.VIRTUAL_FILE);
        if (options == null || physical == null || !physical.isValid() || physical.equals(containingFile.getViewProvider().getVirtualFile())) {
            return null;
        }
        setViewOptionPayloads(element.getProject(), physical, options);
        return physical;
    }

    public static @Nullable Map<String, byte[]> getViewOptions(VirtualFile file) {
        return file.getUserData(VIEW_OPTIONS);
    }

    private static boolean samePayloads(@Nullable Map<String, byte[]> first, @Nullable Map<String, byte[]> second) {
        if (first == null || second == null) {
            return first == second;
        }
        if (!first.keySet().equals(second.keySet())) {
            return false;
        }
        for (Map.Entry<String, byte[]> entry : first.entrySet()) {
            if (!Arrays.equals(entry.getValue(), second.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    private static <T extends Record> @Nullable T decode(byte[] payload, DataExternalizer<T> externalizer) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            return externalizer.read(in);
        }
        catch (IOException e) {
            return null;
        }
    }

    private static @Nullable VirtualFile physicalFileOf(PsiElement context) {
        PsiFile psiFile = context.getContainingFile();
        VirtualFile file = null;
        if (psiFile != null) {
            file = psiFile.getOriginalFile().getViewProvider().getVirtualFile();
            while (file instanceof LightVirtualFileBase light && light.getOriginalFile() != null) {
                file = light.getOriginalFile();
            }
        }
        if (file == null || file instanceof LightVirtualFileBase) {
            VirtualFile attached = context.getUserData(IndexingDataKeys.VIRTUAL_FILE);
            if (attached == null && psiFile != null) {
                attached = psiFile.getUserData(IndexingDataKeys.VIRTUAL_FILE);
            }
            if (attached != null) {
                file = attached;
            }
        }
        return file;
    }

    /**
     * Tells the platform that the options of {@code files} changed for a reason it cannot observe itself, such as an
     * include seed: their cached provider state is dropped and they are scanned again, which reindexes the ones whose
     * recorded options drifted.
     */
    public static void optionsChanged(Project project, Collection<VirtualFile> files, String reason) {
        if (files.isEmpty()) {
            return;
        }
        Application.get().getInstance(ModuleAwareIndexOptionsRescanner.class).optionsChanged(project, files, reason);
    }
}
