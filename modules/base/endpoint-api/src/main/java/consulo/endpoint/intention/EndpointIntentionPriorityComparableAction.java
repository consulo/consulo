// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.intention;

/**
 * Interface to mark microservices intentions and provide order to them.
 */
public interface EndpointIntentionPriorityComparableAction extends Comparable<Object> {
    int getEndpointActionPriority();

    @Override
    default int compareTo(Object other) {
        if (!(other instanceof EndpointIntentionPriorityComparableAction otherAction)) {
            return 0;
        }
        return -Integer.compare(getEndpointActionPriority(), otherAction.getEndpointActionPriority());
    }
}
