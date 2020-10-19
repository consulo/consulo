/*
 * Copyright 2013-2018 consulo.io
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
package consulo.test.light;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.util.ThrowableRunnable;
import org.junit.Assert;
import org.junit.Test;

import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
public class LightProjectBuilderTest {
  public static class TextFileParserTest implements ThrowableRunnable<Throwable> {

    private final PsiFileFactory myPsiFileFactory;

    @Inject
    public TextFileParserTest(PsiFileFactory psiFileFactory) {
      myPsiFileFactory = psiFileFactory;
    }

    @Override
    public void run() throws Throwable {
      String fileName = "test.txt";

      PsiFile file = myPsiFileFactory.createFileFromText(fileName, PlainTextFileType.INSTANCE, "some test");

      Assert.assertNotNull(file);

      Assert.assertEquals(file.getName(), fileName);

      Assert.assertNotNull(DebugUtil.psiToString(file, false));
    }
  }

  @Test
  public void testPlaintTextFileParser() throws Throwable {
    Disposable disposable = Disposable.newDisposable("root");

    LightApplicationBuilder builder = LightApplicationBuilder.create(disposable);

    Application application = builder.build();

    LightProjectBuilder projectBuilder = LightProjectBuilder.create(application);

    Project project = projectBuilder.build();

    TextFileParserTest parser = project.getInjectingContainer().getUnbindedInstance(TextFileParserTest.class);

    parser.run();

    Disposer.dispose(disposable);
  }
}
