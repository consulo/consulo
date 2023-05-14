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
package consulo.ide.impl.idea.openapi.vcs.readOnlyHandler;

import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.EditFileProvider;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.InvokeAfterUpdateMode;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import java.util.function.Function;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ui.ex.awt.Messages;
import consulo.virtualFileSystem.VirtualFile;

import java.util.Collection;
import java.util.List;

/**
 * @author yole
 */
public class VcsHandleType extends HandleType {
  private static final Function<LocalChangeList,String> FUNCTION = LocalChangeList::getName;
  private final AbstractVcs myVcs;
  private final ChangeListManager myChangeListManager;
  private final Function<VirtualFile,Change> myChangeFunction;

  public VcsHandleType(AbstractVcs vcs) {
    super(VcsBundle.message("handle.ro.file.status.type.using.vcs", vcs.getDisplayName()), true);
    myVcs = vcs;
    myChangeListManager = ChangeListManager.getInstance(myVcs.getProject());
    myChangeFunction = myChangeListManager::getChange;
  }

  public void processFiles(final Collection<VirtualFile> files, @jakarta.annotation.Nullable final String changelist) {
    try {
      EditFileProvider provider = myVcs.getEditFileProvider();
      assert provider != null;
      provider.editFiles(VfsUtil.toVirtualFileArray(files));
    }
    catch (VcsException e) {
      Messages.showErrorDialog(VcsBundle.message("message.text.cannot.edit.file", e.getLocalizedMessage()),
                               VcsBundle.message("message.title.edit.files"));
    }
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      public void run() {
        for (final VirtualFile file : files) {
          file.refresh(false, false);
        }
      }
    });
    if (changelist != null) {
      myChangeListManager.invokeAfterUpdate(new Runnable() {
        @Override
        public void run() {
          LocalChangeList list = myChangeListManager.findChangeList(changelist);
          if (list != null) {
            List<Change> changes = ContainerUtil.mapNotNull(files, myChangeFunction);
            myChangeListManager.moveChangesTo(list, changes.toArray(new Change[changes.size()]));
          }
        }
      }, InvokeAfterUpdateMode.SILENT, "", IdeaModalityState.NON_MODAL);
    }
  }

  @Override
  public List<String> getChangelists() {
    return ContainerUtil.map(myChangeListManager.getChangeLists(), FUNCTION);
  }

  @Override
  public String getDefaultChangelist() {
    return myChangeListManager.getDefaultListName();
  }
}