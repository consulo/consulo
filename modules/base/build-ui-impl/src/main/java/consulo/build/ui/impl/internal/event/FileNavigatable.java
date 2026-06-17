// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.impl.internal.event;

import consulo.annotation.DeprecationInfo;
import consulo.build.ui.FilePosition;
import consulo.project.Project;

/**
 * @author Vladislav.Soroka
 */
@Deprecated
@DeprecationInfo("Use FileNavigable, typo-corrected class name")
@SuppressWarnings("SpellCheckingInspection")
public class FileNavigatable extends FileNavigable {
    public FileNavigatable(Project project, FilePosition filePosition) {
        super(project, filePosition);
    }
}
