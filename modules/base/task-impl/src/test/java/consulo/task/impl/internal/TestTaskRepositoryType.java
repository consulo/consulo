/*
 * Copyright 2013-2026 consulo.io
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
package consulo.task.impl.internal;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.task.TaskRepository;
import consulo.task.TaskRepositoryType;
import consulo.ui.image.Image;

public class TestTaskRepositoryType extends TaskRepositoryType<TestTaskRepositoryOptions> {
    private final String myId;

    public TestTaskRepositoryType(String id) {
        myId = id;
    }

    @Override
    public String getId() {
        return myId;
    }

    @Override
    public LocalizeValue getPresentableName() {
        return LocalizeValue.of(myId);
    }

    @Override
    public Image getIcon() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAccountTypeId() {
        return myId + "Account";
    }

    @Override
    public TestTaskRepositoryOptions createOptions() {
        return new TestTaskRepositoryOptions();
    }

    @Override
    public TaskRepository createRepository(Project project, TestTaskRepositoryOptions options) {
        throw new UnsupportedOperationException();
    }
}
