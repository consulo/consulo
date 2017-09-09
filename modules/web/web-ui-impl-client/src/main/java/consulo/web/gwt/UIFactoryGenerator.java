/*
 * Copyright 2013-2016 consulo.io
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
package consulo.web.gwt;

import com.google.gwt.core.ext.*;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import consulo.web.gwt.client.ui.InternalGwtComponent;
import consulo.web.gwt.client.ui.UIFactory;
import consulo.web.gwt.shared.state.UIComponentState;

import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 19-Jun-16
 */
public class UIFactoryGenerator extends IncrementalGenerator {
  @Override
  public RebindResult generateIncrementally(TreeLogger treeLogger, GeneratorContext generatorContext, String s) throws UnableToCompleteException {
    final TypeOracle typeOracle = generatorContext.getTypeOracle();

    final JClassType marker = typeOracle.findType(InternalGwtComponent.class.getName());
    if (marker == null) {
      treeLogger.log(TreeLogger.Type.ERROR, "'InternalGwtComponent' not found");
      return null;
    }

    Map<String, Map.Entry<String, String>> targets = new HashMap<>();
    for (JClassType classType : typeOracle.getTypes()) {
      if (classType.isAbstract()) {
        continue;
      }

      if (classType.isAssignableTo(marker)) {
        String stateType = UIComponentState.class.getName();

        JClassType[] implementedInterfaces = classType.getImplementedInterfaces();
        for (JClassType implementedInterface : implementedInterfaces) {
          if (implementedInterface.isAssignableFrom(marker) && implementedInterface instanceof JParameterizedType) {
            JClassType paramType = ((JParameterizedType)implementedInterface).getTypeArgs()[0];

            stateType = paramType.getQualifiedSourceName();
          }
        }

        final String name = classType.getName();

        targets.put("consulo.ui.internal.W" + name, new AbstractMap.SimpleEntry<>(name, stateType));
      }
    }

    final String implPackageName = UIFactory.class.getPackage().getName();
    String implClassName = UIFactory.class.getSimpleName() + "Impl";

    ClassSourceFileComposerFactory composer = new ClassSourceFileComposerFactory(implPackageName, implClassName);
    PrintWriter printWriter = generatorContext.tryCreate(treeLogger, implPackageName, implClassName);
    if (printWriter != null) {
      composer.addImplementedInterface(UIFactory.class.getName());

      SourceWriter sourceWriter = composer.createSourceWriter(generatorContext, printWriter);

      genClass(implClassName, sourceWriter, treeLogger, targets);
    }

    return new RebindResult(RebindMode.USE_ALL_NEW, composer.getCreatedClassName());
  }

  @Override
  public long getVersionId() {
    return 1;
  }

  private void genClass(String implClassName, SourceWriter sourceWriter, TreeLogger logger, Map<String, Map.Entry<String, String>> allInstantiableClasses) {
    sourceWriter.println("private java.util.Map<String, Class> myStateClassMap = new java.util.HashMap<>();");
    sourceWriter.println("public " + implClassName + "( ) {");
    for (Map.Entry<String, Map.Entry<String, String>> entry : allInstantiableClasses.entrySet()) {
      sourceWriter.indent();

      sourceWriter.println("myStateClassMap.put(\"" + entry.getKey() + "\", " + entry.getValue().getValue() + ".class);");
    }
    sourceWriter.println("}");
    sourceWriter.println();
    sourceWriter.println("public Class<?> getStateType(String type) { return myStateClassMap.get(type); }");

    genMethod(allInstantiableClasses, sourceWriter);

    sourceWriter.commit(logger);
  }

  private void genMethod(Map<String, Map.Entry<String, String>> targets, SourceWriter sourceWriter) {
    sourceWriter.println("public InternalGwtComponent create(String type) {");
    sourceWriter.indent();
    sourceWriter.println("if (type == null) {");
    sourceWriter.indent();
    sourceWriter.println("return null;");
    sourceWriter.outdent();
    sourceWriter.println("}");
    sourceWriter.outdent();

    for (Map.Entry<String, Map.Entry<String, String>> entry : targets.entrySet()) {
      sourceWriter.indent();
      sourceWriter.println("else if (type.equals(\"" + entry.getKey() + "\")) {");
      sourceWriter.indent();
      sourceWriter.println("return new " + entry.getValue().getKey() + "( );");
      sourceWriter.outdent();
      sourceWriter.println("}");
      sourceWriter.outdent();
    }
    sourceWriter.indent();
    sourceWriter.println("return null;");
    sourceWriter.outdent();
    sourceWriter.println("}");
    sourceWriter.outdent();
  }
}
