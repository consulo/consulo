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
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.content.OrderRootType;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkModificator;
import consulo.application.platform.PlatformTarget;
import consulo.application.platform.PlatformTargetManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.UIAccess;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineContext;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SdkPathEditor extends PathEditor {
  private final LocalizeValue myDisplayName;
  private final String myOrderRootType;
  private final boolean myImmutable;

  private @Nullable String myPlatformTargetId;
  private volatile boolean myRootsLoaded = true;

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
    if (!myRootsLoaded) {
      // nothing shown yet, nothing changed - the roots stay as they are
      return;
    }
    sdkModificator.removeRoots(myOrderRootType);
    // add all items
    for (int i = 0; i < getRowCount(); i++) {
      sdkModificator.addRoot(getValueAt(i), myOrderRootType);
    }
    setModified(false);
  }

  /**
   * The roots are found off the UI thread - a root of an SDK of another platform is a file of that machine, an archive root is
   * the archive opened.
   */
  @RequiredUIAccess
  public void reset(@Nullable SdkModificator modificator) {
    if (modificator == null) {
      setEnabled(false);
      return;
    }
    myRootsLoaded = false;
    resetPath(List.of());
    CoroutineContext context = Application.get().coroutineContext().copy();
    context.putCopyableUserData(UIAccess.KEY, UIAccess.current());
    CoroutineScope.launchAsync(
      context,
      () -> Coroutine
        .first(ReadLock.<Object, List<VirtualFile>>apply(ignored -> List.of(modificator.getRoots(myOrderRootType))))
        .then(UIAction.<List<VirtualFile>, List<VirtualFile>>apply(roots -> {
          resetPath(roots);
          myRootsLoaded = true;
          return roots;
        }))
    );
  }

}
