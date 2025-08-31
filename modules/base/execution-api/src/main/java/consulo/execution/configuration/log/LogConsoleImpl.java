/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.execution.configuration.log;

import consulo.content.scope.SearchScope;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import consulo.util.io.FileUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.*;
import java.nio.charset.Charset;

/**
 * @author anna
 * @since 2005-04-19
 */
public abstract class LogConsoleImpl extends LogConsoleBase {
  private final String myPath;
  @Nonnull
  private final File myFile;
  @Nonnull
  private final Charset myCharset;
  private long myOldLength = 0;

  /**
   * @deprecated use {@link #LogConsoleImpl(Project, java.io.File, java.nio.charset.Charset, long, String, boolean, GlobalSearchScope)}
   */
  public LogConsoleImpl(Project project, @Nonnull File file, @Nonnull Charset charset, long skippedContents, String title, boolean buildInActions) {
    this(project, file, charset, skippedContents, title, buildInActions, GlobalSearchScope.allScope(project));
  }

  public LogConsoleImpl(Project project, @Nonnull File file, @Nonnull Charset charset, long skippedContents, String title, boolean buildInActions, SearchScope searchScope) {
    super(project, getReader(file, charset, skippedContents), title, buildInActions, new DefaultLogFilterModel(project), searchScope);
    myPath = file.getAbsolutePath();
    myFile = file;
    myCharset = charset;
  }

  @Nullable
  private static Reader getReader(@Nonnull File file, @Nonnull Charset charset, long skippedContents) {
    Reader reader = null;
    try {
      try {
        FileInputStream inputStream = new FileInputStream(file);
        reader = new BufferedReader(new InputStreamReader(inputStream, charset));
        if (file.length() >= skippedContents) { //do not skip forward
          //noinspection ResultOfMethodCallIgnored
          inputStream.skip(skippedContents);
        }
      }
      catch (FileNotFoundException e) {
        if (FileUtil.createIfDoesntExist(file)) {
          reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
        }
      }
    }
    catch (Throwable e) {
      reader = null;
    }
    return reader;
  }

  @Override
  @Nullable
  public String getTooltip() {
    return myPath;
  }

  public String getPath() {
    return myPath;
  }

  @Nullable
  @Override
  protected BufferedReader updateReaderIfNeeded(@Nullable BufferedReader reader) throws IOException {
    if (reader == null) {
      return null;
    }

    long length = myFile.length();
    if (length < myOldLength) {
      reader.close();
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(myFile), myCharset));
    }
    myOldLength = length;
    return reader;
  }
}
