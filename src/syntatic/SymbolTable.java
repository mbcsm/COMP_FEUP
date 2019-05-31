import java.util.HashMap;
import java.util.Map;

class SymbolTable {
    static HashMap<String, HashMap<String, Symbol>> localVariablesTable;
    static HashMap<String, Symbol> classVariablesTable;
    static HashMap<String, HashMap<String, Symbol>> parametersTable;
    static HashMap<String, Symbol> methodsTable;
    static HashMap<String, Symbol> returnTable;

    SymbolTable() {
        localVariablesTable = new HashMap<String, HashMap<String, Symbol>>();
        classVariablesTable = new HashMap<String, Symbol>();
        parametersTable = new HashMap<String, HashMap<String, Symbol>>();
        methodsTable = new HashMap<String, Symbol>();
        returnTable = new HashMap<String, Symbol>();
    }

    public void addVariables(SimpleNode node) throws SemanticException {

        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            addVariables((SimpleNode) node.jjtGetChild(i));
        }

        if (node.getId() == JavammTreeConstants.JJTMAINDECLARATION && !methodsTable.containsKey("main")) {
            methodsTable.put("main", new Symbol("main", "void"));

            // also get the local variables of that method for the method locals table
            getLocalVariablesLoop(node, "main");

            return;
        }

        if (node.getId() == JavammTreeConstants.JJTMETHODDECLARATION) {

            ASTMethodDeclaration method = (ASTMethodDeclaration) node;

            ASTType methodReturnType = (ASTType) node.jjtGetChild(0);

            String type = methodReturnType.type;

            if (methodReturnType.isArray) {
                type = type + "[]";
            }

            ASTMethodDeclaration name_cast_node = (ASTMethodDeclaration) node;

            // get the name of the method for the method name table
            if (!methodsTable.containsKey(name_cast_node.getMethodName())) {
                methodsTable.put(name_cast_node.getMethodName(), new Symbol(name_cast_node.getMethodName(), type));
            } else
                throw new SemanticException("Method \"" + name_cast_node.getMethodName() + "\" already defined.");

            // also get the local variables of that method for the method locals table
            getLocalVariablesLoop(node, method.methodName);

            return;
        }

        if (node.getId() == JavammTreeConstants.JJTMETHODARGUMENTS) {

            ASTMethodDeclaration parent = (ASTMethodDeclaration) node.jjtGetParent();

            ASTMethodArguments cast_node = (ASTMethodArguments) node;

            if (cast_node.jjtGetNumChildren() > 0) {
                ASTType type_node = (ASTType) cast_node.jjtGetChild(0);

                String type = type_node.type;

                if (type_node.isArray) {
                    type = type + "[]";
                }

                if (!parametersTable.containsKey(parent.methodName)) {

                    String varName = cast_node.getMethodArgumentName();
                    Symbol varSymbol = new Symbol(varName, type);

                    HashMap<String, Symbol> temp = new HashMap<String, Symbol>();

                    temp.put(varName, varSymbol);

                    parametersTable.put(parent.methodName, temp);
                } else {
                    // verificar se ja existe
                    HashMap<String, Symbol> tempParametersVariables = parametersTable.get(parent.methodName);
                    for (String keyb : tempParametersVariables.keySet()) {
                        if (tempParametersVariables.get(cast_node.getMethodArgumentName()) != null)
                            throw new SemanticException(
                                    "Parameter \"" + cast_node.getMethodArgumentName() + "\" already defined.");
                    }

                    for (String key : localVariablesTable.keySet()) {
                        HashMap<String, Symbol> tempParametersVariables2 = parametersTable.get(key);
                        HashMap<String, Symbol> tempLocalVariables = localVariablesTable.get(key);
                        for (String keyb : tempLocalVariables.keySet()) {
                            if (tempParametersVariables2 == null)
                                break;
                            if (tempParametersVariables2.get(keyb) != null) {
                                throw new SemanticException("Parameter \"" + keyb + "\" already defined.");
                            }
                        }
                    }
                    parametersTable.get(parent.methodName).put(cast_node.getMethodArgumentName(),
                            new Symbol(cast_node.getMethodArgumentName(), type));
                }
            }
            return;
        }

        if (node.getId() == JavammTreeConstants.JJTMETHODARGUMENTPIECE) {

            ASTMethodArguments intermediate = (ASTMethodArguments) node.jjtGetParent();
            ASTMethodDeclaration parent = (ASTMethodDeclaration) intermediate.jjtGetParent();
            ASTMethodArgumentPiece cast_node = (ASTMethodArgumentPiece) node;

            ASTType type_node = (ASTType) cast_node.jjtGetChild(0);

            String type = type_node.type;

            if (type_node.isArray) {
                type = type + "[]";
            }

            if (!parametersTable.containsKey(parent.methodName)) {

                String varName = cast_node.getMethodArgumentName();
                Symbol varSymbol = new Symbol(varName, type);

                HashMap<String, Symbol> temp = new HashMap<String, Symbol>();

                temp.put(varName, varSymbol);

                parametersTable.put(parent.methodName, temp);
            }

            else {
                parametersTable.get(parent.methodName).put(cast_node.getMethodArgumentName(),
                        new Symbol(cast_node.getMethodArgumentName(), type));
            }

            return;
        }

        if (node.getId() == JavammTreeConstants.JJTCLASSDECLARATION) {

            for (int i = 0; i < node.jjtGetNumChildren(); i++) {
                Node varDeclNode = node.jjtGetChild(i);

                if (varDeclNode != null && varDeclNode.getId() == JavammTreeConstants.JJTVARDECLARATION
                        && varDeclNode.jjtGetParent().getId() == JavammTreeConstants.JJTCLASSDECLARATION) {

                    ASTVarDeclaration cast_node = (ASTVarDeclaration) varDeclNode;

                    ASTType type_node = (ASTType) cast_node.jjtGetChild(0);

                    if (!classVariablesTable.containsKey(cast_node.getVarName())) {

                        String type = type_node.type;

                        if (type_node.isArray) {
                            type = type + "[]";
                        }

                        classVariablesTable.put(cast_node.getVarName(), new Symbol(cast_node.getVarName(), type));
                    } else {
                        throw new SemanticException(
                                "Class Variable \"" + cast_node.getVarName() + " \" already defined.");
                    }
                }
            }

        }

        return;
    }

    public void fillReturn(SimpleNode node) {

        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            fillReturn((SimpleNode) node.jjtGetChild(i));
        }

        if (node.getId() == JavammTreeConstants.JJTRETURN) {

            ASTMethodDeclaration method_node = (ASTMethodDeclaration) node.jjtGetParent();

            ASTReturn return_node = (ASTReturn) node;

            if (node.jjtGetNumChildren() > 0) {

                ASTExpression expression_cast_node = (ASTExpression) node.jjtGetChild(0);

                for (int i = 0; i < expression_cast_node.jjtGetNumChildren(); i++) {
                    Node temp = expression_cast_node.jjtGetChild(i);

                    // If the return is true
                    if (temp != null && temp.getId() == JavammTreeConstants.JJTTRUE) {
                        if (!returnTable.containsKey("true")) {
                            returnTable.put(method_node.methodName, new Symbol("true", "boolean"));
                        }
                    }

                    // If the return is false
                    if (temp != null && temp.getId() == JavammTreeConstants.JJTFALSE) {
                        if (!returnTable.containsKey("false")) {
                            returnTable.put(method_node.methodName, new Symbol("false", "boolean"));
                        }
                    }
                }
            }

            else {

                // If the return is a number or variable
                if (return_node.val != null) {

                    String key = "" + return_node.val;

                    if (!returnTable.containsKey(key)) {
                        returnTable.put(method_node.methodName, new Symbol(key, "int"));
                    }
                }

                if (return_node.identifier != "") {
                    if (!returnTable.containsKey(return_node.identifier)) {

                        Symbol s1 = null;

                        if (localVariablesTable.containsKey(method_node.methodName)) {
                            if (localVariablesTable.get(method_node.methodName).containsKey(return_node.identifier)) {
                                s1 = localVariablesTable.get(method_node.methodName).get(return_node.identifier);
                            }
                        }

                        if (parametersTable.containsKey(method_node.methodName)) {
                            if (parametersTable.get(method_node.methodName).containsKey(return_node.identifier)) {
                                s1 = localVariablesTable.get(method_node.methodName).get(return_node.identifier);
                            }
                        }

                        if (classVariablesTable.containsKey(return_node.identifier)) {
                            s1 = localVariablesTable.get(method_node.methodName).get(return_node.identifier);
                        }

                        returnTable.put(method_node.methodName, s1);
                    }
                }
            }

            return;
        }

    }

    public void booleanAttribution(SimpleNode node) {

        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            booleanAttribution((SimpleNode) node.jjtGetChild(i));
        }

        if (node.getId() == JavammTreeConstants.JJTSTATEMENT
                && (node.jjtGetParent().getId() == JavammTreeConstants.JJTMAINDECLARATION
                        || node.jjtGetParent().getId() == JavammTreeConstants.JJTMETHODDECLARATION)) {
            if (node.jjtGetNumChildren() > 0) {
                if (node.jjtGetChild(0).getId() == JavammTreeConstants.JJTATTRIBUTION) {
                    ASTAttributionHead attr = (ASTAttributionHead) node.jjtGetChild(0).jjtGetChild(0);

                    for (Map.Entry<String, HashMap<String, Symbol>> entry : localVariablesTable.entrySet()) {
                        String key = entry.getKey();
                        HashMap<String, Symbol> value = entry.getValue();

                        if (value.containsKey(attr.name)) {
                            Symbol s1 = value.get(attr.name);

                            if (node.jjtGetChild(0).jjtGetChild(1).getId() == JavammTreeConstants.JJTEXPRESSION) {
                                // s1 = true
                                if (node.jjtGetChild(0).jjtGetChild(1).jjtGetChild(0)
                                        .getId() == JavammTreeConstants.JJTTRUE) {
                                    s1.setBool("true");
                                }

                                // s1 = false
                                else if (node.jjtGetChild(0).jjtGetChild(1).jjtGetChild(0)
                                        .getId() == JavammTreeConstants.JJTFALSE) {
                                    s1.setBool("false");
                                }

                                // s1 = s2 and s2 is also boolean
                                else if (node.jjtGetChild(0).jjtGetChild(1).jjtGetChild(0)
                                        .getId() == JavammTreeConstants.JJTARITHM) {
                                    ASTArithm arithm = (ASTArithm) node.jjtGetChild(0).jjtGetChild(1).jjtGetChild(0);

                                    if (arithm.jjtGetNumChildren() > 0) {
                                        if (arithm.jjtGetChild(0).jjtGetNumChildren() > 0) {
                                            if (arithm.jjtGetChild(0).jjtGetChild(0).jjtGetNumChildren() > 0) {
                                                if (arithm.jjtGetChild(0).jjtGetChild(0).jjtGetChild(0)
                                                        .getId() == JavammTreeConstants.JJTTERM) {
                                                    ASTTerm term = (ASTTerm) arithm.jjtGetChild(0).jjtGetChild(0)
                                                            .jjtGetChild(0);

                                                    if (localVariablesTable.get(key).containsKey(term.identifier)) {
                                                        Symbol s2 = localVariablesTable.get(key).get(term.identifier);
                                                        System.out.println(s1.name + " = " + s2.name);
                                                        s1.setBool(s2.getBool());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void getLocalVariablesLoop(SimpleNode node, String key) throws SemanticException {

        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            Node varDeclNode = node.jjtGetChild(i);

            if (varDeclNode != null && varDeclNode.getId() == JavammTreeConstants.JJTVARDECLARATION) {

                ASTVarDeclaration var_cast_node = (ASTVarDeclaration) varDeclNode;
                ASTType type_node = (ASTType) var_cast_node.jjtGetChild(0);

                String type = type_node.type;

                if (type_node.isArray) {
                    type = type + "[]";
                }

                if (!localVariablesTable.containsKey(key)) {

                    String varName = var_cast_node.getVarName();
                    Symbol varSymbol = new Symbol(varName, type);

                    HashMap<String, Symbol> temp = new HashMap<String, Symbol>();

                    temp.put(varName, varSymbol);

                    localVariablesTable.put(key, temp);
                }

                else {
                    HashMap<String, Symbol> tempLocalVariables = localVariablesTable.get(key);

                    for (String keyb : tempLocalVariables.keySet()) {
                        if (tempLocalVariables.get(var_cast_node.getVarName()) != null)
                            throw new SemanticException(
                                    "Local Variable \"" + var_cast_node.getVarName() + "\" already defined.");
                    }

                    localVariablesTable.get(key).put(var_cast_node.getVarName(),
                            new Symbol(var_cast_node.getVarName(), type));
                }
            }
        }
    }

    public HashMap<String, Symbol> getMethodsTable() {
        return methodsTable;
    }

    public HashMap<String, HashMap<String, Symbol>> getParametersTable() {
        return parametersTable;
    }

    public HashMap<String, Symbol> getClassVariablesTable() {
        return classVariablesTable;
    }

    public HashMap<String, HashMap<String, Symbol>> getLocalVariablesTable() {
        return localVariablesTable;
    }

    public HashMap<String, Symbol> getReturnTable() {
        return returnTable;
    }
}