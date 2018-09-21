package com.example.administrator.lib.Util;

import com.example.admistrator.Extra;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.List;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * Author:janecer
 * created on 2018/9/21
 */
public class IntentGetBuilder {

    private Elements elementsUtil ;
    private Types typeUtil ;
    private Messager log;

    public IntentGetBuilder(Elements elementsUtil, Types typeUtil,Messager log) {
        this.elementsUtil = elementsUtil;
        this.typeUtil = typeUtil;
        this.log = log;
    }

    public void buildGetTransfDataStatement(Element element, MethodSpec.Builder builder) {
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
                addObjectStatement(statement,  extraName, typeMirror, element,builder);
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
    private void addObjectStatement(String statement,  String extraName,
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
