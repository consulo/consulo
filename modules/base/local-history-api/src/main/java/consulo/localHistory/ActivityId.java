// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.localHistory;

/**
 * Allows to identify which IDE activity created changes or labels in the local history
 * and to customize changes presentation.
 *
 * @param providerId {@link ActivityPresentationProvider#getId()} of the provider responsible for the activity
 * @param kind       additional information that can be used by the provider
 * @see ActivityPresentationProvider
 */
public record ActivityId(String providerId, String kind) {
}
