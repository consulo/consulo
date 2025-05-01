/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.execution.test;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.project.Project;
import jakarta.annotation.Nullable;

@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class TestStatusListener {
    public abstract void testSuiteFinished(@Nullable AbstractTestProxy root);

    public void testSuiteFinished(@Nullable AbstractTestProxy root, Project project) {
        testSuiteFinished(root);
    }

    @Deprecated
    @SuppressWarnings("UnusedDeclaration")
    public static void notifySuiteFinished(AbstractTestProxy root) {
        Application.get().getExtensionPoint(TestStatusListener.class)
            .forEach(statusListener -> statusListener.testSuiteFinished(root));
    }

    public static void notifySuiteFinished(@Nullable AbstractTestProxy root, Project project) {
        Application.get().getExtensionPoint(TestStatusListener.class)
            .forEach(statusListener -> statusListener.testSuiteFinished(root, project));
    }
}
