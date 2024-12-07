/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.ui.tree.node;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.execution.debug.frame.XValueNode;
import consulo.execution.debug.frame.presentation.XRegularValuePresentation;
import consulo.execution.debug.frame.presentation.XValuePresentation;
import consulo.execution.debug.impl.internal.ui.DebuggerUIUtil;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import java.util.function.Function;

public final class XValueNodePresentationConfigurator {
  public interface ConfigurableXValueNode {
    void applyPresentation(@Nullable Image icon, @Nonnull XValuePresentation valuePresenter, boolean hasChildren);
  }

  public static abstract class ConfigurableXValueNodeImpl implements ConfigurableXValueNode, XValueNode {
    @Override
    public void setPresentation(
      @Nullable Image icon,
      @NonNls @Nullable String type,
      @NonNls @Nonnull String value,
      boolean hasChildren
    ) {
      XValueNodePresentationConfigurator.setPresentation(icon, type, value, hasChildren, this);
    }

    @Override
    public void setPresentation(
      @Nullable Image icon,
      @NonNls @Nullable String type,
      @NonNls @Nonnull String separator,
      @NonNls @Nullable String value,
      boolean hasChildren
    ) {
      XValueNodePresentationConfigurator.setPresentation(icon, type, separator, value, hasChildren, this);
    }

    @Override
    public void setPresentation(@Nullable Image icon, @Nonnull XValuePresentation presentation, boolean hasChildren) {
      XValueNodePresentationConfigurator.setPresentation(icon, presentation, hasChildren, this);
    }
  }

  public static void setPresentation(
    @Nullable Image icon,
    @Nonnull XValuePresentation presentation,
    boolean hasChildren,
    ConfigurableXValueNode node
  ) {
    doSetPresentation(icon, presentation, hasChildren, node);
  }

  public static void setPresentation(
    @Nullable Image icon,
    @NonNls @Nullable String type,
    @NonNls @Nonnull String value,
    boolean hasChildren,
    ConfigurableXValueNode node
  ) {
    doSetPresentation(icon, new XRegularValuePresentation(value, type), hasChildren, node);
  }

  public static void setPresentation(
    @Nullable Image icon,
    @NonNls @Nullable String type,
    @NonNls @Nonnull final String separator,
    @NonNls @Nullable String value,
    boolean hasChildren,
    ConfigurableXValueNode node
  ) {
    doSetPresentation(
      icon,
      new XRegularValuePresentation(StringUtil.notNullize(value), type, separator),
      hasChildren,
      node
    );
  }

  public static void setPresentation(
    @Nullable Image icon,
    @NonNls @Nullable String type,
    @NonNls @Nonnull String value,
    @Nullable Function<String, String> valuePresenter,
    boolean hasChildren, ConfigurableXValueNode node
  ) {
    doSetPresentation(
      icon,
      valuePresenter == null
        ? new XRegularValuePresentation(value, type)
        : new XValuePresentationAdapter(value, type, valuePresenter),
      hasChildren,
      node
    );
  }

  private static void doSetPresentation(
    @Nullable final Image icon,
    @Nonnull final XValuePresentation presentation,
    final boolean hasChildren,
    final ConfigurableXValueNode node
  ) {
    if (DebuggerUIUtil.isObsolete(node)) {
      return;
    }

    Application application = ApplicationManager.getApplication();
    if (application.isDispatchThread()) {
      node.applyPresentation(icon, presentation, hasChildren);
    }
    else {
      Runnable updater = () -> node.applyPresentation(icon, presentation, hasChildren);
      if (node instanceof XDebuggerTreeNode xDebuggerTreeNode) {
        xDebuggerTreeNode.invokeNodeUpdate(updater);
      }
      else {
        application.invokeLater(updater);
      }
    }
  }

  private static final class XValuePresentationAdapter extends XValuePresentation {
    private final String myValue;
    private final String myType;
    private final Function<String, String> valuePresenter;

    public XValuePresentationAdapter(String value, String type, Function<String, String> valuePresenter) {
      myValue = value;
      myType = type;
      this.valuePresenter = valuePresenter;
    }

    @Nullable
    @Override
    public String getType() {
      return myType;
    }

    @Override
    public void renderValue(@Nonnull XValueTextRenderer renderer) {
      renderer.renderValue(valuePresenter.apply(myValue));
    }
  }
}