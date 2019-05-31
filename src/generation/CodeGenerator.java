import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import syntatic;

public class CodeGenerator {
    static String motherClassName;
    static SymbolTable table;
    static int registerCounter = 1;

    public static void main(String args[]) throws ParseException, FileNotFoundException, SemanticException {
        try {
            Javamm javamm = new Javamm(new FileInputStream("MonteCarloPi.txt"));
            SimpleNode root = javamm.Module();

            Node firstChild = root.jjtGetChild(0);
            table = new SymbolTable();

            ASTClassDeclaration cast_to_mother_class = (ASTClassDeclaration) firstChild;
            motherClassName = cast_to_mother_class.className;

            table.addVariables(root);
            table.fillReturn(root);
            table.booleanAttribution(root);
            root.semanticAnalysis();
            printAllSymbolTables(table);

            convertCodeToJasmin(firstChild);
            System.out.println(" ");
            System.out.println("Generated Jasmin file!");
        }

        catch (FileNotFoundException ex) {
            // insert code to run when exception occurs
            ex.printStackTrace();
        }
    }

    public static void printAllSymbolTables(SymbolTable table) {

        System.out.println("NAMES OF ALL METHODS:");
        for (String name : table.getMethodsTable().keySet()) {
            String value = table.getMethodsTable().get(name).getType();
            System.out.println(name + " " + value);
        }

        System.out.println("");

        System.out.println("ALL PARAMETERS FOR THOSE METHODS:");
        for (String name : table.getParametersTable().keySet()) {
            HashMap<String, Symbol> temp = table.getParametersTable().get(name);

            System.out.println("Method " + name + ":");

            for (String key : temp.keySet()) {
                Symbol s1 = temp.get(key);
                System.out.println(s1.getName() + " " + s1.getType());
            }

            System.out.println(" ");
        }

        System.out.println("");

        System.out.println("ALL CLASS VARIABLES:");
        for (String name : table.getClassVariablesTable().keySet()) {

            table.getClassVariablesTable().get(name).setRegister(registerCounter);
            registerCounter++;

            String value = table.getClassVariablesTable().get(name).getType();
            System.out.println(name + " " + value);
        }

        System.out.println("");

        System.out.println("ALL LOCAL VARIABLES:");
        for (String name : table.getLocalVariablesTable().keySet()) {
            HashMap<String, Symbol> temp = table.getLocalVariablesTable().get(name);

            System.out.println("Method " + name + ":");

            for (String key : temp.keySet()) {
                Symbol s1 = temp.get(key);

                temp.get(key).setRegister(registerCounter);
                registerCounter++;

                System.out.println(s1.getName() + " " + s1.getType() + " register:" + temp.get(key).getRegister());
            }

            System.out.println(" ");

        }

        System.out.println("");

        System.out.println("RETURN OF ALL METHODS:");
        for (String name : table.getReturnTable().keySet()) {
            String symbolValue = table.getReturnTable().get(name).getType();
            String returnValue = table.getReturnTable().get(name).getName();
            System.out.println(name + " " + returnValue + " " + symbolValue);
        }
    };

    public static PrintWriter printJasminFile() {

        try {
            File dir = new File("jasmin");
            if (!dir.exists())
                dir.mkdirs();

            File file = new File("jasmin/" + motherClassName + ".j");
            if (!file.exists())
                file.createNewFile();

            PrintWriter writer = new PrintWriter(file);

            return writer;

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

    public static void convertCodeToJasmin(Node firstChild) {

        if (firstChild != null) {
            PrintWriter file = printJasminFile();

            file.println(".class public " + motherClassName);
            file.println(".super java/lang/Object\n");

            // functions (methods) conversion of each instruction
            for (int i = 0; i < firstChild.jjtGetNumChildren(); i++) {

                Node node = firstChild.jjtGetChild(i);

                if (node.getId() == JavammTreeConstants.JJTMETHODDECLARATION) {
                    ASTMethodDeclaration method = (ASTMethodDeclaration) node;
                    convertMethod(file, method, null);
                }

                if (node.getId() == JavammTreeConstants.JJTMAINDECLARATION) {
                    ASTMainDeclaration method = (ASTMainDeclaration) node;
                    convertMethod(file, null, method);
                }
            }

            file.close();

        }
    }

    private static void convertMethod(PrintWriter file, ASTMethodDeclaration node, ASTMainDeclaration otherNode) {

        Table methodsTable = null;
        String methodName = null;
        SimpleNode methodNode = null;

        file.print("\n.method public static ");

        if (node != null) {
            methodNode = (ASTMethodDeclaration) node;
            methodName = node.methodName;
            methodsTable = getMethodTable(methodName);

            file.print(header(methodsTable, methodNode, methodName) + "\n");
        }

        else {
            methodNode = (ASTMainDeclaration) otherNode;
            methodName = "main";
            methodsTable = getMethodTable("main");

            file.print("main([Ljava/lang/String;)V\n");
        }

        writeStackNumber(methodsTable, methodName, file);

        // function statements / arithmetric expressions
        for (int i = 0; i < methodNode.jjtGetNumChildren(); i++) {

            Node child = methodNode.jjtGetChild(i);

            if (child.getId() == JavammTreeConstants.JJTSTATEMENT) {
                statementToJvm(file, methodsTable, child);
            }

        }

        // function return
        if (methodsTable.getReturnSymbol() != null) {

            printVariableLoad(file, methodsTable, methodsTable.getReturnSymbol().getName(), "ID");

            if (methodsTable.getReturnSymbol().getType().equals("int")
                    || methodsTable.getReturnSymbol().getType().equals("boolean")) {
                file.println("  ireturn");
            } else if (methodsTable.getReturnSymbol().getType().equals("int[]")) {
                file.println("  areturn");
            }

            else { // void
                file.println("  return");
            }

        } else { // void
            file.println("  return");
        }

        file.println(".end method\n");
    }

    private static void printVariableLoad(PrintWriter file, Table methodsTable, String name, String type) {

        if (type.equals("ID")) {
            if (methodsTable != null && methodsTable.getSymbol(name) != null) { // Local Variables
                Symbol variable = methodsTable.getSymbol(name);

                if (variable.getType().equals("int")) { // ints
                    if (variable.getRegister() >= 0 && variable.getRegister() <= 3)
                        file.println("  iload_" + variable.getRegister());
                    else
                        file.println("  iload " + variable.getRegister());
                }

                else if (variable.getType().equals("int[]")) {// arrays
                    if (variable.getRegister() >= 0 && variable.getRegister() <= 3)
                        file.println("  aload_" + variable.getRegister());
                    else
                        file.println("  aload " + variable.getRegister());
                }

                else if (variable.getType().equals("boolean")) {// booleans
                    if (variable.getName().equals("true")) {
                        file.println("  iconst_" + 1);
                    }

                    else if (variable.getName().equals("false")) {
                        file.println("  iconst_" + 0);
                    }

                    else {
                        if (variable.getBool() == "null") {
                            file.println("  iconst_" + 0);
                        }

                        else {
                            if (variable.getBool() == "true") {
                                file.println("  iconst_" + 1);
                            }

                            else if (variable.getBool() == "false") {
                                file.println("  iconst_" + 0);
                            }
                        }
                    }
                }

            } else { // Global variable
                Symbol globalVariable = table.getClassVariablesTable().get(name);

                if (globalVariable != null) {
                    String globalVariableType = globalVariable.getType();
                    file.println(
                            "  getstatic " + motherClassName + "/" + globalVariable.getName() + globalVariableType);
                }

            }
        } else if (type.equals("Integer")) {

            int number = Integer.parseInt(name);

            if (number == -1) {
                file.println("  iconst_m1");
            } else if (number >= 0 && number <= 5)
                file.println("  iconst_" + number);
            else if (number >= -128 && number <= 127)
                file.println("  bipush " + number);
            else if (number >= -32768 && number <= 32767)
                file.println("  sipush " + number);
            else
                file.println("  ldc " + number);

        } else if (type.equals("String")) {

            file.println("  ldc " + name);
        }

    }

    private static void statementToJvm(PrintWriter file, Table methodsTable, Node child) {
        // Will include ifs, whiles, etc .. for now, will only include function call

        for (int i = 0; i < child.jjtGetNumChildren(); i++) {

            Node grandChild = child.jjtGetChild(i);

            if (grandChild.getId() == JavammTreeConstants.JJTFUNCTIONCALL) {
                methodCallToJvm(file, methodsTable, grandChild, "void");
            }

            if (grandChild.getId() == JavammTreeConstants.JJTSTATEMENTIF) {
                ifToJvm(file, methodsTable, grandChild);
            }
            if (grandChild.getId() == JavammTreeConstants.JJTWHILE) {
                whiletoJvm(file, methodsTable, grandChild.jjtGetParent());
            }

            if (grandChild.getId() == JavammTreeConstants.JJTATTRIBUTION) {

                if (grandChild.jjtGetChild(1).getId() == JavammTreeConstants.JJTEXPRESSION) {

                    if (grandChild.jjtGetChild(1).jjtGetChild(0).getId() == JavammTreeConstants.JJTARITHM) {

                        ASTArithm arithm = (ASTArithm) grandChild.jjtGetChild(1).jjtGetChild(0);

                        arithmToJvm(file, methodsTable, arithm);
                    }
                }
            }
        }

        file.println("\n");
    }

    private static void ifToJvm(PrintWriter file, Table methodsTable, Node node) {
        ASTStatementIf ifNode = (ASTStatementIf) node;
        methodsTable.incLoopCounter();
        int loop_nr = methodsTable.getLoopCounter();
        Node firstChild = ifNode.jjtGetChild(0);
        Node grandChild = firstChild.jjtGetChild(0);
        // in case of if(true) and if(false)
        if (firstChild.getId() == JavammTreeConstants.JJTEXPRESSION) {

            if (grandChild.getId() == JavammTreeConstants.JJTTRUE) {
                for (int i = 0; i < ifNode.jjtGetNumChildren(); i++) {
                    if (ifNode.jjtGetChild(i).getId() == JavammTreeConstants.JJTSTATEMENT) {
                        statementToJvm(file, methodsTable, ifNode.jjtGetChild(i));
                    }
                }
            }

            else if (grandChild.getId() == JavammTreeConstants.JJTFALSE) {
                for (int i = 0; i < ifNode.jjtGetNumChildren(); i++) {
                    if (ifNode.jjtGetChild(i).getId() == JavammTreeConstants.JJTELSE) {
                        ASTElse elseNode = (ASTElse) ifNode.jjtGetChild(i);

                        for (int j = 0; j < elseNode.jjtGetNumChildren(); j++) {
                            statementToJvm(file, methodsTable, elseNode.jjtGetChild(j));
                        }

                    }
                }
            }
        }

        for (int i = 0; i < ifNode.jjtGetNumChildren(); i++) {

            // Expression between parenthesis
            if (ifNode.jjtGetChild(i).getId() == JavammTreeConstants.JJTEXPRESSION) {
                expressionIfToJvm(file, methodsTable, ifNode.jjtGetChild(i).jjtGetChild(0), loop_nr);
                file.print("\n");
            }

            // Else
            else if (ifNode.jjtGetChild(i).getId() == JavammTreeConstants.JJTELSE) {
                ASTElse elseNode = (ASTElse) ifNode.jjtGetChild(i);

                file.println("  goto loop" + loop_nr + "_next\n");
                file.println("loop" + loop_nr + "_end:");

                for (int j = 0; j < elseNode.jjtGetNumChildren(); j++) {
                    statementToJvm(file, methodsTable, elseNode.jjtGetChild(j));
                }

                file.print("loop" + loop_nr + "_next:");
            }

            // If statements
            else {
                statementToJvm(file, methodsTable, ifNode.jjtGetChild(i));
            }
        }

    }

    private static void whiletoJvm(PrintWriter file, Table methodsTable, Node node) {

        methodsTable.incLoopCounter();
        int loop_nr = methodsTable.getLoopCounter();
        Node firstChild = node.jjtGetChild(1);
        Node grandChild = firstChild.jjtGetChild(0);
        file.println("loop" + loop_nr + ":");
        // in case of while(true)
        if (firstChild.getId() == JavammTreeConstants.JJTEXPRESSION) {

            if (grandChild.getId() == JavammTreeConstants.JJTTRUE) {
                for (int i = 1; i < node.jjtGetNumChildren(); i++) {
                    if (node.jjtGetChild(i).getId() == JavammTreeConstants.JJTSTATEMENT) {
                        statementToJvm(file, methodsTable, node.jjtGetChild(i));
                    }
                }
            }
        }
        for (int i = 1; i < node.jjtGetNumChildren(); i++) {
            // Expression between parenthesis
            if (node.jjtGetChild(i).getId() == JavammTreeConstants.JJTEXPRESSION) {
                expressionIfToJvm(file, methodsTable, node.jjtGetChild(i).jjtGetChild(0), loop_nr);
                file.print("\n");
            }
            // While statements
            else {
                statementToJvm(file, methodsTable, node.jjtGetChild(i));
            }
        }
        file.println("  goto loop" + loop_nr + "\n");
        file.print("loop" + loop_nr + "_end:");
    }

    private static void expressionIfToJvm(PrintWriter file, Table methodsTable, Node grandChild, int loop_nr) {

        // Expressions that evaluate to boolean:
        // functions V
        // operator < V with functions! V and then negation V
        // operator && V with functions! V and then negation V
        // single boolean term V
        // ! (Expression) V

        if (grandChild.getId() == JavammTreeConstants.JJTARITHM) {

            ASTArithm cast_arithm = (ASTArithm) grandChild;

            Node tag = cast_arithm.jjtGetChild(0).jjtGetChild(0).jjtGetChild(0);

            // operator <
            if (tag.getId() == JavammTreeConstants.JJTLESS) {
                Node term1 = tag.jjtGetChild(0);
                Node term2 = tag.jjtGetChild(1);

                handleTerms(term1, file, loop_nr, methodsTable);
                handleTerms(term2, file, loop_nr, methodsTable);

                file.println("  if_icmpge loop" + loop_nr + "_end");
            }

            // operator &&
            else if (tag.getId() == JavammTreeConstants.JJTAND) {
                Node term1 = tag.jjtGetChild(0);
                Node term2 = tag.jjtGetChild(1);

                handleTerms(term1, file, loop_nr, methodsTable);
                file.println("  ifeq loop" + loop_nr + "_end");
                handleTerms(term2, file, loop_nr, methodsTable);
                file.println("  ifeq loop" + loop_nr + "_end");
            }

            // single boolean term
            else {
                if (tag.getId() == JavammTreeConstants.JJTTERM) {

                    // no parenthesis
                    if (tag.jjtGetNumChildren() == 0) {
                        ASTTerm term_cast = (ASTTerm) tag;
                        termToJvm(file, methodsTable, term_cast);
                        file.println("  ifeq loop" + loop_nr + "_end");
                    }

                    // ( <something> )
                    else {
                        Node child = tag.jjtGetChild(0);

                        if (child.getId() == JavammTreeConstants.JJTPARENTHESISARITHM) {
                            ASTParenthesisArithm parenthesisArithm = (ASTParenthesisArithm) child;
                            expressionIfToJvm(file, methodsTable, parenthesisArithm.jjtGetChild(0), loop_nr);
                        }

                        if (child.getId() == JavammTreeConstants.JJTTHISCALL) {
                            ASTThisCall thisCall = (ASTThisCall) child;

                            if (thisCall.jjtGetNumChildren() > 0) {
                                // this.funct()
                                ASTFunctionCall conveniece = new ASTFunctionCall(Integer.MAX_VALUE);

                                if (thisCall.jjtGetChild(0).jjtGetNumChildren() > 0) {
                                    if (thisCall.jjtGetChild(0).jjtGetChild(0)
                                            .getId() == JavammTreeConstants.JJTARGUMENTLIST) {
                                        conveniece.jjtAddChild(thisCall.jjtGetChild(0).jjtGetChild(0), 0);
                                    }
                                }

                                conveniece.function = "this";
                                conveniece.module = thisCall.module;

                                methodCallToJvm(file, methodsTable, conveniece, "void");
                                file.println("  ifeq loop" + loop_nr + "_end");
                            }

                            else {
                                // this.variable
                                printVariableLoad(file, methodsTable, thisCall.module, "ID");
                                file.println("  ifeq loop" + loop_nr + "_end");
                            }

                        }

                        // variable.attribute
                        if (child.getId() == JavammTreeConstants.JJTEXPRESSIONMETHOD) {
                            if (child.jjtGetChild(0).getId() == JavammTreeConstants.JJTMETHODS) {
                                ASTTerm term_cast = (ASTTerm) tag;
                                String function = term_cast.identifier;
                                ASTMethods method = (ASTMethods) child.jjtGetChild(0);
                                String module = method.module;

                                if (module.equals("length")) {
                                    file.println("  arraylength");
                                    file.println("  ifeq loop" + loop_nr + "_end");
                                }

                                else {
                                    ASTFunctionCall conveniece = new ASTFunctionCall(0);

                                    if (method.jjtGetNumChildren() > 0) {
                                        if (method.jjtGetChild(0).getId() == JavammTreeConstants.JJTARGUMENTLIST) {
                                            conveniece.jjtAddChild(method.jjtGetChild(0), 0);
                                        }
                                    }

                                    conveniece.function = function;
                                    conveniece.module = module;

                                    methodCallToJvm(file, methodsTable, conveniece, "void");
                                    file.println("  ifeq loop" + loop_nr + "_end");
                                }
                            }
                        }

                    }
                }
            }

        }

        // Exclamation Mark
        else if (grandChild.getId() == JavammTreeConstants.JJTEXCLAMATIONMARK) {
            expressionRecursion(file, methodsTable, loop_nr, grandChild.jjtGetChild(0).jjtGetChild(0));
        }

        // Functions
        else if (grandChild.getId() == JavammTreeConstants.JJTFUNCTIONCALL) {
            methodCallToJvm(file, methodsTable, grandChild, "void");
            file.println("  ifeq loop" + loop_nr + "_end");
        }

    }

    private static void expressionRecursion(PrintWriter file, Table methodsTable, int loop_nr, Node grandChild) {

        if (grandChild.getId() == JavammTreeConstants.JJTARITHM) {

            ASTArithm cast_arithm = (ASTArithm) grandChild;

            Node tag = cast_arithm.jjtGetChild(0).jjtGetChild(0).jjtGetChild(0);

            // negates <
            if (tag.getId() == JavammTreeConstants.JJTLESS) {
                Node term1 = tag.jjtGetChild(0);
                Node term2 = tag.jjtGetChild(1);

                handleTerms(term1, file, loop_nr, methodsTable);
                handleTerms(term2, file, loop_nr, methodsTable);

                file.println("  if_icmplt loop" + loop_nr + "_end");
            }

            // negates &&
            else if (tag.getId() == JavammTreeConstants.JJTAND) {
                Node term1 = tag.jjtGetChild(0);
                Node term2 = tag.jjtGetChild(1);

                handleTerms(term1, file, loop_nr, methodsTable);
                file.println("  ifeq loop" + loop_nr + "_end");
                handleTerms(term2, file, loop_nr, methodsTable);
                file.println("  ifne loop" + loop_nr + "_end");
            }

            // negates single boolean term
            else {
                if (tag.getId() == JavammTreeConstants.JJTTERM) {

                    if (tag.jjtGetNumChildren() == 0) {
                        ASTTerm term_cast = (ASTTerm) tag;
                        termToJvm(file, methodsTable, term_cast);
                        file.println("  ifne loop" + loop_nr + "_end");
                    }

                    else {
                        Node child = tag.jjtGetChild(0);

                        if (child.getId() == JavammTreeConstants.JJTPARENTHESISARITHM) {
                            ASTParenthesisArithm parenthesisArithm = (ASTParenthesisArithm) child;
                            expressionRecursion(file, methodsTable, loop_nr, parenthesisArithm.jjtGetChild(0));
                        }

                        if (child.getId() == JavammTreeConstants.JJTTHISCALL) {
                            thisCallProcedure(child, file, loop_nr, methodsTable);
                        }

                        // negate variable.something
                        if (child.getId() == JavammTreeConstants.JJTEXPRESSIONMETHOD) {
                            if (child.jjtGetChild(0).getId() == JavammTreeConstants.JJTMETHODS) {
                                ASTTerm term_cast = (ASTTerm) tag;
                                String function = term_cast.identifier;
                                ASTMethods method = (ASTMethods) child.jjtGetChild(0);
                                String module = method.module;

                                if (module.equals("length")) {
                                    file.println("  arraylength");
                                    file.println("  ifne loop" + loop_nr + "_end");
                                }

                                else {
                                    ASTFunctionCall conveniece = new ASTFunctionCall(0);

                                    if (method.jjtGetNumChildren() > 0) {
                                        if (method.jjtGetChild(0).getId() == JavammTreeConstants.JJTARGUMENTLIST) {
                                            conveniece.jjtAddChild(method.jjtGetChild(0), 0);
                                        }
                                    }

                                    conveniece.function = function;
                                    conveniece.module = module;

                                    methodCallToJvm(file, methodsTable, conveniece, "void");
                                    file.println("  ifne loop" + loop_nr + "_end");
                                }
                            }
                        }
                    }
                }

            }

        }

        // Exclamation Mark
        else if (grandChild.getId() == JavammTreeConstants.JJTEXCLAMATIONMARK) {
            expressionRecursion(file, methodsTable, loop_nr, grandChild.jjtGetChild(0));
        }

        // negates boolean function
        else if (grandChild.getId() == JavammTreeConstants.JJTFUNCTIONCALL) {
            methodCallToJvm(file, methodsTable, grandChild, "void");
            file.println("  ifne loop" + loop_nr + "_end");
        }
    }

    private static void arithmToJvm(PrintWriter file, Table methodsTable, Node grandChild) {

        ASTArithm arithm = (ASTArithm) grandChild;
        String operator = determineOperator(arithm, "");

        ASTTerm term1 = null;
        ASTTerm term2 = null;

        if (arithm.jjtGetChild(0).getId() == JavammTreeConstants.JJTADD
                || arithm.jjtGetChild(0).getId() == JavammTreeConstants.JJTSUB) {
            term1 = goThroughArithmChildrenUntilTerm(arithm.jjtGetChild(0).jjtGetChild(0), 0);
            term2 = goThroughArithmChildrenUntilTerm(arithm.jjtGetChild(0).jjtGetChild(1), 0);
        }

        else if (arithm.jjtGetChild(0).jjtGetChild(0).getId() == JavammTreeConstants.JJTMULT
                || arithm.jjtGetChild(0).jjtGetChild(0).getId() == JavammTreeConstants.JJTDIV) {
            term1 = goThroughArithmChildrenUntilTerm(arithm.jjtGetChild(0).jjtGetChild(0).jjtGetChild(0), 0);
            term2 = goThroughArithmChildrenUntilTerm(arithm.jjtGetChild(0).jjtGetChild(0).jjtGetChild(1), 0);
        }

        if (term2 != null && term1 != null) {
            termToJvm(file, methodsTable, term1);
            termToJvm(file, methodsTable, term2);
        }

        switch (operator) {
        case "+":
            file.println("  iadd");
            break;
        case "-":
            file.println("  isub");
            break;
        case "*":
            file.println("  imul");
            break;
        case "/":
            file.println("  idiv");
            break;
        }
    }

    private static void termToJvm(PrintWriter file, Table methodsTable, ASTTerm term) {
        if (term.val != null) {
            printVariableLoad(file, methodsTable, "" + term.val, "Integer");
        }

        if (!term.identifier.equals("")) {
            printVariableLoad(file, methodsTable, term.identifier, "ID");
        }
    }

    private static ASTTerm goThroughArithmChildrenUntilTerm(Node child, int index) {

        if (child.getId() == JavammTreeConstants.JJTTERM) {
            return (ASTTerm) child;
        }

        else {
            return goThroughArithmChildrenUntilTerm(child.jjtGetChild(index), index);
        }
    }

    public static String determineOperator(ASTArithm arithm, String operator) {
        if (arithm.operator == "+") {
            operator = "+";
        }

        else if (arithm.operator == "-") {
            operator = "-";
        }

        else if (arithm.jjtGetChild(0).getId() == JavammTreeConstants.JJTEXPRESSIONMULT) {
            ASTExpressionMult child = (ASTExpressionMult) arithm.jjtGetChild(0);

            if (child.operator == "*") {
                operator = "*";
            }

            else if (child.operator == "/") {
                operator = "/";
            }
        }

        return operator;
    }

    private static void methodCallToJvm(PrintWriter file, Table methodsTable, Node grandChild, String returnType) {

        ASTFunctionCall call = (ASTFunctionCall) grandChild;

        ASTArgumentList argumentList = null;

        if (call.jjtGetNumChildren() > 0 && call.jjtGetChild(0).getId() == JavammTreeConstants.JJTARGUMENTLIST) {

            argumentList = (ASTArgumentList) call.jjtGetChild(0);

            for (int i = 0; i < argumentList.jjtGetNumChildren(); i++) {
                ASTArgument argument = (ASTArgument) argumentList.jjtGetChild(i);
                printVariableLoad(file, methodsTable, argument.name, argument.type);
            }
        }

        if (call.jjtGetNumChildren() == 0 && call.function.equals("main")) {
            file.println("  aconst_null");
            file.println("  invokestatic " + motherClassName + "/main([Ljava/lang/String;)V");
        }

        if (call.function.equals("this")) {
            file.println("  invokevirtual " + motherClassName + "/"
                    + functionHeaderInvoke(file, methodsTable, call.module, argumentList, returnType));
        }

        else if (methodsTable.getVars() != null) {
            if (methodsTable.getVars().get(call.function) == null || table.getMethodsTable().get(call.module) == null
                    || !methodsTable.getVars().get(call.function).getType().equals(motherClassName)) {
                file.println("  invokestatic " + call.function + "/"
                        + functionHeaderInvoke(file, methodsTable, call.module, argumentList, returnType));
            } else if ((methodsTable.getVars().get(call.function) != null
                    && methodsTable.getVars().get(call.function).getType().equals(motherClassName))
                    && table.getMethodsTable().get(call.module) != null) {
                file.println("  invokevirtual " + call.function + "/"
                        + functionHeaderInvoke(file, methodsTable, call.module, argumentList, returnType));
            }
        }

        else {
            file.println("  invokestatic " + call.function + "/"
                    + functionHeaderInvoke(file, methodsTable, call.module, argumentList, returnType));
        }
    }

    private static String functionHeaderInvoke(PrintWriter file, Table methodsTable, String function,
            ASTArgumentList arguments, String returnType) {

        String functionHeader = function + "(";

        if (arguments != null) {
            for (int i = 0; i < arguments.jjtGetNumChildren(); i++) {
                ASTArgument argument = (ASTArgument) arguments.jjtGetChild(i);
                if (argument.type.equals("ID")) {

                    if (methodsTable.getVars() != null) {
                        Symbol s1 = methodsTable.getVars().get(argument.name);

                        if (s1 != null) {
                            String type = s1.type;

                            if (type.equals("int[]")) {
                                functionHeader = functionHeader + "[I";
                            }

                            else
                                functionHeader = functionHeader + "I";
                        }

                    }

                    if (methodsTable.getParams() != null) {
                        Symbol s1 = methodsTable.getParams().get(argument.name);

                        if (s1 != null) {
                            String type = s1.type;

                            if (type.equals("int[]")) {
                                functionHeader = functionHeader + "[I";
                            }

                            else
                                functionHeader = functionHeader + "I";
                        }
                    }

                    else
                        functionHeader = functionHeader + "I";
                } else if (argument.type.equals("String"))
                    functionHeader = functionHeader + "Ljava/lang/String;";
                else if (argument.type.equals("Integer"))
                    functionHeader = functionHeader + "I";
                else if (argument.jjtGetNumChildren() > 0) {

                    if (argument.jjtGetChild(0).getId() == JavammTreeConstants.JJTARGUMENTFUNCTIONCALL) {

                        ASTArgumentFunctionCall newFunc = (ASTArgumentFunctionCall) argument.jjtGetChild(0);
                        ASTArgumentList argumentList = null;

                        if (newFunc.jjtGetNumChildren() > 0
                                && newFunc.jjtGetChild(0).getId() == JavammTreeConstants.JJTARGUMENTLIST) {

                            argumentList = (ASTArgumentList) newFunc.jjtGetChild(0);

                            for (int j = 0; j < argumentList.jjtGetNumChildren(); j++) {
                                ASTArgument arg = (ASTArgument) argumentList.jjtGetChild(j);
                                printVariableLoad(file, methodsTable, arg.name, arg.type);
                            }
                        }

                        if (methodsTable.getVars() != null) {
                            if (methodsTable.getVars().get(newFunc.function) == null
                                    || table.getMethodsTable().get(newFunc.module) == null || !methodsTable.getVars()
                                            .get(newFunc.function).getType().equals(motherClassName)) {
                                file.println("  invokestatic " + newFunc.function + "/" + functionHeaderInvoke(file,
                                        methodsTable, newFunc.module, argumentList, returnType));
                            } else if ((methodsTable.getVars().get(newFunc.function) != null
                                    && methodsTable.getVars().get(newFunc.function).getType().equals(motherClassName))
                                    && table.getMethodsTable().get(newFunc.module) != null) {
                                file.println("  invokevirtual " + newFunc.function + "/" + functionHeaderInvoke(file,
                                        methodsTable, newFunc.module, argumentList, returnType));
                            }
                        }

                        else {
                            file.println("  invokestatic " + newFunc.function + "/" + functionHeaderInvoke(file,
                                    methodsTable, newFunc.module, argumentList, returnType));
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
                                printVariableLoad(file, methodsTable, arg.name, arg.type);
                            }
                        }

                        if (methodsTable.getVars() != null) {
                            if (table.getMethodsTable().get(newFunc.module) == null || !methodsTable.getVars()
                                    .get(newFunc.function).getType().equals(motherClassName)) {
                                file.println("  invokestatic " + newFunc.function + "/" + functionHeaderInvoke(file,
                                        methodsTable, newFunc.module, argumentList, returnType));
                            } else if (methodsTable.getVars().get(newFunc.function).getType().equals(motherClassName)
                                    && table.getMethodsTable().get(newFunc.module) != null) {
                                file.println("  invokevirtual " + newFunc.function + "/" + functionHeaderInvoke(file,
                                        methodsTable, newFunc.module, argumentList, returnType));
                            }
                        }

                        else {
                            if (table.getMethodsTable().get(newFunc.module) == null
                                    || !newFunc.function.equals(motherClassName)) {
                                file.println("  invokestatic " + newFunc.function + "/" + functionHeaderInvoke(file,
                                        methodsTable, newFunc.module, argumentList, returnType));
                            } else {
                                file.println("  invokevirtual " + newFunc.function + "/" + functionHeaderInvoke(file,
                                        methodsTable, newFunc.module, argumentList, returnType));
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
                    functionHeader = functionHeader + ")I";
                else if (returnSymbol.getType().equals("int[]"))
                    functionHeader = functionHeader + ")[I";
                else
                    functionHeader = functionHeader + ")V";

            } else { // from external module
                if (returnType.equals("void")) {
                    functionHeader = functionHeader + ")V";
                } else if (returnType.equals("int") || returnType.equals("boolean")) {
                    functionHeader = functionHeader + ")I";
                } else if (returnType.equals("int[]")) {
                    functionHeader = functionHeader + ")[I";
                }
            }
        }

        else
            functionHeader = functionHeader + ")V";

        return functionHeader;
    }

    private static String header(Table methodTable, SimpleNode methodNode, String methodName) {

        ASTMethodDeclaration cast_node = (ASTMethodDeclaration) methodNode;

        String functionHeader = methodName + "(";

        if (cast_node.jjtGetChild(1).getId() == JavammTreeConstants.JJTMETHODARGUMENTS) {
            ASTMethodArguments first_arg = (ASTMethodArguments) cast_node.jjtGetChild(1);

            if (first_arg.jjtGetNumChildren() > 0) {
                ASTType type_node = (ASTType) first_arg.jjtGetChild(0);

                if (type_node.type.equals("int") && type_node.isArray) {
                    functionHeader = functionHeader + "[I";
                }

                if (type_node.type.equals("int") && !type_node.isArray) {
                    functionHeader = functionHeader + "I";
                }

                if (type_node.type.equals("boolean")) {
                    functionHeader = functionHeader + "Z";
                }

                if (first_arg.jjtGetNumChildren() > 1) {
                    for (int i = 1; i < first_arg.jjtGetNumChildren(); i++) {
                        ASTMethodArgumentPiece child = (ASTMethodArgumentPiece) first_arg.jjtGetChild(i);

                        ASTType other_type = (ASTType) child.jjtGetChild(0);

                        if (other_type.type.equals("int") && other_type.isArray) {
                            functionHeader = functionHeader + "[I";
                        }

                        if (other_type.type.equals("int") && !other_type.isArray) {
                            functionHeader = functionHeader + "I";
                        }

                        if (other_type.type.equals("boolean")) {
                            functionHeader = functionHeader + "Z";
                        }

                    }
                }
            }
        }

        if (table.getMethodsTable().containsKey(methodName)) {
            Symbol s1 = table.getMethodsTable().get(methodName);

            if (s1.type.equals("void")) {
                functionHeader = functionHeader + ")V";
            }

            else if (s1.type.equals("int") || s1.type.equals("boolean")) {
                functionHeader = functionHeader + ")I";
            }

            else if (s1.type.equals("int[]")) {
                functionHeader = functionHeader + ")[I";
            }

            else {
                functionHeader = functionHeader + ")";
            }
        }

        return functionHeader;

    }

    private static void thisCallProcedure(Node child, PrintWriter file, int loop_nr, Table methodsTable) {
        ASTThisCall thisCall = (ASTThisCall) child;

        if (thisCall.jjtGetNumChildren() > 0) {
            // this.funct()
            ASTFunctionCall conveniece = new ASTFunctionCall(Integer.MAX_VALUE);

            if (thisCall.jjtGetChild(0).jjtGetNumChildren() > 0) {
                if (thisCall.jjtGetChild(0).jjtGetChild(0).getId() == JavammTreeConstants.JJTARGUMENTLIST) {
                    conveniece.jjtAddChild(thisCall.jjtGetChild(0).jjtGetChild(0), 0);
                }
            }

            conveniece.function = "this";
            conveniece.module = thisCall.module;

            methodCallToJvm(file, methodsTable, conveniece, "void");
        }
    }

    private static void handleTerms(Node term, PrintWriter file, int loop_nr, Table methodsTable) {
        // single term
        if (term.jjtGetNumChildren() == 0) {
            ASTTerm term1_cast = (ASTTerm) term;
            termToJvm(file, methodsTable, term1_cast);
        }

        else {

            // this.something
            if (term.jjtGetChild(0).getId() == JavammTreeConstants.JJTTHISCALL) {
                thisCallProcedure(term.jjtGetChild(0), file, loop_nr, methodsTable);
            }

            // variable.something
            if (term.jjtGetChild(0).getId() == JavammTreeConstants.JJTEXPRESSIONMETHOD) {
                if (term.jjtGetChild(0).jjtGetChild(0).getId() == JavammTreeConstants.JJTMETHODS) {
                    ASTTerm term_cast = (ASTTerm) term;
                    String function = term_cast.identifier;
                    ASTMethods method = (ASTMethods) term.jjtGetChild(0).jjtGetChild(0);
                    String module = method.module;

                    if (module.equals("length")) {
                        file.println("  arraylength");
                    }

                    else {
                        ASTFunctionCall conveniece = new ASTFunctionCall(0);

                        if (method.jjtGetNumChildren() > 0) {
                            if (method.jjtGetChild(0).getId() == JavammTreeConstants.JJTARGUMENTLIST) {
                                conveniece.jjtAddChild(method.jjtGetChild(0), 0);
                            }
                        }

                        conveniece.function = function;
                        conveniece.module = module;

                        methodCallToJvm(file, methodsTable, conveniece, "void");
                    }
                }
            }

        }
    }

    private static void writeStackNumber(Table methodsTable, String methodName, PrintWriter file) {
        int localsNr = methodsTable.getNumLocalVariables();

        file.println("  .limit stack " + 999);

        if (localsNr != 0) {
            file.println("  .limit locals " + localsNr);
        }
    }

    private static Table getMethodTable(String methodName) {
        Table methodsTable = new Table();

        HashMap<String, Symbol> vars = table.getLocalVariablesTable().get(methodName);
        HashMap<String, Symbol> params = table.getParametersTable().get(methodName);

        Symbol s1 = table.getReturnTable().get(methodName);

        methodsTable.setParameters(params);
        methodsTable.setVariables(vars);
        methodsTable.setReturn(s1);

        return methodsTable;
    }

}