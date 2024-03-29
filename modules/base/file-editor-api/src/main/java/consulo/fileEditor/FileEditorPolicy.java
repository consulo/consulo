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
package consulo.fileEditor;

public enum FileEditorPolicy {

  /**
   * Place created editor before default IDE editor (if any)
   *
   */
  /*
   * should be the first declaration
   */
  PLACE_BEFORE_DEFAULT_EDITOR,

  /**
   * No policy
   */
  NONE,

  /**
   * Do not create default IDE editor (if any) for the file
   */
  HIDE_DEFAULT_EDITOR,

  /**
   * Place created editor after the default IDE editor (if any)
   *
   *
   */
  /*
   * should be the last declaration
   */
  PLACE_AFTER_DEFAULT_EDITOR
}
