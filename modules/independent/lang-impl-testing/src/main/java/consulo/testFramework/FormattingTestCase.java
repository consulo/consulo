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

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author VISTALL
 * @since 19.04.2015
 */
public class FormattingTestCase extends OneFileAtProjectTestCase {
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public static @interface Setup {
    @Nonnull
    Class<Consumer<CodeStyleSettings>> value();
  }

  public FormattingTestCase(@NonNls @Nonnull String dataPath, @Nonnull String ext) {
    super(dataPath, ext);
  }

  @Override
  protected void runTestInternal() throws Throwable {
    String name = getName();
    assertNotNull("TestCase.fName cannot be null", name); // Some VMs crash when calling getMethod(null,null);
    Method runMethod = null;
    try {
      // use getMethod to get all public inherited
      // methods. getDeclaredMethods returns all
      // methods of this class but excludes the
      // inherited ones.
      runMethod = getClass().getMethod(name, (Class[])null);
    }
    catch (NoSuchMethodException e) {
      fail("Method \"" + name + "\" not found");
    }
    if (!Modifier.isPublic(runMethod.getModifiers())) {
      fail("Method \"" + name + "\" should be public");
    }


    Setup annotation = runMethod.getAnnotation(Setup.class);
    CodeStyleSettings codeStyleSettings = new CodeStyleSettings();
    if(annotation != null) {
      Class<Consumer<CodeStyleSettings>> value = annotation.value();

      Consumer<CodeStyleSettings> codeStyleSettingsConsumer = value.newInstance();
      codeStyleSettingsConsumer.consume(codeStyleSettings);
    }

    CodeStyleSettingsManager.getInstance(myProject).setTemporarySettings(codeStyleSettings);

    doTest();

    CodeStyleSettingsManager.getInstance(myProject).dropTemporarySettings();
  }

  protected void doTest() throws IOException {
    String name = getTestName(false);
    String text = loadFile(name + ".before." + myExtension);
    myFile = createPsiFile(name, text);

    ensureParsed(myFile);

    assertEquals(text, myFile.getText());

    WriteCommandAction.runWriteCommandAction(myProject, new Runnable() {
      @Override
      public void run() {
        CodeStyleManager.getInstance(myProject).reformat(myFile);
      }
    });

    checkResult(name + ".after." + myExtension, myFile.getText());
  }
}
