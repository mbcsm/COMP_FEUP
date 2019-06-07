import java.util.HashMap;
import java.util.Map;

class SymbolTable {
    static HashMap<String, HashMap<String, Symbol>> parameters;
    static HashMap<String, Symbol> classVariables;
    static HashMap<String, HashMap<String, Symbol>> localVariables;
    static HashMap<String, Symbol> returnTable;
    static HashMap<String, Symbol> methodsTable;

    SymbolTable() {
        parameters = new HashMap<String, HashMap<String, Symbol>>();
        classVariables = new HashMap<String, Symbol>();

        localVariables = new HashMap<String, HashMap<String, Symbol>>();
        methodsTable = new HashMap<String, Symbol>();
        returnTable = new HashMap<String, Symbol>();
    }

    public void addVariables(SimpleNode node) throws SemanticException {

        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            addVariables((SimpleNode) node.jjtGetChild(i));
        }

        if (node.getId() == JavammTreeConstants.JJTMAINDECLARATION && !methodsTable.containsKey("main")) {
            methodsTable.put("main", new Symbol("main", "void"));
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

            if (!methodsTable.containsKey(name_cast_node.getMethodName())) {
                methodsTable.put(name_cast_node.getMethodName(), new Symbol(name_cast_node.getMethodName(), type));
            } else
                throw new SemanticException("Method \"" + name_cast_node.getMethodName() + "\" already defined.");

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

                if (!parameters.containsKey(parent.methodName)) {
                    String varName = cast_node.getMethodArgumentName();
                    HashMap<String, Symbol> temp = new HashMap<String, Symbol>();
                    Symbol varSymbol = new Symbol(varName, type);

                    temp.put(varName, varSymbol);
                    parameters.put(parent.methodName, temp);
                } else {
                    HashMap<String, Symbol> tempParametersVariables = parameters.get(parent.methodName);
                    for (String _key : tempParametersVariables.keySet()) {
                        if (tempParametersVariables.get(cast_node.getMethodArgumentName()) != null)
                            throw new SemanticException(
                                    "Parameter <" + cast_node.getMethodArgumentName() + "> already defined.");
                    }

                    for (String key : localVariables.keySet()) {
                        HashMap<String, Symbol> _tempParametersVariables = parameters.get(key);
                        HashMap<String, Symbol> tempLocalVariables = localVariables.get(key);
                        for (String _key : tempLocalVariables.keySet()) {
                            if (_tempParametersVariables == null)
                                break;
                            if (_tempParametersVariables.get(_key) != null) {
                                throw new SemanticException("Parameter <" + _key + "> already defined.");
                            }
                        }
                    }
                    parameters.get(parent.methodName).put(cast_node.getMethodArgumentName(),
                            new Symbol(cast_node.getMethodArgumentName(), type));
                }
            }
            return;
        }

        if (node.getId() == JavammTreeConstants.JJTMETHODARGUMENTPIECE) {

            ASTMethodArguments mid = (ASTMethodArguments) node.jjtGetParent();
            ASTMethodDeclaration parent = (ASTMethodDeclaration) mid.jjtGetParent();
            ASTMethodArgumentPiece cast = (ASTMethodArgumentPiece) node;
            ASTType type_node = (ASTType) cast.jjtGetChild(0);

            String type = type_node.type;

            if (type_node.isArray) {
                type = type + "[]";
            }

            if (!parameters.containsKey(parent.methodName)) {

                String _name = cast.getMethodArgumentName();
                Symbol _symbol = new Symbol(_name, type);

                HashMap<String, Symbol> temp = new HashMap<String, Symbol>();
                temp.put(_name, _symbol);
                parameters.put(parent.methodName, temp);
            } else {
                parameters.get(parent.methodName).put(cast.getMethodArgumentName(),
                        new Symbol(cast.getMethodArgumentName(), type));
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

                    if (!classVariables.containsKey(cast_node.getVarName())) {

                        String type = type_node.type;
                        if (type_node.isArray) {
                            type = type + "[]";
                        }

                        classVariables.put(cast_node.getVarName(), new Symbol(cast_node.getVarName(), type));
                    } else {
                        throw new SemanticException("Class Variable <" + cast_node.getVarName() + "> already defined.");
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

                    if (temp != null && temp.getId() == JavammTreeConstants.JJTTRUE) {
                        if (!returnTable.containsKey("true")) {
                            returnTable.put(method_node.methodName, new Symbol("true", "boolean"));
                        }
                    }
                    if (temp != null && temp.getId() == JavammTreeConstants.JJTFALSE) {
                        if (!returnTable.containsKey("false")) {
                            returnTable.put(method_node.methodName, new Symbol("false", "boolean"));
                        }
                    }
                }
            }

            else {
                if (return_node.val != null) {
                    String key = "" + return_node.val;

                    if (!returnTable.containsKey(key)) {
                        returnTable.put(method_node.methodName, new Symbol(key, "int"));
                    }
                }

                if (return_node.identifier != "") {
                    if (!returnTable.containsKey(return_node.identifier)) {

                        Symbol _s = null;
                        if (localVariables.containsKey(method_node.methodName)) {
                            if (localVariables.get(method_node.methodName).containsKey(return_node.identifier)) {
                                _s = localVariables.get(method_node.methodName).get(return_node.identifier);
                            }
                        }

                        if (parameters.containsKey(method_node.methodName)) {
                            if (parameters.get(method_node.methodName).containsKey(return_node.identifier)) {
                                _s = localVariables.get(method_node.methodName).get(return_node.identifier);
                            }
                        }

                        if (classVariables.containsKey(return_node.identifier)) {
                            _s = localVariables.get(method_node.methodName).get(return_node.identifier);
                        }

                        returnTable.put(method_node.methodName, _s);
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

                    for (Map.Entry<String, HashMap<String, Symbol>> entry : localVariables.entrySet()) {
                        String key = entry.getKey();
                        HashMap<String, Symbol> value = entry.getValue();

                        if (value.containsKey(attr.name)) {
                            Symbol _s1 = value.get(attr.name);

                            if (node.jjtGetChild(0).jjtGetChild(1).getId() == JavammTreeConstants.JJTEXPRESSION) {
                                if (node.jjtGetChild(0).jjtGetChild(1).jjtGetChild(0)
                                        .getId() == JavammTreeConstants.JJTTRUE) {
                                    _s1.setBool("true");
                                } else if (node.jjtGetChild(0).jjtGetChild(1).jjtGetChild(0)
                                        .getId() == JavammTreeConstants.JJTFALSE) {
                                    _s1.setBool("false");
                                } else if (node.jjtGetChild(0).jjtGetChild(1).jjtGetChild(0)
                                        .getId() == JavammTreeConstants.JJTARITHM) {
                                    ASTArithm arithm = (ASTArithm) node.jjtGetChild(0).jjtGetChild(1).jjtGetChild(0);

                                    if (arithm.jjtGetNumChildren() > 0) {
                                        if (arithm.jjtGetChild(0).jjtGetNumChildren() > 0) {
                                            if (arithm.jjtGetChild(0).jjtGetChild(0).jjtGetNumChildren() > 0) {
                                                if (arithm.jjtGetChild(0).jjtGetChild(0).jjtGetChild(0)
                                                        .getId() == JavammTreeConstants.JJTTERM) {
                                                    ASTTerm term = (ASTTerm) arithm.jjtGetChild(0).jjtGetChild(0)
                                                            .jjtGetChild(0);

                                                    if (localVariables.get(key).containsKey(term.identifier)) {
                                                        Symbol _s2 = localVariables.get(key).get(term.identifier);
                                                        System.out.println(_s1.name + " = " + _s2.name);
                                                        _s1.setBool(_s2.getBool());
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

                ASTVarDeclaration cast_node = (ASTVarDeclaration) varDeclNode;
                ASTType type_node = (ASTType) cast_node.jjtGetChild(0);
                String type = type_node.type;

                if (type_node.isArray) {
                    type = type + "[]";
                }

                if (!localVariables.containsKey(key)) {
                    String varName = cast_node.getVarName();
                    Symbol varSymbol = new Symbol(varName, type);

                    HashMap<String, Symbol> temp = new HashMap<String, Symbol>();
                    temp.put(varName, varSymbol);
                    localVariables.put(key, temp);
                }

                else {
                    HashMap<String, Symbol> tempLocalVariables = localVariables.get(key);
                    for (String _key : tempLocalVariables.keySet()) {
                        if (tempLocalVariables.get(cast_node.getVarName()) != null)
                            throw new SemanticException(
                                    "Local Variable <" + cast_node.getVarName() + "> already defined.");
                    }

                    localVariables.get(key).put(cast_node.getVarName(), new Symbol(cast_node.getVarName(), type));
                }
            }
        }
    }

    public HashMap<String, Symbol> getReturnTable() {
        return returnTable;
    }

    public HashMap<String, HashMap<String, Symbol>> getParametersTable() {
        return parameters;
    }

    public HashMap<String, HashMap<String, Symbol>> getLocalVariablesTable() {
        return localVariables;
    }

    public HashMap<String, Symbol> getClassVariablesTable() {
        return classVariables;
    }

    public HashMap<String, Symbol> getMethodsTable() {
        return methodsTable;
    }

}