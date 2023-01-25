/*
 * Copyright 2013-2023 consulo.io
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
package consulo.internal.injecting.binding;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author VISTALL
 * @since 25/01/2023
 */
public abstract class BindingProcessor extends AbstractProcessor {
  protected record AppendTypeResult(String result, List<TypeName> types, List<TypeName> args, int argsCount) {
  }

  protected AppendTypeResult appendTypes(List<? extends VariableElement> args, List<TypeName> paramTypes, String start, String end, boolean withParamTypes) {
    List<TypeName> argsTypes = new ArrayList<>();

    for (VariableElement parameter : args) {
      TypeName argType = toTypeName(parameter.asType());

      paramTypes.add(argType);
      argsTypes.add(argType);
    }


    List<TypeName> paramTypesForMethod = new ArrayList<>();
    // array creation
    paramTypesForMethod.add(TypeName.get(Type.class));

    StringBuilder paramTypesBuilder = new StringBuilder();
    paramTypesBuilder.append(start);
    paramTypesBuilder.append("new $T[] {");

    for (int i = 0; i < paramTypes.size(); i++) {
      if (i != 0) {
        paramTypesBuilder.append(", ");
      }

      TypeName injectType = paramTypes.get(i);
      // simple type
      if (injectType instanceof ClassName) {
        paramTypesBuilder.append("$T.class");
        paramTypesForMethod.add(injectType);
      }
      else if (injectType instanceof ParameterizedTypeName parType) {
        if (withParamTypes) {
          paramTypesBuilder.append("new $T(");
          paramTypesForMethod.add(ClassName.bestGuess("consulo.component.bind.ParameterizedTypeImpl"));
          paramTypesBuilder.append("$T.class, ");
          paramTypesForMethod.add(parType.rawType);

          for (int j = 0; j < parType.typeArguments.size(); j++) {
            if (j != 0) {
              paramTypesBuilder.append(", ");
            }

            paramTypesBuilder.append("$T.class");
            TypeName typeArgType = parType.typeArguments.get(j);
            if (typeArgType instanceof WildcardTypeName) {
              paramTypesForMethod.add(TypeName.OBJECT);
            }
            else {
              paramTypesForMethod.add(typeArgType);
            }
          }
          paramTypesBuilder.append(")");
        }
        else {
          paramTypesBuilder.append("$T.class");
          paramTypesForMethod.add(parType.rawType);
        }
      } else {
        paramTypesBuilder.append("$T.class");
        paramTypesForMethod.add(injectType);
      }
    }
    paramTypesBuilder.append("}");
    paramTypesBuilder.append(end);
    return new AppendTypeResult(paramTypesBuilder.toString(), paramTypesForMethod, argsTypes, args.size());
  }

  protected void log(String msg) {
    if (processingEnv.getOptions().containsKey("debug")) {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
    }
  }

  protected void warning(String msg, Element element, AnnotationMirror annotation) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, msg, element, annotation);
  }

  protected void error(String msg, Element element, AnnotationMirror annotation) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, element, annotation);
  }

  protected void fatalError(String msg) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "FATAL ERROR: " + msg);
  }

  protected TypeName toTypeName(TypeMirror typeMirror) {
    return TypeName.get(typeMirror);
  }

  protected TypeName toTypeName(TypeElement typeElement) {
    PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(typeElement);

    return ClassName.get(packageElement.getQualifiedName().toString(), typeElement.getSimpleName().toString());
  }

  protected void generateConfigFiles(Map<String, Set<String>> providers) {
    Filer filer = processingEnv.getFiler();

    for (String providerInterface : providers.keySet()) {
      String resourceFile = "META-INF/services/" + providerInterface;
      log("Working on resource file: " + resourceFile);
      try {
        SortedSet<String> allServices = new TreeSet<>();
        try {
          // would like to be able to print the full path
          // before we attempt to get the resource in case the behavior
          // of filer.getResource does change to match the spec, but there's
          // no good way to resolve CLASS_OUTPUT without first getting a resource.
          FileObject existingFile = filer.getResource(StandardLocation.CLASS_OUTPUT, "", resourceFile);
          log("Looking for existing resource file at " + existingFile.toUri());
          Set<String> oldServices = ServicesFiles.readServiceFile(existingFile.openInputStream());
          log("Existing service entries: " + oldServices);
          allServices.addAll(oldServices);
        }
        catch (IOException e) {
          // According to the javadoc, Filer.getResource throws an exception
          // if the file doesn't already exist.  In practice this doesn't
          // appear to be the case.  Filer.getResource will happily return a
          // FileObject that refers to a non-existent file but will throw
          // IOException if you try to open an input stream for it.
          log("Resource file did not already exist.");
        }

        Set<String> newServices = new HashSet<>(providers.get(providerInterface));
        if (!allServices.addAll(newServices)) {
          log("No new service entries being added.");
          continue;
        }

        log("New service file contents: " + allServices);
        FileObject fileObject = filer.createResource(StandardLocation.CLASS_OUTPUT, "", resourceFile);
        try (OutputStream out = fileObject.openOutputStream()) {
          ServicesFiles.writeServiceFile(allServices, out);
        }
        log("Wrote to: " + fileObject.toUri());
      }
      catch (IOException e) {
        fatalError("Unable to create " + resourceFile + ", " + e);
        return;
      }
    }
  }
}
