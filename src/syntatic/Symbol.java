class Symbol {
    String type = "", name = "", boolValue = "";
    int register;

    Symbol(String name, String type) {
        this.boolValue = "null";
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public void setBool(String bool) {
        boolValue = bool;
    }

    public String getBool() {
        return boolValue;
    }

    public void setRegister(int reg) {
        register = reg;
    }

    public int getRegister() {
        return register;
    }

}