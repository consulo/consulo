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
package consulo.fileEditor;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of opening a file in the editor. Contains the list of
 * editors (paired with their providers) that were opened or found.
 *
 * @author VISTALL
 * @since 2026-03-07
 */
public final class FileEditorOpenResult {
    public static final FileEditorOpenResult EMPTY = new FileEditorOpenResult(List.of());

    private final List<FileEditorWithProvider> myEntries;

    public FileEditorOpenResult(@Nonnull List<FileEditorWithProvider> entries) {
        myEntries = entries;
    }

    /**
     * Creates a result from parallel arrays of editors and providers.
     */
    @Nonnull
    public static FileEditorOpenResult of(
        @Nonnull FileEditor[] editors,
        @Nonnull FileEditorProvider[] providers) {
        if (editors.length == 0) {
            return EMPTY;
        }
        List<FileEditorWithProvider> list = new ArrayList<>(editors.length);
        for (int i = 0; i < editors.length; i++) {
            list.add(new FileEditorWithProvider(editors[i], providers[i]));
        }
        return new FileEditorOpenResult(List.copyOf(list));
    }

    @Nonnull
    public List<FileEditorWithProvider> getEntries() {
        return myEntries;
    }

    public boolean isEmpty() {
        return myEntries.isEmpty();
    }

    /**
     * Convenience: returns all editors as an array.
     */
    @Nonnull
    public FileEditor[] getEditors() {
        return myEntries.stream()
            .map(FileEditorWithProvider::getFileEditor)
            .toArray(FileEditor[]::new);
    }

    /**
     * Convenience: returns all providers as an array.
     */
    @Nonnull
    public FileEditorProvider[] getProviders() {
        return myEntries.stream()
            .map(FileEditorWithProvider::getProvider)
            .toArray(FileEditorProvider[]::new);
    }
}
