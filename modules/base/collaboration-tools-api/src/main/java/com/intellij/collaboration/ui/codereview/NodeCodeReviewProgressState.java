// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui.codereview;

/**
 * @param isLoading if true, the state data for the node is still loading.
 */
record NodeCodeReviewProgressState(boolean isLoading, boolean isRead, int discussionsCount) {
}
