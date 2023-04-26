/*
 * Copyright 2013-2023 consulo.io
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
package consulo.disposer.internal;

import consulo.disposer.Disposable;

import java.util.List;
import java.util.ServiceLoader;

/**
 * @author VISTALL
 * @since 26/04/2023
 */
public class DisposerChecker {
  private static boolean checkDisposer = Boolean.getBoolean("consulo.checker.disposer.register");

  private static List<DiposerRegisterChecker> CHECKERS;

  public static void checkRegister(Disposable parent, Disposable target) {
    if (!checkDisposer) {
      return;
    }

    if (CHECKERS == null) {
      CHECKERS = ServiceLoader.load(DiposerRegisterChecker.class, DisposerChecker.class.getClassLoader())
                              .stream()
                              .map(ServiceLoader.Provider::get)
                              .toList();
    }

    for (DiposerRegisterChecker checker : CHECKERS) {
      checker.checkRegister(parent, target);
    }
  }
}
