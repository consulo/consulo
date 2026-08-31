/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.compiler.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.compiler.CompileContext;
import consulo.compiler.ResourceCompilerExtension;
import consulo.compiler.localize.CompilerLocalize;
import consulo.compiler.resourceCompiler.ResourceCompiler;
import consulo.compiler.resourceCompiler.ResourceCompilerConfiguration;
import consulo.compiler.scope.CompileScope;
import consulo.compiler.util.CompilerUtil;
import consulo.compiler.util.MakeUtil;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.Chunk;
import consulo.util.io.FileUtil;
import consulo.util.lang.ExceptionUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * @author Eugene Zhuravlev
 * @since 2003-01-17
 */
@ExtensionImpl(id = "resourceCompiler")
public class ResourceCompilerImpl implements ResourceCompiler {
    private static final Logger LOG = Logger.getInstance(ResourceCompilerImpl.class);

    private final ResourceCompilerConfiguration myResourceCompilerConfiguration;
    private final ProjectFileIndex myProjectFileIndex;

    @Inject
    public ResourceCompilerImpl(Project project) {
        myResourceCompilerConfiguration = ResourceCompilerConfiguration.getInstance(project);
        myProjectFileIndex = ProjectFileIndex.getInstance(project);
    }

    @Override
    public String getDescription() {
        return CompilerLocalize.resourceCompilerDescription().get();
    }

    @Override
    @RequiredUIAccess
    public boolean validateConfiguration(CompileScope scope) {
        myResourceCompilerConfiguration.convertPatterns();
        return true;
    }

    @Override
    public boolean isCompilableFile(Path file, CompileContext context) {
        Module module = context.getModuleByFile(file);
        if (module == null) {
            return false;
        }

        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByNioFile(file);
        if (virtualFile != null && (myProjectFileIndex.isInResource(virtualFile) || myProjectFileIndex.isInTestResource(virtualFile))) {
            return true;
        }
        //noinspection SimplifiableIfStatement
        if (skipStandardResourceCompiler(module)) {
            return false;
        }
        return myResourceCompilerConfiguration.isResourceFile(file);
    }

    @Override
    public void compile(CompileContext context, Chunk<Module> moduleChunk, Collection<Path> files, OutputSink sink) {
        context.getProgressIndicator().pushState();
        context.getProgressIndicator().setText(CompilerLocalize.progressCopyingResources());

        Map<String, Collection<OutputItem>> processed = new HashMap<>();
        LinkedList<CopyCommand> copyCommands = new LinkedList<>();
        Module singleChunkModule = moduleChunk.getNodes().size() == 1 ? moduleChunk.getNodes().iterator().next() : null;
        Application.get().runReadAction(() -> {
            for (Path file : files) {
                if (context.getProgressIndicator().isCanceled()) {
                    break;
                }
                Module module = singleChunkModule != null ? singleChunkModule : context.getModuleByFile(file);
                if (module == null) {
                    continue; // looks like file invalidated
                }
                Path fileRoot = MakeUtil.getSourceRoot(context, module, file);
                if (fileRoot == null) {
                    continue;
                }
                String sourcePath = FileUtil.toSystemIndependentName(file.toString());
                String relativePath = FileUtil.toSystemIndependentName(fileRoot.relativize(file).toString());
                Path outputDir = context.getOutputForFile(module, file);
                if (outputDir == null) {
                    continue;
                }
                String outputPath = FileUtil.toSystemIndependentName(outputDir.toString());

                String packagePrefix = getPackagePrefix(fileRoot);
                String targetPath;
                if (packagePrefix != null && packagePrefix.length() > 0) {
                    targetPath = outputPath + "/" + packagePrefix.replace('.', '/') + "/" + relativePath;
                }
                else {
                    targetPath = outputPath + "/" + relativePath;
                }
                if (sourcePath.equals(targetPath)) {
                    addToMap(processed, outputPath, new OutputItem(targetPath, file));
                }
                else {
                    copyCommands.add(new CopyCommand(outputPath, sourcePath, targetPath, file));
                }
            }
        });

        List<File> filesToRefresh = new ArrayList<>();
        // do actual copy outside of read action to reduce the time the application is locked on it
        while (!copyCommands.isEmpty()) {
            CopyCommand command = copyCommands.removeFirst();
            if (context.getProgressIndicator().isCanceled()) {
                break;
            }
            context.getProgressIndicator().setText2(LocalizeValue.localizeTODO("Copying " + command.getFromPath() + "..."));
            try {
                OutputItem outputItem = command.copy(filesToRefresh);
                addToMap(processed, command.getOutputPath(), outputItem);
            }
            catch (IOException e) {
                context.newError(
                        CompilerLocalize.errorCopying(command.getFromPath(), command.getToPath(), ExceptionUtil.getThrowableText(e))
                    )
                    .url(command.getSourceFileUrl())
                    .add();
            }
        }

        if (!filesToRefresh.isEmpty()) {
            CompilerUtil.refreshIOFiles(filesToRefresh);
            filesToRefresh.clear();
        }

        for (Iterator<Map.Entry<String, Collection<OutputItem>>> it = processed.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Collection<OutputItem>> entry = it.next();
            sink.add(entry.getKey(), entry.getValue(), List.of());
            it.remove(); // to free memory
        }
        context.getProgressIndicator().popState();
    }

    private String getPackagePrefix(Path fileRoot) {
        VirtualFile rootFile = LocalFileSystem.getInstance().findFileByNioFile(fileRoot);
        return rootFile == null ? null : myProjectFileIndex.getPackageNameByDirectory(rootFile);
    }

    @Override
    public FileType[] getInputFileTypes() {
        return FileType.EMPTY_ARRAY;
    }

    @Override
    public FileType[] getOutputFileTypes() {
        return FileType.EMPTY_ARRAY;
    }

    private boolean skipStandardResourceCompiler(Module module) {
        for (ResourceCompilerExtension extension : module.getApplication().getExtensionPoint(ResourceCompilerExtension.class)) {
            if (extension.skipStandardResourceCompiler(module)) {
                return true;
            }
        }
        return false;
    }

    private static void addToMap(Map<String, Collection<OutputItem>> map, String outputDir, OutputItem item) {
        Collection<OutputItem> list = map.get(outputDir);
        if (list == null) {
            list = new ArrayList<>();
            map.put(outputDir, list);
        }
        list.add(item);
    }

    private static class CopyCommand {
        private final String myOutputPath;
        private final String myFromPath;
        private final String myToPath;
        private final Path mySourceFile;

        private CopyCommand(String outputPath, String fromPath, String toPath, Path sourceFile) {
            myOutputPath = outputPath;
            myFromPath = fromPath;
            myToPath = toPath;
            mySourceFile = sourceFile;
        }

        public OutputItem copy(List<File> filesToRefresh) throws IOException {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Copying " + myFromPath + " to " + myToPath);
            }
            Path targetFile = Path.of(FileUtil.toSystemDependentName(myToPath));
            Path parent = targetFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(mySourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            filesToRefresh.add(targetFile.toFile());
            return new OutputItem(myToPath, mySourceFile);
        }

        public String getOutputPath() {
            return myOutputPath;
        }

        public String getFromPath() {
            return myFromPath;
        }

        public String getToPath() {
            return myToPath;
        }

        public String getSourceFileUrl() {
            return VirtualFileUtil.pathToUrl(myFromPath);
        }
    }

}
