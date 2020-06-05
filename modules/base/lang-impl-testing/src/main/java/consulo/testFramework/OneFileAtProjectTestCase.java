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
package consulo.testFramework;

import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.ex.ProjectEx;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.testFramework.TestDataFile;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.VfsTestUtil;
import com.intellij.util.FileComparisonFailure;
import consulo.disposer.Disposer;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.lang.LanguageVersion;
import consulo.lang.util.LanguageVersionUtil;
import consulo.testFramework.util.TestPathUtil;
import consulo.ui.UIAccess;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author VISTALL
 * @since 19.04.2015
 */
public class OneFileAtProjectTestCase extends UsefulTestCase {
  protected String myExtension;
  @NonNls
  protected final String myFullDataPath;
  protected PsiFile myFile;
  protected PsiManager myPsiManager;
  protected PsiFileFactoryImpl myFileFactory;
  protected ProjectEx myProject;

  public OneFileAtProjectTestCase(@NonNls @Nonnull String dataPath, @Nonnull String ext) {
    myFullDataPath = TestPathUtil.getTestDataPath(getTestDataPath() + "/" + dataPath);
    myExtension = ext;
  }

  protected void runStartupActivities() {
    final StartupManagerImpl startupManager = (StartupManagerImpl)StartupManager.getInstance(myProject);
    startupManager.runStartupActivities(UIAccess.get());
    startupManager.runPostStartupActivities(UIAccess.get());
  }

  @Override
  protected boolean shouldContainTempFiles() {
    return false;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    File tempDirectory = FileUtil.createTempDirectory("consulo", "test_project", true);
    ProjectManagerEx projectManagerEx = ProjectManagerEx.getInstanceEx();
    myProject = (ProjectEx)projectManagerEx.newProject(tempDirectory.getName(), tempDirectory.getPath(), true, false);
    assert myProject != null;
    projectManagerEx.openTestProject(myProject);
    runStartupActivities();

    myPsiManager = PsiManager.getInstance(myProject);
    myFileFactory = (PsiFileFactoryImpl)PsiFileFactory.getInstance(myProject);
  }

  @Override
  @RequiredUIAccess
  protected void tearDown() throws Exception {
    super.tearDown();
    myFile = null;
    ProjectManagerEx.getInstanceEx().closeTestProject(myProject);
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        Disposer.dispose(myProject);
      }
    });
    myProject = null;
    myPsiManager = null;
  }

  protected String getTestDataPath() {
    return "";
  }

  protected PsiFile createPsiFile(String name, String text) {
    String fileName = name + "." + myExtension;
    FileType fileTypeByFileName = FileTypeManager.getInstance().getFileTypeByFileName(fileName);
    return createFile(fileName, fileTypeByFileName, text);
  }

  @Nonnull
  protected LanguageVersion resolveLanguageVersion(@Nonnull FileType fileType) {
    if(fileType instanceof LanguageFileType) {
      return LanguageVersionUtil.findDefaultVersion(((LanguageFileType)fileType).getLanguage());
    }
    throw new IllegalArgumentException(fileType.getName() + " is not extends 'LanguageFileType'");
  }

  @Nonnull
  protected PsiFile createFile(@NonNls String name, @Nonnull FileType fileType, String text) {
    LanguageVersion languageVersion = resolveLanguageVersion(fileType);

    return myFileFactory.createFileFromText(name, languageVersion.getLanguage(), languageVersion, text);
  }

  protected void checkResult(@TestDataFile @NonNls String targetDataName, final String text) throws IOException {
    doCheckResult(myFullDataPath, targetDataName, text);
  }

  public static void doCheckResult(String fullPath, String targetDataName, String text) throws IOException {
    text = text.trim();
    String expectedFileName = fullPath + File.separatorChar + targetDataName;
    if (OVERWRITE_TESTDATA) {
      VfsTestUtil.overwriteTestData(expectedFileName, text);
      System.out.println("File " + expectedFileName + " created.");
    }
    try {
      String expectedText = doLoadFile(fullPath, targetDataName);
      if (!Comparing.equal(expectedText, text)) {
        throw new FileComparisonFailure(targetDataName, expectedText, text, expectedFileName);
      }
    }
    catch (FileNotFoundException e) {
      VfsTestUtil.overwriteTestData(expectedFileName, text);
      fail("No output text found. File " + expectedFileName + " created.");
    }
  }

  protected String loadFile(@NonNls @TestDataFile String name) throws IOException {
    return doLoadFile(myFullDataPath, name);
  }

  private static String doLoadFile(String myFullDataPath, String name) throws IOException {
    String text = FileUtil.loadFile(new File(myFullDataPath, name), CharsetToolkit.UTF8).trim();
    text = StringUtil.convertLineSeparators(text);
    return text;
  }

  public static void ensureParsed(PsiFile file) {
    file.accept(new PsiElementVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        element.acceptChildren(this);
      }
    });
  }
}
