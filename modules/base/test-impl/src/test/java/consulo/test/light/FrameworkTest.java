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

import consulo.application.Application;
import consulo.disposer.AutoDisposable;
import consulo.disposer.Disposer;
import consulo.language.impl.DebugUtil;
import consulo.language.plain.PlainTextFileType;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.project.Project;
import consulo.util.lang.function.ThrowableRunnable;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
public class FrameworkTest {
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

      Assertions.assertNotNull(file);

      Assertions.assertEquals(file.getName(), fileName);

      Assertions.assertNotNull(DebugUtil.psiToString(file, false));
    }
  }

  @AfterEach
  public void after() {
    Disposer.assertIsEmpty();
  }

  @Test
  public void testLocalFileSystem() throws Throwable {
    try (AutoDisposable disposable = AutoDisposable.newAutoDisposable("testLocalFileSystem")) {
      LightApplicationBuilder builder = LightApplicationBuilder.create(disposable);

      Application application = builder.build();

      VirtualFileManager virtualFileManager = application.getInstance(VirtualFileManager.class);

      Path tempFile = Files.createTempFile("someTest", "txt");
      Files.writeString(tempFile, "test");

      VirtualFile path = virtualFileManager.findFileByNioPath(tempFile);

      Assertions.assertNotNull(path);
    }
  }

  @Test
  public void testPlaintTextFileParser() throws Throwable {
    try (AutoDisposable disposable = AutoDisposable.newAutoDisposable("testPlaintTextFileParser")) {
      LightApplicationBuilder builder = LightApplicationBuilder.create(disposable);

      Application application = builder.build();

      LightProjectBuilder projectBuilder = LightProjectBuilder.create(application);

      Project project = projectBuilder.build();

      TextFileParserTest parser = project.getUnbindedInstance(TextFileParserTest.class);

      parser.run();
    }
  }
}
