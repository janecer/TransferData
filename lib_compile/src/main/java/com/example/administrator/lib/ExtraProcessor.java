package com.example.administrator.lib;

import com.example.administrator.lib.Util.BundleGetBuilder;
import com.example.administrator.lib.Util.BundleSaveBuilder;
import com.example.administrator.lib.Util.Consts;
import com.example.administrator.lib.Util.Utils;
import com.example.admistrator.Extra;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import static javax.lang.model.element.Modifier.PUBLIC;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes({"com.example.admistrator.Extra"})
public class ExtraProcessor extends AbstractProcessor {

    private Map<TypeElement,List<Element>> parentAndChilds = new HashMap<>();
    private Elements elementsUtil ;
    private Types typeUtil ;
    private Filer filer ;
    private Messager log;

    BundleGetBuilder mBundleGetBuilder ;
    BundleSaveBuilder mBundleSaveBuilder ;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        elementsUtil = processingEnvironment.getElementUtils() ;
        typeUtil = processingEnvironment.getTypeUtils() ;
        filer = processingEnvironment.getFiler() ;
        log = processingEnvironment.getMessager() ;
        mBundleGetBuilder = new BundleGetBuilder(elementsUtil,typeUtil,log) ;
        mBundleSaveBuilder = new BundleSaveBuilder(elementsUtil,typeUtil,log) ;
        log.printMessage(Diagnostic.Kind.NOTE,"__init");
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        log.printMessage(Diagnostic.Kind.NOTE,"__process");
        if(null == set || set.isEmpty()){
            return false ;
        }
        Set<? extends Element> extraElements = roundEnvironment.getElementsAnnotatedWith(Extra.class) ;
        if(null == extraElements || extraElements.isEmpty()){
            return false ;
        }
        collecExtraInfo(extraElements);
        try {
            return makeJavaFile() ;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false ;
    }

    private boolean makeJavaFile() throws Exception {
        if(null == parentAndChilds || parentAndChilds.isEmpty()){
            return false;
        }
        TypeMirror typeActivity = elementsUtil.getTypeElement("android.app.Activity").asType() ;
        TypeMirror typeFragment = elementsUtil.getTypeElement("android.app.Fragment").asType() ;


        TypeElement typeExtra = elementsUtil.getTypeElement("com.example.lib_core.IExtra") ;
        ParameterSpec objectParamSpec = ParameterSpec.builder(TypeName.OBJECT, "target").build();
        ParameterSpec bundleParamSpec = ParameterSpec.builder(ClassName.get("android.os","Bundle"),"bundle").build();

        for (Map.Entry<TypeElement,List<Element>> entrys : parentAndChilds.entrySet()){
            TypeElement typeElement = entrys.getKey() ;


            List<Element> elements = parentAndChilds.get(typeElement) ;

            if(elements != null && !elements.isEmpty()){

                //添加获取bundle数据
                MethodSpec.Builder loadExtra = MethodSpec.methodBuilder(Consts
                        .METHOD_LOAD_EXTRA)
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(objectParamSpec)
                        .addParameter(bundleParamSpec);

                loadExtra.addStatement("$L t = ($L)target",typeElement.getSimpleName(),typeElement.getSimpleName()) ;

                if(typeUtil.isSubtype(typeElement.asType(),typeActivity)){
                    //构建activity的获取extra代码
                    makeActivityMethod(typeElement, elements, loadExtra);
                } else if(typeUtil.isSubtype(typeElement.asType(),typeFragment)) {
                    //构建fragment的获取extra代码
                    loadExtra.addStatement("bundle = bundle == null ? t.getArguments() : bundle") ;
                    for (Element element : elements){
                        mBundleGetBuilder.buildGetTransfDataStatement(element,loadExtra);
                    }
                } else {
                    throw new RuntimeException("only support type {android.app.Activity,android.app.Fragment}" + typeElement);
                }
                //添加保存bundle方法
                MethodSpec.Builder saveExtra = MethodSpec.methodBuilder(Consts
                        .METHOD_SAVE_EXTRA)
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(objectParamSpec)
                        .addParameter(bundleParamSpec);

                saveExtra.addStatement("$L t = ($L)target",typeElement.getSimpleName(),typeElement.getSimpleName()) ;
                for (Element element : elements){
                    mBundleSaveBuilder.buildSaveTransfDataStatement(element,saveExtra);
                }

                // 生成java类名
                String extraClassName = typeElement.getSimpleName() + Consts.NAME_OF_EXTRA;
                // 生成 XX$$Autowired
                JavaFile.builder( ClassName.get(typeElement).packageName(), TypeSpec.classBuilder(extraClassName)
                        .addSuperinterface(ClassName.get(typeExtra))
                        .addModifiers(PUBLIC)
                        .addMethod(loadExtra.build()).addMethod(saveExtra.build()).build())
                        .build().writeTo(filer);
            }
        }
        return true ;
    }

    private void makeActivityMethod(TypeElement typeElement, List<Element> elements, MethodSpec.Builder loadExtra) {

        loadExtra.beginControlFlow("if(bundle == null)") ;
        for (Element element : elements){
            buildGetTransfDataStatement(element,loadExtra);
        }
        loadExtra.nextControlFlow("else") ;
        for (Element element : elements){
            mBundleGetBuilder.buildGetTransfDataStatement(element,loadExtra);
        }
        loadExtra.endControlFlow() ;
    }

    /**
     * 将该类中相关有Extra的属性存储到集合中，集中key为相关类
     * @param elements
     */
    private void collecExtraInfo(Set<? extends Element> elements) {
        for(Element element : elements) {
            TypeElement parentElement = (TypeElement) element.getEnclosingElement();
            if(parentAndChilds.containsKey(parentElement)){
                parentAndChilds.get(parentElement).add(element) ;
            } else {
                ArrayList<Element> elementChilds = new ArrayList<>() ;
                elementChilds.add(element) ;
                parentAndChilds.put(parentElement,elementChilds) ;
            }
        }
    }

    public void buildGetTransfDataStatement(Element element,MethodSpec.Builder builder) {
        TypeMirror typeMirror = element.asType();
        int type = typeMirror.getKind().ordinal();
        //属性名 String text 获得text
        String fieldName = element.getSimpleName().toString();

        //获得注解 name值
        String extraName = element.getAnnotation(Extra.class).name();
        log.printMessage(Diagnostic.Kind.NOTE,"filed:" + fieldName +" extraName:" + extraName);
        extraName = Utils.isEmpty(extraName) ? fieldName : extraName;
        String defaultValue = "t." + fieldName;
        String statement = defaultValue + " = t.getIntent().";
        if (type == TypeKind.BOOLEAN.ordinal()) {
            statement += "getBooleanExtra($S, " + defaultValue + ")";
        } else if (type == TypeKind.BYTE.ordinal()) {
            statement += "getByteExtra($S, " + defaultValue + ")";
        } else if (type == TypeKind.SHORT.ordinal()) {
            statement += "getShortExtra($S, " + defaultValue + ")";
        } else if (type == TypeKind.INT.ordinal()) {
            statement += "getIntExtra($S, " + defaultValue + ")";
        } else if (type == TypeKind.LONG.ordinal()) {
            statement += "getLongExtra($S, " + defaultValue + ")";
        } else if (type == TypeKind.CHAR.ordinal()) {
            statement += "getCharExtra($S, " + defaultValue + ")";
        } else if (type == TypeKind.FLOAT.ordinal()) {
            statement += "getFloatExtra($S, " + defaultValue + ")";
        } else if (type == TypeKind.DOUBLE.ordinal()) {
            statement += "getDoubleExtra($S, " + defaultValue + ")";
        } else {
            //数组类型
            if (type == TypeKind.ARRAY.ordinal()) {
                addArrayStatement(statement, fieldName, extraName, typeMirror, element,builder);
            } else {
                //Object
                addObjectStatement(statement, fieldName, extraName, typeMirror, element,builder);
            }
            return;
        }
        log.printMessage(Diagnostic.Kind.NOTE,"extraName: " + extraName);
        builder.addStatement(statement, extraName);
    }

    /**
     * 添加数组
     *
     * @param statement
     * @param fieldName
     * @param typeMirror
     * @param element
     */
    private void addArrayStatement(String statement, String fieldName, String extraName, TypeMirror
            typeMirror, Element element, MethodSpec.Builder builder) {
        TypeMirror parcelableType = elementsUtil.getTypeElement(Consts.PARCELABLE).asType() ;
        //数组
        switch (typeMirror.toString()) {
            case Consts.BOOLEANARRAY:
                statement += "getBooleanArrayExtra($S)";
                break;
            case Consts.INTARRAY:
                statement += "getIntArrayExtra($S)";
                break;
            case Consts.SHORTARRAY:
                statement += "getShortArrayExtra($S)";
                break;
            case Consts.FLOATARRAY:
                statement += "getFloatArrayExtra($S)";
                break;
            case Consts.DOUBLEARRAY:
                statement += "getDoubleArrayExtra($S)";
                break;
            case Consts.BYTEARRAY:
                statement += "getByteArrayExtra($S)";
                break;
            case Consts.CHARARRAY:
                statement += "getCharArrayExtra($S)";
                break;
            case Consts.LONGARRAY:
                statement += "getLongArrayExtra($S)";
                break;
            case Consts.STRINGARRAY:
                statement += "getStringArrayExtra($S)";
                break;
            default:
                //Parcelable 数组
                String defaultValue = "t." + fieldName;
                //object数组 componentType获得object类型
                ArrayTypeName arrayTypeName = (ArrayTypeName) ClassName.get(typeMirror);
                TypeElement typeElement = elementsUtil.getTypeElement(arrayTypeName
                        .componentType.toString());
                //是否为 Parcelable 类型
                if (!typeUtil.isSubtype(typeElement.asType(), parcelableType)) {
                    throw new RuntimeException("不支持参数类型 " + typeMirror + " " +element);
                }
                statement = "$T[] " + fieldName + " = t.getIntent()" +
                        ".getParcelableArrayExtra" +
                        "($S)";
                builder.addStatement(statement, parcelableType, extraName);
                builder.beginControlFlow("if( null != $L)", fieldName);
                statement = defaultValue + " = new $T[" + fieldName + ".length]";
                builder.addStatement(statement, arrayTypeName.componentType)
                        .beginControlFlow("for (int i = 0; i < " + fieldName + "" +
                                ".length; " +
                                "i++)")
                        .addStatement(defaultValue + "[i] = ($T)" + fieldName + "[i]",
                                arrayTypeName.componentType)
                        .endControlFlow();
                builder.endControlFlow();
                return;
        }
        builder.addStatement(statement, extraName);
    }


    /**
     * 添加对象 String/List/Parcelable
     *
     * @param statement
     * @param extraName
     * @param typeMirror
     * @param element
     */
    private void addObjectStatement(String statement, String fieldName, String extraName,
                                    TypeMirror typeMirror,
                                    Element element,MethodSpec.Builder builder) {
        //Parcelable
        TypeMirror parcelableType = elementsUtil.getTypeElement(Consts.PARCELABLE).asType() ;
        if (typeUtil.isSubtype(typeMirror, parcelableType)) {
            statement += "getParcelableExtra($S)";
        } else if (typeMirror.toString().equals(Consts.STRING)) {
            statement += "getStringExtra($S)";
        } else {
            //List
            TypeName typeName = ClassName.get(typeMirror);
            //泛型
            if (typeName instanceof ParameterizedTypeName) {
                //list 或 arraylist
                ClassName rawType = ((ParameterizedTypeName) typeName).rawType;
                //泛型类型
                List<TypeName> typeArguments = ((ParameterizedTypeName) typeName)
                        .typeArguments;
                if (!rawType.toString().equals(Consts.ARRAYLIST) && !rawType.toString()
                        .equals(Consts.LIST)) {
                    throw new RuntimeException("Not Support Inject Type:" + typeMirror + " " +
                            element);
                }
                if (typeArguments.isEmpty() || typeArguments.size() != 1) {
                    throw new RuntimeException("List Must Specify Generic Type:" + typeArguments);
                }
                TypeName typeArgumentName = typeArguments.get(0);
                TypeElement typeElement = elementsUtil.getTypeElement(typeArgumentName
                        .toString());
                // Parcelable 类型
                if (typeUtil.isSubtype(typeElement.asType(), parcelableType)) {
                    statement += "getParcelableArrayListExtra($S)";
                } else if (typeElement.asType().toString().equals(Consts.STRING)) {
                    statement += "getStringArrayListExtra($S)";
                } else if (typeElement.asType().toString().equals(Consts.INTEGER)) {
                    statement += "getIntegerArrayListExtra($S)";
                } else {
                    throw new RuntimeException("Not Support Generic Type : " + typeMirror + " " +
                            element);
                }
            } else {
                throw new RuntimeException("Not Support Extra Type : " + typeMirror + " " +
                        element);
            }
        }
        builder.addStatement(statement, extraName);
    }


}
