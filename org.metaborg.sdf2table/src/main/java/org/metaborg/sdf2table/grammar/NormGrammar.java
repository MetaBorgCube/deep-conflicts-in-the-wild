package org.metaborg.sdf2table.grammar;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.metaborg.sdf2table.parsetable.ContextualProduction;
import org.metaborg.sdf2table.parsetable.ContextualSymbol;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

public class NormGrammar implements INormGrammar, Serializable {
    
    private static final long serialVersionUID = -13739894962185282L;

    // all files used in this grammar
    public Set<File> sdf3_files;

    public IProduction initial_prod;
    
    // to handle Sort.Cons in priorities
    public Map<ProductionReference, IProduction> sort_cons_prods;
    
    // merging same productions with different attributes
    public SetMultimap<IProduction, IAttribute> prod_attrs;

    // necessary for calculating deep priority conflicts
    public Map<UniqueProduction, IProduction> prods;
    public BiMap<IProduction, ContextualProduction> contextual_prods;
    public Set<ContextualProduction> derived_contextual_prods;
    public Set<ContextualSymbol> contextual_symbols;
    public SetMultimap<Symbol, Symbol> leftRecursive;
    public SetMultimap<Symbol, Symbol> rightRecursive;
    public SetMultimap<Symbol, IProduction> longest_match_prods;    
    BiMap<IProduction, Integer> prod_labels;
    
    // priorities
    public Set<IPriority> transitive_prio;
    public Set<IPriority> non_transitive_prio;
    SetMultimap<IPriority, Integer> prios;

    // extra collections to calculate the transitive closure
    public Set<IProduction> prio_prods;
    public SetMultimap<IPriority, Integer> trans_prio_arguments;
    public SetMultimap<IPriority, Integer> non_trans_prio_arguments;

    public HashMap<String, Symbol> symbols_read; // caching symbols read
    public HashMap<String, IProduction> productions_read; // caching productions read

    // get all productions for a certain symbol
    public SetMultimap<Symbol, IProduction> symbol_prods;
    
    public NormGrammar() {
        this.sdf3_files = Sets.newHashSet();
        this.prods = Maps.newHashMap();
        this.sort_cons_prods = Maps.newHashMap();
        this.contextual_prods = HashBiMap.create();
        this.leftRecursive = HashMultimap.create();
        this.rightRecursive = HashMultimap.create();
        this.derived_contextual_prods = Sets.newHashSet();
        this.contextual_symbols = Sets.newHashSet();
        this.longest_match_prods = HashMultimap.create();
        this.prod_attrs = HashMultimap.create();
        this.prios = HashMultimap.create();
        this.transitive_prio = Sets.newHashSet();
        this.non_transitive_prio = Sets.newHashSet();
        this.prio_prods = Sets.newHashSet();
        this.trans_prio_arguments = HashMultimap.create();
        this.non_trans_prio_arguments = HashMultimap.create();
        this.symbol_prods = HashMultimap.create();
        this.symbols_read = Maps.newHashMap();
        this.productions_read = Maps.newHashMap();
    }


    @Override public Map<UniqueProduction, IProduction> syntax() {
        return prods;
    }


    @Override public SetMultimap<IPriority, Integer> priorities() {
        return prios;
    }

    public void priorityTransitiveClosure() {
        prios = HashMultimap.create();

        // Floyd Warshall Algorithm to calculate the transitive closure
        for(IProduction intermediate_prod : prio_prods) {
            for(IProduction first_prod : prio_prods) {
                for(IProduction second_prod : prio_prods) {
                    IPriority first_sec = new Priority(first_prod, second_prod, true);
                    IPriority first_k = new Priority(first_prod, intermediate_prod, true);
                    IPriority k_second = new Priority(intermediate_prod, second_prod, true);
                    // if there is no priority first_prod > second_prod
                    if(!transitive_prio.contains(first_sec)) {
                        // if there are priorities first_prod > intermediate_prod and
                        // intermediate_prod > second_prod
                        // add priority first_prod > second_prod
                        if(transitive_prio.contains(first_k) && transitive_prio.contains(k_second)) {
                            transitive_prio.add(first_sec);
                            trans_prio_arguments.putAll(first_sec, trans_prio_arguments.get(first_k));
                        }
                    } else {
                        if(transitive_prio.contains(first_k) && transitive_prio.contains(k_second)) {
                            trans_prio_arguments.putAll(first_sec, trans_prio_arguments.get(first_k));
                        }
                    }
                }
            }
        }

        prios.putAll(non_trans_prio_arguments);
        prios.putAll(trans_prio_arguments);
    }


    public BiMap<IProduction, Integer> getProdLabels() {
        return prod_labels;
    }


    public void setProdLabels(BiMap<IProduction, Integer> prod_labels) {
        this.prod_labels = prod_labels;
    }    

}
