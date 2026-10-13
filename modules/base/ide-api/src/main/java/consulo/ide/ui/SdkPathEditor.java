/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.ui;

import consulo.application.Application;
import consulo.content.OrderRootType;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkModificator;
import consulo.content.platform.PlatformTarget;
import consulo.content.platform.PlatformTargetManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SdkPathEditor extends PathEditor {
  private final LocalizeValue myDisplayName;
  private final String myOrderRootType;
  private final boolean myImmutable;

  private @Nullable String myPlatformTargetId;

  public SdkPathEditor(LocalizeValue displayName, String orderRootType, FileChooserDescriptor descriptor, Sdk sdk) {
    this(displayName, orderRootType, descriptor, sdk.isPredefined());
    myPlatformTargetId = sdk.getPlatformTargetId();
  }

  public SdkPathEditor(LocalizeValue displayName, String orderRootType, FileChooserDescriptor descriptor, boolean immutable) {
    super(descriptor);
    myDisplayName = displayName;
    myOrderRootType = orderRootType;
    myImmutable = immutable;
  }

  /**
   * Roots of an SDK on another machine are chosen there, connecting to it first.
   */
  @RequiredUIAccess
  @Override
  protected CompletableFuture<Platform> connectPlatform() {
    String targetId = myPlatformTargetId;
    if (targetId == null) {
      return super.connectPlatform();
    }
    PlatformTarget target = Application.get().getInstance(PlatformTargetManager.class).findTarget(targetId);
    if (target == null) {
      return CompletableFuture.failedFuture(new IllegalStateException(ProjectLocalize.errorPlatformTargetMissing(targetId).get()));
    }
    return target.connect();
  }

  @Override
  public boolean isImmutable() {
    return myImmutable;
  }

  public LocalizeValue getDisplayName() {
    return myDisplayName;
  }

  public String getOrderRootType() {
    return myOrderRootType;
  }

  public void apply(SdkModificator sdkModificator) {
    sdkModificator.removeRoots(myOrderRootType);
    // add all items
    for (int i = 0; i < getRowCount(); i++) {
      sdkModificator.addRoot(getValueAt(i), myOrderRootType);
    }
    setModified(false);
  }

  @RequiredUIAccess
  public void reset(@Nullable SdkModificator modificator) {
    if (modificator != null) {
      resetPath(List.of(modificator.getRoots(myOrderRootType)));
    }
    else {
      setEnabled(false);
    }
  }

}
