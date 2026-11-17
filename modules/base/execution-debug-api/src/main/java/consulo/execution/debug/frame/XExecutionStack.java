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
package consulo.execution.debug.frame;

import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.execution.debug.Obsolescent;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Represents a stack of executions frames usually corresponding to a thread. It is shown in 'Frames' panel of
 * 'Debug' tool window
 *
 * @author nik
 */
public abstract class XExecutionStack {
  /**
   * The execution stacks selected in a view of the threads, for the actions of the threads.
   */
  public static final Key<List<XExecutionStack>> SELECTED_STACKS = Key.create("XExecutionStacks");

  public static final XExecutionStack[] EMPTY_ARRAY = new XExecutionStack[0];
  private final String myDisplayName;
  private volatile @Nullable Image myIcon;

  /**
   * @param displayName presentable name of the thread to be shown in the combobox in 'Frames' tab
   */
  protected XExecutionStack(String displayName) {
    this(displayName, ExecutionDebugIconGroup.threadThreadsuspended());
  }

  /**
   * @param displayName presentable name of the thread to be shown in the combobox in 'Frames' tab
   * @param icon icon to be shown in the combobox in 'Frames' tab
   */
  protected XExecutionStack(String displayName, @Nullable Image icon) {
    myDisplayName = displayName;
    myIcon = icon;
  }

  
  public final String getDisplayName() {
    return myDisplayName;
  }

  public final @Nullable Image getIcon() {
    return myIcon;
  }

  /**
   * Changes the icon, for example when the state of the thread changes - the views show it when they are rebuilt.
   */
  public final void setIcon(@Nullable Image icon) {
    myIcon = icon;
  }

  /**
   * Additional information shown after the name of the thread, the state of the thread for example.
   */
  public @Nullable AdditionalDisplayInfo getAdditionalDisplayInfo() {
    return null;
  }

  /**
   * Override this method to provide an icon with optional tooltip and popup actions. This icon will be shown on the editor gutter to the
   * left of the execution line when this thread is selected in 'Frames' tab.
   * @return
   */
  public @Nullable GutterIconRenderer getExecutionLineIconRenderer() {
    return null;
  }

  /**
   * Return top stack frame synchronously
   * @return top stack frame or <code>null</code> if it isn't available
   */
  public abstract @Nullable XStackFrame getTopFrame();

  /**
   * Start computing stack frames top-down starting from <code>firstFrameIndex</code>. This method is called from the Event Dispatch Thread
   * so it should return quickly
   * @param container callback
   */
  public abstract void computeStackFrames(XStackFrameContainer container);

  public static class AdditionalDisplayInfo {
    private final LocalizeValue myText;
    private final LocalizeValue myTooltip;

    public AdditionalDisplayInfo(LocalizeValue text, LocalizeValue tooltip) {
      myText = text;
      myTooltip = tooltip;
    }

    public LocalizeValue getText() {
      return myText;
    }

    public LocalizeValue getTooltip() {
      return myTooltip;
    }
  }

  public interface XStackFrameContainer extends Obsolescent, XValueCallback {
    /**
     * Add stack frames to the list
     * @param stackFrames stack frames to add
     * @param last <code>true</code> if all frames are added
     */
    void addStackFrames(List<? extends XStackFrame> stackFrames, boolean last);
  }
}
