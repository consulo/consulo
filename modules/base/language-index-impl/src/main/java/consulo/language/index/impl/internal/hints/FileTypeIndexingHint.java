// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.hints;

import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.IndexedFile;
import consulo.util.lang.ThreeState;
import consulo.virtualFileSystem.fileType.FileType;

/**
 * <b>TL;DR;</b>
 * Use {@link FileTypeIndexingHint} with {@link FileBasedIndex.InputFilter} to speed up scanning.
 * <p>
 * {@link FileTypeIndexingHint} provides a hint to indexing framework if this index filter accepts some specific filetype. Note that
 * filetype can be either real filetype, or {@link consulo.language.internal.SubstitutedFileType}.
 * <p>
 * Use {@link BaseFileTypeInputFilter} as a base class instead of implementing this interface directly.
 * <p>
 * <b>Long story</b>
 * <p>
 * The main goal of the hints framework is to avoid iterating the whole VFS on startup just to evaluate if particular
 * file should be indexed by a particular indexer. That's why we try to shift the focus from individual files to large group of files.
 * <p>
 * In runtime indexing framework first evaluates {@link #acceptsFileTypeFastPath}. If {@link #acceptsFileTypeFastPath} returns
 * {@code YES} or {@code NO}, this value is used for any file with the same filetype. If {@link #acceptsFileTypeFastPath} returns
 * {@link ThreeState#UNSURE}, the framework will switch to "slow" mode and will invoke {@link #slowPathIfFileTypeHintUnsure} for each
 * indexable file.
 * <p>
 * Hint results are cached (at least until IDE restart). In particular, if hint uses ExtensionPoints to evaluate result, changes
 * in relevant extension points (e.g. loading/unloading plugins) should reset caches.
 * <p>
 * {@link #slowPathIfFileTypeHintUnsure} is only invoked when {@link #acceptsFileTypeFastPath} returned {@code UNSURE}, there is no need
 * to add the logic from {@link #acceptsFileTypeFastPath} to {@link #slowPathIfFileTypeHintUnsure}. But this logic still should be added
 * to {@link FileBasedIndex.InputFilter#acceptInput} as explained below.
 * <p>
 * <b>When used with {@link FileBasedIndex.InputFilter}:</b>
 * <p>
 * Indexing framework evaluates {@link #acceptsFileTypeFastPath} and falls back to {@link #slowPathIfFileTypeHintUnsure}.
 * {@link #slowPathIfFileTypeHintUnsure} must answer either {@code true} or {@code false}. This means that indexing framework
 * will not invoke {@link FileBasedIndex.InputFilter#acceptInput}. However, there may be other clients which may
 * invoke {@code acceptInput} without analyzing any hints. Therefore, {@code acceptInput} should provide answer just like if the filter
 * didn't have any hints in the first place.
 * <p>
 * <b>When used with {@link consulo.language.index.impl.internal.GlobalIndexFilter}:</b>
 * <p>
 * You cannot use this {@link FileTypeIndexingHint} with {@link consulo.language.index.impl.internal.GlobalIndexFilter} directly.
 * Please use {@link GlobalIndexSpecificIndexingHint} instead.
 * <p>
 * <b>A few words about filetype substitution.</b>
 * <p>
 * Languages can be adjusted on the fly (usually, more generic languages are substituted with more specific languages,
 * for example, YAML language can be interpreted as EERbLanguage, or SQL can be substituted with concrete dialect).
 * <p>
 * If a language for given file is substituted, IDE has two options how to index the file: either using original file type, or
 * default filetype of substituted language.
 * I.e. in the case when particular YAML is substituted with EERb language, {@link FileTypeIndexingHint} will
 * be invoked with {@code SubstitutedFileType{ErbFileType, YAMLFileType}} (because ERbFileType is the default filetype for EERbLanguage).
 * {@link BaseFileTypeInputFilter} in its turn will resolve {@code SubstitutedFileType} to {@code ErbFileType}
 * (this simplifies {@code BaseFileTypeInputFilter} subclasses implementation so that they don't need to care much about
 * {@code SubstitutedFileType})
 *
 * @see consulo.language.psi.LanguageSubstitutor
 */
public interface FileTypeIndexingHint {
    /**
     * @return {@link ThreeState#YES} if this filter accepts a file of given fileType, {@link ThreeState#NO} if it doesn't, and
     * {@link ThreeState#UNSURE} if {@link #slowPathIfFileTypeHintUnsure} must be called to find out
     */
    ThreeState acceptsFileTypeFastPath(FileType fileType);

    /**
     * @return true if a file should be included in indexing, false otherwise
     */
    boolean slowPathIfFileTypeHintUnsure(IndexedFile file);
}
