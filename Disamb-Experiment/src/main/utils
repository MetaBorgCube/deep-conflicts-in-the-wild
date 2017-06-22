package utils;

import org.metaborg.sdf2table.grammar.ConstructorAttribute;
import org.metaborg.sdf2table.grammar.ContextFreeSymbol;
import org.metaborg.sdf2table.grammar.Symbol;

public class UniqueConstructor {
    
    private Symbol s;
    private ConstructorAttribute cons;
    // for the Pattern match case where the production is duplicated
    private boolean emptyLongestMatch;

    public UniqueConstructor(Symbol s, ConstructorAttribute cons) {
        if(s instanceof ContextFreeSymbol) {
            this.s = ((ContextFreeSymbol) s).getSymbol();
        } else {
        this.s = s;
        }
        this.cons = cons;
        this.emptyLongestMatch = false;
    }
    
    public UniqueConstructor(Symbol s, ConstructorAttribute cons, boolean emptyLongestMatch) {
        if(s instanceof ContextFreeSymbol) {
            this.s = ((ContextFreeSymbol) s).getSymbol();
        } else {
        this.s = s;
        }
        this.cons = cons;
        this.setEmptyLongestMatch(emptyLongestMatch);
    }

    public Symbol getSymbol() {
        return s;
    }

    public void setSymbol(Symbol s) {
        this.s = s;
    }

    public ConstructorAttribute getCons() {
        return cons;
    }

    public void setCons(ConstructorAttribute cons) {
        this.cons = cons;
    }
        
    public boolean isEmptyLongestMatch() {
        return emptyLongestMatch;
    }

    public void setEmptyLongestMatch(boolean emptyLongestMatch) {
        this.emptyLongestMatch = emptyLongestMatch;
    }

    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((cons == null) ? 0 : cons.hashCode());
        result = prime * result + (emptyLongestMatch ? 1231 : 1237);
        result = prime * result + ((s == null) ? 0 : s.hashCode());
        return result;
    }

    @Override public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        UniqueConstructor other = (UniqueConstructor) obj;
        if(cons == null) {
            if(other.cons != null)
                return false;
        } else if(!cons.equals(other.cons))
            return false;
        if(emptyLongestMatch != other.emptyLongestMatch)
            return false;
        if(s == null) {
            if(other.s != null)
                return false;
        } else if(!s.equals(other.s))
            return false;
        return true;
    }

    @Override public String toString() {
        // TODO Auto-generated method stub
        return s + "." + cons;
    }

}
