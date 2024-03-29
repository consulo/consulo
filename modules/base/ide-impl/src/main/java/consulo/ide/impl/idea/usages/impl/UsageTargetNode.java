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
package consulo.ide.impl.idea.usages.impl;

import consulo.usage.UsageTarget;
import consulo.usage.UsageView;
import consulo.util.lang.ObjectUtil;

import jakarta.annotation.Nonnull;

/**
 * @author max
 */
class UsageTargetNode extends Node {
  UsageTargetNode(@Nonnull UsageTarget target) {
    setUserObject(target);
  }

  @Override
  public String tree2string(int indent, String lineSeparator) {
    return getTarget().getName();
  }

  @Override
  protected boolean isDataValid() {
    return getTarget().isValid();
  }

  @Override
  protected boolean isDataReadOnly() {
    return getTarget().isReadOnly();
  }

  @Override
  protected boolean isDataExcluded() {
    return false;
  }

  @Nonnull
  @Override
  protected String getText(@Nonnull final UsageView view) {
    return ObjectUtil.notNull(getTarget().getPresentation().getPresentableText(), "");
  }

  @Nonnull
  public UsageTarget getTarget() {
    return (UsageTarget)getUserObject();
  }

  @Override
  protected void updateNotify() {
    super.updateNotify();
    getTarget().update();
  }
}
