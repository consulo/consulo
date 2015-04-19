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
package org.mustbe.consulo.testFramework;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageVersion;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.PathManagerEx;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.ex.ProjectEx;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.TestDataFile;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.VfsTestUtil;
import com.intellij.util.FileComparisonFailure;
import com.intellij.util.LanguageVersionUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

public abstract class ParsingTestCase extends UsefulTestCase {
  protected String myFilePrefix = "";
  protected String myExtension;
  @NonNls
  protected final String myFullDataPath;
  protected PsiFile myFile;
  private PsiManager myPsiManager;
  private PsiFileFactoryImpl myFileFactory;
  private ProjectEx myProject;

  public ParsingTestCase(@NonNls @NotNull String dataPath, @NotNull String ext) {
    myFullDataPath = PathManagerEx.getTestDataPath(getTestDataPath() + "/" + dataPath);
    myExtension = ext;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    File tempDirectory = FileUtil.createTempDirectory("consulo", "test_project", true);
    ProjectManagerEx projectManagerEx = ProjectManagerEx.getInstanceEx();
    myProject = (ProjectEx)projectManagerEx.newProject(tempDirectory.getName(), tempDirectory.getPath(), true, false);
    assert myProject != null;
    projectManagerEx.openTestProject(myProject);
    myPsiManager = PsiManager.getInstance(myProject);
    myFileFactory = (PsiFileFactoryImpl)PsiFileFactory.getInstance(myProject);
  }

  public ProjectEx getProject() {
    return myProject;
  }

  public PsiManager getPsiManager() {
    return myPsiManager;
  }

  @Override
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

  protected boolean includeRanges() {
    return false;
  }

  protected boolean skipSpaces() {
    return false;
  }

  protected boolean checkAllPsiRoots() {
    return true;
  }

  protected void doTest(boolean checkResult) {
    String name = getTestName(false);
    try {
      String text = loadFile(name + "." + myExtension);
      myFile = createPsiFile(name, text);
      ensureParsed(myFile);
      FileViewProvider viewProvider = myFile.getViewProvider();
      VirtualFile virtualFile = viewProvider.getVirtualFile();
      if (virtualFile instanceof LightVirtualFile) {
        assertEquals("light virtual file text mismatch", text, ((LightVirtualFile)virtualFile).getContent());
      }
      assertEquals("virtual file text mismatch", text, LoadTextUtil.loadText(virtualFile));
      assertEquals("doc text mismatch", text, viewProvider.getDocument().getText());
      assertEquals("psi text mismatch", text, myFile.getText());
      if (checkResult) {
        checkResult(name, myFile);
      }
      else {
        toParseTreeText(myFile, skipSpaces(), includeRanges());
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected void doTest(String suffix) throws IOException {
    String name = getTestName(false);
    String text = loadFile(name + "." + myExtension);
    myFile = createPsiFile(name, text);
    ensureParsed(myFile);
    assertEquals(text, myFile.getText());
    checkResult(name + suffix, myFile);
  }

  protected void doCodeTest(String code) throws IOException {
    String name = getTestName(false);
    myFile = createPsiFile("a", code);
    ensureParsed(myFile);
    assertEquals(code, myFile.getText());
    checkResult(myFilePrefix + name, myFile);
  }

  protected PsiFile createPsiFile(String name, String text) {
    String fileName = name + "." + myExtension;
    FileType fileTypeByFileName = FileTypeManager.getInstance().getFileTypeByFileName(fileName);
    return createFile(fileName, fileTypeByFileName, text);
  }

  @NotNull
  public LanguageVersion<?> resolveLanguageVersion(@NotNull FileType fileType) {
    if(fileType instanceof LanguageFileType) {
      return LanguageVersionUtil.findDefaultVersion(((LanguageFileType)fileType).getLanguage());
    }
    throw new IllegalArgumentException(fileType.getName() + " is not extends 'LanguageFileType'");
  }

  protected PsiFile createFile(@NonNls String name, @NotNull FileType fileType, String text) {
    LanguageVersion<?> languageVersion = resolveLanguageVersion(fileType);

    return myFileFactory.createFileFromText(name, languageVersion.getLanguage(), languageVersion, text);
  }

  protected void checkResult(@NonNls @TestDataFile String targetDataName, final PsiFile file) throws IOException {
    doCheckResult(myFullDataPath, file, checkAllPsiRoots(), targetDataName, skipSpaces(), includeRanges());
  }

  public static void doCheckResult(String myFullDataPath,
                                   PsiFile file,
                                   boolean checkAllPsiRoots,
                                   String targetDataName,
                                   boolean skipSpaces,
                                   boolean printRanges) throws IOException {
    FileViewProvider provider = file.getViewProvider();
    Set<Language> languages = provider.getLanguages();

    if (!checkAllPsiRoots || languages.size() == 1) {
      doCheckResult(myFullDataPath, targetDataName + ".txt", toParseTreeText(file, skipSpaces, printRanges).trim());
      return;
    }

    for (Language language : languages) {
      PsiFile root = provider.getPsi(language);
      String expectedName = targetDataName + "." + language.getID() + ".txt";
      doCheckResult(myFullDataPath, expectedName, toParseTreeText(root, skipSpaces, printRanges).trim());
    }
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

  protected static String toParseTreeText(final PsiElement file, boolean skipSpaces, boolean printRanges) {
    return DebugUtil.psiToString(file, skipSpaces, printRanges);
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
