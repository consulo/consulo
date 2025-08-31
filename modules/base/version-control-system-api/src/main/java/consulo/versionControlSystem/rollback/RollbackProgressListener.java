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
package consulo.versionControlSystem.rollback;

import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.Change;
import consulo.virtualFileSystem.VirtualFile;

import java.io.File;
import java.util.List;

/**
 * {@link RollbackEnvironment} implementation should notify about starting change processing
 * using this interface; change processing can be reported using any <code>accept()</code> method signature,
 * should be reported once per change
 */
public interface RollbackProgressListener {
  RollbackProgressListener EMPTY = new RollbackProgressListener() {
    @Override
    public void accept(Change change) {
    }

    @Override
    public void accept(FilePath filePath) {
    }

    @Override
    public void accept(List<FilePath> paths) {
    }

    @Override
    public void accept(File file) {
    }

    @Override
    public void accept(VirtualFile file) {
    }

    @Override
    public void checkCanceled() {
    }

    @Override
    public void indeterminate() {
    }

    @Override
    public void determinate() {
    }
  };

  void determinate();

  void indeterminate();

  void accept(Change change);

  void accept(FilePath filePath);

  void accept(List<FilePath> paths);

  void accept(File file);

  void accept(VirtualFile file);

  void checkCanceled();
}
