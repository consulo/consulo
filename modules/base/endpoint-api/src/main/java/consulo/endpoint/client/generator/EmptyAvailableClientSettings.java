// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.client.generator;

final class EmptyAvailableClientSettings implements AvailableClientSettings {
    static final EmptyAvailableClientSettings INSTANCE = new EmptyAvailableClientSettings();

    private EmptyAvailableClientSettings() {
    }
}
