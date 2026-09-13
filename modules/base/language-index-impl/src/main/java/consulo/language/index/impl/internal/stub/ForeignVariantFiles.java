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
package consulo.language.index.impl.internal.stub;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.file.LanguageFileType;
import consulo.language.impl.internal.psi.stub.FileContentImpl;
import consulo.language.index.impl.internal.moduleAware.VariantDescriptor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.stub.IndexingDataKeys;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A copy of a file parsed under a stored variant other than the one the file is currently viewed under. Its tree has
 * the same shape as the stub tree indexed for that variant, so stub ids of that variant address its elements, while
 * the physical file's own tree stays untouched.
 */
public final class ForeignVariantFiles {
    private static final Key<ConcurrentMap<String, Entry>> CACHE = Key.create("module.aware.foreign.variant.files");

    private record Entry(long stamp, Reference<PsiFile> psi) {
    }

    private ForeignVariantFiles() {
    }

    @RequiredReadAction
    public static @Nullable PsiFile get(Project project, VirtualFile file, VariantDescriptor descriptor) {
        PsiFile original = PsiManager.getInstance(project).findFile(file);
        if (original == null) {
            return null;
        }
        ConcurrentMap<String, Entry> cache = file.getUserData(CACHE);
        if (cache == null) {
            cache = file.putUserDataIfAbsent(CACHE, new ConcurrentHashMap<>());
        }
        String key = project.getLocationHash() + '#' + descriptor.key();
        long stamp = original.getViewProvider().getModificationStamp();
        Entry entry = cache.get(key);
        PsiFile cached = entry == null ? null : entry.psi().get();
        if (cached != null && entry.stamp() == stamp && cached.isValid()) {
            return cached;
        }
        FileType fileType = original.getFileType();
        if (!(fileType instanceof LanguageFileType languageFileType)) {
            return null;
        }
        PsiFile copy = FileContentImpl.createFileFromText(project, original.getViewProvider().getContents(), languageFileType, file, file.getName());
        copy.putUserData(IndexingDataKeys.INDEX_OPTIONS, descriptor.payloadMap());
        copy.putUserData(IndexingDataKeys.VIRTUAL_FILE, file);
        cache.put(key, new Entry(stamp, new SoftReference<>(copy)));
        return copy;
    }
}
