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
package com.intellij.ide;

import com.intellij.ide.dnd.LinuxDragAndDropSupport;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import consulo.application.AccessRule;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.InvalidDnDOperationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class PsiCopyPasteManager {
  public static PsiCopyPasteManager getInstance() {
    return ServiceManager.getService(PsiCopyPasteManager.class);
  }

  private static final Logger LOG = Logger.getInstance(PsiCopyPasteManager.class);

  private MyData myRecentData;
  private final CopyPasteManagerEx myCopyPasteManager;

  @Inject
  public PsiCopyPasteManager(Application application, CopyPasteManager copyPasteManager) {
    myCopyPasteManager = (CopyPasteManagerEx) copyPasteManager;
    application.getMessageBus().connect().subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
      @Override
      public void projectClosing(@Nonnull Project project) {
        if (myRecentData != null && myRecentData.getProject() == project) {
          myRecentData = null;
        }
      }
    });
  }

  @Nullable
  public PsiElement[] getElements(boolean[] isCopied) {
    try {
      Object transferData = myCopyPasteManager.getContents(ourDataFlavor);
      if (!(transferData instanceof MyData)) {
        return null;
      }
      MyData dataProxy = (MyData)transferData;
      if (!Comparing.equal(dataProxy, myRecentData)) {
        return null;
      }
      if (isCopied != null) {
        isCopied[0] = myRecentData.isCopied();
      }
      return myRecentData.getElements();
    }
    catch (Exception e) {
      LOG.debug(e);
      return null;
    }
  }

  @Nullable
  static PsiElement[] getElements(final Transferable content) {
    if (content == null) return null;
    Object transferData;
    try {
      transferData = content.getTransferData(ourDataFlavor);
    }
    catch (UnsupportedFlavorException e) {
      return null;
    }
    catch (IOException e) {
      return null;
    }
    catch (InvalidDnDOperationException e) {
      return null;
    }

    return transferData instanceof MyData ? ((MyData)transferData).getElements() : null;
  }

  public void clear() {
    myRecentData = null;
    myCopyPasteManager.setContents(new StringSelection(""));
  }

  public void setElements(PsiElement[] elements, boolean copied) {
    myRecentData = new MyData(elements, copied);
    myCopyPasteManager.setContents(new MyTransferable(myRecentData));
  }

  public boolean isCutElement(Object element) {
    if (myRecentData == null) return false;
    if (myRecentData.isCopied()) return false;
    PsiElement[] elements = myRecentData.getElements();
    if (elements == null) return false;
    for (PsiElement aElement : elements) {
      if (aElement == element) return true;
    }
    return false;
  }

  private static final DataFlavor ourDataFlavor;

  static {
    try {
      final Class<MyData> flavorClass = MyData.class;
      final Thread currentThread = Thread.currentThread();
      final ClassLoader currentLoader = currentThread.getContextClassLoader();
      try {
        currentThread.setContextClassLoader(flavorClass.getClassLoader());
        ourDataFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + flavorClass.getName());
      }
      finally {
        currentThread.setContextClassLoader(currentLoader);
      }
    }
    catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }


  public static class MyData {
    private final Project myProject;
    private final List<SmartPsiElementPointer> myPointers = new ArrayList<>();
    private final boolean myIsCopied;

    public MyData(PsiElement[] elements, boolean copied) {
      myProject = elements.length == 0 ? null : elements[0].getProject();
      for (PsiElement element : elements) {
        myPointers.add(SmartPointerManager.createPointer(element));
      }
      myIsCopied = copied;
    }

    public PsiElement[] getElements() {
      return AccessRule.read(() -> {
        List<PsiElement> result = new ArrayList<>();
        for (SmartPsiElementPointer pointer : myPointers) {
          PsiElement element = pointer.getElement();
          if (element != null) {
            result.add(element);
          }
        }
        return result.toArray(PsiElement.EMPTY_ARRAY);
      });
    }

    public boolean isCopied() {
      return myIsCopied;
    }

    public boolean isValid() {
      return myPointers.size() > 0 && myPointers.get(0).getElement() != null;
    }

    @Nullable
    public Project getProject() {
      return myProject;
    }
  }

  public static class MyTransferable implements Transferable {
    private static final DataFlavor[] DATA_FLAVORS_COPY = {
            ourDataFlavor, DataFlavor.stringFlavor, DataFlavor.javaFileListFlavor,
            LinuxDragAndDropSupport.uriListFlavor, LinuxDragAndDropSupport.gnomeFileListFlavor
    };
    private static final DataFlavor[] DATA_FLAVORS_CUT = {
            ourDataFlavor, DataFlavor.stringFlavor, DataFlavor.javaFileListFlavor,
            LinuxDragAndDropSupport.uriListFlavor, LinuxDragAndDropSupport.gnomeFileListFlavor, LinuxDragAndDropSupport.kdeCutMarkFlavor
    };

    private final MyData myDataProxy;

    public MyTransferable(MyData data) {
      myDataProxy = data;
    }

    public MyTransferable(PsiElement[] selectedValues) {
      this(new PsiCopyPasteManager.MyData(selectedValues, true));
    }

    @Override
    @Nullable
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
      if (ourDataFlavor.equals(flavor)) {
        return myDataProxy;
      }
      else if (DataFlavor.stringFlavor.equals(flavor)) {
        return getDataAsText();
      }
      else if (DataFlavor.javaFileListFlavor.equals(flavor)) {
        return getDataAsFileList();
      }
      else if (flavor.equals(LinuxDragAndDropSupport.uriListFlavor)) {
        final List<File> files = getDataAsFileList();
        if (files != null) {
          return LinuxDragAndDropSupport.toUriList(files);
        }
      }
      else if (flavor.equals(LinuxDragAndDropSupport.gnomeFileListFlavor)) {
        final List<File> files = getDataAsFileList();
        if (files != null) {
          final String string = (myDataProxy.isCopied() ? "copy\n" : "cut\n") + LinuxDragAndDropSupport.toUriList(files);
          return new ByteArrayInputStream(string.getBytes(CharsetToolkit.UTF8_CHARSET));
        }
      }
      else if (flavor.equals(LinuxDragAndDropSupport.kdeCutMarkFlavor) && !myDataProxy.isCopied()) {
        return new ByteArrayInputStream("1".getBytes(CharsetToolkit.UTF8_CHARSET));
      }

      return null;
    }

    @Nullable
    private String getDataAsText() {
      return AccessRule.read(() -> {
        final List<String> names = new ArrayList<>();
        for (PsiElement element : myDataProxy.getElements()) {
          if (element instanceof PsiNamedElement) {
            String name = ((PsiNamedElement)element).getName();
            if (name != null) {
              names.add(name);
            }
          }
        }
        return names.isEmpty() ? null : StringUtil.join(names, "\n");
      });
    }

    @Nullable
    private List<File> getDataAsFileList() {
      return AccessRule.read(() -> asFileList(myDataProxy.getElements()));
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
      return myDataProxy.isCopied() ? DATA_FLAVORS_COPY : DATA_FLAVORS_CUT;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
      return ArrayUtil.find(getTransferDataFlavors(), flavor) != -1;
    }

    public PsiElement[] getElements() {
      return myDataProxy.getElements();
    }
  }

  @Nullable
  public static List<File> asFileList(final PsiElement[] elements) {
    final List<File> result = new ArrayList<>();
    for (PsiElement element : elements) {
      final PsiFileSystemItem psiFile;
      if (element instanceof PsiFileSystemItem) {
        psiFile = (PsiFileSystemItem)element;
      }
      else if (element instanceof PsiDirectoryContainer) {
        final PsiDirectory[] directories = ((PsiDirectoryContainer)element).getDirectories();
        psiFile = directories[0];
      }
      else {
        psiFile = element.getContainingFile();
      }
      if (psiFile != null) {
        VirtualFile vFile = psiFile.getVirtualFile();
        if (vFile != null && vFile.getFileSystem() instanceof LocalFileSystem) {
          result.add(new File(vFile.getPath()));
        }
      }
    }
    return result.isEmpty() ? null : result;
  }
}
