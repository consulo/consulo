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
package consulo.it.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.language.psi.PsiFile;
import consulo.language.psi.search.PsiTodoSearchHelper;
import consulo.language.psi.search.TodoItem;
import consulo.language.psi.search.TodoPattern;
import jakarta.inject.Singleton;

/**
 * TODO scanning lives in its own module, whose tool window cannot be registered headlessly, so the helper is
 * answered here instead. The highlighting pass asks for it on every file it highlights, so it has to exist;
 * reporting no TODO items is the honest answer when nothing scans for them.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessPsiTodoSearchHelper extends PsiTodoSearchHelper {
    private static final PsiFile[] NO_FILES = new PsiFile[0];
    private static final TodoItem[] NO_ITEMS = new TodoItem[0];

    @Override
    public PsiFile[] findFilesWithTodoItems() {
        return NO_FILES;
    }

    @Override
    public TodoItem[] findTodoItems(PsiFile file) {
        return NO_ITEMS;
    }

    @Override
    public TodoItem[] findTodoItems(PsiFile file, int startOffset, int endOffset) {
        return NO_ITEMS;
    }

    @Override
    public TodoItem[] findTodoItemsLight(PsiFile file) {
        return NO_ITEMS;
    }

    @Override
    public TodoItem[] findTodoItemsLight(PsiFile file, int startOffset, int endOffset) {
        return NO_ITEMS;
    }

    @Override
    public int getTodoItemsCount(PsiFile file) {
        return 0;
    }

    @Override
    public int getTodoItemsCount(PsiFile file, TodoPattern pattern) {
        return 0;
    }
}
