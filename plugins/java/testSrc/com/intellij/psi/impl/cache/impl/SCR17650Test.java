package com.intellij.psi.impl.cache.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.PathManagerEx;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PsiTestCase;
import com.intellij.testFramework.PsiTestUtil;
import org.consulo.lombok.annotations.Logger;

import java.io.File;
import java.io.IOException;

/**
 * @author max
 */
@Logger
public class SCR17650Test extends PsiTestCase {
  private static final String TEST_ROOT = PathManagerEx.getTestDataPath() + "/psi/repositoryUse/cls";

  private VirtualFile myDir;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    final File root = FileUtil.createTempFile(getName(), "");
    root.delete();
    root.mkdir();
    myFilesToDelete.add(root);

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        try {
          VirtualFile rootVFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(root.getAbsolutePath().replace(File.separatorChar, '/'));

          myDir = rootVFile.createChildDirectory(null, "contentAndLibrary");

          VirtualFile file1 = myDir.createChildData(null, "A.java");
          VfsUtil.saveText(file1, "package p; public class A{ public void foo(); }");
          VfsUtilCore.copyFile(null, getClassFile(), myDir);

          PsiTestUtil.addSourceRoot(myModule, myDir);
          ModuleRootModificationUtil.addModuleLibrary(myModule, myDir.getUrl());
        }
        catch (IOException e) {
          LOGGER.error(e);
        }
      }
    });
  }

  private static VirtualFile getClassFile() {
    VirtualFile vDir = LocalFileSystem.getInstance().findFileByPath(TEST_ROOT.replace(File.separatorChar, '/'));
    VirtualFile child = vDir.findChild("pack").findChild("MyClass.class");
    return child;
  }

  public void test17650() throws Exception {
    assertEquals("p.A", myJavaFacade.findClass("p.A").getQualifiedName());
    assertEquals("pack.MyClass", myJavaFacade.findClass("pack.MyClass").getQualifiedName());
  }
}
