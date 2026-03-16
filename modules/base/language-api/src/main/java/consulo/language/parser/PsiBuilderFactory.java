/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.language.parser;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.LighterLazyParseableNode;
import consulo.language.lexer.Lexer;
import consulo.language.version.LanguageVersion;
import consulo.project.Project;

import org.jspecify.annotations.Nullable;

/**
 * @author yole
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class PsiBuilderFactory {
    public static PsiBuilderFactory getInstance() {
        return Application.get().getInstance(PsiBuilderFactory.class);
    }

    
    public abstract PsiBuilder createBuilder(
        Project project,
        ASTNode chameleon,
        LanguageVersion languageVersion
    );

    
    public abstract PsiBuilder createBuilder(
        Project project,
        LighterLazyParseableNode chameleon,
        LanguageVersion languageVersion
    );

    
    public abstract PsiBuilder createBuilder(
        Project project,
        ASTNode chameleon,
        @Nullable Lexer lexer,
        Language lang,
        LanguageVersion languageVersion,
        CharSequence seq
    );

    
    public abstract PsiBuilder createBuilder(
        Project project,
        LighterLazyParseableNode chameleon,
        @Nullable Lexer lexer,
        Language lang,
        LanguageVersion languageVersion,
        CharSequence seq
    );

    
    public abstract PsiBuilder createBuilder(
        ParserDefinition parserDefinition,
        Lexer lexer,
        LanguageVersion languageVersion,
        CharSequence seq
    );
}
