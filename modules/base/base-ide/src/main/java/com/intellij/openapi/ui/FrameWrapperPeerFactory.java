/*
 * Copyright 2013-2019 consulo.io
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
package com.intellij.openapi.ui;

import com.intellij.openapi.wm.IdeFrame;

import javax.swing.*;

/**
* @author VISTALL
* @since 2019-02-15
 *
 * Hack for extract desktop dep to desktop module
 * TODO [VISTALL] drop this class when FrameWrapper removed
*/
public interface FrameWrapperPeerFactory {
  JFrame createJFrame(FrameWrapper owner, IdeFrame parent);

  JDialog createJDialog(FrameWrapper owner, IdeFrame parent);
}
