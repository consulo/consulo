/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.fileEditor.ex;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.component.util.BusyObject;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileEditor.EditorDataProvider;
import consulo.fileEditor.FileEditorManager;
import consulo.project.Project;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class FileEditorManagerEx extends FileEditorManager implements BusyObject {
  protected final List<EditorDataProvider> myDataProviders = new ArrayList<>();

  public static FileEditorManagerEx getInstanceEx(Project project) {
    return (FileEditorManagerEx)getInstance(project);
  }

  @Override
  @Nullable
  public final Object getData(@Nonnull Key dataId, @Nonnull Editor editor, @Nonnull Caret caret) {
    for (final EditorDataProvider dataProvider : myDataProviders) {
      final Object o = dataProvider.getData(dataId, editor, caret);
      if (o != null) return o;
    }
    return null;
  }

  @Override
  public void registerExtraEditorDataProvider(@Nonnull final EditorDataProvider provider, Disposable parentDisposable) {
    myDataProviders.add(provider);
    if (parentDisposable != null) {
      Disposer.register(parentDisposable, () -> myDataProviders.remove(provider));
    }
  }
}
