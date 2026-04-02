// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.async;

@ApiStatus.Internal
public sealed interface Change<V> permits AddedFirst, AddedLast, AddedAllLast, Deleted, Updated {
}
