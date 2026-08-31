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
package consulo.ide.impl.idea.ide;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.AccessRule;
import consulo.application.Application;
import consulo.ui.ex.awt.internal.LinuxDragAndDropSupport;
import consulo.language.editor.PsiCopyPasteManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.event.ProjectManagerListener;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.clipboard.DataTransferType;
import consulo.ui.ex.CopyPasteManager;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jspecify.annotations.Nullable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.InvalidDnDOperationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Singleton
@ServiceImpl
public class PsiCopyPasteManagerImpl implements PsiCopyPasteManager {
  private static final Logger LOG = Logger.getInstance(PsiCopyPasteManagerImpl.class);

  public static final DataTransferType<MyData> PSI_DATA = DataTransferType.create("consulo.psi.elements");

  private MyData myRecentData;
  private final CopyPasteManager myCopyPasteManager;

  @Inject
  public PsiCopyPasteManagerImpl(Application application, CopyPasteManager copyPasteManager) {
    myCopyPasteManager = copyPasteManager;
    application.getMessageBus().connect().subscribe(ProjectManagerListener.class, new ProjectManagerListener() {
      @Override
      public void projectClosing(Project project) {
        if (myRecentData != null && myRecentData.getProject() == project) {
          myRecentData = null;
        }
      }
    });
  }

  @Override
  public @Nullable PsiElement[] getElements(boolean[] isCopied) {
    MyData dataProxy = myCopyPasteManager.getLocalContents().get(PSI_DATA);
    if (dataProxy == null || !Comparing.equal(dataProxy, myRecentData)) {
      return null;
    }
    if (isCopied != null) {
      isCopied[0] = myRecentData.isCopied();
    }
    return myRecentData.getElements();
  }

  static @Nullable PsiElement[] getElements(Transferable content) {
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

  static @Nullable PsiElement[] getElements(DataTransfer transfer) {
    MyData data = transfer.get(PSI_DATA);
    return data == null ? null : data.getElements();
  }

  @Override
  public void clear() {
    myRecentData = null;
    myCopyPasteManager.setCutElements(null);
    myCopyPasteManager.setText("");
  }

  @Override
  public void setElements(PsiElement[] elements, boolean copied) {
    myRecentData = new MyData(elements, copied);

    String text = AccessRule.read(() -> {
      List<String> names = new ArrayList<>();
      for (PsiElement element : elements) {
        if (element instanceof PsiNamedElement namedElement) {
          String name = namedElement.getName();
          if (name != null) {
            names.add(name);
          }
        }
      }
      return names.isEmpty() ? null : StringUtil.join(names, "\n");
    });
    List<File> files = AccessRule.read(() -> PsiCopyPasteManager.asFileList(elements));

    myCopyPasteManager.setContents(DataTransfer.builder()
      .put(DataTransferType.TEXT, text)
      .put(DataTransferType.FILE_LIST, files)
      .put(PSI_DATA, myRecentData)
      .build());
    myCopyPasteManager.setCutElements(copied ? null : elements);
  }

  @Override
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
      Class<MyData> flavorClass = MyData.class;
      Thread currentThread = Thread.currentThread();
      ClassLoader currentLoader = currentThread.getContextClassLoader();
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

    @RequiredReadAction
    public boolean isValid() {
      return myPointers.size() > 0 && myPointers.get(0).getElement() != null;
    }

    public @Nullable Project getProject() {
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
      this(new PsiCopyPasteManagerImpl.MyData(selectedValues, true));
    }

    @Override
    public @Nullable Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
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
        List<File> files = getDataAsFileList();
        if (files != null) {
          return LinuxDragAndDropSupport.toUriList(files);
        }
      }
      else if (flavor.equals(LinuxDragAndDropSupport.gnomeFileListFlavor)) {
        List<File> files = getDataAsFileList();
        if (files != null) {
          String string = (myDataProxy.isCopied() ? "copy\n" : "cut\n") + LinuxDragAndDropSupport.toUriList(files);
          return new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
        }
      }
      else if (flavor.equals(LinuxDragAndDropSupport.kdeCutMarkFlavor) && !myDataProxy.isCopied()) {
        return new ByteArrayInputStream("1".getBytes(StandardCharsets.UTF_8));
      }

      return null;
    }

    private @Nullable String getDataAsText() {
      return AccessRule.read(() -> {
        List<String> names = new ArrayList<>();
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

    private @Nullable List<File> getDataAsFileList() {
      return AccessRule.read(() -> PsiCopyPasteManager.asFileList(myDataProxy.getElements()));
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
}
