package com.github.gs618.sprouts.beans;

import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

/**
 * @author sgao
 */
@SupportedAnnotationTypes({"com.github.gs618.sprouts.beans.BeanMapper"})
public class BeanMapperProcessor extends AbstractProcessor {

    /**
     * 抽象语法树
     */
    private JavacTrees trees;
    /**
     * AST
     */
    private TreeMaker treeMaker;
    /**
     * 名称标识符
     */
    private Names names;

    /**
     * 日志处理
     */
    private Messager messager;

    /**
     * 写文件
     */
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        trees = JavacTrees.instance(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        treeMaker = TreeMaker.instance(context);
        messager = processingEnv.getMessager();
        names = Names.instance(context);
        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getRootElements();

        for (Element element : elements) {
            messager.printMessage(Diagnostic.Kind.NOTE, element.getSimpleName() + " has been processed");
            if (!element.getKind().isClass()) {
                continue;
            }
            JCTree jcTree = trees.getTree(element);
            jcTree.accept(new TreeTranslator() {

                @Override
                public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                    java.util.List<? extends Element> enclosedElements = element.getEnclosedElements();

                    // list all local field, mapping target can't be any field.
                    java.util.List<Element> fieldElements = new ArrayList<>();
                    java.util.List<String> fieldStrings = new ArrayList<>();
                    for (Element enclosedElement : enclosedElements) {
                        if (enclosedElement.getKind().isField()) {
                            fieldElements.add(enclosedElement);
                            fieldStrings.add(enclosedElement.getSimpleName().toString());
                        }
                    }

                    for (Element fieldElement : fieldElements) {
                        BeanMapper annotation = fieldElement.getAnnotation(BeanMapper.class);
                        if (Objects.isNull(annotation)) {
                            continue;
                        }
                        JCTree field = trees.getTree(fieldElement);
                        JCTree.JCVariableDecl jcVariableDecl = (JCTree.JCVariableDecl) field;
                        String[] mappings = annotation.targets();
                        for (String mapping : mappings) {
                            if (!fieldStrings.contains(mapping)) {
                                treeMaker.pos = jcClassDecl.pos;
                                jcClassDecl.defs = jcClassDecl.defs.prepend(generateSetterMethod(jcVariableDecl, mapping));
                            }
                        }
                    }
                    super.visitClassDef(jcClassDecl);
                }
            });
        }
        return false;
    }

    /**
     * 备忘
     * 生成Get方法
     *
     * @param jcVariable jc变量描述
     * @return 方法定义描述
     */
    @Deprecated
    private JCTree.JCMethodDecl generateGetterMethod(JCTree.JCVariableDecl jcVariable) {
        //修改方法级别
        JCTree.JCModifiers jcModifiers = treeMaker.Modifiers(Flags.PUBLIC);
        //添加方法名称
        Name methodName = generateMethodSignature(jcVariable.getName().toString(), "get");
        //添加方法内容
        ListBuffer<JCTree.JCStatement> jcStatements = new ListBuffer<>();
        jcStatements.append(
                treeMaker.Return(treeMaker.Select(treeMaker.Ident(names.fromString("this")), jcVariable.getName())));
        JCTree.JCBlock jcBlock = treeMaker.Block(0, jcStatements.toList());
        //添加返回值类型
        JCTree.JCExpression returnType = jcVariable.vartype;
        //参数类型
        List<JCTree.JCTypeParameter> typeParameters = List.nil();
        //参数变量
        List<JCTree.JCVariableDecl> parameters = List.nil();
        //声明异常
        List<JCTree.JCExpression> throwsClauses = List.nil();
        //构建方法
        return treeMaker
                .MethodDef(jcModifiers, methodName, returnType, typeParameters, parameters, throwsClauses, jcBlock, null);
    }

    /**
     * 生成Set方法
     *
     * @param jcVariable jc变量描述
     * @param mapping    映射的set名称
     * @return 方法定义描述
     */
    private JCTree.JCMethodDecl generateSetterMethod(JCTree.JCVariableDecl jcVariable, String mapping) {
        //修改方法级别
        JCTree.JCModifiers modifiers = treeMaker.Modifiers(Flags.PUBLIC);
        //添加方法名称
        Name variableName = jcVariable.getName();
        Name mappingName = names.fromString(mapping);
        Name methodName = generateMethodSignature(mapping, "set");
        //设置方法体
        ListBuffer<JCTree.JCStatement> jcStatements = new ListBuffer<>();
        jcStatements.append(treeMaker.Exec(treeMaker
                .Assign(treeMaker.Select(treeMaker.Ident(names.fromString("this")), variableName),
                        treeMaker.Ident(mappingName))));
        //定义方法体
        JCTree.JCBlock jcBlock = treeMaker.Block(0, jcStatements.toList());
        //添加返回值类型
        JCTree.JCExpression returnType = treeMaker.Type(new Type.JCVoidType());
        //参数类型
        List<JCTree.JCTypeParameter> typeParameters = List.nil();
        //定义参数
        JCTree.JCVariableDecl variableDecl = treeMaker
                .VarDef(treeMaker.Modifiers(Flags.PARAMETER, List.nil()), mappingName, jcVariable.vartype, null);
        List<JCTree.JCVariableDecl> parameters = List.of(variableDecl);
        //声明异常
        List<JCTree.JCExpression> throwsClauses = List.nil();
        //构建方法
        return treeMaker
                .MethodDef(modifiers, methodName, returnType, typeParameters, parameters, throwsClauses, jcBlock, null);
    }

    /**
     * 生成set/get格式的方法名
     *
     * @param name   需要set/get的变量名称
     * @param prefix set｜get作为前缀
     * @return Name描述
     */
    private Name generateMethodSignature(String name, String prefix) {
        return names.fromString(prefix + name.substring(0, 1).toUpperCase() + name.substring(1));
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }


}
