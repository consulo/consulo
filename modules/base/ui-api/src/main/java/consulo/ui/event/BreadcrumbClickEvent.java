/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ui.event;

import consulo.ui.Breadcrumbs;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.details.ProgrammaticInputDetails;

/**
 * @author VISTALL
 * @since 2026-10-26
 */
public final class BreadcrumbClickEvent<E> extends ComponentEvent<Breadcrumbs<E>> {
    private final E myItem;

    public BreadcrumbClickEvent(Breadcrumbs<E> component, E item) {
        this(component, item, ProgrammaticInputDetails.INSTANCE);
    }

    public BreadcrumbClickEvent(Breadcrumbs<E> component, E item, InputDetails inputDetails) {
        super(component, inputDetails);
        myItem = item;
    }

    public E getItem() {
        return myItem;
    }
}
