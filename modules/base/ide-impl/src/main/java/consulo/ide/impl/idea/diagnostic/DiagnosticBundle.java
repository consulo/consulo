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
package consulo.ide.impl.idea.diagnostic;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.internal.MigratedExtensionsTo;
import consulo.component.util.localize.AbstractBundle;
import consulo.externalService.localize.ExternalServiceLocalize;
import org.jetbrains.annotations.PropertyKey;

/**
 * @author yole
 */
@Deprecated(forRemoval = true)
@DeprecationInfo("Use ExternalServiceLocalize")
@MigratedExtensionsTo(ExternalServiceLocalize.class)
public class DiagnosticBundle extends AbstractBundle {
  private static final DiagnosticBundle ourInstance = new DiagnosticBundle();

  private DiagnosticBundle() {
    super("messages.DiagnosticBundle");
  }

  public static String message(@PropertyKey(resourceBundle = "messages.DiagnosticBundle") String key) {
    return ourInstance.getMessage(key);
  }

  public static String message(@PropertyKey(resourceBundle = "messages.DiagnosticBundle") String key, Object... params) {
    return ourInstance.getMessage(key, params);
  }
}
