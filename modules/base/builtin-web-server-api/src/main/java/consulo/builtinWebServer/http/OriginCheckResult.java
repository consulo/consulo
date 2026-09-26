// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.builtinWebServer.http;

public enum OriginCheckResult {
    ALLOW,
    FORBID,
    /**
     * Any origin is allowed but user confirmation is required
     */
    ASK_CONFIRMATION
}
