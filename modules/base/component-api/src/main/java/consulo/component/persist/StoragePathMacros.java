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
package consulo.component.persist;

import consulo.annotation.DeprecationInfo;

import jakarta.annotation.Nonnull;

/**
 * We have a framework for persisting component states (see {@link State} {@link Storage}). It allows to specify which file
 * should hold persisting data. There is a number of standard file system anchors like 'workspace file', 'project config dir' which
 * can be used for defining a storage file's path. Hence, IJ provides special support for such anchors in the form of macros,
 * i.e. special markers that are mapped to the current file system environment at runtime.
 * <p>
 * This class holds those markers and utility method for working with them.
 *
 * @author Denis Zhdanov
 * @since 2012-05-02
 */
public class StoragePathMacros {
  /**
   * Points to the application-level settings root directory.
   */
  @Nonnull
  public static final String ROOT_CONFIG = "$ROOT_CONFIG$";

  /**
   * Points to {@link #ROOT_CONFIG}/options by default. Should be used to store single files and {@link #ROOT_CONFIG} to store subdirectories with many files.
   */
  @Nonnull
  public static final String APP_CONFIG = "$APP_CONFIG$";

  /**
   * <code>'.ipr'</code> file path key.
   */
  @Deprecated
  @DeprecationInfo(value = "Project file types is not supported anymore")
  @Nonnull
  public static final String PROJECT_FILE = "$PROJECT_FILE$";

  /**
   * <code>'.consulo'</code> directory path key.
   */
  @Nonnull
  public static final String PROJECT_CONFIG_DIR = "$PROJECT_CONFIG_DIR$";

  /**
   * {@link Project#getWorkspaceFile() Workspace} file key.
   * <p>
   * <code>'Workspace file'</code> holds settings that are local to a particular environment and should not be shared with another
   * team members.
   */
  @Nonnull
  public static final String WORKSPACE_FILE = "$WORKSPACE_FILE$";

  /**
   * Default file for storage. For application it's 'other.xml', for project it's 'misc.xml'
   */
  @Nonnull
  public static final String DEFAULT_FILE = "$DEFAULT_FILE$";

  @Nonnull
  @Deprecated
  @DeprecationInfo(value = "Module file types is not supported anymore")
  public static final String MODULE_FILE = "$MODULE_FILE$";

  private StoragePathMacros() {
  }

  /**
   * Allows to extract macro name from the given macro definition.
   * <p>
   * Basically, performs conversion like {@code '$NAME$' -> 'NAME'}.
   *
   * @param macro macro definition which name should be extracted.
   * @return name of the given macro definition
   * @throws IllegalArgumentException if given macro definition has unexpected format
   */
  @Nonnull
  @Deprecated
  public static String getMacroName(@Nonnull String macro) throws IllegalArgumentException {
    if (macro.length() < 3 || macro.charAt(0) != '$' || macro.charAt(macro.length() - 1) != '$') {
      throw new IllegalArgumentException("Malformed macro definition (" + macro + ")");
    }
    return macro.substring(1, macro.length() - 1);
  }
}
