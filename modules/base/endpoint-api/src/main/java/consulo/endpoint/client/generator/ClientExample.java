// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.client.generator;

import consulo.language.file.FileTypeManager;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class ClientExample {
    private final String myText;
    private final FileType myFileType;

    public ClientExample(String text, FileType fileType) {
        myText = text;
        myFileType = fileType;
    }

    public static ClientExample fromFileExtension(String text, String extension) {
        FileType fileType = FileTypeManager.getInstance().getFileTypeByExtension(extension);

        return new ClientExample(text, fileType);
    }

    public String getText() {
        return myText;
    }

    public FileType getFileType() {
        return myFileType;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClientExample that)) {
            return false;
        }
        return myText.equals(that.myText) && myFileType.equals(that.myFileType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myText, myFileType);
    }

    @Override
    public String toString() {
        return "ClientExample(text=" + myText + ", fileType=" + myFileType + ")";
    }
}
