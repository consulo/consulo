/*
 * Copyright 2013-2025 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.eap;

import consulo.application.eap.EarlyAccessProgramDescriptor;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

/**
 * @author UNV
 * @since 2025-04-19
 */
record EarlyAccessProgramDescription(
    boolean available,
    @Nonnull String name,
    @Nonnull String decription,
    @Nonnull Class<? extends EarlyAccessProgramDescriptor> clazz,
    boolean restartRequired,
    @Nonnull EarlyAccessProgramDescriptor descriptor
) implements Comparable<EarlyAccessProgramDescription> {
    public EarlyAccessProgramDescription(@Nonnull EarlyAccessProgramDescriptor descriptor) {
        this(
            descriptor.isAvailable(),
            descriptor.getName(),
            StringUtil.notNullize(descriptor.getDescription()),
            descriptor.getClass(),
            descriptor.isRestartRequired(),
            descriptor
        );
    }

    @Override
    public int compareTo(@Nonnull EarlyAccessProgramDescription o) {
        if (available == o.available) {
            return name.compareToIgnoreCase(o.name);
        }
        return available ? -1 : +1;
    }
}
