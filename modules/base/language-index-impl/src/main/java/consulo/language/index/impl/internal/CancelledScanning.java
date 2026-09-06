// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

final class CancelledScanning implements ScanningParameters {
    static final CancelledScanning INSTANCE = new CancelledScanning();

    private CancelledScanning() {
    }

    @Override
    public String toString() {
        return "CancelledScanning";
    }
}
