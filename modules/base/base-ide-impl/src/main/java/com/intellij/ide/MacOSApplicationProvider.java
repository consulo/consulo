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
package com.intellij.ide;

import com.intellij.openapi.components.ServiceManager;
import consulo.logging.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.StreamUtil;

import javax.annotation.Nullable;
import jakarta.inject.Singleton;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * @author max
 */
@Singleton
public class MacOSApplicationProvider {
  public static MacOSApplicationProvider getInstance() {
    return ServiceManager.getService(MacOSApplicationProvider.class);
  }

  private static final Logger LOG = Logger.getInstance(MacOSApplicationProvider.class);

  private static final String GENERIC_RGB_PROFILE_PATH = "/System/Library/ColorSync/Profiles/Generic RGB Profile.icc";

  private final ColorSpace genericRgbColorSpace;

  public MacOSApplicationProvider() {
    genericRgbColorSpace = SystemInfo.isMac ? initializeNativeColorSpace() : null;
  }

  private static ColorSpace initializeNativeColorSpace() {
    InputStream is = null;
    try {
      is = new FileInputStream(GENERIC_RGB_PROFILE_PATH);
      ICC_Profile profile = ICC_Profile.getInstance(is);
      return new ICC_ColorSpace(profile);
    }
    catch (Throwable e) {
      LOG.warn("Couldn't load generic RGB color profile", e);
      return null;
    }
    finally {
      StreamUtil.closeStream(is);
    }
  }

  @Nullable
  public ColorSpace getGenericRgbColorSpace() {
    return genericRgbColorSpace;
  }
}
