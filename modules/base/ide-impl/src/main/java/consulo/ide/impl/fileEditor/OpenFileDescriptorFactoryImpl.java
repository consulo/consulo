/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.fileEditor;

import consulo.ide.impl.idea.openapi.fileEditor.OpenFileDescriptorImpl;
import consulo.navigation.OpenFileDescriptor;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 19-Feb-22
 */
@Singleton
public class OpenFileDescriptorFactoryImpl implements OpenFileDescriptorFactory {

  private static class BuilderImp implements Builder {
    private final Project myProject;
    private final VirtualFile myFile;

    private int myOffset = -1;
    private int myLine = -1;
    private int myColumn = -1;
    private boolean myUseCurrentWindow;

    public BuilderImp(Project project, VirtualFile file) {
      myProject = project;
      myFile = file;
    }

    @Nonnull
    @Override
    public Builder offset(int offset) {
      myOffset = offset;
      return this;
    }

    @Nonnull
    @Override
    public Builder line(int line) {
      myLine = line;
      return this;
    }

    @Nonnull
    @Override
    public Builder column(int column) {
      myColumn = column;
      return this;
    }

    @Nonnull
    @Override
    public Builder useCurrentWindow(boolean useCurrentWindow) {
      myUseCurrentWindow = useCurrentWindow;
      return this;
    }

    @Nonnull
    @Override
    public OpenFileDescriptor build() {
      OpenFileDescriptorImpl descriptor = new OpenFileDescriptorImpl(myProject, myFile, myLine, myColumn, myOffset, false);
      descriptor.setUseCurrentWindow(myUseCurrentWindow);
      return descriptor;
    }
  }

  private final Project myProject;

  @Inject
  public OpenFileDescriptorFactoryImpl(Project project) {
    myProject = project;
  }

  @Nonnull
  @Override
  public Builder builder(@Nonnull VirtualFile file) {
    return new BuilderImp(myProject, file);
  }
}
