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
package consulo.language.editor.template;

import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.document.util.TextRange;
import consulo.language.editor.template.event.TemplateEditingListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 20-Mar-22
 */
public interface TemplateState extends Disposable {
  @Nullable
  TextRange getCurrentVariableRange();

  int getCurrentVariableNumber();

  @Nullable
  TextResult getVariableValue(@Nonnull String name);

  void nextTab();

  void cancelTemplate();

  default void gotoEnd() {
    gotoEnd(true);
  }

  void gotoEnd(boolean brokenOff);

  int getSegmentsCount();

  @Nonnull
  TextRange getSegmentRange(int index);

  @Nullable
  TextRange getVariableRange(String varName);

  @Nonnull
  Editor getEditor();

  boolean isFinished();

  void addTemplateStateListener(TemplateEditingListener listener);
}
