// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar.model;

import java.util.List;

public record NavBarItemExpandResultData(
    List<NavBarVmItem> children,
    boolean navigateOnClick
) implements NavBarItemExpandResult {
}
