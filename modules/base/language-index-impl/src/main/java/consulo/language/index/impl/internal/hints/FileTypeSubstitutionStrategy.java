// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.hints;

import consulo.language.internal.SubstitutedFileType;

/**
 * {@link SubstitutedFileType} is an error-prone way to use filetype. Effectively it means that some file has two file types:
 * before substitution ({@link SubstitutedFileType#getOriginalFileType()}) and after substitution
 * ({@link SubstitutedFileType#getFileType()}).
 * <p>
 * Client code often does not expect that it needs to deal with some artificial {@link SubstitutedFileType} that is not equal to
 * any real filetype (e.g. JavaFileType). To simplify API, some classes (e.g. {@link BaseFileTypeInputFilter}) resolve
 * {@link SubstitutedFileType} to real filetype. {@link FileTypeSubstitutionStrategy} defines the resolution strategy.
 */
public enum FileTypeSubstitutionStrategy {
    BEFORE_SUBSTITUTION,
    AFTER_SUBSTITUTION
}
