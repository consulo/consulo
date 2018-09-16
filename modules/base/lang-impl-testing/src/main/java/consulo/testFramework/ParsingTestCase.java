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

import com.intellij.lang.Language;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.TestDataFile;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import java.io.IOException;
import java.util.Set;

public abstract class ParsingTestCase extends OneFileAtProjectTestCase {
  public ParsingTestCase(@NonNls @Nonnull String dataPath, @Nonnull String ext) {
    super(dataPath, ext);
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
    checkResult(name, myFile);
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

  protected static String toParseTreeText(final PsiElement file, boolean skipSpaces, boolean printRanges) {
    return DebugUtil.psiToString(file, skipSpaces, printRanges);
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
}
