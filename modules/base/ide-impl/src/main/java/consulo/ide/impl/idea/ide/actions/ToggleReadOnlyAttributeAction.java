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

/**
 * @author Vladimir Kondratyev
 */
package consulo.ide.impl.idea.ide.actions;

import consulo.application.CommonBundle;
import consulo.application.ApplicationManager;
import consulo.dataContext.DataContext;
import consulo.document.FileDocumentManager;
import consulo.application.dumb.DumbAware;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.io.ReadOnlyAttributeUtil;

import java.io.IOException;
import java.util.ArrayList;

public class ToggleReadOnlyAttributeAction extends AnAction implements DumbAware {
  static VirtualFile[] getFiles(DataContext dataContext){
    ArrayList<VirtualFile> filesList = new ArrayList<VirtualFile>();
    VirtualFile[] files = dataContext.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
    for(int i=0;files!=null&&i<files.length;i++){
      VirtualFile file=files[i];
      if(file.isInLocalFileSystem()){
        filesList.add(file);
      }
    }
    return VfsUtil.toVirtualFileArray(filesList);
  }

  public void update(AnActionEvent e){
    VirtualFile[] files=getFiles(e.getDataContext());
    e.getPresentation().setEnabled(files.length>0);
    if (files.length > 0) {
      boolean allReadOnly = true;
      boolean allWritable = true;
      for (VirtualFile file : files) {
        if (file.isWritable()) {
          allReadOnly = false;
        }
        else {
          allWritable = false;
        }
      }
      if (allReadOnly) {
        e.getPresentation().setText(files.length > 1 ? "Make Files Writable" : "Make File Writable");
      }
      else if (allWritable) {
        e.getPresentation().setText(files.length > 1 ? "Make Files Read-only" : "Make File Read-only");
      }
      else {
        e.getPresentation().setText("Toggle Read-only Attribute");
      }
    }

  }

  public void actionPerformed(final AnActionEvent e){
    ApplicationManager.getApplication().runWriteAction(
      new Runnable(){
        public void run(){
          // Save all documents. We won't be able to save changes to the files that became read-only afterwards.
          FileDocumentManager.getInstance().saveAllDocuments();

          try {
            VirtualFile[] files=getFiles(e.getDataContext());
            for (VirtualFile file : files) {
              ReadOnlyAttributeUtil.setReadOnlyAttribute(file, file.isWritable());
            }
          }
          catch(IOException exc){
            Project project = e.getData(CommonDataKeys.PROJECT);
            Messages.showMessageDialog(
              project,
              exc.getMessage(),
              CommonBundle.getErrorTitle(),Messages.getErrorIcon()
            );
          }
        }
      }
    );
  }
}
