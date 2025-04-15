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
package consulo.usage;

import consulo.component.util.localize.BundleBase;
import consulo.localize.LocalizeValue;
import consulo.usage.localize.UsageLocalize;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;

public final class UsageType {
    public static final UsageType CLASS_INSTANCE_OF = new UsageType(UsageLocalize.usageTypeInstanceof());
    public static final UsageType CLASS_IMPORT = new UsageType(UsageLocalize.usageTypeImport());
    public static final UsageType CLASS_CAST_TO = new UsageType(UsageLocalize.usageTypeCastTarget());
    public static final UsageType CLASS_EXTENDS_IMPLEMENTS_LIST = new UsageType(UsageLocalize.usageTypeExtends());
    public static final UsageType CLASS_STATIC_MEMBER_ACCESS = new UsageType(UsageLocalize.usageTypeStaticMember());
    public static final UsageType CLASS_NESTED_CLASS_ACCESS = new UsageType(UsageLocalize.usageTypeNestedClass());
    public static final UsageType CLASS_METHOD_THROWS_LIST = new UsageType(UsageLocalize.usageTypeThrowsList());
    public static final UsageType CLASS_CLASS_OBJECT_ACCESS = new UsageType(UsageLocalize.usageTypeClassObject());
    public static final UsageType CLASS_FIELD_DECLARATION = new UsageType(UsageLocalize.usageTypeFieldDeclaration());
    public static final UsageType CLASS_LOCAL_VAR_DECLARATION = new UsageType(UsageLocalize.usageTypeLocalDeclaration());
    public static final UsageType CLASS_METHOD_PARAMETER_DECLARATION = new UsageType(UsageLocalize.usageTypeParameterDeclaration());
    public static final UsageType CLASS_CATCH_CLAUSE_PARAMETER_DECLARATION = new UsageType(UsageLocalize.usageTypeCatchDeclaration());
    public static final UsageType CLASS_METHOD_RETURN_TYPE = new UsageType(UsageLocalize.usageTypeReturn());
    public static final UsageType CLASS_NEW_OPERATOR = new UsageType(UsageLocalize.usageTypeNew());
    public static final UsageType CLASS_ANONYMOUS_NEW_OPERATOR = new UsageType(UsageLocalize.usageTypeNewAnonymous());
    public static final UsageType CLASS_NEW_ARRAY = new UsageType(UsageLocalize.usageTypeNewArray());
    public static final UsageType ANNOTATION = new UsageType(UsageLocalize.usageTypeAnnotation());
    public static final UsageType TYPE_PARAMETER = new UsageType(UsageLocalize.usageTypeTypeParameter());

    public static final UsageType READ = new UsageType(UsageLocalize.usageTypeRead());
    public static final UsageType WRITE = new UsageType(UsageLocalize.usageTypeWrite());

    public static final UsageType LITERAL_USAGE = new UsageType(UsageLocalize.usageTypeStringConstant());
    public static final UsageType COMMENT_USAGE = new UsageType(UsageLocalize.usageTypeComment());

    public static final UsageType UNCLASSIFIED = new UsageType(UsageLocalize.usageTypeUnclassified());

    public static final UsageType RECURSION = new UsageType(UsageLocalize.usageTypeRecursion());
    public static final UsageType DELEGATE_TO_SUPER = new UsageType(UsageLocalize.usageTypeDelegateToSuperMethod());
    public static final UsageType DELEGATE_TO_SUPER_PARAMETERS_CHANGED =
        new UsageType(UsageLocalize.usageTypeDelegateToSuperMethodParametersChanged());
    public static final UsageType DELEGATE_TO_ANOTHER_INSTANCE = new UsageType(UsageLocalize.usageTypeDelegateToAnotherInstanceMethod());
    public static final UsageType DELEGATE_TO_ANOTHER_INSTANCE_PARAMETERS_CHANGED =
        new UsageType(UsageLocalize.usageTypeDelegateToAnotherInstanceMethodParametersChanged());

    private final String myName;

    public UsageType(@Nonnull LocalizeValue name) {
        myName = name.get();
    }

    @Deprecated
    public UsageType(String name) {
        myName = name;
    }

    @Nonnull
    public String toString(@Nonnull UsageViewPresentation presentation) {
        String word = presentation.getUsagesWord();
        String usageWord = StringUtil.startsWithChar(myName, '{') ? StringUtil.capitalize(word) : word;
        return BundleBase.format(myName, usageWord);
    }

    @Override
    public String toString() {
        return myName;
    }
}
