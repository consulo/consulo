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
package consulo.ai.editor.impl.internal;

import consulo.ai.AIAgentFileSystem;
import consulo.ai.editor.localize.AIEditorLocalize;
import consulo.annotation.component.ServiceImpl;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.application.concurrent.coroutine.WriteLock;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.language.editor.WriteCommandAction;
import consulo.project.Project;
import consulo.project.ProjectLocator;
import consulo.ui.UIAction;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Files of an open project go through its documents, in its coroutine context; any other file through the disk.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ServiceImpl
@Singleton
public class AIAgentFileSystemImpl implements AIAgentFileSystem {
    private static final Object NO_INPUT = new Object();

    private final ApplicationConcurrency myConcurrency;

    @Inject
    public AIAgentFileSystemImpl(ApplicationConcurrency concurrency) {
        myConcurrency = concurrency;
    }

    @Override
    public CompletableFuture<String> readText(Path path) {
        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
        Project project = file == null ? null : ProjectLocator.getInstance().guessProjectForFile(file);
        if (file == null || project == null) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return Files.readString(path, StandardCharsets.UTF_8);
                }
                catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }, myConcurrency.executor());
        }
        return Coroutine.first(ReadLock.<Object, String>apply(ignored -> {
            Document document = FileDocumentManager.getInstance().getDocument(file);
            if (document != null) {
                return document.getText();
            }
            try {
                return VirtualFileUtil.loadText(file);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        })).runAsync(CoroutineScope.of(project.coroutineContext()), NO_INPUT).toFuture();
    }

    @Override
    public CompletableFuture<?> writeText(Path path, String text) {
        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
        VirtualFile parent = file != null ? file.getParent() : LocalFileSystem.getInstance().refreshAndFindFileByNioFile(existingParent(path));
        Project project = parent == null ? null : ProjectLocator.getInstance().guessProjectForFile(file != null ? file : parent);
        if (project == null) {
            return CompletableFuture.runAsync(() -> {
                try {
                    Path directory = path.getParent();
                    if (directory != null) {
                        Files.createDirectories(directory);
                    }
                    Files.writeString(path, text, StandardCharsets.UTF_8);
                }
                catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
            }, myConcurrency.executor());
        }
        // a new file is made first, the text then goes through its document like any other edit
        return Coroutine.first(WriteLock.<Object, VirtualFile>apply(ignored -> file != null ? file : create(path)))
            .then(UIAction.<VirtualFile, Object>apply(target -> {
                Document document = FileDocumentManager.getInstance().getDocument(target);
                if (document == null) {
                    throw new IllegalStateException("Not a text file: " + path);
                }
                WriteCommandAction.runWriteCommandAction(project, AIEditorLocalize.commandAgentEdit().get(), null, () -> {
                    document.setText(text);
                    FileDocumentManager.getInstance().saveDocument(document);
                });
                return target;
            }))
            .runAsync(CoroutineScope.of(project.coroutineContext()), NO_INPUT)
            .toFuture();
    }

    private static Path existingParent(Path path) {
        Path parent = path.toAbsolutePath().getParent();
        while (parent != null && !Files.isDirectory(parent)) {
            parent = parent.getParent();
        }
        return parent == null ? path.toAbsolutePath().getRoot() : parent;
    }

    private static VirtualFile create(Path path) {
        try {
            Path directory = path.toAbsolutePath().getParent();
            VirtualFile parent = directory == null ? null : VirtualFileUtil.createDirectories(directory.toString().replace('\\', '/'));
            if (parent == null) {
                throw new IOException("Cannot create the directory of " + path);
            }
            return parent.createChildData(AIAgentFileSystemImpl.class, path.getFileName().toString());
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
