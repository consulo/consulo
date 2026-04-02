// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details;

sealed interface ReviewAction permits ReviewAction.Checkout, ReviewAction.ShowInLog, ReviewAction.CopyBranchName {
    final class Checkout implements ReviewAction {
        static final Checkout INSTANCE = new Checkout();

        private Checkout() {
        }
    }

    final class ShowInLog implements ReviewAction {
        static final ShowInLog INSTANCE = new ShowInLog();

        private ShowInLog() {
        }
    }

    final class CopyBranchName implements ReviewAction {
        static final CopyBranchName INSTANCE = new CopyBranchName();

        private CopyBranchName() {
        }
    }
}
