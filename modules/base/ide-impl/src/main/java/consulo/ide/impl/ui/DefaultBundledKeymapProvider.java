/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.ui;

import consulo.annotation.component.ExtensionImpl;
import consulo.ui.ex.keymap.BundledKeymapProvider;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 17-Jun-22
 */
@ExtensionImpl
public class DefaultBundledKeymapProvider implements BundledKeymapProvider {
  @Nonnull
  @Override
  public String[] getKeymapFiles() {
    return new String[]{
    "/keymap/Keymap_Default.xml",
    "/keymap/Keymap_DefaultXWin.xml",
    "/keymap/Keymap_DefaultGNOME.xml",
    "/keymap/Keymap_DefaultKDE.xml",
    "/keymap/Keymap_DefaultMac.xml",
    "/keymap/Keymap_MacClassic.xml",
    "/keymap/Keymap_Emacs.xml",
    "/keymap/Keymap_VisualStudio.xml",
    "/keymap/Keymap_VisualStudioForMac.xml",
    "/keymap/Keymap_Eclipse.xml",
    "/keymap/Keymap_EclipseMac.xml",
    "/keymap/Keymap_Netbeans.xml",
    "/keymap/Keymap_VSCode.xml",
    "/keymap/Keymap_VSCode_OSX.xml",
    };
  }
}
