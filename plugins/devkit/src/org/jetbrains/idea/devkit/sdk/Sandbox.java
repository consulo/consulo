/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package org.jetbrains.idea.devkit.sdk;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.projectRoots.SdkModel;
import com.intellij.openapi.projectRoots.ValidatableSdkAdditionalData;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.LocalFileSystem;
import org.consulo.lombok.annotations.Logger;
import org.jdom.Element;
import org.jetbrains.idea.devkit.DevKitBundle;

/**
 * User: anna
 * Date: Nov 22, 2004
 */
@Logger
public class Sandbox implements ValidatableSdkAdditionalData, JDOMExternalizable {
  @SuppressWarnings({"WeakerAccess"})
  public String mySandboxHome;

  private LocalFileSystem.WatchRequest mySandboxRoot = null;

  public Sandbox(String sandboxHome) {
    mySandboxHome = sandboxHome;
    if (mySandboxHome != null) {
      mySandboxRoot = LocalFileSystem.getInstance().addRootToWatch(mySandboxHome, true);
    }
  }

  public Sandbox() {
  }

  public String getSandboxHome() {
    return mySandboxHome;
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return new Sandbox(mySandboxHome);
  }

  @Override
  public void checkValid(SdkModel sdkModel) throws ConfigurationException {
    if (mySandboxHome == null || mySandboxHome.length() == 0) {
      throw new ConfigurationException(DevKitBundle.message("sandbox.specification"));
    }
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    DefaultJDOMExternalizer.readExternal(this, element);
    LOGGER.assertTrue(mySandboxRoot == null);

    if (mySandboxHome != null) {
      mySandboxRoot = LocalFileSystem.getInstance().addRootToWatch(mySandboxHome, true);
    }
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    DefaultJDOMExternalizer.writeExternal(this, element);
  }

  void cleanupWatchedRoots() {
    if (mySandboxRoot != null) {
      LocalFileSystem.getInstance().removeWatchedRoot(mySandboxRoot);
    }
  }
}
