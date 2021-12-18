/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.editor.impl;

import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import consulo.editor.impl.CodeEditorBase;
import consulo.editor.impl.CodeEditorScrollingModelBase;

import javax.annotation.Nonnull;
import java.awt.*;

/**
 * @author VISTALL
 * @since 18/12/2021
 */
public class DesktopSwtCodeEditorScrollingModelImpl extends CodeEditorScrollingModelBase {
  public DesktopSwtCodeEditorScrollingModelImpl(CodeEditorBase editor) {
    super(editor);
  }

  @Override
  public void accumulateViewportChanges() {

  }

  @Override
  public void flushViewportChanges() {

  }

  @Nonnull
  @Override
  public Rectangle getVisibleArea() {
    return new Rectangle(0, 0);
  }

  @Nonnull
  @Override
  public Rectangle getVisibleAreaOnScrollingFinished() {
    return new Rectangle(0, 0);
  }

  @Override
  public void scrollToCaret(@Nonnull ScrollType scrollType) {

  }

  @Override
  public void scrollTo(@Nonnull LogicalPosition pos, @Nonnull ScrollType scrollType) {

  }

  @Override
  public void runActionOnScrollingFinished(@Nonnull Runnable action) {

  }

  @Override
  public void disableAnimation() {

  }

  @Override
  public void enableAnimation() {

  }

  @Override
  public int getVerticalScrollOffset() {
    return 0;
  }

  @Override
  public int getHorizontalScrollOffset() {
    return 0;
  }

  @Override
  public void scrollVertically(int scrollOffset) {

  }

  @Override
  public void scrollHorizontally(int scrollOffset) {

  }

  @Override
  public void scroll(int horizontalOffset, int verticalOffset) {

  }
}
