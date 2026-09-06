// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.hints;

import consulo.language.psi.stub.IndexedFile;
import consulo.util.lang.StringUtil;

public class FileNameExtensionInputFilter extends BaseWeakBinaryFileInputFilter {
    private final String myDotExtension;
    private final boolean myIgnoreCase;

    public FileNameExtensionInputFilter(String extension, boolean ignoreCase) {
        this(extension, ignoreCase, BinaryFileTypePolicy.BINARY_OR_NON_BINARY);
    }

    public FileNameExtensionInputFilter(String extension, boolean ignoreCase, BinaryFileTypePolicy binary) {
        super(binary, FileTypeSubstitutionStrategy.BEFORE_SUBSTITUTION);
        myDotExtension = "." + extension;
        myIgnoreCase = ignoreCase;
    }

    @Override
    public boolean slowPathIfFileTypeHintUnsure(IndexedFile file) {
        return myIgnoreCase
            ? StringUtil.endsWithIgnoreCase(file.getFileName(), myDotExtension)
            : StringUtil.endsWith(file.getFileName(), myDotExtension);
    }
}
