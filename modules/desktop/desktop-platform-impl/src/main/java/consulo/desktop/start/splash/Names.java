/*
 * Copyright 2013-2017 consulo.io
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
package consulo.desktop.start.splash;

/**
 * @author VISTALL
 * @since 11-May-17
 */
class Names {
  final static String[] ourEasterNames = {"VISTALL", "IJ_IDEA", ".WORMIX"};

  final static String ourName = "CONSULO";

  static {
    for (String easterEgg : ourEasterNames) {
      if (easterEgg.length() != ourName.length()) {
        throw new IllegalArgumentException("bad text: " + easterEgg);
      }
    }
  }
}
