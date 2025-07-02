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
import consulo.compiler.CompilerMessageCategory;
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
import consulo.util.io.FilePermissionCopier;
import consulo.util.io.FileUtil;
import consulo.util.lang.ExceptionUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.io.File;
import java.io.IOException;
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
    @Nonnull
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
    public boolean isCompilableFile(VirtualFile file, CompileContext context) {
        Module module = context.getModuleByFile(file);
        if (module == null) {
            return false;
        }

        if (myProjectFileIndex.isInResource(file) || myProjectFileIndex.isInTestResource(file)) {
            return true;
        }
        //noinspection SimplifiableIfStatement
        if (skipStandardResourceCompiler(module)) {
            return false;
        }
        return myResourceCompilerConfiguration.isResourceFile(file);
    }

    @Override
    public void compile(CompileContext context, Chunk<Module> moduleChunk, VirtualFile[] files, OutputSink sink) {
        context.getProgressIndicator().pushState();
        context.getProgressIndicator().setTextValue(CompilerLocalize.progressCopyingResources());

        Map<String, Collection<OutputItem>> processed = new HashMap<>();
        LinkedList<CopyCommand> copyCommands = new LinkedList<>();
        Module singleChunkModule = moduleChunk.getNodes().size() == 1 ? moduleChunk.getNodes().iterator().next() : null;
        Application.get().runReadAction(() -> {
            for (VirtualFile file : files) {
                if (context.getProgressIndicator().isCanceled()) {
                    break;
                }
                Module module = singleChunkModule != null ? singleChunkModule : context.getModuleByFile(file);
                if (module == null) {
                    continue; // looks like file invalidated
                }
                VirtualFile fileRoot = MakeUtil.getSourceRoot(context, module, file);
                if (fileRoot == null) {
                    continue;
                }
                String sourcePath = file.getPath();
                String relativePath = VirtualFileUtil.getRelativePath(file, fileRoot, '/');
                VirtualFile outputDir = context.getOutputForFile(module, file);
                if (outputDir == null) {
                    continue;
                }
                String outputPath = outputDir.getPath();

                String packagePrefix = myProjectFileIndex.getPackageNameByDirectory(fileRoot);
                String targetPath;
                if (packagePrefix != null && packagePrefix.length() > 0) {
                    targetPath = outputPath + "/" + packagePrefix.replace('.', '/') + "/" + relativePath;
                }
                else {
                    targetPath = outputPath + "/" + relativePath;
                }
                if (sourcePath.equals(targetPath)) {
                    addToMap(processed, outputPath, new MyOutputItem(targetPath, file));
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
            //context.getProgressIndicator().setFraction((idx++) * 1.0 / total);
            context.getProgressIndicator().setText2Value(LocalizeValue.localizeTODO("Copying " + command.getFromPath() + "..."));
            try {
                MyOutputItem outputItem = command.copy(filesToRefresh);
                addToMap(processed, command.getOutputPath(), outputItem);
            }
            catch (IOException e) {
                context.addMessage(
                    CompilerMessageCategory.ERROR,
                    CompilerLocalize.errorCopying(command.getFromPath(), command.getToPath(), ExceptionUtil.getThrowableText(e)).get(),
                    command.getSourceFileUrl(),
                    -1,
                    -1
                );
            }
        }

        if (!filesToRefresh.isEmpty()) {
            CompilerUtil.refreshIOFiles(filesToRefresh);
            filesToRefresh.clear();
        }

        for (Iterator<Map.Entry<String, Collection<OutputItem>>> it = processed.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Collection<OutputItem>> entry = it.next();
            sink.add(entry.getKey(), entry.getValue(), VirtualFile.EMPTY_ARRAY);
            it.remove(); // to free memory
        }
        context.getProgressIndicator().popState();
    }

    @Nonnull
    @Override
    public FileType[] getInputFileTypes() {
        return FileType.EMPTY_ARRAY;
    }

    @Nonnull
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
        private final VirtualFile mySourceFile;

        private CopyCommand(String outputPath, String fromPath, String toPath, VirtualFile sourceFile) {
            myOutputPath = outputPath;
            myFromPath = fromPath;
            myToPath = toPath;
            mySourceFile = sourceFile;
        }

        public MyOutputItem copy(List<File> filesToRefresh) throws IOException {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Copying " + myFromPath + " to " + myToPath);
            }
            File targetFile = new File(myToPath);
            FileUtil.copyContent(new File(myFromPath), targetFile, FilePermissionCopier.BY_NIO2);
            filesToRefresh.add(targetFile);
            return new MyOutputItem(myToPath, mySourceFile);
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
            // do not use mySourseFile.getUrl() directly as it requires read action
            return VirtualFileManager.constructUrl(mySourceFile.getFileSystem().getProtocol(), myFromPath);
        }
    }

    private static class MyOutputItem implements OutputItem {
        private final String myTargetPath;
        private final VirtualFile myFile;

        private MyOutputItem(String targetPath, VirtualFile sourceFile) {
            myTargetPath = targetPath;
            myFile = sourceFile;
        }

        @Override
        public String getOutputPath() {
            return myTargetPath;
        }

        @Override
        public VirtualFile getSourceFile() {
            return myFile;
        }
    }
}
