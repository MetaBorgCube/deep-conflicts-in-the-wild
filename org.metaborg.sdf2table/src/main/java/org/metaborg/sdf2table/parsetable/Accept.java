package org.metaborg.sdf2table.parsetable;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

public class Accept extends Action {

    public Accept() {
    }

    @Override public IStrategoTerm toAterm(ITermFactory tf, ITableGenerator pt) {
        return tf.makeAppl(tf.makeConstructor("accept", 0));
    }
    
    @Override public String toString() {        
        return "accept()";
    }

    @Override public int hashCode() {
        return "accept".hashCode();
    }

    @Override public boolean equals(Object obj) {
        return true;
    }

}
