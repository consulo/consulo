/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * User: anna
 * Date: 25-Jan-2008
 */
package consulo.ide.impl.psi.search.scope.packageSet;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.lexer.Lexer;
import consulo.component.extension.ExtensionPointName;
import consulo.content.scope.PackageSet;
import consulo.content.scope.ParsingException;

import jakarta.annotation.Nullable;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface PackageSetParserExtension {
    ExtensionPointName<PackageSetParserExtension> EP_NAME = ExtensionPointName.create(PackageSetParserExtension.class);

    @Nullable
    PackageSet parsePackageSet(Lexer lexer, final String scope, String modulePattern) throws ParsingException;

    @Nullable
    String parseScope(Lexer lexer);
}