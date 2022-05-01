/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.fileEditor.impl.http;

import consulo.application.CommonBundle;
import consulo.application.AllIcons;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ide.impl.idea.openapi.fileEditor.OpenFileDescriptorImpl;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.roots.ui.FileAppearanceService;
import consulo.ui.ex.awt.Messages;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.http.HttpVirtualFile;
import consulo.virtualFileSystem.http.RemoteFileState;
import consulo.ide.impl.psi.search.FilenameIndex;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.JBList;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;

/**
 * @author nik
 */
public class JumpFromRemoteFileToLocalAction extends AnAction {
  private final HttpVirtualFile myFile;
  private final Project myProject;

  public JumpFromRemoteFileToLocalAction(HttpVirtualFile file, Project project) {
    super("Find Local File", "", AllIcons.General.AutoscrollToSource);
    myFile = file;
    myProject = project;
  }

  @Override
  public void update(AnActionEvent e) {
    final RemoteFileState state = myFile.getFileInfo().getState();
    e.getPresentation().setEnabled(state == RemoteFileState.DOWNLOADED);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final String url = myFile.getUrl();
    final String fileName = myFile.getName();
    Collection<VirtualFile> files = findLocalFiles(myProject, url, fileName);

    if (files.isEmpty()) {
      Messages.showErrorDialog(myProject, "Cannot find local file for '" + url + "'", CommonBundle.getErrorTitle());
      return;
    }

    if (files.size() == 1) {
      navigateToFile(myProject, ContainerUtil.getFirstItem(files, null));
    }
    else {
      final JList list = new JBList(VfsUtilCore.toVirtualFileArray(files));
      list.setCellRenderer(new ColoredListCellRenderer() {
        @Override
        protected void customizeCellRenderer(JList list, Object value, int index, boolean selected, boolean hasFocus) {
          FileAppearanceService.getInstance().forVirtualFile((VirtualFile)value).customize(this);
        }
      });
      new PopupChooserBuilder(list)
       .setTitle("Select Target File")
       .setMovable(true)
       .setItemChoosenCallback(new Runnable() {
         @Override
         public void run() {
           for (Object value : list.getSelectedValues()) {
             navigateToFile(myProject, (VirtualFile)value);
           }
         }
       }).createPopup().showUnderneathOf(e.getInputEvent().getComponent());
    }
  }

  public static Collection<VirtualFile> findLocalFiles(Project project, String url, String fileName) {
    for (LocalFileFinder finder : LocalFileFinder.EP_NAME.getExtensions()) {
      final VirtualFile file = finder.findLocalFile(url, project);
      if (file != null) {
        return Collections.singletonList(file);
      }
    }

    return FilenameIndex.getVirtualFilesByName(project, fileName, GlobalSearchScope.allScope(project));
  }

  private static void navigateToFile(Project project, @Nonnull VirtualFile file) {
    new OpenFileDescriptorImpl(project, file).navigate(true);
  }
}
