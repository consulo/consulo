/*
 * Copyright 2013-2022 consulo.io
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
package consulo.codeEditor.internal;

import consulo.codeEditor.Caret;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataContextWrapper;
import consulo.document.Document;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 18-Mar-22
 */
@Singleton
public class EditorInternalHelper {
  protected static class CaretDataContext extends DataContextWrapper {
    protected final Caret myCaret;

    public CaretDataContext(@Nonnull DataContext delegate, @Nonnull Caret caret) {
      super(delegate);
      myCaret = caret;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getData(@Nonnull Key<T> dataId) {
      if (Caret.KEY == dataId) return (T)myCaret;
      return super.getData(dataId);
    }
  }

  public static EditorInternalHelper getInstance(@Nonnull Project project) {
    return project.getInstance(EditorInternalHelper.class);
  }

  protected final Project myProject;

  @Inject
  public EditorInternalHelper(Project project) {
    myProject = project;
  }

  @Nonnull
  public CaretDataContext createDataContext(@Nonnull DataContext delegate,@Nonnull Caret caret) {
    return new CaretDataContext(delegate, caret);
  }

  public boolean ensureInjectionUpToDate(@Nonnull Caret hostCaret) {
    return false;
  }

  @Nullable
  public String getProperIndent(Document document, int offset) {
    return null;
  }
}
