// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.ide.impl.idea.find.impl;

import consulo.fileEditor.UniqueVFilePathBuilder;
import consulo.fileEditor.VfsPresentationUtil;
import consulo.language.editor.scratch.ScratchUtil;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.usage.TextChunk;
import consulo.usage.UsageInfo2UsageAdapter;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Data model for text search results displayed in Search Everywhere.
 */
public class SearchEverywhereItem {
    private final UsageInfo2UsageAdapter myUsage;
    private final UsagePresentation myPresentation;

    public SearchEverywhereItem(UsageInfo2UsageAdapter usage, UsagePresentation presentation) {
        myUsage = usage;
        myPresentation = presentation;
    }

    public UsageInfo2UsageAdapter getUsage() {
        return myUsage;
    }

    public UsagePresentation getPresentation() {
        return myPresentation;
    }

    public String getPresentableText() {
        return Arrays.stream(myPresentation.getText()).map(TextChunk::getText).collect(Collectors.joining());
    }

    public SearchEverywhereItem withPresentation(UsagePresentation presentation) {
        return new SearchEverywhereItem(myUsage, presentation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchEverywhereItem other = (SearchEverywhereItem) o;
        // Compare by file and offset
        VirtualFile thisFile = myUsage.getFile();
        VirtualFile otherFile = other.myUsage.getFile();
        if (thisFile == null || otherFile == null) return false;
        if (!thisFile.equals(otherFile)) return false;
        if (myUsage.getNavigationOffset() != other.myUsage.getNavigationOffset()) return false;
        return getPresentableText().equals(other.getPresentableText());
    }

    @Override
    public int hashCode() {
        return getPresentableText().hashCode();
    }

    @Override
    public String toString() {
        return "Text: `" + getPresentableText() + "', Usage: " + myUsage;
    }

    /**
     * Presentation data for rendering a search result.
     */
    public static class UsagePresentation {
        private final TextChunk[] myText;
        private final @Nullable ColorValue myBackgroundColor;
        private final String myFileString;

        public UsagePresentation(TextChunk[] text, @Nullable ColorValue backgroundColor, String fileString) {
            myText = text;
            myBackgroundColor = backgroundColor;
            myFileString = fileString;
        }

        public TextChunk[] getText() {
            return myText;
        }

        public @Nullable ColorValue getBackgroundColor() {
            return myBackgroundColor;
        }

        public String getFileString() {
            return myFileString;
        }
    }

    /**
     * Creates a UsagePresentation from a usage adapter.
     * Must be called from a non-dispatch thread.
     */
    public static UsagePresentation usagePresentation(Project project, GlobalSearchScope scope, UsageInfo2UsageAdapter usage) {
        TextChunk[] text = usage.getPresentation().getText();
        VirtualFile file = usage.getFile();
        return new UsagePresentation(
            text,
            file != null ? VfsPresentationUtil.getFileBackgroundColor(project, file) : null,
            file != null ? getPresentableFilePath(project, scope, file) : ""
        );
    }

    public static String getPresentableFilePath(Project project, GlobalSearchScope scope, VirtualFile file) {
        if (ScratchUtil.isScratch(file)) {
            return ScratchUtil.getRelativePath(project, file);
        }
        else {
            return UniqueVFilePathBuilder.getInstance().getUniqueVirtualFilePath(project, file, scope);
        }
    }
}
