/*
 * Copyright 2013-2022 consulo.io
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
import consulo.annotation.component.*;
import jakarta.inject.Inject;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author VISTALL
 * @see https://github.com/google/auto/blob/master/service/processor/src/main/java/com/google/auto/service/processor/AutoServiceProcessor.java
 * @since 16-Jun-22
 */
@SupportedAnnotationTypes({InjectingBindingProcessor.SERVICE_IMPL, InjectingBindingProcessor.EXTENSION_IMPL, InjectingBindingProcessor.TOPIC_IMPL})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class InjectingBindingProcessor extends AbstractProcessor {
  private static record AnnotationResolveInfo(Annotation annotation, TypeElement typeElement) {
  }

  public static final String SERVICE_IMPL = "consulo.annotation.component.ServiceImpl";
  public static final String EXTENSION_IMPL = "consulo.annotation.component.ExtensionImpl";
  public static final String TOPIC_IMPL = "consulo.annotation.component.TopicImpl";

  private Map<String, Class<? extends Annotation>> myApiAnnotations = new HashMap<>();

  public InjectingBindingProcessor() {
    myApiAnnotations.put(SERVICE_IMPL, Service.class);
    myApiAnnotations.put(EXTENSION_IMPL, Extension.class);
    myApiAnnotations.put(TOPIC_IMPL, Topic.class);
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    AnnotationSpec suppressWarning = AnnotationSpec.builder(SuppressWarnings.class).addMember("value", CodeBlock.of("$S", "ALL")).build();

    Filer filer = processingEnv.getFiler();

    Map<String, Set<String>> providers = new HashMap<>();

    String injectingBindingClassName = "consulo.component.bind.InjectingBinding";
    ClassName injectingBindingClass = ClassName.bestGuess(injectingBindingClassName);

    for (TypeElement annotation : annotations) {
      Set<? extends Element> elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(annotation);

      Class<? extends Annotation> apiClass = myApiAnnotations.get(annotation.getQualifiedName().toString());
      if (apiClass == null) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@" + annotation.getQualifiedName() + " not supported");
      }

      for (Element element : elementsAnnotatedWith) {
        if (!(element instanceof TypeElement)) {
          continue;
        }

        TypeElement typeElement = (TypeElement)element;
        AnnotationResolveInfo apiInfo = findAnnotationInSuper(typeElement, apiClass);
        if (apiInfo == null) {
          processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Can't find @" + apiClass + " annotation for: " + typeElement.getQualifiedName(), typeElement);
          return false;
        }

        try {
          String bindingQualifiedName = typeElement.getQualifiedName() + "_Binding";
          JavaFileObject bindingObject = filer.createSourceFile(bindingQualifiedName);

          providers.computeIfAbsent(injectingBindingClassName, (c) -> new HashSet<>()).add(bindingQualifiedName);

          TypeSpec.Builder bindBuilder = TypeSpec.classBuilder(typeElement.getSimpleName().toString() + "_Binding");
          bindBuilder.addModifiers(Modifier.PUBLIC, Modifier.FINAL);
          bindBuilder.addAnnotation(suppressWarning);
          bindBuilder.addSuperinterface(injectingBindingClass);

          bindBuilder.addMethod(MethodSpec.methodBuilder("getApiClassName").returns(String.class).addModifiers(Modifier.PUBLIC)
                                        .addCode(CodeBlock.of("return $S;", apiInfo.typeElement().getQualifiedName().toString())).build());
          bindBuilder.addMethod(MethodSpec.methodBuilder("getApiClass").returns(Class.class).addModifiers(Modifier.PUBLIC)
                                        .addCode(CodeBlock.of("return $T.class;", ClassName.bestGuess(apiInfo.typeElement().getQualifiedName().toString()))).build());

          bindBuilder.addMethod(
                  MethodSpec.methodBuilder("getImplClassName").returns(String.class).addModifiers(Modifier.PUBLIC).addCode(CodeBlock.of("return $S;", typeElement.getQualifiedName().toString()))
                          .build());
          bindBuilder.addMethod(MethodSpec.methodBuilder("getImplClass").returns(Class.class).addModifiers(Modifier.PUBLIC).addCode(CodeBlock.of("return $T.class;", typeElement)).build());

          bindBuilder.addMethod(MethodSpec.methodBuilder("getComponentAnnotationClass").addModifiers(Modifier.PUBLIC).returns(Class.class)
                                        .addCode(CodeBlock.of("return $T.class;", apiInfo.annotation().annotationType())).build());

          ComponentScope scope;
          // use TopicImpl scope
          if (apiInfo.annotation() instanceof Topic) {
            TopicImpl topicImpl = typeElement.getAnnotation(TopicImpl.class);
            scope = topicImpl.value();
          } else {
            scope = getScope(apiInfo.annotation());
          }
          
          bindBuilder.addMethod(
                  MethodSpec.methodBuilder("getComponentScope").addModifiers(Modifier.PUBLIC).returns(ComponentScope.class).addCode(CodeBlock.of("return $T.$L;", ComponentScope.class, scope.name()))
                          .build());

          if (!isLazy(apiInfo.annotation())) {
            bindBuilder.addMethod(MethodSpec.methodBuilder("isLazy").addModifiers(Modifier.PUBLIC).returns(boolean.class).addCode(CodeBlock.of("return false;")).build());
          }

          List<? extends VariableElement> injectParameters = null;

          List<? extends Element> allMembers = processingEnv.getElementUtils().getAllMembers(typeElement);
          for (Element member : allMembers) {
            if (member instanceof ExecutableElement) {
              Name simpleName = member.getSimpleName();
              if ("<init>".equals(simpleName.toString())) {
                List<? extends VariableElement> parameters = ((ExecutableElement)member).getParameters();

                Inject injectAnnotation = member.getAnnotation(Inject.class);
                if (injectAnnotation != null) {
                  injectParameters = parameters;
                  break;
                }
              }
            }
          }

          if (injectParameters == null) {
            for (Element member : allMembers) {
              if (member instanceof ExecutableElement) {
                Name simpleName = member.getSimpleName();
                if ("<init>".equals(simpleName.toString())) {
                  List<? extends VariableElement> parameters = ((ExecutableElement)member).getParameters();

                  // default constructor
                  if (parameters.size() == 0 && member.getModifiers().contains(Modifier.PUBLIC)) {
                    injectParameters = parameters;
                    break;
                  }
                }
              }
            }
          }

          if (injectParameters == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "There no public constructor or constructor with @Inject annotation. Injecting impossible", typeElement);
            return false;
          }

          // TODO stub
          bindBuilder.addMethod(MethodSpec.methodBuilder("getComponentProfiles").addModifiers(Modifier.PUBLIC).returns(String[].class).addCode(CodeBlock.of("return new String[0];")).build());

          bindBuilder.addMethod(MethodSpec.methodBuilder("getParametersCount").addModifiers(Modifier.PUBLIC).returns(int.class).addCode(CodeBlock.of("return $L;", injectParameters.size())).build());

          List<TypeName> paramTypes = new ArrayList<>();

          if (!injectParameters.isEmpty()) {
            for (VariableElement parameter : injectParameters) {
              paramTypes.add(toTypeName(parameter.asType()));
            }

            List<TypeName> paramTypesForMethod = new ArrayList<>();
            // array creation
            paramTypesForMethod.add(TypeName.get(Type.class));

            StringBuilder paramTypesBuilder = new StringBuilder();
            paramTypesBuilder.append("return new $T[] {");
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
                paramTypesBuilder.append("new $T(");
                paramTypesForMethod.add(ClassName.bestGuess("consulo.component.bind.ParameterizedTypeImpl"));
                paramTypesBuilder.append("$T.class, ");
                paramTypesForMethod.add(parType.rawType);

                for (int j = 0; j < parType.typeArguments.size(); j++) {
                  if (j != 0) {
                    paramTypesBuilder.append(", ");
                  }

                  paramTypesBuilder.append("$T.class");
                  paramTypesForMethod.add(parType.typeArguments.get(j));
                }
                paramTypesBuilder.append(")");
              }
            }
            paramTypesBuilder.append("};");

            bindBuilder.addMethod(
                    MethodSpec.methodBuilder("getParameterTypes").addModifiers(Modifier.PUBLIC).returns(Type[].class).addCode(CodeBlock.of(paramTypesBuilder.toString(), paramTypesForMethod.toArray()))
                            .build());
          }
          else {
            bindBuilder.addMethod(MethodSpec.methodBuilder("getParameterTypes").addModifiers(Modifier.PUBLIC).returns(Type[].class).addCode(CodeBlock.of("return EMPTY_TYPES;")).build());
          }

          List<TypeName> argsTypes = new ArrayList<>();
          argsTypes.add(toTypeName(typeElement));
          argsTypes.addAll(paramTypes);

          StringBuilder newCreationBuilder = new StringBuilder();
          newCreationBuilder.append("return new $T(");
          for (int i = 0; i < injectParameters.size(); i++) {
            if (i != 0) {
              newCreationBuilder.append(", ");
            }
            newCreationBuilder.append("($T) args[").append(i).append("]");
          }

          newCreationBuilder.append(");");

          bindBuilder.addMethod(MethodSpec.methodBuilder("create").addParameter(Object[].class, "args").addModifiers(Modifier.PUBLIC).returns(toTypeName(typeElement))
                                        .addCode(CodeBlock.of(newCreationBuilder.toString(), argsTypes.toArray())).build());

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

  private void generateConfigFiles(Map<String, Set<String>> providers) {
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

  private void log(String msg) {
    if (processingEnv.getOptions().containsKey("debug")) {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
    }
  }

  private void warning(String msg, Element element, AnnotationMirror annotation) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, msg, element, annotation);
  }

  private void error(String msg, Element element, AnnotationMirror annotation) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, element, annotation);
  }

  private void fatalError(String msg) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "FATAL ERROR: " + msg);
  }

  private TypeName toTypeName(TypeMirror typeMirror) {
    return TypeName.get(typeMirror);
  }

  private TypeName toTypeName(TypeElement typeElement) {
    PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(typeElement);

    return ClassName.get(packageElement.getQualifiedName().toString(), typeElement.getSimpleName().toString());
  }

  private static ComponentScope getScope(Annotation annotation) {
    if (annotation instanceof Service) {
      return ((Service)annotation).value();
    }

    if (annotation instanceof Extension) {
      return ((Extension)annotation).value();
    }

    throw new UnsupportedOperationException(annotation.getClass().getName());
  }

  private static boolean isLazy(Annotation annotation) {
    if (annotation instanceof Service) {
      return ((Service)annotation).lazy();
    }
    return true;
  }

  private static <T extends Annotation> AnnotationResolveInfo findAnnotationInSuper(TypeElement typeElement, Class<T> annotationClass) {
    T annotation = typeElement.getAnnotation(annotationClass);
    if (annotation != null) {
      return new AnnotationResolveInfo(annotation, typeElement);
    }

    TypeMirror superclass = typeElement.getSuperclass();
    if (superclass != null) {
      if (superclass instanceof DeclaredType) {
        AnnotationResolveInfo inSuper = findAnnotationInSuper((TypeElement)((DeclaredType)superclass).asElement(), annotationClass);
        if (inSuper != null) {
          return inSuper;
        }
      }
    }

    for (TypeMirror typeMirror : typeElement.getInterfaces()) {
      if (typeMirror instanceof DeclaredType) {
        AnnotationResolveInfo inSuper = findAnnotationInSuper((TypeElement)((DeclaredType)typeMirror).asElement(), annotationClass);
        if (inSuper != null) {
          return inSuper;
        }
      }
    }
    return null;
  }
}
