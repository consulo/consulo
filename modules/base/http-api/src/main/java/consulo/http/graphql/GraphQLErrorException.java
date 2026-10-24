// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.http.graphql;

import consulo.http.graphql.dto.GraphQLErrorDTO;
import consulo.http.localize.HttpLocalize;

import java.util.List;

public class GraphQLErrorException extends RuntimeException {
    private final List<GraphQLErrorDTO> myErrors;

    public GraphQLErrorException(List<? extends GraphQLErrorDTO> errors) {
        super("GraphQL error: " + errors);
        myErrors = List.copyOf(errors);
    }

    public List<GraphQLErrorDTO> getErrors() {
        return myErrors;
    }

    @Override
    public String getLocalizedMessage() {
        return HttpLocalize.graphqlErrors(myErrors.toString()).get();
    }
}
