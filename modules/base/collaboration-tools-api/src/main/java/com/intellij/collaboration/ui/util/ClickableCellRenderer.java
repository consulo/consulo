// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

@ApiStatus.Internal
public interface ClickableCellRenderer<T> extends ListCellRenderer<T> {
    @Nullable
    Object getTagAt(@Nonnull Point point);
}
