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
package consulo.ide.impl.idea.usages.impl;

import consulo.util.lang.StringUtil;
import consulo.navigation.Navigatable;
import consulo.usage.Usage;
import consulo.usage.UsageView;
import jakarta.annotation.Nonnull;

import java.util.Arrays;

/**
 * @author max
 */
public class UsageNode extends Node implements Comparable<UsageNode>, Navigatable {
  @Deprecated
  // todo remove in 2018.1
  public UsageNode(@Nonnull Usage usage, UsageViewTreeModelBuilder model) {
    this(null, usage);
  }

  public UsageNode(Node parent, @Nonnull Usage usage) {
    setUserObject(usage);
    setParent(parent);
  }

  public String toString() {
    return getUsage().toString();
  }

  @Override
  public String tree2string(int indent, String lineSeparator) {
    StringBuffer result = new StringBuffer();
    StringUtil.repeatSymbol(result, ' ', indent);
    result.append(getUsage());
    return result.toString();
  }

  @Override
  public int compareTo(@Nonnull UsageNode usageNode) {
    return UsageViewImpl.USAGE_COMPARATOR.compare(getUsage(), usageNode.getUsage());
  }

  @Nonnull
  public Usage getUsage() {
    return (Usage)getUserObject();
  }

  @Override
  public void navigate(boolean requestFocus) {
    getUsage().navigate(requestFocus);
  }

  @Override
  public boolean canNavigate() {
    return getUsage().isValid() && getUsage().canNavigate();
  }

  @Override
  public boolean canNavigateToSource() {
    return getUsage().isValid() && getUsage().canNavigate();
  }

  @Override
  protected boolean isDataValid() {
    return getUsage().isValid();
  }

  @Override
  protected boolean isDataReadOnly() {
    return getUsage().isReadOnly();
  }

  @Override
  protected boolean isDataExcluded() {
    return isExcluded();
  }

  @Nonnull
  @Override
  protected String getText(@Nonnull final UsageView view) {
    try {
      return getUsage().getPresentation().getPlainText();
    }
    catch(AbstractMethodError e) {
      return Arrays.asList(getUsage().getPresentation().getText()).toString();
    }
  }
}
