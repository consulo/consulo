// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem;

import java.io.File;

public record Ignored(File ignoreFile, String matchedPattern) implements IgnoredCheckResult {
}
