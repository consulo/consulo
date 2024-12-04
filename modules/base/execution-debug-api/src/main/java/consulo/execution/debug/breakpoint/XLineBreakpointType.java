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

package consulo.execution.debug.breakpoint;

import consulo.annotation.UsedInPlugin;
import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.execution.debug.XDebugProcess;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XDebuggerBundle;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;

import java.util.Collections;
import java.util.List;

/**
 * Implement this class to support new type of line breakpoints. An implementation should be registered in a plugin.xml:
 * <p>
 * &lt;extensions defaultExtensionNs="com.intellij"&gt;<br>
 * &nbsp;&nbsp;&lt;xdebugger.breakpointType implementation="qualified-class-name"/&gt;<br>
 * &lt;/extensions&gt;
 * <p><p>
 * In order to support actual setting breakpoints in a debugging process create a {@link XBreakpointHandler} implementation and return it
 * from {@link XDebugProcess#getBreakpointHandlers()} method
 *
 * @author nik
 */
public abstract class XLineBreakpointType<P extends XBreakpointProperties> extends XBreakpointType<XLineBreakpoint<P>,P> {
  protected XLineBreakpointType(@NonNls @Nonnull final String id, @Nls @Nonnull final String title) {
    super(id, title);
  }

  /**
   * return non-null value if a breakpoint should have specific properties besides containing file and line. These properties will be stored in
   * {@link XBreakpoint} instance and can be obtained by using {@link XBreakpoint#getProperties()} method
   */
  @Nullable
  public abstract P createBreakpointProperties(@Nonnull VirtualFile file, int line);

  @Override
  public String getDisplayText(final XLineBreakpoint<P> breakpoint) {
    return fileLineDisplayText(breakpoint.getPresentableFilePath(), breakpoint.getLine());
  }

  private static String fileLineDisplayText(String path, int line) {
    return XDebuggerBundle.message("xbreakpoint.default.display.text", line + 1, path);
  }

  /**
   * Source position for line breakpoint is determined by its file and line
   */
  @Override
  public XSourcePosition getSourcePosition(@Nonnull XBreakpoint<P> breakpoint) {
    return null;
  }

  @Override
  public String getShortText(XLineBreakpoint<P> breakpoint) {
    return fileLineDisplayText(breakpoint.getShortFilePath(), breakpoint.getLine());
  }

  /**
   * Default line breakpoints aren't supported
   */
  @Override
  public final XLineBreakpoint<P> createDefaultBreakpoint(@Nonnull XBreakpointCreator<P> creator) {
    return null;
  }

  public List<? extends AnAction> getAdditionalPopupMenuActions(@Nonnull XLineBreakpoint<P> breakpoint, @jakarta.annotation.Nullable XDebugSession currentSession) {
    return Collections.emptyList();
  }

  @Nonnull
  public Image getTemporaryIcon() {
    return ExecutionDebugIconGroup.breakpointBreakpoint();
  }

  /**
   * Return true if this breakpoint could be hit on lines other than the one specified,
   * an example is method breakpoint in java - it could be hit on any method overriding the one specified
   */
  public boolean canBeHitInOtherPlaces() {
    return false;
  }

  /**
   * @return range to highlight on the line, null to highlight the whole line
   */
  @Nullable
  public TextRange getHighlightRange(XLineBreakpoint<P> breakpoint) {
    return null;
  }

  /**
   * The priority is considered when several breakpoint types can be set inside a folded code block,
   * in this case we choose the type with the highest priority.
   * The priority also affects types sorting in various places.
   */
  public int getPriority() {
    return 0;
  }

  /**
   * Return a list of variants if there can be more than one breakpoint on the line
   */
  @Nonnull
  public List<? extends XLineBreakpointVariant> computeVariants(@Nonnull Project project, @Nonnull XSourcePosition position) {
    return Collections.emptyList();
  }

  public abstract class XLineBreakpointVariant {
    @RequiredReadAction
    @Nonnull
    public abstract String getText();

    @Nullable
    @RequiredReadAction
    public abstract Image getIcon();

    @Nullable
    @RequiredReadAction
    public abstract TextRange getHighlightRange();

    @Nullable
    public abstract P createProperties();
  }

  public class XLineBreakpointAllVariant extends XLineBreakpointVariant {
    protected final XSourcePosition mySourcePosition;

    public XLineBreakpointAllVariant(@Nonnull XSourcePosition position) {
      mySourcePosition = position;
    }

    @Nonnull
    @RequiredReadAction
    @Override
    public String getText() {
      return "All";
    }

    @RequiredReadAction
    @Nullable
    @Override
    public Image getIcon() {
      return null;
    }

    @RequiredReadAction
    @Nullable
    @Override
    public TextRange getHighlightRange() {
      return null;
    }

    @Override
    @Nullable
    public P createProperties() {
      return createBreakpointProperties(mySourcePosition.getFile(),
                                        mySourcePosition.getLine());
    }
  }

  @UsedInPlugin
  public class XLinePsiElementBreakpointVariant extends XLineBreakpointAllVariant {
    private final PsiElement myElement;

    public XLinePsiElementBreakpointVariant(@Nonnull XSourcePosition position, PsiElement element) {
      super(position);

      myElement = element;
    }

    @RequiredReadAction
    @Override
    public Image getIcon() {
      return IconDescriptorUpdaters.getIcon(myElement, 0);
    }

    @Nonnull
    @RequiredReadAction
    @Override
    public String getText() {
      return StringUtil.shortenTextWithEllipsis(myElement.getText(), 100, 0);
    }

    @RequiredReadAction
    @Override
    public TextRange getHighlightRange() {
      return myElement.getTextRange();
    }
  }
}
