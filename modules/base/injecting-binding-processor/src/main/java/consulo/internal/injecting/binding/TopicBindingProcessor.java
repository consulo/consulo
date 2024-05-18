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

import com.squareup.javapoet.*;

import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * @author VISTALL
 * @since 25/01/2023
 */
@SupportedAnnotationTypes({TopicBindingProcessor.TOPIC_API})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class TopicBindingProcessor extends BindingProcessor {
  public static final String TOPIC_API = "consulo.annotation.component.TopicAPI";

  private record TopicMethodInfo(String name, AppendTypeResult types) {
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (annotations.isEmpty()) {
      return true;
    }

    AnnotationSpec suppressWarning = AnnotationSpec.builder(SuppressWarnings.class).addMember("value", CodeBlock.of("$S", "ALL")).build();

    Filer filer = processingEnv.getFiler();

    Map<String, Set<String>> providers = new HashMap<>();

    String topicBindingClassName = "consulo.component.bind.TopicBinding";
    ClassName topicBindingClass = ClassName.bestGuess(topicBindingClassName);

    ClassName topicMethod = ClassName.bestGuess("consulo.component.bind.TopicMethod");

    for (TypeElement annotation : annotations) {
      Set<? extends Element> elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(annotation);

      for (Element element : elementsAnnotatedWith) {
        if (!(element instanceof TypeElement)) {
          continue;
        }

        TypeElement typeElement = (TypeElement)element;

        if (typeElement.getKind() != ElementKind.INTERFACE) {
          error(typeElement.getQualifiedName() + " must be interface", typeElement, null);
          return false;
        }

        try {
          String bindingQualifiedName = typeElement.getQualifiedName() + "_Binding";
          JavaFileObject bindingObject = filer.createSourceFile(bindingQualifiedName);

          providers.computeIfAbsent(topicBindingClassName, (c) -> new HashSet<>()).add(bindingQualifiedName);

          TypeName topicClassRef = TypeName.get(typeElement.asType());

          TypeSpec.Builder bindBuilder = TypeSpec.classBuilder(typeElement.getSimpleName().toString() + "_Binding");
          bindBuilder.addModifiers(Modifier.PUBLIC, Modifier.FINAL);
          bindBuilder.addAnnotation(suppressWarning);
          bindBuilder.addSuperinterface(topicBindingClass);

          List<TopicMethodInfo> methods = new ArrayList<>();
          List<? extends Element> allMembers = processingEnv.getElementUtils().getAllMembers(typeElement);
          for (Element member : allMembers) {
            if (member instanceof ExecutableElement executableElement) {
              Element enclosingElement = executableElement.getEnclosingElement();
              if (enclosingElement != element) {
                continue;
              }

              TypeMirror returnType = executableElement.getReturnType();
              if (!isVoid(returnType)) {
                error(typeElement.getQualifiedName() + "." + executableElement.getSimpleName() + " must return void", typeElement, null);
                return false;
              }

              List<TypeName> paramTypes = new ArrayList<>();

              AppendTypeResult types = appendTypes(executableElement.getParameters(), paramTypes, "", "", false);

              methods.add(new TopicMethodInfo(member.getSimpleName().toString(), types));
            }
          }

          if (methods.isEmpty()) {
            error(typeElement.getQualifiedName() + " no methods for call", typeElement, null);
            return false;
          }

          bindBuilder.addMethod(
                  MethodSpec.methodBuilder("getApiClassName").returns(String.class).addModifiers(Modifier.PUBLIC).addCode(CodeBlock.of("return $S;", typeElement.getQualifiedName().toString()))
                          .build());

          List<Object> methodsArgs = new ArrayList<>();
          methodsArgs.add(ArrayTypeName.of(topicMethod));

          StringBuilder methodsBuilder = new StringBuilder();
          methodsBuilder.append("return new $T {\n");

          for (int i = 0; i < methods.size(); i++) {
            if (i != 0) {
              methodsBuilder.append(",\n");
            }

            TopicMethodInfo topicMethodInfo = methods.get(i);

            methodsBuilder.append("$T.create($S, ");
            methodsArgs.add(topicMethod);
            methodsArgs.add(topicMethodInfo.name());

            AppendTypeResult types = topicMethodInfo.types();
            if (types.argsCount() == 0) {
              methodsBuilder.append("EMPTY_TYPES");
            }
            else {
              methodsBuilder.append(types.result());
              methodsArgs.addAll(types.types());
            }

            methodsBuilder.append(", (o, args) -> (($T) o).$L(");

            methodsArgs.add(topicClassRef);
            methodsArgs.add(topicMethodInfo.name());

            for (int a = 0; a < types.argsCount(); a++) {
              if (a != 0) {
                methodsBuilder.append(", ");
              }

              methodsBuilder.append("($T) args[").append(a).append("]");
              methodsArgs.add(types.args().get(a));
            }

            methodsBuilder.append(")");

            methodsBuilder.append(")");
          }

          methodsBuilder.append("\n};");

          bindBuilder.addMethod(
                  MethodSpec.methodBuilder("methods").addModifiers(Modifier.PUBLIC).returns(ArrayTypeName.of(topicMethod)).addCode(CodeBlock.of(methodsBuilder.toString(), methodsArgs.toArray()))
                          .build());

          TypeSpec bindClass = bindBuilder.build();

          PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(typeElement);

          JavaFile javaFile = JavaFile.builder(packageElement.getQualifiedName().toString(), bindClass).build();

          try (Writer writer = bindingObject.openWriter()) {
            javaFile.writeTo(writer);
          }
        }
        catch (IOException e) {
          processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), typeElement);
        }
      }
    }

    generateConfigFiles(providers);

    return true;
  }

  private static boolean isVoid(TypeMirror mirror) {
    if (mirror instanceof NoType) {
      return mirror.getKind() == TypeKind.VOID;
    }
    return false;
  }
}
