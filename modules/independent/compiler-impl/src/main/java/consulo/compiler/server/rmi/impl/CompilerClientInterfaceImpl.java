/*
 * Copyright 2013-2016 consulo.io
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
package consulo.compiler.server.rmi.impl;

import com.intellij.compiler.ProblemsView;
import com.intellij.compiler.progress.CompilerTask;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import consulo.compiler.server.rmi.CompilerClientInterface;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * @author VISTALL
 * @since 15:39/19.08.13
 */
public class CompilerClientInterfaceImpl extends UnicastRemoteObject implements CompilerClientInterface {
  private final Project myProject;

  public CompilerClientInterfaceImpl(Project project) throws RemoteException {
    myProject = project;
  }

  @Override
  public void addMessage(@Nonnull CompilerMessageCategory category, String message, String url, int lineNum, int columnNum)
    throws RemoteException {

    final int type = CompilerTask.translateCategory(category);
    final String[] text = ProblemsView.convertMessage(message);
    VirtualFile file = findFileByUrl(url);
    final String groupName = file != null? file.getPresentableUrl() : category.getPresentableText();

    ProblemsView.getInstance(myProject).addMessage(type, text, groupName, null, null, null);
  }

  @Override
  public void compilationFinished(boolean aborted, int errors, int warnings) throws RemoteException {
  }

  @Nonnull
  @Override
  public String getProjectDir() {
    return myProject.getProjectFilePath();
  }

  @Nullable
  private static VirtualFile findFileByUrl(@Nullable String url) {
    if (url == null) {
      return null;
    }
    VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
    if (file == null) {
      // groovy stubs may be placed in completely random directories which aren't refreshed automatically
      return VirtualFileManager.getInstance().refreshAndFindFileByUrl(url);
    }
    return file;
  }
}
