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
package com.intellij.openapi.projectRoots;

import consulo.bundle.BundleHolder;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EventListener;

/**
 * Represents the current state of the Bundle list in the Bundle configuration dialog.
 */
public interface SdkModel extends BundleHolder {
  /**
   * Allows to receive notifications when the SDK list has been changed by the
   * user configuring the SDKs.
   */

  interface Listener extends EventListener {
    /**
     * Called when a SDK has been added.
     *
     * @param sdk the added SDK.
     */
    @RequiredUIAccess
    default void sdkAdded(Sdk sdk) {
    }

    /**
     * Called after a SDK is removed.
     *
     * @param sdk the removed SDK.
     */
    @RequiredUIAccess
    default void sdkRemove(Sdk sdk) {
    }

    /**
     * Called when a SDK has been changed or renamed.
     *
     * @param sdk          the changed or renamed SDK.
     * @param previousName the old name of the changed or renamed SDK.
     * @since 5.0.1
     */
    @RequiredUIAccess
    default void sdkChanged(Sdk sdk, String previousName) {
    }

    /**
     * Called when the home directory of a SDK has been changed.
     *
     * @param sdk        the changed SDK.
     * @param newSdkHome the new home directory.
     */
    @RequiredUIAccess
    default void sdkHomeSelected(Sdk sdk, String newSdkHome) {
    }
  }

  @Nonnull
  @Override
  default Sdk[] getBundles() {
    return getSdks();
  }

  /**
   * Returns the list of SDKs in the table.
   *
   * @return the SDK list.
   */
  Sdk[] getSdks();

  /**
   * Returns the SDK with the specified name, or null if one is not found.
   *
   * @param sdkName the name of the SDK to find.
   * @return the SDK instance or null.
   */
  @Nullable
  Sdk findSdk(String sdkName);

  /**
   * Adds the specified SDK (already created and initialized) to the model.
   *
   * @param sdk the SDK to add
   */
  void addSdk(Sdk sdk);

  /**
   * Adds a listener for receiving notifications about changes in the list.
   *
   * @param listener the listener instance.
   */
  void addListener(Listener listener);

  /**
   * Adds a listener for receiving notifications about changes in the list.
   *
   * @param listener the listener instance.
   */
  void addListener(Listener listener, Disposable disposable);

  /**
   * Removes a listener for receiving notifications about changes in the list.
   *
   * @param listener the listener instance.
   */
  void removeListener(Listener listener);

  Listener getMulticaster();
}
