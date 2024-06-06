/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.dnd;

import consulo.ui.ex.awt.CopyPasteManager;
import consulo.ui.ex.awt.dnd.DnDEvent;
import consulo.ui.ex.awt.dnd.DnDNativeTarget;
import consulo.util.lang.function.Condition;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.logging.Logger;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorMap;
import java.awt.datatransfer.SystemFlavorMap;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileCopyPasteUtil {
  private static final Logger LOG = Logger.getInstance(FileCopyPasteUtil.class);

  private FileCopyPasteUtil() { }

  public static DataFlavor createDataFlavor(@Nonnull final String mimeType) {
    return createDataFlavor(mimeType, null, false);
  }

  public static DataFlavor createDataFlavor(@Nonnull final String mimeType, @Nullable final Class<?> klass) {
    return createDataFlavor(mimeType, klass, false);
  }

  public static DataFlavor createDataFlavor(@Nonnull final String mimeType, @Nullable final Class<?> klass, final boolean register) {
    try {
      final DataFlavor flavor =
              klass != null ? new DataFlavor(mimeType + ";class=" + klass.getName(), null, klass.getClassLoader()) : new DataFlavor(mimeType);

      if (register) {
        final FlavorMap map = SystemFlavorMap.getDefaultFlavorMap();
        if (map instanceof SystemFlavorMap) {
          ((SystemFlavorMap)map).addUnencodedNativeForFlavor(flavor, mimeType);
        }
      }

      return flavor;
    }
    catch (ClassNotFoundException e) {
      LOG.error(e);
      //noinspection ConstantConditions
      return null;
    }
  }

  public static DataFlavor createJvmDataFlavor(@Nonnull final Class<?> klass) {
    return createDataFlavor(DataFlavor.javaJVMLocalObjectMimeType, klass, false);
  }

  public static boolean isFileListFlavorAvailable() {
    return CopyPasteManager.getInstance().areDataFlavorsAvailable(
            DataFlavor.javaFileListFlavor, LinuxDragAndDropSupport.uriListFlavor, LinuxDragAndDropSupport.gnomeFileListFlavor
    );
  }

  public static boolean isFileListFlavorAvailable(@Nonnull DnDEvent event) {
    return event.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
           event.isDataFlavorSupported(LinuxDragAndDropSupport.uriListFlavor) ||
           event.isDataFlavorSupported(LinuxDragAndDropSupport.gnomeFileListFlavor);
  }

  public static boolean isFileListFlavorAvailable(@Nonnull DataFlavor[] transferFlavors) {
    for (DataFlavor flavor : transferFlavors) {
      if (flavor != null && (flavor.equals(DataFlavor.javaFileListFlavor) ||
                             flavor.equals(LinuxDragAndDropSupport.uriListFlavor) ||
                             flavor.equals(LinuxDragAndDropSupport.gnomeFileListFlavor))) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  public static List<File> getFileList(@Nonnull final Transferable transferable) {
    try {
      if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        @SuppressWarnings({"unchecked"})
        final List<File> fileList = (List<File>)transferable.getTransferData(DataFlavor.javaFileListFlavor);
        return ContainerUtil.filter(fileList, new Condition<File>() {
          @Override
          public boolean value(File file) {
            return !StringUtil.isEmptyOrSpaces(file.getPath());
          }
        });
      }
      else {
        return LinuxDragAndDropSupport.getFiles(transferable);
      }
    }
    catch (Exception ignore) { }

    return null;
  }

  @Nonnull
  public static List<File> getFileListFromAttachedObject(Object attached) {
    List<File> result;
    if (attached instanceof TransferableWrapper) {
      result = ((TransferableWrapper)attached).asFileList();
    }
    else if (attached instanceof DnDNativeTarget.EventInfo) {
      result = getFileList(((DnDNativeTarget.EventInfo)attached).getTransferable());
    }
    else {
      result = null;
    }
    return result == null? Collections.<File>emptyList() : result;
  }

  @Nonnull
  public static List<VirtualFile> getVirtualFileListFromAttachedObject(Object attached) {
    List<VirtualFile> result;
    List<File> fileList = getFileListFromAttachedObject(attached);
    if (fileList.isEmpty()) {
      result = Collections.emptyList();
    }
    else {
      result = new ArrayList<>(fileList.size());
      for (File file : fileList) {
        VirtualFile virtualFile = VfsUtil.findFileByIoFile(file, true);
        if (virtualFile == null) continue;
        result.add(virtualFile);
        // detect and store file type for Finder-2-IDEA drag-n-drop
        virtualFile.getFileType();
      }
    }
    return result;
  }
}
