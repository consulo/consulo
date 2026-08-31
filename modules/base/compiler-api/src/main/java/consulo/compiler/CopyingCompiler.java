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
package consulo.compiler;

import consulo.compiler.localize.CompilerLocalize;
import consulo.compiler.scope.CompileScope;
import consulo.index.io.data.IOUtil;
import consulo.util.io.FilePermissionCopier;
import consulo.util.io.FileUtil;
import org.jspecify.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Compiler which copies the compiled files to a different directory.
 */
public abstract class CopyingCompiler implements PackagingCompiler {
    public abstract Path[] getFilesToCopy(CompileContext context);

    public abstract String getDestinationPath(CompileContext context, Path sourceFile);

    @Override
    public final void processOutdatedItem(CompileContext context, Path file, @Nullable ValidityState state) {
        if (state != null) {
            String destinationPath = ((DestinationFileInfo) state).getDestinationPath();
            new File(destinationPath).delete();
        }
    }

    @Override
    public final ProcessingItem[] getProcessingItems(CompileContext context) {
        Path[] filesToCopy = getFilesToCopy(context);
        ProcessingItem[] items = new ProcessingItem[filesToCopy.length];
        for (int idx = 0; idx < filesToCopy.length; idx++) {
            Path file = filesToCopy[idx];
            items[idx] = new CopyItem(file, getDestinationPath(context, file));
        }
        return items;
    }

    @Override
    public ProcessingItem[] process(CompileContext context, ProcessingItem[] items) {
        List<ProcessingItem> successfullyProcessed = new ArrayList<>(items.length);
        for (ProcessingItem item : items) {
            CopyItem copyItem = (CopyItem) item;
            String toPath = copyItem.getDestinationPath();
            try {
                if (isDirectoryCopying()) {
                    FileUtil.copyDir(copyItem.getFile().toFile(), new File(toPath), FilePermissionCopier.BY_NIO2);
                }
                else {
                    FileUtil.copy(copyItem.getFile().toFile(), new File(toPath), FilePermissionCopier.BY_NIO2);
                }

                successfullyProcessed.add(copyItem);
            }
            catch (IOException e) {
                context.newError(CompilerLocalize.errorCopying(item.getFile().toString(), toPath, e.getMessage())).add();
            }
        }
        return successfullyProcessed.toArray(new ProcessingItem[successfullyProcessed.size()]);
    }

    protected boolean isDirectoryCopying() {
        return false;
    }

    @Override
    public String getDescription() {
        return CompilerLocalize.fileCopyingCompilerDescription().get();
    }

    @Override
    public boolean validateConfiguration(CompileScope scope) {
        return true;
    }

    @Override
    public ValidityState createValidityState(DataInput in) throws IOException {
        return new DestinationFileInfo(IOUtil.readString(in), true);
    }

    private static class CopyItem implements FileProcessingCompiler.ProcessingItem {
        private final Path myFile;
        private final DestinationFileInfo myInfo;

        public CopyItem(Path file, String destinationPath) {
            myFile = file;
            myInfo = new DestinationFileInfo(destinationPath, new File(destinationPath).exists());
        }

        @Override
        public Path getFile() {
            return myFile;
        }

        @Override
        public ValidityState getValidityState() {
            return myInfo;
        }

        public String getDestinationPath() {
            return myInfo.getDestinationPath();
        }
    }

    private static class DestinationFileInfo implements ValidityState {
        private final String destinationPath;
        private final boolean myFileExists;

        public DestinationFileInfo(String destinationPath, boolean fileExists) {
            this.destinationPath = destinationPath;
            myFileExists = fileExists;
        }

        @Override
        public boolean equalsTo(ValidityState otherState) {
            //noinspection SimplifiableIfStatement
            if (!(otherState instanceof DestinationFileInfo destinationFileInfo)) {
                return false;
            }
            return myFileExists == destinationFileInfo.myFileExists
                && destinationPath.equals(destinationFileInfo.destinationPath);
        }

        @Override
        public void save(DataOutput out) throws IOException {
            IOUtil.writeString(destinationPath, out);
        }

        public String getDestinationPath() {
            return destinationPath;
        }
    }
}
