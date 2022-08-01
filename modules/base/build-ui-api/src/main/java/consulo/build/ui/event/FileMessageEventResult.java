// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.event;

import consulo.build.ui.FilePosition;

/**
 * @author Vladislav.Soroka
 */
public interface FileMessageEventResult extends MessageEventResult {
  FilePosition getFilePosition();
}
