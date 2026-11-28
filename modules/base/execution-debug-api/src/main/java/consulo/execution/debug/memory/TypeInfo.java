// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.memory;

/**
 * A type of the debugged runtime in the Memory view - its instances are listed by
 * {@link XMemoryViewHandler#computeInstances}.
 */
public interface TypeInfo {
    String name();

    /**
     * False when the runtime can not list the instances of the type.
     */
    boolean canGetInstanceInfo();
}
