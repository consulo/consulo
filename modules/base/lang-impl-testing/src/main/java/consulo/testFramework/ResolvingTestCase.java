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

import consulo.testFramework.util.TestPathUtil;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.*;
import com.intellij.testFramework.LightPlatformCodeInsightTestCase;
import com.intellij.testFramework.VfsTestUtil;
import com.intellij.util.FileComparisonFailure;
import com.intellij.util.Function;
import com.intellij.util.containers.LinkedMultiMap;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import consulo.annotation.access.RequiredReadAction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * @author VISTALL
 * @since 06.04.2016
 */
public class ResolvingTestCase extends LightPlatformCodeInsightTestCase {
  protected String myExtension;
  @NonNls
  protected final String myFullDataPath;

  public ResolvingTestCase(@NonNls @Nonnull String dataPath, @Nonnull String ext) {
    myFullDataPath = TestPathUtil.getTestDataPath(dataPath);
    if (!StringUtil.endsWithChar(dataPath, '/')) {
      throw new IllegalArgumentException("directory required endWiths '/'");
    }
    myExtension = ext;
  }

  @Nonnull
  @Override
  protected String getTestDataPath() {
    return myFullDataPath;
  }

  @Override
  @RequiredReadAction
  protected void runTestInternal() throws Throwable {
    String testName = getTestName(false);

    checkResult(testName + "." + myExtension);
  }

  @RequiredReadAction
  private void checkResult(@Nonnull String filePath) throws Exception {
    configureByFile(filePath);

    PsiFile file = myFile;
    final MultiMap<PsiReference, ResolveResult> refs = new LinkedMultiMap<PsiReference, ResolveResult>();
    file.accept(new PsiRecursiveElementVisitor() {
      @Override
      public void visitElement(final PsiElement element) {
        super.visitElement(element);
        PsiReference[] references = element.getReferences();
        for (final PsiReference reference : references) {
          final ResolveResult[] resolveResults;
          if (reference instanceof PsiPolyVariantReference) {
            resolveResults = ((PsiPolyVariantReference)reference).multiResolve(false);
          }
          else {
            resolveResults = new ResolveResult[]{new ResolveResult() {
              @javax.annotation.Nullable
              @Override
              public PsiElement getElement() {
                return reference.resolve();
              }

              @Override
              public boolean isValidResult() {
                return true;
              }
            }};
          }

          refs.putValues(reference, Arrays.asList(resolveResults));
        }
      }
    });

    StringBuilder builder = new StringBuilder();
    for (Map.Entry<PsiReference, Collection<ResolveResult>> entry : refs.entrySet()) {
      PsiReference reference = entry.getKey();
      Collection<ResolveResult> results = entry.getValue();

      PsiElement element = reference.getElement();
      builder.append(element.getText()).append(element.getTextRange()).append(": ");
      if (results.isEmpty()) {
        builder.append("empty");
      }
      else {
        builder.append(StringUtil.join(results, new Function<ResolveResult, String>() {
          @Override
          @RequiredReadAction
          public String fun(ResolveResult resolveResult) {
            return buildReferenceResultText(resolveResult);
          }
        }, ", "));
      }
      builder.append("\n");
    }

    doCheckResult(getTestDataPath() + filePath + ".txt", builder.toString());
  }

  private static void doCheckResult(String path, String text) throws IOException {
    text = text.trim();
    if (OVERWRITE_TESTDATA) {
      VfsTestUtil.overwriteTestData(path, text);
      System.out.println("File " + path + " created.");
    }
    try {
      String expectedText = doLoadFile(path);
      if (!Comparing.equal(expectedText, text)) {
        throw new FileComparisonFailure(path, expectedText, text, path);
      }
    }
    catch (FileNotFoundException e) {
      VfsTestUtil.overwriteTestData(path, text);
      fail("No output text found. File " + path + " created.");
    }
  }

  private static String doLoadFile(String path) throws IOException {
    File file = new File(path);
    String text = FileUtil.loadFile(file, CharsetToolkit.UTF8).trim();
    text = StringUtil.convertLineSeparators(text);
    return text;
  }

  @Nonnull
  @RequiredReadAction
  private String buildReferenceResultText(@Nonnull ResolveResult resolveResult) {
    PsiElement element = resolveResult.getElement();
    if (element == null) {
      return "null-element";
    }
    return createReferenceResultBuilder().fun(resolveResult);
  }

  @Nonnull
  protected Function<ResolveResult, String> createReferenceResultBuilder() {
    return new Function<ResolveResult, String>() {
      @Override
      public String fun(ResolveResult resolveResult) {
        PsiElement element = resolveResult.getElement();
        assert element != null;
        throw new UnsupportedOperationException("Please override 'getReferenceResultBuilder()' argument type: " + element.getClass());
      }
    };
  }
}
