package io.insight.restrpc;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.compiler.PluginProtos;
import com.squareup.javapoet.*;
import io.insight.utils.StringUtils;

import javax.annotation.Generated;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.List;

public class ProtoGen {
  public static void main(String[] args) {
    try {
      PluginProtos.CodeGeneratorRequest codeGeneratorRequest =
          PluginProtos.CodeGeneratorRequest.parseFrom(System.in);
      PluginProtos.CodeGeneratorResponse.Builder response = PluginProtos.CodeGeneratorResponse.newBuilder();
      for (DescriptorProtos.FileDescriptorProto file : codeGeneratorRequest.getProtoFileList()) {
        generateFile(file, response);
      }
      response.build().writeTo(System.out);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void generateFile(DescriptorProtos.FileDescriptorProto file,
                                   PluginProtos.CodeGeneratorResponse.Builder response) throws IOException, ClassNotFoundException {
    String packageName = getPackage(file);

    for (DescriptorProtos.ServiceDescriptorProto service : file.getServiceList()) {
      PluginProtos.CodeGeneratorResponse.File.Builder generateFile = PluginProtos.CodeGeneratorResponse.File.newBuilder();
      String className = service.getName() + "RestRpc";
      TypeSpec.Builder serviceClassBuilder = TypeSpec.classBuilder(className)
          .addModifiers(Modifier.PUBLIC);
      serviceClassBuilder.addAnnotation(AnnotationSpec.builder(Generated.class)
          .addMember("value", "$S", "by rest-rpc proto compiler")
          .addMember("comments", "$S", "Source: " + file.getName())
          .build());

      serviceClassBuilder.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());

      String serviceName = file.getPackage() + "." + service.getName();
      serviceClassBuilder.addField(FieldSpec.builder(String.class, "SERVICE_NAME",
          Modifier.STATIC, Modifier.PUBLIC, Modifier.FINAL)
          .initializer("$S", serviceName)
          .build());

      addMethodFields(packageName, service, serviceClassBuilder, serviceName);

      buildSupplierClasses(file, service, serviceClassBuilder);

      buildServiceClasses(file, service, serviceClassBuilder);


      JavaFile javaFile = JavaFile.builder(packageName, serviceClassBuilder.build()).build();
      StringBuffer content = new StringBuffer();
      javaFile.writeTo(content);
      generateFile.setContent(content.toString());
      generateFile.setName(javaFile.toJavaFileObject().getName());
      response.addFile(generateFile);
    }

  }

  private static void addMethodFields(String packageName,
                                      DescriptorProtos.ServiceDescriptorProto service,
                                      TypeSpec.Builder serviceClassBuilder,
                                      String serviceName) {
    List<DescriptorProtos.MethodDescriptorProto> methodList = service.getMethodList();
    for (int i = 0; i < methodList.size(); i++) {
      DescriptorProtos.MethodDescriptorProto method = methodList.get(i);
      String snakeMethodName = "METHOD_" + StringUtils.camel2snake(method.getName()).toUpperCase();
      String snakeMethodId = "METHODID_" + StringUtils.camel2snake(method.getName()).toUpperCase();
      ClassName inputType = ClassName.get(packageName, getProtoTypeClass(method.getInputType()));
      ClassName outputType = ClassName.get(packageName, getProtoTypeClass(method.getOutputType()));
      ParameterizedTypeName methodFieldType = ParameterizedTypeName.get(
          ClassName.get(io.grpc.MethodDescriptor.class),
          inputType,
          outputType);
      serviceClassBuilder.addField(FieldSpec.builder(methodFieldType, snakeMethodName,
          Modifier.STATIC, Modifier.PUBLIC, Modifier.FINAL)
          .initializer(CodeBlock.builder()
              .add("io.grpc.MethodDescriptor.<$T, $T>newBuilder()\n" +
                      "              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)\n" +
                      "              .setFullMethodName(io.grpc.MethodDescriptor.generateFullMethodName($S , $S))\n" +
                      "              .setSampledToLocalTracing(true)\n" +
                      "              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(\n" +
                      "                  $T.getDefaultInstance()))\n" +
                      "              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(\n" +
                      "                 $T.getDefaultInstance()))\n" +
                      "                  .setSchemaDescriptor(new $L($S))\n" +
                      "                  .build()",
                  inputType, outputType, serviceName, method.getName(),
                  inputType, outputType, service.getName() + "MethodDescriptorSupplier", method.getName()
              )
              .build())
          .build());

      serviceClassBuilder.addField(FieldSpec.builder(TypeName.INT, snakeMethodId,
          Modifier.STATIC, Modifier.PUBLIC, Modifier.FINAL).initializer("$L",  i).build());
    }
  }

  private static void buildServiceClasses(DescriptorProtos.FileDescriptorProto file,
                                          DescriptorProtos.ServiceDescriptorProto service,
                                          TypeSpec.Builder serviceClassBuilder) {
    String packageName = getPackage(file);
    String serviceClassName = StringUtils.toCamelCase(service.getName() + "ImplBase", true);
    TypeSpec.Builder serviceImplBuilder = TypeSpec.classBuilder(serviceClassName)
        .addSuperinterface(BindableService.class)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.ABSTRACT);

    MethodSpec.Builder bindMethod = MethodSpec.methodBuilder("bindService")
        .returns(ServiceDefinition.class)
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    bindMethod.addStatement("$T builder = $T.builder($L)", ServiceDefinition.Builder.class,
        ServiceDefinition.class, "SERVICE_NAME");

    for (DescriptorProtos.MethodDescriptorProto method : service.getMethodList()) {
      String methodName = StringUtils.toCamelCase(method.getName(), false);
      ClassName inputType = ClassName.get(packageName, getProtoTypeClass(method.getInputType()));
      ClassName outputType = ClassName.get(packageName, getProtoTypeClass(method.getOutputType()));
      serviceImplBuilder.addMethod(MethodSpec.methodBuilder(methodName)
          .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
          .addParameter(inputType, "request")
          .addParameter(ParameterizedTypeName.get(ClassName.get(Context.class),outputType), "context")
          .build());

      String snakeMethodName = "METHOD_" + StringUtils.camel2snake(method.getName()).toUpperCase();
      bindMethod.addStatement("builder.addMethod($L, (req, ctx) -> this.$L(req, ctx))",
          snakeMethodName,
           methodName);
    }
    bindMethod.addStatement("return builder.build()");
    serviceImplBuilder.addMethod(bindMethod.build());
    serviceClassBuilder.addType(serviceImplBuilder.build());
  }

  private static void buildSupplierClasses(DescriptorProtos.FileDescriptorProto file,
                                           DescriptorProtos.ServiceDescriptorProto service,
                                           TypeSpec.Builder serviceClassBuilder) {
    String packageName = getPackage(file);
    String serviceBaseSupplierName = service.getName() + "BaseDescriptorSupplier";
    TypeSpec.Builder serviceBaseSupplier = TypeSpec.classBuilder(serviceBaseSupplierName)
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.ABSTRACT)
        .addSuperinterface(io.grpc.protobuf.ProtoFileDescriptorSupplier.class)
        .addSuperinterface(io.grpc.protobuf.ProtoServiceDescriptorSupplier.class);
    serviceBaseSupplier.addMethod(MethodSpec.methodBuilder("getFileDescriptor")
        .addAnnotation(ClassName.get(Override.class))
        .addModifiers(Modifier.PUBLIC)
        .returns(Descriptors.FileDescriptor.class)
        .addStatement("return $T.getDescriptor()", ClassName.get(packageName, getOuterClassName(file)))
        .build());
    serviceBaseSupplier.addMethod(MethodSpec.methodBuilder("getServiceDescriptor")
        .addAnnotation(ClassName.get(Override.class))
        .addModifiers(Modifier.PUBLIC)
        .returns(Descriptors.ServiceDescriptor.class)
        .addStatement("return getFileDescriptor().findServiceByName($S)", service.getName())
        .build());
    serviceClassBuilder.addType(serviceBaseSupplier.build());
    TypeSpec.Builder serviceMethodSupplier = TypeSpec.classBuilder(service.getName() + "MethodDescriptorSupplier")
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
        .superclass(ClassName.bestGuess(serviceBaseSupplierName))
        .addSuperinterface(io.grpc.protobuf.ProtoMethodDescriptorSupplier.class);
    serviceMethodSupplier.addField(String.class, "methodName", Modifier.PRIVATE, Modifier.FINAL);
    serviceMethodSupplier.addMethod(MethodSpec.constructorBuilder()
        .addParameter(String.class, "methodName")
        .addStatement("this.methodName = methodName")
        .build());
    serviceMethodSupplier.addMethod(MethodSpec.methodBuilder("getMethodDescriptor")
        .addModifiers(Modifier.PUBLIC)
        .returns(Descriptors.MethodDescriptor.class)
        .addStatement("return getServiceDescriptor().findMethodByName(methodName)").build());
    serviceClassBuilder.addType(serviceMethodSupplier.build());
  }

  private static String getOuterClassName(DescriptorProtos.FileDescriptorProto file) {
    if (file.getOptions().hasJavaOuterClassname()) {
      return file.getOptions().getJavaOuterClassname();
    } else {
      String name[] = file.getName().split("\\.");
      return StringUtils.toCamelCase(name[0], true);
    }
  }

  private static String getPackage(DescriptorProtos.FileDescriptorProto file) {
    String packageName = file.getPackage();
    if (file.getOptions().hasJavaPackage()) {
      packageName = file.getOptions().getJavaPackage();
    }
    return packageName;
  }

  private static String getProtoTypeClass(String type) {
    String[] names = type.split("\\.");
    return names[names.length - 1];
  }

}
