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
package com.intellij.xdebugger.frame;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.xdebugger.Obsolescent;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Represents a stack of executions frames usually corresponding to a thread. It is shown in 'Frames' panel of
 * 'Debug' tool window
 *
 * @author nik
 */
public abstract class XExecutionStack {
  public static final XExecutionStack[] EMPTY_ARRAY = new XExecutionStack[0];
  private final String myDisplayName;
  private final Image myIcon;

  /**
   * @param displayName presentable name of the thread to be shown in the combobox in 'Frames' tab
   */
  protected XExecutionStack(final String displayName) {
    this(displayName, AllIcons.Debugger.ThreadSuspended);
  }

  /**
   * @param displayName presentable name of the thread to be shown in the combobox in 'Frames' tab
   * @param icon icon to be shown in the combobox in 'Frames' tab
   */
  protected XExecutionStack(final @Nonnull String displayName, final @Nullable Image icon) {
    myDisplayName = displayName;
    myIcon = icon;
  }

  @Nonnull
  public final String getDisplayName() {
    return myDisplayName;
  }

  @Nullable
  public final Image getIcon() {
    return myIcon;
  }

  /**
   * Override this method to provide an icon with optional tooltip and popup actions. This icon will be shown on the editor gutter to the
   * left of the execution line when this thread is selected in 'Frames' tab.
   * @return
   */
  @Nullable
  public GutterIconRenderer getExecutionLineIconRenderer() {
    return null;
  }

  /**
   * Return top stack frame synchronously
   * @return top stack frame or <code>null</code> if it isn't available
   */
  @Nullable
  public abstract XStackFrame getTopFrame();

  /**
   * Start computing stack frames top-down starting from <code>firstFrameIndex</code>. This method is called from the Event Dispatch Thread
   * so it should return quickly
   * @param container callback
   */
  public abstract void computeStackFrames(XStackFrameContainer container);

  public interface XStackFrameContainer extends Obsolescent, XValueCallback {
    /**
     * Add stack frames to the list
     * @param stackFrames stack frames to add
     * @param last <code>true</code> if all frames are added
     */
    void addStackFrames(@Nonnull List<? extends XStackFrame> stackFrames, final boolean last);
  }
}
