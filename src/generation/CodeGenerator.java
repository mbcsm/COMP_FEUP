import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

public class CodeGenerator {
    static String className;
    static SymbolTable symbolTable;
    static int registerStack = 1;

    public static void main(String args[]) throws ParseException, FileNotFoundException, SemanticException {
        try {

            if (args.length != 1) {
                Utils.printUsage();
                return;
            }

            Javamm javamm = new Javamm(new FileInputStream(args[0]));
            SimpleNode root = javamm.Module();

            Node firstNode = root.jjtGetChild(0);
            symbolTable = new SymbolTable();

            ASTClassDeclaration mainClass = (ASTClassDeclaration) firstNode;
            className = mainClass.className;

            symbolTable.addVariables(root);
            symbolTable.fillReturn(root);
            symbolTable.booleanAttribution(root);
            root.semanticAnalysis();
            printAllSymbolTables(symbolTable);

            toJasmin(firstNode);
            System.out.println("\nJasmin file created.");
        }

        catch (FileNotFoundException ex) {
            System.out.println("Failed getting file <" + args[0] + ">");
            Utils.printUsage();
            ex.printStackTrace();
        } catch (ParseException e) {
            System.out.println("Parse Exception reading <" + args[0] + ">");
            Utils.printUsage();
            e.printStackTrace();
        }
    }

    public static void toJasmin(Node firstChild) {

        if (firstChild != null) {
            PrintWriter output = Utils.toFile(className);

            output.println(".class public " + className);
            output.println(".super java/lang/Object\n");

            for (int i = 0; i < firstChild.jjtGetNumChildren(); i++) {

                Node node = firstChild.jjtGetChild(i);

                if (node.getId() == JavammTreeConstants.JJTMETHODDECLARATION) {
                    ASTMethodDeclaration method = (ASTMethodDeclaration) node;
                    toMethod(output, method, null);
                }

                if (node.getId() == JavammTreeConstants.JJTMAINDECLARATION) {
                    ASTMainDeclaration method = (ASTMainDeclaration) node;
                    toMethod(output, null, method);
                }
            }
            output.close();
        }
    }

    private static void toMethod(PrintWriter output, ASTMethodDeclaration node, ASTMainDeclaration otherNode) {
        String methodName = null;
        Table methodsTable = null;
        SimpleNode methodNode = null;

        output.print("\n.method public static ");

        if (node != null) {
            methodNode = (ASTMethodDeclaration) node;
            methodName = node.methodName;
            methodsTable = getMethodTable(methodName);

            output.print(header(methodsTable, methodNode, methodName) + "\n");
        } else {
            methodName = "main";
            methodNode = (ASTMainDeclaration) otherNode;
            methodsTable = getMethodTable("main");
            output.print("main([Ljava/lang/String;)V\n");
        }

        writeStackNumber(methodsTable, methodName, output);

        for (int i = 0; i < methodNode.jjtGetNumChildren(); i++) {
            Node child = methodNode.jjtGetChild(i);

            if (child.getId() == JavammTreeConstants.JJTSTATEMENT) {
                statementToJvm(output, methodsTable, child);
            }
        }

        if (methodsTable.getReturnSymbol() != null) {

            printPayload(output, methodsTable, methodsTable.getReturnSymbol().getName(), "ID");

            if (methodsTable.getReturnSymbol().getType().equals("int")
                    || methodsTable.getReturnSymbol().getType().equals("boolean")) {
                output.println("  ireturn");
            } else if (methodsTable.getReturnSymbol().getType().equals("int[]")) {
                output.println("  areturn");
            } else {
                output.println("  return");
            }

        } else {
            output.println("  return");
        }

        output.println(".end method\n");
    }

    private static void printPayload(PrintWriter output, Table methodsTable, String name, String type) {

        if (type.equals("ID")) {
            if (methodsTable != null && methodsTable.getSymbol(name) != null) {
                Symbol var = methodsTable.getSymbol(name);

                if (var.getType().equals("int")) {
                    if (var.getRegister() >= 0 && var.getRegister() <= 3)
                        output.println("  iload_" + var.getRegister());
                    else
                        output.println("  iload " + var.getRegister());
                }

                else if (var.getType().equals("int[]")) {// TODO arrays
                    if (var.getRegister() >= 0 && var.getRegister() <= 3)
                        output.println("  aload_" + var.getRegister());
                    else
                        output.println("  aload " + var.getRegister());
                }

                else if (var.getType().equals("boolean")) {
                    if (var.getName().equals("true")) {
                        output.println("  iconst_" + 1);
                    } else if (var.getName().equals("false")) {
                        output.println("  iconst_" + 0);
                    } else {
                        if (var.getBool() == "null") {
                            output.println("  iconst_" + 0);
                        } else {
                            if (var.getBool() == "true") {
                                output.println("  iconst_" + 1);
                            } else if (var.getBool() == "false") {
                                output.println("  iconst_" + 0);
                            }
                        }
                    }
                }

            } else {
                Symbol global = symbolTable.getClassVariablesTable().get(name);

                if (global != null) {
                    String globalType = global.getType();
                    output.println("  getstatic " + className + "/" + global.getName() + globalType);
                }
            }
        } else if (type.equals("Integer")) {
            int number = Integer.parseInt(name);

            if (number == -1) {
                output.println("  iconst_m1");
            } else if (number >= 0 && number <= 5)
                output.println("  iconst_" + number);
            else if (number >= -128 && number <= 127)
                output.println("  bipush " + number);
            else if (number >= -32768 && number <= 32767)
                output.println("  sipush " + number);
            else
                output.println("  ldc " + number);

        } else if (type.equals("String")) {
            output.println("  ldc " + name);
        } else {
            output.println("\n\nELSE TYPE <" + type + ">\n\n");
        }
    }

    private static void ifToJvm(PrintWriter output, Table methodsTable, Node node) {
        methodsTable.incLoopCounter();
        ASTStatementIf ifNode = (ASTStatementIf) node;
        int loop_num = methodsTable.getLoopCounter();
        Node firstChild = ifNode.jjtGetChild(0);
        Node nextChild = firstChild.jjtGetChild(0);

        if (firstChild.getId() == JavammTreeConstants.JJTEXPRESSION) {

            if (nextChild.getId() == JavammTreeConstants.JJTTRUE) {
                for (int i = 0; i < ifNode.jjtGetNumChildren(); i++) {
                    if (ifNode.jjtGetChild(i).getId() == JavammTreeConstants.JJTSTATEMENT) {
                        statementToJvm(output, methodsTable, ifNode.jjtGetChild(i));
                    }
                }
            } else if (nextChild.getId() == JavammTreeConstants.JJTFALSE) {
                for (int i = 0; i < ifNode.jjtGetNumChildren(); i++) {
                    if (ifNode.jjtGetChild(i).getId() == JavammTreeConstants.JJTELSE) {
                        ASTElse elseNode = (ASTElse) ifNode.jjtGetChild(i);

                        for (int j = 0; j < elseNode.jjtGetNumChildren(); j++) {
                            statementToJvm(output, methodsTable, elseNode.jjtGetChild(j));
                        }

                    }
                }
            }
        }

        for (int i = 0; i < ifNode.jjtGetNumChildren(); i++) {

            if (ifNode.jjtGetChild(i).getId() == JavammTreeConstants.JJTEXPRESSION) {
                expressionIfToJvm(output, methodsTable, ifNode.jjtGetChild(i).jjtGetChild(0), loop_num);
                output.print("\n");
            } else if (ifNode.jjtGetChild(i).getId() == JavammTreeConstants.JJTELSE) {
                ASTElse elseNode = (ASTElse) ifNode.jjtGetChild(i);

                output.println("  goto loop" + loop_num + "_next\n");
                output.println("loop" + loop_num + "_end:");

                for (int j = 0; j < elseNode.jjtGetNumChildren(); j++) {
                    statementToJvm(output, methodsTable, elseNode.jjtGetChild(j));
                }

                output.print("loop" + loop_num + "_next:");
            } else {
                statementToJvm(output, methodsTable, ifNode.jjtGetChild(i));
            }
        }
    }

    private static void statementToJvm(PrintWriter output, Table methodsTable, Node child) {
        for (int i = 0; i < child.jjtGetNumChildren(); i++) {

            Node nextChild = child.jjtGetChild(i);

            if (nextChild.getId() == JavammTreeConstants.JJTFUNCTIONCALL) {
                methodToJvm(output, methodsTable, nextChild, "void");
            }

            if (nextChild.getId() == JavammTreeConstants.JJTSTATEMENTIF) {
                ifToJvm(output, methodsTable, nextChild);
            }
            if (nextChild.getId() == JavammTreeConstants.JJTWHILE) {
                whileToJvm(output, methodsTable, nextChild.jjtGetParent());
            }

            if (nextChild.getId() == JavammTreeConstants.JJTATTRIBUTION) {

                if (nextChild.jjtGetChild(1).getId() == JavammTreeConstants.JJTEXPRESSION) {

                    if (nextChild.jjtGetChild(1).jjtGetChild(0).getId() == JavammTreeConstants.JJTARITHM) {

                        ASTArithm arithm = (ASTArithm) nextChild.jjtGetChild(1).jjtGetChild(0);
                        arithmeticGeneration(output, methodsTable, arithm);
                    }
                }
            }
        }
        output.println("\n");
    }

    private static void whileToJvm(PrintWriter output, Table methodsTable, Node node) {

        methodsTable.incLoopCounter();

        Node firstChild = node.jjtGetChild(1);
        Node nextChild = firstChild.jjtGetChild(0);
        int loop_num = methodsTable.getLoopCounter();

        output.println("loop" + loop_num + ":");

        if (firstChild.getId() == JavammTreeConstants.JJTEXPRESSION) {

            if (nextChild.getId() == JavammTreeConstants.JJTTRUE) {
                for (int i = 1; i < node.jjtGetNumChildren(); i++) {
                    if (node.jjtGetChild(i).getId() == JavammTreeConstants.JJTSTATEMENT) {
                        statementToJvm(output, methodsTable, node.jjtGetChild(i));
                    }
                }
            }
        }
        for (int i = 1; i < node.jjtGetNumChildren(); i++) {

            if (node.jjtGetChild(i).getId() == JavammTreeConstants.JJTEXPRESSION) {
                expressionIfToJvm(output, methodsTable, node.jjtGetChild(i).jjtGetChild(0), loop_num);
                output.print("\n");
            } else {
                statementToJvm(output, methodsTable, node.jjtGetChild(i));
            }
        }
        output.println("  goto loop" + loop_num + "\n");
        output.print("loop" + loop_num + "_end:");
    }

    private static void expressionIfToJvm(PrintWriter output, Table methodsTable, Node nextChild, int loop_num) {

        if (nextChild.getId() == JavammTreeConstants.JJTARITHM) {

            ASTArithm arithmetic = (ASTArithm) nextChild;
            Node tagged = arithmetic.jjtGetChild(0).jjtGetChild(0).jjtGetChild(0);

            if (tagged.getId() == JavammTreeConstants.JJTLESS) {
                Node first = tagged.jjtGetChild(0);
                Node second = tagged.jjtGetChild(1);

                handleTerm(first, output, loop_num, methodsTable);
                handleTerm(second, output, loop_num, methodsTable);

                output.println("  if_icmpge loop" + loop_num + "_end");
            }

            else if (tagged.getId() == JavammTreeConstants.JJTAND) {
                Node first = tagged.jjtGetChild(0);
                Node second = tagged.jjtGetChild(1);

                handleTerm(first, output, loop_num, methodsTable);
                output.println("  ifeq loop" + loop_num + "_end");
                handleTerm(second, output, loop_num, methodsTable);
                output.println("  ifeq loop" + loop_num + "_end");
            } else {
                if (tagged.getId() == JavammTreeConstants.JJTTERM) {

                    if (tagged.jjtGetNumChildren() == 0) {
                        ASTTerm term_cast = (ASTTerm) tagged;
                        termToJvm(output, methodsTable, term_cast);
                        output.println("  ifeq loop" + loop_num + "_end");
                    } else {
                        Node child = tagged.jjtGetChild(0);

                        if (child.getId() == JavammTreeConstants.JJTPARENTHESISARITHM) {
                            ASTParenthesisArithm parenthesisArithm = (ASTParenthesisArithm) child;
                            expressionIfToJvm(output, methodsTable, parenthesisArithm.jjtGetChild(0), loop_num);
                        }

                        if (child.getId() == JavammTreeConstants.JJTTHISCALL) {
                            ASTThisCall thisCall = (ASTThisCall) child;

                            if (thisCall.jjtGetNumChildren() > 0) {

                                ASTFunctionCall call = new ASTFunctionCall(Integer.MAX_VALUE);

                                if (thisCall.jjtGetChild(0).jjtGetNumChildren() > 0) {
                                    if (thisCall.jjtGetChild(0).jjtGetChild(0)
                                            .getId() == JavammTreeConstants.JJTARGUMENTLIST) {
                                        call.jjtAddChild(thisCall.jjtGetChild(0).jjtGetChild(0), 0);
                                    }
                                }
                                call.module = thisCall.module;
                                call.function = "this";
                                methodToJvm(output, methodsTable, call, "void");
                                output.println("  ifeq loop" + loop_num + "_end");
                            } else {
                                printPayload(output, methodsTable, thisCall.module, "ID");
                                output.println("  ifeq loop" + loop_num + "_end");
                            }
                        }

                        if (child.getId() == JavammTreeConstants.JJTEXPRESSIONMETHOD) {
                            if (child.jjtGetChild(0).getId() == JavammTreeConstants.JJTMETHODS) {
                                ASTTerm term_cast = (ASTTerm) tagged;
                                String function = term_cast.identifier;
                                ASTMethods method = (ASTMethods) child.jjtGetChild(0);
                                String module = method.module;

                                if (module.equals("length")) {
                                    output.println("  arraylength");
                                    output.println("  ifeq loop" + loop_num + "_end");
                                } else {
                                    ASTFunctionCall call = new ASTFunctionCall(0);

                                    if (method.jjtGetNumChildren() > 0) {
                                        if (method.jjtGetChild(0).getId() == JavammTreeConstants.JJTARGUMENTLIST) {
                                            call.jjtAddChild(method.jjtGetChild(0), 0);
                                        }
                                    }
                                    call.module = module;
                                    call.function = function;
                                    methodToJvm(output, methodsTable, call, "void");
                                    output.println("  ifeq loop" + loop_num + "_end");
                                }
                            }
                        }

                    }
                }
            }

        } else if (nextChild.getId() == JavammTreeConstants.JJTEXCLAMATIONMARK) {
            expressionIterator(output, methodsTable, loop_num, nextChild.jjtGetChild(0).jjtGetChild(0));
        } else if (nextChild.getId() == JavammTreeConstants.JJTFUNCTIONCALL) {
            methodToJvm(output, methodsTable, nextChild, "void");
            output.println("  ifeq loop" + loop_num + "_end");
        }

    }

    private static void expressionIterator(PrintWriter output, Table methodsTable, int loop_num, Node nextChild) {

        if (nextChild.getId() == JavammTreeConstants.JJTARITHM) {

            ASTArithm arithmetic = (ASTArithm) nextChild;
            Node tag = arithmetic.jjtGetChild(0).jjtGetChild(0).jjtGetChild(0);

            if (tag.getId() == JavammTreeConstants.JJTLESS) {
                Node first = tag.jjtGetChild(0);
                Node second = tag.jjtGetChild(1);

                handleTerm(first, output, loop_num, methodsTable);
                handleTerm(second, output, loop_num, methodsTable);

                output.println("  if_icmplt loop" + loop_num + "_end");
            } else if (tag.getId() == JavammTreeConstants.JJTAND) {
                Node first = tag.jjtGetChild(0);
                Node second = tag.jjtGetChild(1);

                handleTerm(first, output, loop_num, methodsTable);
                output.println("  ifeq loop" + loop_num + "_end");
                handleTerm(second, output, loop_num, methodsTable);
                output.println("  ifne loop" + loop_num + "_end");
            } else {
                if (tag.getId() == JavammTreeConstants.JJTTERM) {

                    if (tag.jjtGetNumChildren() == 0) {
                        ASTTerm term_cast = (ASTTerm) tag;
                        termToJvm(output, methodsTable, term_cast);
                        output.println("  ifne loop" + loop_num + "_end");
                    }

                    else {
                        Node child = tag.jjtGetChild(0);

                        if (child.getId() == JavammTreeConstants.JJTPARENTHESISARITHM) {
                            ASTParenthesisArithm parenthesisArithm = (ASTParenthesisArithm) child;
                            expressionIterator(output, methodsTable, loop_num, parenthesisArithm.jjtGetChild(0));
                        }

                        if (child.getId() == JavammTreeConstants.JJTTHISCALL) {
                            callProcedure(child, output, loop_num, methodsTable);
                        }

                        if (child.getId() == JavammTreeConstants.JJTEXPRESSIONMETHOD) {
                            if (child.jjtGetChild(0).getId() == JavammTreeConstants.JJTMETHODS) {
                                ASTTerm term_cast = (ASTTerm) tag;
                                String function = term_cast.identifier;
                                ASTMethods method = (ASTMethods) child.jjtGetChild(0);
                                String module = method.module;

                                if (module.equals("length")) {
                                    output.println("  arraylength");
                                    output.println("  ifne loop" + loop_num + "_end");
                                }

                                else {
                                    ASTFunctionCall call = new ASTFunctionCall(0);

                                    if (method.jjtGetNumChildren() > 0) {
                                        if (method.jjtGetChild(0).getId() == JavammTreeConstants.JJTARGUMENTLIST) {
                                            call.jjtAddChild(method.jjtGetChild(0), 0);
                                        }
                                    }

                                    call.function = function;
                                    call.module = module;

                                    methodToJvm(output, methodsTable, call, "void");
                                    output.println("  ifne loop" + loop_num + "_end");
                                }
                            }
                        }
                    }
                }

            }

        } else if (nextChild.getId() == JavammTreeConstants.JJTEXCLAMATIONMARK) {
            expressionIterator(output, methodsTable, loop_num, nextChild.jjtGetChild(0));
        } else if (nextChild.getId() == JavammTreeConstants.JJTFUNCTIONCALL) {
            methodToJvm(output, methodsTable, nextChild, "void");
            output.println("  ifne loop" + loop_num + "_end");
        }
    }

    private static void arithmeticGeneration(PrintWriter output, Table methodsTable, Node nextChild) {

        ASTArithm arithm = (ASTArithm) nextChild;
        String operator = whichOperator(arithm, "");

        ASTTerm first = null;
        ASTTerm second = null;

        if (arithm.jjtGetChild(0).getId() == JavammTreeConstants.JJTADD
                || arithm.jjtGetChild(0).getId() == JavammTreeConstants.JJTSUB) {
            first = termCycle(arithm.jjtGetChild(0).jjtGetChild(0), 0);
            second = termCycle(arithm.jjtGetChild(0).jjtGetChild(1), 0);
        }

        else if (arithm.jjtGetChild(0).jjtGetChild(0).getId() == JavammTreeConstants.JJTMULT
                || arithm.jjtGetChild(0).jjtGetChild(0).getId() == JavammTreeConstants.JJTDIV) {
            first = termCycle(arithm.jjtGetChild(0).jjtGetChild(0).jjtGetChild(0), 0);
            second = termCycle(arithm.jjtGetChild(0).jjtGetChild(0).jjtGetChild(1), 0);
        }

        if (second != null && first != null) {
            termToJvm(output, methodsTable, first);
            termToJvm(output, methodsTable, second);
        }

        switch (operator) {
        case "+":
            output.println("  iadd");
            break;
        case "-":
            output.println("  isub");
            break;
        case "*":
            output.println("  imul");
            break;
        case "/":
            output.println("  idiv");
            break;
        }
    }

    public static String whichOperator(ASTArithm arithm, String operator) {
        if (arithm.operator == "+") {
            operator = "+";
        } else if (arithm.operator == "-") {
            operator = "-";
        } else if (arithm.jjtGetChild(0).getId() == JavammTreeConstants.JJTEXPRESSIONMULT) {
            ASTExpressionMult child = (ASTExpressionMult) arithm.jjtGetChild(0);
            if (child.operator == "*") {
                operator = "*";
            } else if (child.operator == "/") {
                operator = "/";
            }
        }
        return operator;
    }

    private static ASTTerm termCycle(Node child, int index) {

        if (child.getId() == JavammTreeConstants.JJTTERM) {
            return (ASTTerm) child;
        } else {
            return termCycle(child.jjtGetChild(index), index);
        }
    }

    private static void termToJvm(PrintWriter output, Table methodsTable, ASTTerm term) {
        if (term.val != null) {
            printPayload(output, methodsTable, "" + term.val, "Integer");
        }

        if (!term.identifier.equals("")) {
            printPayload(output, methodsTable, term.identifier, "ID");
        }
    }

    private static void methodToJvm(PrintWriter output, Table methodsTable, Node nextChild, String returnType) {
        ASTArgumentList argumentList = null;
        ASTFunctionCall call = (ASTFunctionCall) nextChild;

        if (call.jjtGetNumChildren() > 0 && call.jjtGetChild(0).getId() == JavammTreeConstants.JJTARGUMENTLIST) {

            argumentList = (ASTArgumentList) call.jjtGetChild(0);

            for (int i = 0; i < argumentList.jjtGetNumChildren(); i++) {
                ASTArgument argument = (ASTArgument) argumentList.jjtGetChild(i);
                printPayload(output, methodsTable, argument.name, argument.type);
            }
        }

        if (call.jjtGetNumChildren() == 0 && call.function.equals("main")) {
            output.println("  aconst_null");
            output.println("  invokestatic " + className + "/main([Ljava/lang/String;)V");
        }

        if (call.function.equals("this")) {
            output.println("  invokevirtual " + className + "/"
                    + headerInvoke(output, methodsTable, call.module, argumentList, returnType));
        } else if (methodsTable.getVars() != null) {
            if (methodsTable.getVars().get(call.function) == null
                    || symbolTable.getMethodsTable().get(call.module) == null
                    || !methodsTable.getVars().get(call.function).getType().equals(className)) {
                output.println("  invokestatic " + call.function + "/"
                        + headerInvoke(output, methodsTable, call.module, argumentList, returnType));
            } else if ((methodsTable.getVars().get(call.function) != null
                    && methodsTable.getVars().get(call.function).getType().equals(className))
                    && symbolTable.getMethodsTable().get(call.module) != null) {
                output.println("  invokevirtual " + call.function + "/"
                        + headerInvoke(output, methodsTable, call.module, argumentList, returnType));
            }
        } else {
            output.println("  invokestatic " + call.function + "/"
                    + headerInvoke(output, methodsTable, call.module, argumentList, returnType));
        }
    }

    private static String header(Table methodTable, SimpleNode methodNode, String methodName) {

        ASTMethodDeclaration cast_node = (ASTMethodDeclaration) methodNode;

        String headerFunc = methodName + "(";

        if (cast_node.jjtGetChild(1).getId() == JavammTreeConstants.JJTMETHODARGUMENTS) {
            ASTMethodArguments first = (ASTMethodArguments) cast_node.jjtGetChild(1);

            if (first.jjtGetNumChildren() > 0) {
                ASTType type_node = (ASTType) first.jjtGetChild(0);

                if (type_node.type.equals("int") && type_node.isArray) {
                    headerFunc = headerFunc + "[I";
                }

                if (type_node.type.equals("int") && !type_node.isArray) {
                    headerFunc = headerFunc + "I";
                }

                if (type_node.type.equals("boolean")) {
                    headerFunc = headerFunc + "Z";
                }

                if (first.jjtGetNumChildren() > 1) {
                    for (int i = 1; i < first.jjtGetNumChildren(); i++) {
                        ASTMethodArgumentPiece child = (ASTMethodArgumentPiece) first.jjtGetChild(i);

                        ASTType other = (ASTType) child.jjtGetChild(0);

                        if (other.type.equals("int") && other.isArray) {
                            headerFunc = headerFunc + "[I";
                        }

                        if (other.type.equals("int") && !other.isArray) {
                            headerFunc = headerFunc + "I";
                        }

                        if (other.type.equals("boolean")) {
                            headerFunc = headerFunc + "Z";
                        }

                    }
                }
            }
        }

        if (symbolTable.getMethodsTable().containsKey(methodName)) {
            Symbol _s = symbolTable.getMethodsTable().get(methodName);

            if (_s.type.equals("void")) {
                headerFunc = headerFunc + ")V";
            } else if (_s.type.equals("int") || _s.type.equals("boolean")) {
                headerFunc = headerFunc + ")I";
            } else if (_s.type.equals("int[]")) {
                headerFunc = headerFunc + ")[I";
            } else {
                headerFunc = headerFunc + ")";
            }
        }

        return headerFunc;
    }

    private static String headerInvoke(PrintWriter output, Table methodsTable, String function,
            ASTArgumentList arguments, String returnType) {

        String headerFunc = function + "(";

        if (arguments != null) {
            for (int i = 0; i < arguments.jjtGetNumChildren(); i++) {
                ASTArgument argument = (ASTArgument) arguments.jjtGetChild(i);
                if (argument.type.equals("ID")) {

                    if (methodsTable.getVars() != null) {
                        Symbol _s = methodsTable.getVars().get(argument.name);

                        if (_s != null) {
                            String type = _s.type;

                            if (type.equals("int[]")) {
                                headerFunc = headerFunc + "[I";
                            } else
                                headerFunc = headerFunc + "I";
                        }
                    }

                    if (methodsTable.getParams() != null) {
                        Symbol _s = methodsTable.getParams().get(argument.name);

                        if (_s != null) {
                            String type = _s.type;

                            if (type.equals("int[]")) {
                                headerFunc = headerFunc + "[I";
                            } else
                                headerFunc = headerFunc + "I";
                        }
                    } else
                        headerFunc = headerFunc + "I";
                } else if (argument.type.equals("String"))
                    headerFunc = headerFunc + "Ljava/lang/String;";
                else if (argument.type.equals("Integer"))
                    headerFunc = headerFunc + "I";
                else if (argument.jjtGetNumChildren() > 0) {

                    if (argument.jjtGetChild(0).getId() == JavammTreeConstants.JJTARGUMENTFUNCTIONCALL) {

                        ASTArgumentFunctionCall newFunc = (ASTArgumentFunctionCall) argument.jjtGetChild(0);
                        ASTArgumentList argumentList = null;

                        if (newFunc.jjtGetNumChildren() > 0
                                && newFunc.jjtGetChild(0).getId() == JavammTreeConstants.JJTARGUMENTLIST) {

                            argumentList = (ASTArgumentList) newFunc.jjtGetChild(0);

                            for (int j = 0; j < argumentList.jjtGetNumChildren(); j++) {
                                ASTArgument arg = (ASTArgument) argumentList.jjtGetChild(j);
                                printPayload(output, methodsTable, arg.name, arg.type);
                            }
                        }

                        if (methodsTable.getVars() != null) {
                            if (methodsTable.getVars().get(newFunc.function) == null
                                    || symbolTable.getMethodsTable().get(newFunc.module) == null
                                    || !methodsTable.getVars().get(newFunc.function).getType().equals(className)) {
                                output.println("  invokestatic " + newFunc.function + "/"
                                        + headerInvoke(output, methodsTable, newFunc.module, argumentList, returnType));
                            } else if ((methodsTable.getVars().get(newFunc.function) != null
                                    && methodsTable.getVars().get(newFunc.function).getType().equals(className))
                                    && symbolTable.getMethodsTable().get(newFunc.module) != null) {
                                output.println("  invokevirtual " + newFunc.function + "/"
                                        + headerInvoke(output, methodsTable, newFunc.module, argumentList, returnType));
                            }
                        } else {
                            output.println("  invokestatic " + newFunc.function + "/"
                                    + headerInvoke(output, methodsTable, newFunc.module, argumentList, returnType));
                        }
                    }

                    if (argument.jjtGetChild(0).getId() == JavammTreeConstants.JJTNEWARGUMENTFUNCTIONCALL) {

                        ASTNewArgumentFunctionCall newFunc = (ASTNewArgumentFunctionCall) argument.jjtGetChild(0);
                        ASTArgumentList argumentList = null;

                        if (newFunc.jjtGetNumChildren() > 0
                                && newFunc.jjtGetChild(0).getId() == JavammTreeConstants.JJTARGUMENTLIST) {

                            argumentList = (ASTArgumentList) newFunc.jjtGetChild(0);

                            for (int j = 0; j < argumentList.jjtGetNumChildren(); j++) {
                                ASTArgument arg = (ASTArgument) argumentList.jjtGetChild(j);
                                printPayload(output, methodsTable, arg.name, arg.type);
                            }
                        }

                        if (methodsTable.getVars() != null) {
                            if (symbolTable.getMethodsTable().get(newFunc.module) == null
                                    || !methodsTable.getVars().get(newFunc.function).getType().equals(className)) {
                                output.println("  invokestatic " + newFunc.function + "/"
                                        + headerInvoke(output, methodsTable, newFunc.module, argumentList, returnType));
                            } else if (methodsTable.getVars().get(newFunc.function).getType().equals(className)
                                    && symbolTable.getMethodsTable().get(newFunc.module) != null) {
                                output.println("  invokevirtual " + newFunc.function + "/"
                                        + headerInvoke(output, methodsTable, newFunc.module, argumentList, returnType));
                            }
                        }

                        else {
                            if (symbolTable.getMethodsTable().get(newFunc.module) == null
                                    || !newFunc.function.equals(className)) {
                                output.println("  invokestatic " + newFunc.function + "/"
                                        + headerInvoke(output, methodsTable, newFunc.module, argumentList, returnType));
                            } else {
                                output.println("  invokevirtual " + newFunc.function + "/"
                                        + headerInvoke(output, methodsTable, newFunc.module, argumentList, returnType));
                            }
                        }
                    }
                }
            }
        }

        Table functionTable = getMethodTable(function);

        if (functionTable.getReturnSymbol() != null) {
            Symbol returnSymbol = functionTable.getReturnSymbol();

            if (returnSymbol != null) {
                if (returnSymbol.getType().equals("int") || returnSymbol.getType().equals("boolean"))
                    headerFunc = headerFunc + ")I";
                else if (returnSymbol.getType().equals("int[]"))
                    headerFunc = headerFunc + ")[I";
                else
                    headerFunc = headerFunc + ")V";

            } else {
                if (returnType.equals("void")) {
                    headerFunc = headerFunc + ")V";
                } else if (returnType.equals("int") || returnType.equals("boolean")) {
                    headerFunc = headerFunc + ")I";
                } else if (returnType.equals("int[]")) {
                    headerFunc = headerFunc + ")[I";
                }
            }
        } else
            headerFunc = headerFunc + ")V";

        return headerFunc;
    }

    private static void callProcedure(Node child, PrintWriter output, int loop_num, Table methodsTable) {
        ASTThisCall thisCall = (ASTThisCall) child;

        if (thisCall.jjtGetNumChildren() > 0) {
            ASTFunctionCall call = new ASTFunctionCall(Integer.MAX_VALUE);

            if (thisCall.jjtGetChild(0).jjtGetNumChildren() > 0) {
                if (thisCall.jjtGetChild(0).jjtGetChild(0).getId() == JavammTreeConstants.JJTARGUMENTLIST) {
                    call.jjtAddChild(thisCall.jjtGetChild(0).jjtGetChild(0), 0);
                }
            }
            call.module = thisCall.module;
            call.function = "this";

            methodToJvm(output, methodsTable, call, "void");
        }
    }

    private static void handleTerm(Node term, PrintWriter output, int loop_num, Table methodsTable) {
        if (term.jjtGetNumChildren() == 0) {
            ASTTerm first_cast = (ASTTerm) term;
            termToJvm(output, methodsTable, first_cast);
        } else {
            if (term.jjtGetChild(0).getId() == JavammTreeConstants.JJTTHISCALL) {
                callProcedure(term.jjtGetChild(0), output, loop_num, methodsTable);
            }

            if (term.jjtGetChild(0).getId() == JavammTreeConstants.JJTEXPRESSIONMETHOD) {
                if (term.jjtGetChild(0).jjtGetChild(0).getId() == JavammTreeConstants.JJTMETHODS) {
                    ASTTerm term_cast = (ASTTerm) term;
                    String function = term_cast.identifier;
                    ASTMethods method = (ASTMethods) term.jjtGetChild(0).jjtGetChild(0);
                    String module = method.module;

                    if (module.equals("length")) {
                        output.println("  arraylength");
                    }

                    else {
                        ASTFunctionCall call = new ASTFunctionCall(0);

                        if (method.jjtGetNumChildren() > 0) {
                            if (method.jjtGetChild(0).getId() == JavammTreeConstants.JJTARGUMENTLIST) {
                                call.jjtAddChild(method.jjtGetChild(0), 0);
                            }
                        }
                        call.module = module;
                        call.function = function;

                        methodToJvm(output, methodsTable, call, "void");
                    }
                }
            }

        }
    }

    private static Table getMethodTable(String methodName) {
        Table methodsTable = new Table();

        HashMap<String, Symbol> params = symbolTable.getParametersTable().get(methodName);

        HashMap<String, Symbol> vars = symbolTable.getLocalVariablesTable().get(methodName);

        Symbol _s = symbolTable.getReturnTable().get(methodName);

        methodsTable.setVariables(vars);
        methodsTable.setParameters(params);
        methodsTable.setReturn(_s);

        return methodsTable;
    }

    private static void writeStackNumber(Table methodsTable, String methodName, PrintWriter output) {
        int localsNr = methodsTable.getNumLocalVariables();

        output.println("  .limit stack " + 999);

        if (localsNr != 0) {
            output.println("  .limit locals " + localsNr);
        }
    }

    public static void printAllSymbolTables(SymbolTable table) {

        System.out.println("METHODS NAME:");

        for (String name : table.getMethodsTable().keySet()) {
            String value = table.getMethodsTable().get(name).getType();
            System.out.println(name + " " + value);
        }

        System.out.println("\nMETHOD PARAMETERS:");

        for (String name : table.getParametersTable().keySet()) {
            HashMap<String, Symbol> temp = table.getParametersTable().get(name);

            System.out.println("Method " + name + ":");

            for (String key : temp.keySet()) {
                Symbol _s = temp.get(key);
                System.out.println(_s.getName() + " " + _s.getType());
            }

            System.out.println();
        }

        System.out.println("\nCLASS VARIABLES:");

        for (String name : table.getClassVariablesTable().keySet()) {

            table.getClassVariablesTable().get(name).setRegister(registerStack);
            registerStack++;

            String value = table.getClassVariablesTable().get(name).getType();
            System.out.println(name + " " + value);
        }

        System.out.println("\nLOCAL VARIABLES:");

        for (String name : table.getLocalVariablesTable().keySet()) {
            HashMap<String, Symbol> temp = table.getLocalVariablesTable().get(name);

            System.out.println("Method " + name + ":");

            for (String key : temp.keySet()) {
                Symbol _s = temp.get(key);

                temp.get(key).setRegister(registerStack);
                registerStack++;

                System.out.println(_s.getName() + " " + _s.getType() + " register:" + temp.get(key).getRegister());
            }

            System.out.println();
        }

        System.out.println("\nMETHODS RETURN:");

        for (String name : table.getReturnTable().keySet()) {
            String symbolValue = table.getReturnTable().get(name).getType();
            String returnValue = table.getReturnTable().get(name).getName();
            System.out.println(name + " " + returnValue + " " + symbolValue);
        }
    }
}