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

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.LeakHunter;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.util.ui.UIUtil;
import junit.framework.TestCase;

/**
 * This must be the last test.
 * @author max
 */
public class _LastInSuiteTest extends TestCase {
  public void testProjectLeak() throws Exception {
    new WriteCommandAction.Simple(null) {
      @Override
      protected void run() throws Throwable {
        LightPlatformTestCase.closeAndDeleteProject();
      }
    }.execute().throwException();

    final Application application = ApplicationManager.getApplication();

    // disposes default project too
    UIUtil.invokeAndWaitIfNeeded(new Runnable() {
      @Override
      public void run() {
        LightPlatformTestCase.disposeApplication();
      }
    });

    LeakHunter.checkProjectLeak(application);
    Disposer.assertIsEmpty(true);
  }
}
