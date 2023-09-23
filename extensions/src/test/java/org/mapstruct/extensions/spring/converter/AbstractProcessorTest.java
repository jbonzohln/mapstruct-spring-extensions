package org.mapstruct.extensions.spring.converter;

import static com.google.common.collect.Iterables.concat;
import static java.nio.file.Files.createTempDirectory;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static javax.lang.model.element.Modifier.*;
import static javax.tools.StandardLocation.CLASS_OUTPUT;

import com.squareup.javapoet.*;
import java.io.IOException;
import java.util.Set;
import javax.annotation.processing.Processor;
import javax.tools.*;

class AbstractProcessorTest {
  protected static final String PACKAGE_NAME = "test";
  protected static final String CAR_SIMPLE_NAME = "Car";
  protected static final String CAR_DTO_SIMPLE_NAME = "CarDto";
  protected static final ClassName CAR_CLASS_NAME = ClassName.get(PACKAGE_NAME, CAR_SIMPLE_NAME);
  protected static final ClassName CAR_DTO_CLASS_NAME =
      ClassName.get(PACKAGE_NAME, CAR_DTO_SIMPLE_NAME);
  private static final JavaFileObject CAR_JAVA_FILE_OBJECT =
      toJavaFileObject(
          JavaFile.builder(PACKAGE_NAME, buildSimpleModelClassTypeSpec(CAR_SIMPLE_NAME)));
  private static final JavaFileObject CAR_DTO_JAVA_FILE_OBJECT =
      toJavaFileObject(
          JavaFile.builder(PACKAGE_NAME, buildSimpleModelClassTypeSpec(CAR_DTO_SIMPLE_NAME)));
  private static final JavaFileObject GENERATED_JAVA_FILE_OBJECT =
      toJavaFileObject(JavaFile.builder("javax.annotation", buildGeneratedAnnotationTypeSpec()));
  private static final JavaFileObject COMPONENT_JAVA_FILE_OBJECT =
      toJavaFileObject(
          JavaFile.builder(
              "org.springframework.stereotype", buildSimpleAnnotationTypeSpec("Component")));
  private static final JavaFileObject LAZY_JAVA_FILE_OBJECT =
      toJavaFileObject(
          JavaFile.builder(
              "org.springframework.context.annotation", buildSimpleAnnotationTypeSpec("Lazy")));

  protected static JavaFileObject toJavaFileObject(JavaFile.Builder fileBuilder) {
    return fileBuilder.skipJavaLangImports(true).build().toJavaFileObject();
  }

  protected static final Set<JavaFileObject> COMMON_COMPILATION_UNITS =
      Set.of(
          CAR_JAVA_FILE_OBJECT,
          CAR_DTO_JAVA_FILE_OBJECT,
          GENERATED_JAVA_FILE_OBJECT,
          COMPONENT_JAVA_FILE_OBJECT,
          LAZY_JAVA_FILE_OBJECT);

  protected static boolean compile(
      final Processor processor, final JavaFileObject... additionalCompilationUnits)
      throws IOException {
    return compile(processor, concat(COMMON_COMPILATION_UNITS, asList(additionalCompilationUnits)));
  }

  protected static boolean compile(
      final Processor processor, final Iterable<JavaFileObject> compilationUnits)
      throws IOException {
    final var compiler = ToolProvider.getSystemJavaCompiler();

    final var diagnostics = new DiagnosticCollector<JavaFileObject>();
    final var fileManager = compiler.getStandardFileManager(diagnostics, null, null);
    fileManager.setLocation(CLASS_OUTPUT, singletonList(createTempDirectory("classes").toFile()));

    final var task = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits);
    task.setProcessors(singletonList(processor));

    final var success = task.call();
    diagnostics.getDiagnostics().forEach(System.err::println);
    return success;
  }

  private static TypeSpec buildSimpleModelClassTypeSpec(final String className) {
    final FieldSpec makeField = FieldSpec.builder(String.class, "make", PRIVATE).build();
    final ParameterSpec makeParameter = ParameterSpec.builder(String.class, "make", FINAL).build();
    return TypeSpec.classBuilder(className)
        .addModifiers(PUBLIC)
        .addField(makeField)
        .addMethod(
            MethodSpec.methodBuilder("getMake")
                .returns(String.class)
                .addStatement("return $N", makeField)
                .build())
        .addMethod(
            MethodSpec.methodBuilder("setMake")
                .addParameter(makeParameter)
                .addStatement("this.$N = $N", makeField, makeParameter)
                .build())
        .build();
  }

  protected static TypeSpec buildGeneratedAnnotationTypeSpec() {
    return TypeSpec.annotationBuilder("Generated")
        .addModifiers(PUBLIC)
        .addMethod(
            MethodSpec.methodBuilder("value")
                .returns(String.class)
                .addModifiers(PUBLIC, ABSTRACT)
                .build())
        .addMethod(
            MethodSpec.methodBuilder("date")
                .returns(String.class)
                .addModifiers(PUBLIC, ABSTRACT)
                .build())
        .build();
  }

  private static TypeSpec buildSimpleAnnotationTypeSpec(final String annotationName) {
    return TypeSpec.annotationBuilder(annotationName).addModifiers(PUBLIC).build();
  }
}
