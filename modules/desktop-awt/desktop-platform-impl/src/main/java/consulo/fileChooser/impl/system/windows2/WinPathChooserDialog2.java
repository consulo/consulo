/*
 * Copyright 2013-2021 consulo.io
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
package consulo.fileChooser.impl.system.windows2;

import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.CommandProcessorEx;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.impl.FileChooserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.OwnerOptional;
import com.intellij.util.ui.UIUtil;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static consulo.fileChooser.impl.system.windows2.IFileOpenDialog.CLSID_FILEOPENDIALOG;
import static consulo.fileChooser.impl.system.windows2.IFileOpenDialog.IID_IFILEOPENDIALOG;

/**
 * @author VISTALL
 * @since 24/09/2021
 */
public class WinPathChooserDialog2 implements FileChooserDialog {
  private final FileChooserDescriptor myChooserDescriptor;
  private final Component myParent;
  private final Project myProject;

  public WinPathChooserDialog2(FileChooserDescriptor chooserDescriptor, Component parent, Project project) {
    myChooserDescriptor = chooserDescriptor;
    myParent = parent;
    myProject = project;
  }

  @Nonnull
  @Override
  public VirtualFile[] choose(@Nullable Project project, @Nonnull VirtualFile... toSelect) {
    throw new UnsupportedOperationException("impl async version");
  }

  public static FileOpenDialog createNativeOpenDialog() {
    PointerByReference pbr = new PointerByReference();

    Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_APARTMENTTHREADED);

    WinNT.HRESULT hres = Ole32.INSTANCE.CoCreateInstance(CLSID_FILEOPENDIALOG, null, WTypes.CLSCTX_ALL, IID_IFILEOPENDIALOG, pbr);
    if (COMUtils.FAILED(hres)) {
      return null;
    }

    return new FileOpenDialog(pbr.getValue());
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public AsyncResult<VirtualFile[]> chooseAsync(@Nullable Project project, @Nonnull VirtualFile[] toSelectFiles) {
    Window window = OwnerOptional.fromComponent(myParent).get();

    String directoryName = null;
    String fileName = null;
    VirtualFile toSelect = toSelectFiles.length > 1 ? toSelectFiles[0] : null;

    final VirtualFile lastOpenedFile = FileChooserUtil.getLastOpenedFile(myProject);
    final VirtualFile newSelectFile = FileChooserUtil.getFileToSelect(myChooserDescriptor, myProject, toSelect, lastOpenedFile);
    if (newSelectFile != null && newSelectFile.isValid()) {
      toSelect = newSelectFile;
    }

    if (toSelect != null && toSelect.getParent() != null) {
      if (toSelect.isDirectory()) {
        directoryName = toSelect.getCanonicalPath();
      }
      else {
        directoryName = toSelect.getParent().getCanonicalPath();
        fileName = toSelect.getPath();
      }
    }

    UIUtil.dispatchAllInvocationEvents();

    AsyncResult<VirtualFile[]> result = AsyncResult.undefined();

    final String finalDirectoryName = directoryName;
    SwingUtilities.invokeLater(() -> {
      Pointer owner = Native.getWindowPointer(window);
      FileOpenDialog dialog = createNativeOpenDialog();
      assert dialog != null;

      dialog.SetTitle(new WString(myChooserDescriptor.getTitleValue().get()));

      if (finalDirectoryName != null) {
        PointerByReference ppv = new PointerByReference();

        COMUtils.checkRC(Shell32.INSTANCE.SHCreateItemFromParsingName(new WString(FileUtil.toSystemDependentName(finalDirectoryName)), Pointer.NULL, new Guid.REFIID(IShellItem.IID_ISHELLITEM), ppv));

        dialog.SetFolder(ppv.getValue());
      }

      final CommandProcessorEx commandProcessor = (CommandProcessorEx)CommandProcessor.getInstance();

      commandProcessor.enterModal();
      LaterInvocator.enterModal(this);

      int options = 0;
      if (myChooserDescriptor.isChooseFolders()) {
        options |= ShTypes.FILEOPENDIALOGOPTIONS.FOS_PICKFOLDERS;
      }

      if (myChooserDescriptor.isChooseMultiple()) {
        options |= ShTypes.FILEOPENDIALOGOPTIONS.FOS_ALLOWMULTISELECT;
      }

      dialog.SetOptions(options);

      WinNT.HRESULT show = dialog.Show(new WinDef.HWND(owner));

      commandProcessor.leaveModal();
      LaterInvocator.leaveModal(this);

      if (COMUtils.SUCCEEDED(show)) {
        PointerByReference items = new PointerByReference();

        COMUtils.checkRC(dialog.GetResults(items));

        ShellItemArray shellItemArray = new ShellItemArray(items.getValue());

        IntByReference count = new IntByReference();

        COMUtils.checkRC(shellItemArray.GetCount(count));

        List<VirtualFile> files = new ArrayList<>();

        for (int i = 0; i < count.getValue(); i++) {
          PointerByReference itemRef = new PointerByReference();

          COMUtils.checkRC(shellItemArray.GetItemAt(i, itemRef));

          ShellItem shellItem = new ShellItem(itemRef.getValue());

          PointerByReference descRef = new PointerByReference();

          shellItem.GetDisplayName(ShTypes.SIGDN.SIGDN_DESKTOPABSOLUTEPARSING, descRef);

          String filePath = descRef.getValue().getWideString(0);

          ContainerUtil.addIfNotNull(files, LocalFileSystem.getInstance().findFileByPath(filePath));
        }

        result.setDone(VfsUtilCore.toVirtualFileArray(files));
      }
      else {
        result.setRejected();
      }
    });

    return result;
  }
}
