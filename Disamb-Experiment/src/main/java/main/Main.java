package main;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.PropertyConfigurator;
import org.metaborg.sdf2table.grammar.ConstructorAttribute;
import org.metaborg.sdf2table.grammar.ContextFreeSymbol;
import org.metaborg.sdf2table.grammar.IAttribute;
import org.metaborg.sdf2table.grammar.IPriority;
import org.metaborg.sdf2table.grammar.IProduction;
import org.metaborg.sdf2table.grammar.IterStarSymbol;
import org.metaborg.sdf2table.grammar.IterSymbol;
import org.metaborg.sdf2table.grammar.Priority;
import org.metaborg.sdf2table.grammar.ProductionReference;
import org.metaborg.sdf2table.grammar.Sort;
import org.metaborg.sdf2table.grammar.Symbol;
import org.metaborg.sdf2table.parsetable.Context;
import org.metaborg.sdf2table.parsetable.ContextPosition;
import org.metaborg.sdf2table.parsetable.ContextType;
import org.metaborg.sdf2table.parsetable.ContextualProduction;
import org.metaborg.sdf2table.parsetable.ContextualSymbol;
import org.metaborg.sdf2table.parsetable.ParseTableGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.InvalidParseTableException;
import org.spoofax.jsglr.client.MultiBadTokenException;
import org.spoofax.jsglr.client.ParseTable;
import org.spoofax.jsglr.client.SGLRParseResult;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.jsglr.client.imploder.TermTreeFactory;
import org.spoofax.jsglr.client.imploder.TreeBuilder;
import org.spoofax.jsglr.io.SGLR;
import org.spoofax.terms.TermFactory;
import org.spoofax.terms.attachments.ParentAttachment;
import org.spoofax.terms.attachments.ParentTermFactory;
import org.strategoxt.stratego_aterm.aterm_escape_strings_0_0;
import org.strategoxt.stratego_aterm.pp_aterm_box_0_0;
import org.strategoxt.stratego_gpp.box2text_string_0_1;

import com.google.common.base.Charsets;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import utils.UniqueConstructor;

public class Main {

    private final static ITermFactory tf = new TermFactory();
    private static Logger debugLogger = LoggerFactory.getLogger(Main.class);
    private static org.apache.log4j.Logger resultLogger = org.apache.log4j.Logger.getLogger("reportsLogger");
    private static org.apache.log4j.Logger JAVAresultLogger = org.apache.log4j.Logger.getLogger("JAVAreportsLogger");
    private final static boolean JAVA = true;
    private final static boolean OCAML = true;
    private final static boolean TESTING = false;
    private final static boolean TABLECREATION = true;
    private final static String testingFile = "disamb.ml";
    private final static String testingJavaFile = "disamb.java";

    public static void main(String[] args) {
    	PropertyConfigurator.configure("src/main/resources/log4j.properties");
        // tests for Java
        try {
            if(JAVA) {
                runJavaExperiment();
            }
        } catch(Exception e) {
            debugLogger.error("OCaml experiment failed with exception\n{}", e.getMessage());
            if(e instanceof MultiBadTokenException) {
                MultiBadTokenException mbe = (MultiBadTokenException) e;
                debugLogger.error("parsing failed at line {}", mbe.getLineNumber());
            }
        }

        // tests for OCaml
        try {
            if(OCAML) {
                runOCamlExperiment();
            }
        } catch(Exception e) {
            debugLogger.error("OCaml experiment failed with exception\n{}", e.getMessage());
            e.printStackTrace();
            if(e instanceof MultiBadTokenException) {
                MultiBadTokenException mbe = (MultiBadTokenException) e;
                debugLogger.error("parsing failed at line {}", mbe.getLineNumber());
            }
        }


    }

    private static void runJavaExperiment() throws Exception {
        // Go through all Java files:
        // count LOC
        // parse them
        // count AST nodes
        // parse them using a new dynamic parse table and the same parse table as the other programs
        // parse them disabling contextual productions and count ambiguities
        // count disambiguation by brackets
        org.apache.log4j.Logger filesLogger = org.apache.log4j.Logger.getLogger("JAVAfilesLogger");
        org.apache.log4j.Logger probfilesLogger = org.apache.log4j.Logger.getLogger("JAVAproblematicfilesLogger");


        int filesParsed = 0;

        debugLogger.info("-------------------------------------");
        debugLogger.info("Creating the list of all Java Files:");
        File oCamlDir = new File("test/Java/");
        Collection<File> javaFiles = FileUtils.listFiles(oCamlDir, new String[] { "java" }, true);
        debugLogger.info("{} files created.", javaFiles.size()-1);
        debugLogger.info("-------------------------------------");

        final TermTreeFactory factory = new TermTreeFactory(new ParentTermFactory(tf));
        File mainJavaFile = new File("normalizedGrammars/Java/normalized/java-front-norm.aterm");
        ParseTableGenerator ptg = new ParseTableGenerator(mainJavaFile, null, null, null,
            Lists.newArrayList("normalizedGrammars/Java"), false);

        IStrategoTerm ptAterm, noOperatorAmbPTAterm, noDanglingElsePTAterm, noLongestMatchPTAterm;
        ParseTableGenerator ambiguousGrammarPtg, noOperatorAmbPtg, noDanglingElsePtg, noLongestMatchPtg,
            onlyOperatorAmbPtg, onlyDanglingElsePtg, onlyLongestMatchPtg;
        SGLR parserAllFiles, noOperatorAmbParser, noDanglingElseParser, noLongestMatchParser;
        ParseTable pt, noOperatorAmbPT, noDanglingElsePT, noLongestMatchPT;

        try {
            ptAterm = ptg.createDynamicTable(true, true, true, true);

            ambiguousGrammarPtg = new ParseTableGenerator(mainJavaFile, null, null, null,
                Lists.newArrayList("normalizedGrammars/Java"), false);
            ambiguousGrammarPtg.createDynamicTable(false, false, false, true);

            noOperatorAmbPtg = new ParseTableGenerator(mainJavaFile, null, null, null,
                Lists.newArrayList("normalizedGrammars/Java"), false);
            noOperatorAmbPTAterm = noOperatorAmbPtg.createDynamicTable(false, true, true, true);

            noDanglingElsePtg = new ParseTableGenerator(mainJavaFile, null, null, null,
                Lists.newArrayList("normalizedGrammars/Java"), false);
            noDanglingElsePTAterm = noDanglingElsePtg.createDynamicTable(true, false, true, true);

            noLongestMatchPtg = new ParseTableGenerator(mainJavaFile, null, null, null,
                Lists.newArrayList("normalizedGrammars/Java"), false);
            noLongestMatchPTAterm = noLongestMatchPtg.createDynamicTable(true, true, false, true);

            onlyOperatorAmbPtg = new ParseTableGenerator(mainJavaFile, null, null, null,
                Lists.newArrayList("normalizedGrammars/Java"), false);
            onlyOperatorAmbPtg.createDynamicTable(true, false, false, true);

            onlyDanglingElsePtg = new ParseTableGenerator(mainJavaFile, null, null, null,
                Lists.newArrayList("normalizedGrammars/Java"), false);
            onlyDanglingElsePtg.createDynamicTable(false, true, false, true);

            onlyLongestMatchPtg = new ParseTableGenerator(mainJavaFile, null, null, null,
                Lists.newArrayList("normalizedGrammars/Java"), false);
            onlyLongestMatchPtg.createDynamicTable(false, false, true, true);



            pt = new ParseTable(ptAterm, tf, ptg.getGrammar());
            noOperatorAmbPT = new ParseTable(noOperatorAmbPTAterm, tf, noOperatorAmbPtg.getGrammar());
            noDanglingElsePT = new ParseTable(noDanglingElsePTAterm, tf, noDanglingElsePtg.getGrammar());
            noLongestMatchPT = new ParseTable(noLongestMatchPTAterm, tf, noLongestMatchPtg.getGrammar());


            parserAllFiles = new SGLR(new TreeBuilder(factory), pt);
            noOperatorAmbParser = new SGLR(new TreeBuilder(factory), noOperatorAmbPT);
            noDanglingElseParser = new SGLR(new TreeBuilder(factory), noDanglingElsePT);
            noLongestMatchParser = new SGLR(new TreeBuilder(factory), noLongestMatchPT);

            setParser(parserAllFiles);

        } catch(Exception e) {
            debugLogger.info("Failed to create the parse table");
            return;
        }

        int i = 0;

        for(File f : javaFiles) {
            // if testing, only use disamb.ml, if not, skip disamb.ml
            if(TESTING) {
                if(!f.getName().equals(testingJavaFile)) {
                    continue;
                }
            } else {
                if(f.getName().equals(testingJavaFile)) {
                    continue;
                }
            }

            // FIXME the files below cause a stack overflow when imploding
            if(checkProblematicFiles(f, debugLogger, JAVAresultLogger)) {
                probfilesLogger.info(f.getAbsolutePath());
                continue;
            }

            if(i != 0) {
                debugLogger.info("");
                JAVAresultLogger.info("\n");
            }
            i++;

            debugLogger.info(f.getAbsolutePath());
            JAVAresultLogger.info(f.getName() + ";");

            long lineCount;
            String input;
            try {
                input = FileUtils.readFileToString(f, Charsets.UTF_8);
                lineCount = getLineCount(f);
            } catch(IOException e) {
                debugLogger.info("Could not open file.");
                JAVAresultLogger.info("could not open file");
                e.printStackTrace();
                probfilesLogger.info(f.getAbsolutePath());
                continue;
            }

            debugLogger.info("{} lines.", lineCount);
            JAVAresultLogger.info(lineCount + ";");

            ParseTable individualPT;
            try {
                individualPT = new ParseTable(ptAterm, tf, ptg.getGrammar());
            } catch(InvalidParseTableException e) {
                debugLogger.info("Could not create individual parse table.");
                JAVAresultLogger.info("could not create individual parse table;");
                e.printStackTrace();
                probfilesLogger.info(f.getAbsolutePath());
                continue;
            }

            SGLR individualParser = new SGLR(new TreeBuilder(factory), individualPT);


            SGLRParseResult parseResult;
            try {
                parseResult = parserAllFiles.parse(input, f.getName(), "Start");
            } catch(Exception e) {
                debugLogger.info("Parsing failed with exception {}", e.getMessage());
                JAVAresultLogger.info("parsing failed.");
                e.printStackTrace();
                probfilesLogger.info(f.getAbsolutePath());
                continue;
            }

            SGLRParseResult individualParseResult;
            try {
                individualParseResult = individualParser.parse(input, f.getName(), "Start");
            } catch(Exception e) {
                debugLogger.info("Parsing failed with exception {}", e.getMessage());
                JAVAresultLogger.info("parsing failed.");
                e.printStackTrace();
                probfilesLogger.info(f.getAbsolutePath());
                continue;
            }

            SGLRParseResult noOperatorAmbResult;
            try {
                noOperatorAmbResult = noOperatorAmbParser.parse(input, f.getName(), "Start");
            } catch(Exception e) {
                debugLogger.info("Operator-style ambiguity parser failed with exception {}", e.getMessage());
                JAVAresultLogger.info("operator-style parsing failed.");
                e.printStackTrace();
                probfilesLogger.info(f.getAbsolutePath());
                continue;
            }

            SGLRParseResult noDanglingElseResult;
            try {
                noDanglingElseResult = noDanglingElseParser.parse(input, f.getName(), "Start");
            } catch(Exception e) {
                debugLogger.info("Dangling else parser failed with exception {}", e.getMessage());
                JAVAresultLogger.info("dangling else parsing failed.");
                e.printStackTrace();
                probfilesLogger.info(f.getAbsolutePath());
                continue;
            }

            SGLRParseResult noLongestMatchResult;
            try {
                noLongestMatchResult = noLongestMatchParser.parse(input, f.getName(), "Start");
            } catch(Exception e) {
                debugLogger.info("Longest match parser failed with exception {}", e.getMessage());
                JAVAresultLogger.info("longest match parsing failed.");
                e.printStackTrace();
                probfilesLogger.info(f.getAbsolutePath());
                continue;
            }

            IStrategoTerm ast = (IStrategoTerm) parseResult.output;
            IStrategoTerm individualAst = (IStrategoTerm) individualParseResult.output;
            IStrategoTerm noOperatorAmbAst = (IStrategoTerm) noOperatorAmbResult.output;
            IStrategoTerm noDanglingElseAst = (IStrategoTerm) noDanglingElseResult.output;
            IStrategoTerm noLongestMatchAst = (IStrategoTerm) noLongestMatchResult.output;

            if(!ast.equals(individualAst)) {
                debugLogger.info("AST from local parser is different from AST from global parser.");
                JAVAresultLogger.info("local and global asts are different;");
                probfilesLogger.info(f.getAbsolutePath());
                continue;
            }

            long ambs = parserAllFiles.getAmbiguitiesCount();
            long opStyleAmbs = noOperatorAmbParser.getAmbiguitiesCount();
            long danglingElseAmbs = noDanglingElseParser.getAmbiguitiesCount();
            long longestMatchAmbs = noLongestMatchParser.getAmbiguitiesCount();

            long opStyleUniqueAmbs = 0, danglingElseUniqueAmbs = 0, longestMatchUniqueAmbs = 0;
            long opStyleTopLevelAmbs = 0, danglingElseTopLevelAmbs = 0, longestMatchTopLevelAmbs = 0;

            if(ambs != 0) {
                debugLogger.info("Original input contains ambiguities");
                JAVAresultLogger.info("input contains ambiguities;");
                probfilesLogger.info(f.getAbsolutePath());
                continue;
            }

            if(opStyleAmbs != 0) {
                opStyleUniqueAmbs = collectAmbs(noOperatorAmbAst).size(); // (f, noOperatorAmbAst, "operator-style");
                opStyleTopLevelAmbs = exportAmbiguities(f, noOperatorAmbAst, "operator-style");
            }

            if(danglingElseAmbs != 0) {
                danglingElseUniqueAmbs = collectAmbs(noDanglingElseAst).size();// exportAmbiguities(f,
                                                                               // noDanglingElseAst, "dangling-else");
                danglingElseTopLevelAmbs = exportAmbiguities(f, noDanglingElseAst, "dangling-else");
            }

            if(longestMatchAmbs != 0) {
                longestMatchUniqueAmbs = collectAmbs(noLongestMatchAst).size();// exportAmbiguities(f,
                                                                               // noLongestMatchAst, "longest-match");
                longestMatchTopLevelAmbs = exportAmbiguities(f, noLongestMatchAst, "longest-match");
            }

            long nodeCount = getNodeCount(ast);
            long totalStates = individualPT.getPTgenerator().totalStates();
            long processedStates = individualPT.getPTgenerator().getProcessedStates();
            long productionsUsed = individualParser.numberOfProductionsUsedSingleParse();
            debugLogger.info("{} ast nodes.", nodeCount);
            debugLogger.info("Visited {} states.", totalStates);
            debugLogger.info("And processed {} states.", processedStates);
            debugLogger.info("Used {} productions.", productionsUsed);

            JAVAresultLogger.info(nodeCount + ";");
            JAVAresultLogger.info(totalStates + ";");
            JAVAresultLogger.info(processedStates + ";");
            JAVAresultLogger.info(productionsUsed + ";");

            debugLogger.info("{} ambiguities.", ambs);
            debugLogger.info("{} operator-style top-level ambiguities.", opStyleTopLevelAmbs);
            debugLogger.info("{} dangling else top-level ambiguities.", danglingElseTopLevelAmbs);
            debugLogger.info("{} longest match top-level ambiguities.", longestMatchTopLevelAmbs);
            debugLogger.info("{} operator-style ambiguities.", opStyleUniqueAmbs);
            debugLogger.info("{} dangling else ambiguities.", danglingElseUniqueAmbs);
            debugLogger.info("{} longest match ambiguities.", longestMatchUniqueAmbs);

            JAVAresultLogger.info(opStyleTopLevelAmbs + ";");
            JAVAresultLogger.info(danglingElseTopLevelAmbs + ";");
            JAVAresultLogger.info(longestMatchTopLevelAmbs + ";");
            JAVAresultLogger.info(opStyleUniqueAmbs + ";");
            JAVAresultLogger.info(danglingElseUniqueAmbs + ";");
            JAVAresultLogger.info(longestMatchUniqueAmbs + ";");

            countBrackets(ast, ptg, debugLogger, JAVAresultLogger);

            filesLogger.info(f.getAbsolutePath());
            filesParsed++;
        }

        long finalStates = pt.getPTgenerator().totalStates();
        long finalProcessedStates = pt.getPTgenerator().getProcessedStates();
        long finalProductionsUsed = parserAllFiles.numberOfProductionsUsedAllParses();

        long totalProductions = ptg.labels().size();
        long noOperatorAmbTotalProductions = noOperatorAmbPtg.labels().size();
        long noDanglingElseTotalProductions = noDanglingElsePtg.labels().size();
        long noLongestMatchProductions = noLongestMatchPtg.labels().size();
        long onlyOperatorAmbTotalProductions = onlyOperatorAmbPtg.labels().size();
        long onlyDanglingElseTotalProductions = onlyDanglingElsePtg.labels().size();
        long onlyLongestMatchProductions = onlyLongestMatchPtg.labels().size();
        long ambiguousGrammartotalProductions = ambiguousGrammarPtg.labels().size();

        long totalStates;
        long noOperatorAmbTotalStates;
        long noDanglingElseTotalStates;
        long noLongestMatchTotalStates;
        long onlyOperatorAmbTotalStates;
        long onlyDanglingElseTotalStates;
        long onlyLongestMatchTotalStates;
        long ambiguousGrammarTotalStates;

        if(!TABLECREATION) {
            totalStates = 0;
            noOperatorAmbTotalStates = 0;
            noDanglingElseTotalStates = 0;
            noLongestMatchTotalStates = 0;
            onlyOperatorAmbTotalStates = 0;
            onlyDanglingElseTotalStates = 0;
            onlyLongestMatchTotalStates = 0;
            ambiguousGrammarTotalStates = 0;

        } else {
            ptg.createCompleteTable();
            noOperatorAmbPtg.createCompleteTable();
            noDanglingElsePtg.createCompleteTable();
            noLongestMatchPtg.createCompleteTable();
            onlyOperatorAmbPtg.createCompleteTable();
            onlyDanglingElsePtg.createCompleteTable();
            onlyLongestMatchPtg.createCompleteTable();
            ambiguousGrammarPtg.createCompleteTable();


            totalStates = ptg.totalStates();
            noOperatorAmbTotalStates = noOperatorAmbPtg.totalStates();
            noDanglingElseTotalStates = noDanglingElsePtg.totalStates();
            noLongestMatchTotalStates = noLongestMatchPtg.totalStates();
            onlyOperatorAmbTotalStates = onlyOperatorAmbPtg.totalStates();
            onlyDanglingElseTotalStates = onlyDanglingElsePtg.totalStates();
            onlyLongestMatchTotalStates = onlyLongestMatchPtg.totalStates();
            ambiguousGrammarTotalStates = ambiguousGrammarPtg.totalStates();
        }


        debugLogger.info("");
        debugLogger.info("-------------------------------------");
        debugLogger.info("The grammar that solves all deep conflicts has {} productions and {} states.",
            totalProductions, totalStates);
        debugLogger.info("The grammar that solves all conflicts but operator-style has {} productions and {} states.",
            noOperatorAmbTotalProductions, noOperatorAmbTotalStates);
        debugLogger.info("The grammar that solves all conflicts but dangling else has {} productions and {} states.",
            noDanglingElseTotalProductions, noDanglingElseTotalStates);
        debugLogger.info("The grammar that solves all conflicts but longest-match has {} productions and {} states.",
            noLongestMatchProductions, noLongestMatchTotalStates);
        debugLogger.info("The grammar that solves only operator-style has {} productions and {} states.",
            onlyOperatorAmbTotalProductions, onlyOperatorAmbTotalStates);
        debugLogger.info("The grammar that solves only dangling else has {} productions and {} states.",
            onlyDanglingElseTotalProductions, onlyDanglingElseTotalStates);
        debugLogger.info("The grammar that solves only longest-match has {} productions and {} states.",
            onlyLongestMatchProductions, onlyLongestMatchTotalStates);
        debugLogger.info("The grammar that solves no deep conflicts has {} productions and {} states.",
            ambiguousGrammartotalProductions, ambiguousGrammarTotalStates);
        debugLogger.info("All programs visited {} states.", finalStates);
        debugLogger.info("processed {} states.", finalProcessedStates);
        debugLogger.info("and used {} productions.", finalProductionsUsed);
        debugLogger.info("Files parsed: {}", filesParsed);
        debugLogger.info("Files that did not parse: {}", javaFiles.size() - filesParsed - 1);
        debugLogger.info("-------------------------------------");

        JAVAresultLogger.info("\n\n\n" + totalProductions + ";" + totalStates + ";" + finalStates + ";"
            + finalProcessedStates + ";" + finalProductionsUsed);
        JAVAresultLogger.info("\n" + noOperatorAmbTotalProductions + ";" + noOperatorAmbTotalStates);
        JAVAresultLogger.info("\n" + noDanglingElseTotalProductions + ";" + noDanglingElseTotalStates);
        JAVAresultLogger.info("\n" + noLongestMatchProductions + ";" + noLongestMatchTotalStates);
        JAVAresultLogger.info("\n" + onlyOperatorAmbTotalProductions + ";" + onlyOperatorAmbTotalStates);
        JAVAresultLogger.info("\n" + onlyDanglingElseTotalProductions + ";" + onlyDanglingElseTotalStates);
        JAVAresultLogger.info("\n" + onlyLongestMatchProductions + ";" + onlyLongestMatchTotalStates);
        JAVAresultLogger.info("\n" + ambiguousGrammartotalProductions + ";" + ambiguousGrammarTotalStates);
    }

    /*
     * private static void resumeJavaExperiment() { // TODO implement java experiment
     * 
     * }
     */

    private static void runOCamlExperiment() throws Exception {
        // Go through all OCaml files:
        // count LOC
        // parse them
        // count AST nodes
        // parse them using a new dynamic parse table and the same parse table as the other programs
        // parse them disabling contextual productions and count ambiguities
        org.apache.log4j.Logger filesLogger = org.apache.log4j.Logger.getLogger("filesLogger");
        org.apache.log4j.Logger probfilesLogger = org.apache.log4j.Logger.getLogger("problematicfilesLogger");

        int filesParsed = 0;

        debugLogger.info("-------------------------------------");
        debugLogger.info("Creating the list of all OCaml Files:");
        File oCamlDir = new File("test/OCaml/");
        Collection<File> oCamlFiles = FileUtils.listFiles(oCamlDir, new String[] { "ml" }, true);
        debugLogger.info("{} files created.", oCamlFiles.size() - 1);
        debugLogger.info("-------------------------------------");

        final TermTreeFactory factory = new TermTreeFactory(new ParentTermFactory(tf));
        File mainOcamlFile = new File("normalizedGrammars/OCaml/normalized/OCaml-norm.aterm");
        ParseTableGenerator ptg = new ParseTableGenerator(mainOcamlFile, null, null, null,
            Lists.newArrayList("normalizedGrammars/OCaml"), false);

        IStrategoTerm ptAterm, noOperatorAmbPTAterm, noDanglingElsePTAterm, noLongestMatchPTAterm;
        ParseTableGenerator ambiguousGrammarPtg, noOperatorAmbPtg, noDanglingElsePtg, noLongestMatchPtg,
            onlyOperatorAmbPtg, onlyDanglingElsePtg, onlyLongestMatchPtg;
        SGLR parserAllFiles, noOperatorAmbParser, noDanglingElseParser, noLongestMatchParser;
        ParseTable pt, noOperatorAmbPT, noDanglingElsePT, noLongestMatchPT;

        try {
            ptAterm = ptg.createDynamicTable(true, true, true, true);

            ambiguousGrammarPtg = new ParseTableGenerator(mainOcamlFile, null, null, null,
                Lists.newArrayList("normalizedGrammars/OCaml"), false);
            ambiguousGrammarPtg.createDynamicTable(false, false, false, true);

            noOperatorAmbPtg = new ParseTableGenerator(mainOcamlFile, null, null, null,
                Lists.newArrayList("normalizedGrammars/OCaml"), false);
            noOperatorAmbPTAterm = noOperatorAmbPtg.createDynamicTable(false, true, true, true);

            noDanglingElsePtg = new ParseTableGenerator(mainOcamlFile, null, null, null,
                Lists.newArrayList("normalizedGrammars/OCaml"), false);
            noDanglingElsePTAterm = noDanglingElsePtg.createDynamicTable(true, false, true, true);

            noLongestMatchPtg = new ParseTableGenerator(mainOcamlFile, null, null, null,
                Lists.newArrayList("normalizedGrammars/OCaml"), false);
            noLongestMatchPTAterm = noLongestMatchPtg.createDynamicTable(true, true, false, true);

            onlyOperatorAmbPtg = new ParseTableGenerator(mainOcamlFile, null, null, null,
                Lists.newArrayList("normalizedGrammars/OCaml"), false);
            onlyOperatorAmbPtg.createDynamicTable(true, false, false, true);

            onlyDanglingElsePtg = new ParseTableGenerator(mainOcamlFile, null, null, null,
                Lists.newArrayList("normalizedGrammars/OCaml"), false);
            onlyDanglingElsePtg.createDynamicTable(false, true, false, true);

            onlyLongestMatchPtg = new ParseTableGenerator(mainOcamlFile, null, null, null,
                Lists.newArrayList("normalizedGrammars/OCaml"), false);
            onlyLongestMatchPtg.createDynamicTable(false, false, true, true);

            pt = new ParseTable(ptAterm, tf, ptg.getGrammar());
            noOperatorAmbPT = new ParseTable(noOperatorAmbPTAterm, tf, noOperatorAmbPtg.getGrammar());
            noDanglingElsePT = new ParseTable(noDanglingElsePTAterm, tf, noDanglingElsePtg.getGrammar());
            noLongestMatchPT = new ParseTable(noLongestMatchPTAterm, tf, noLongestMatchPtg.getGrammar());


            parserAllFiles = new SGLR(new TreeBuilder(factory), pt);
            noOperatorAmbParser = new SGLR(new TreeBuilder(factory), noOperatorAmbPT);
            noDanglingElseParser = new SGLR(new TreeBuilder(factory), noDanglingElsePT);
            noLongestMatchParser = new SGLR(new TreeBuilder(factory), noLongestMatchPT);

            setParser(parserAllFiles);

        } catch(Exception e) {
            debugLogger.info("Failed to create the parse table");
            return;
        }

        int i = 0;

        for(File f : oCamlFiles) {
            // if testing, only use disamb.ml, if not, skip disamb.ml
            if(TESTING) {
                if(!f.getName().equals(testingFile)) {
                    continue;
                }
            } else {
                if(f.getName().equals(testingFile)) {
                    continue;
                }
            }

            if(i != 0) {
                debugLogger.info("");
                resultLogger.info("\n");
            }
            i++;

            debugLogger.info(f.getAbsolutePath());
            resultLogger.info(f.getName() + ";");

            long lineCount;
            String input;
            try {
                input = FileUtils.readFileToString(f, Charsets.UTF_8);
                lineCount = getLineCount(f);
            } catch(IOException e) {
                debugLogger.info("Could not open file.");
                resultLogger.info("could not open file");
                e.printStackTrace();
                probfilesLogger.info(f.getAbsolutePath());
                continue;
            }

            debugLogger.info("{} lines.", lineCount);
            resultLogger.info(lineCount + ";");

            // FIXME the files below cause a stack overflow when imploding
            if(checkProblematicFiles(f, debugLogger, resultLogger)) {
                probfilesLogger.info(f.getAbsolutePath());
                continue;
            }



            ParseTable individualPT;
            try {
                individualPT = new ParseTable(ptAterm, tf, ptg.getGrammar());
            } catch(InvalidParseTableException e) {
                debugLogger.info("Could not create individual parse table.");
                resultLogger.info("could not create individual parse table;");
                e.printStackTrace();
                probfilesLogger.info(f.getAbsolutePath());
                continue;
            }

            SGLR individualParser = new SGLR(new TreeBuilder(factory), individualPT);


            SGLRParseResult parseResult;
            try {
                parseResult = parserAllFiles.parse(input, f.getName(), "Start");
            } catch(Exception e) {
                debugLogger.info("Parsing failed with exception {}", e.getMessage());
                resultLogger.info("parsing failed.");
                e.printStackTrace();
                probfilesLogger.info(f.getAbsolutePath());
                continue;
            }

            SGLRParseResult individualParseResult;
            try {
                individualParseResult = individualParser.parse(input, f.getName(), "Start");
            } catch(Exception e) {
                debugLogger.info("Parsing failed with exception {}", e.getMessage());
                resultLogger.info("parsing failed.");
                e.printStackTrace();
                probfilesLogger.info(f.getAbsolutePath());
                continue;
            }

            SGLRParseResult noOperatorAmbResult;
            try {
                noOperatorAmbResult = noOperatorAmbParser.parse(input, f.getName(), "Start");
            } catch(Exception e) {
                debugLogger.info("Operator-style ambiguity parser failed with exception {}", e.getMessage());
                resultLogger.info("operator-style parsing failed.");
                e.printStackTrace();
                probfilesLogger.info(f.getAbsolutePath());
                continue;
            }

            SGLRParseResult noDanglingElseResult;
            try {
                noDanglingElseResult = noDanglingElseParser.parse(input, f.getName(), "Start");
            } catch(Exception e) {
                debugLogger.info("Dangling else parser failed with exception {}", e.getMessage());
                resultLogger.info("dangling else parsing failed.");
                e.printStackTrace();
                probfilesLogger.info(f.getAbsolutePath());
                continue;
            }

            SGLRParseResult noLongestMatchResult;
            try {
                noLongestMatchResult = noLongestMatchParser.parse(input, f.getName(), "Start");
            } catch(Exception e) {
                debugLogger.info("Longest match parser failed with exception {}", e.getMessage());
                resultLogger.info("longest match parsing failed.");
                e.printStackTrace();
                probfilesLogger.info(f.getAbsolutePath());
                continue;
            }

            IStrategoTerm ast = (IStrategoTerm) parseResult.output;
            IStrategoTerm individualAst = (IStrategoTerm) individualParseResult.output;
            IStrategoTerm noOperatorAmbAst = (IStrategoTerm) noOperatorAmbResult.output;
            IStrategoTerm noDanglingElseAst = (IStrategoTerm) noDanglingElseResult.output;
            IStrategoTerm noLongestMatchAst = (IStrategoTerm) noLongestMatchResult.output;

            if(!ast.equals(individualAst)) {
                debugLogger.info("AST from local parser is different from AST from global parser.");
                resultLogger.info("local and global asts are different;");
                probfilesLogger.info(f.getAbsolutePath());
                continue;
            }

            long ambs = parserAllFiles.getAmbiguitiesCount();
            long opStyleAmbs = noOperatorAmbParser.getAmbiguitiesCount();
            long danglingElseAmbs = noDanglingElseParser.getAmbiguitiesCount();
            long longestMatchAmbs = noLongestMatchParser.getAmbiguitiesCount();

            long opStyleUniqueAmbs = 0, danglingElseUniqueAmbs = 0, longestMatchUniqueAmbs = 0;
            long opStyleTopLevelAmbs = 0, danglingElseTopLevelAmbs = 0, longestMatchTopLevelAmbs = 0;

            if(ambs != 0) {
                debugLogger.info("Original input contains ambiguities");
                resultLogger.info("input contains ambiguities;");
                probfilesLogger.info(f.getAbsolutePath());
                continue;
            }

            if(opStyleAmbs != 0) {
                opStyleUniqueAmbs = collectAmbs(noOperatorAmbAst).size();
                opStyleTopLevelAmbs = exportAmbiguities(f, noOperatorAmbAst, "operator-style"); // countTopLevelAmbiguities(noOperatorAmbAst);
            }

            if(danglingElseAmbs != 0) {
                danglingElseUniqueAmbs = collectAmbs(noDanglingElseAst).size();
                danglingElseTopLevelAmbs = exportAmbiguities(f, noDanglingElseAst, "dangling-else"); // countTopLevelAmbiguities(noDanglingElseAst);
            }

            if(longestMatchAmbs != 0) {
                longestMatchUniqueAmbs = collectAmbs(noLongestMatchAst).size();
                longestMatchTopLevelAmbs = exportAmbiguities(f, noLongestMatchAst, "longest-match"); // countTopLevelAmbiguities(noLongestMatchAst);
            }

            long nodeCount = getNodeCount(ast);
            long totalStates = individualPT.getPTgenerator().totalStates();
            long processedStates = individualPT.getPTgenerator().getProcessedStates();
            long productionsUsed = individualParser.numberOfProductionsUsedSingleParse();

            debugLogger.info("{} ast nodes.", nodeCount);
            debugLogger.info("Visited {} states.", totalStates);
            debugLogger.info("And processed {} states.", processedStates);
            debugLogger.info("Used {} productions.", productionsUsed);


            resultLogger.info(nodeCount + ";");
            resultLogger.info(totalStates + ";");
            resultLogger.info(processedStates + ";");
            resultLogger.info(productionsUsed + ";");

            debugLogger.info("{} ambiguities.", ambs);
            debugLogger.info("{} operator-style top-level ambiguities.", opStyleTopLevelAmbs);
            debugLogger.info("{} dangling else top-level ambiguities.", danglingElseTopLevelAmbs);
            debugLogger.info("{} longest match top-level ambiguities.", longestMatchTopLevelAmbs);
            debugLogger.info("{} operator-style ambiguities.", opStyleUniqueAmbs);
            debugLogger.info("{} dangling else ambiguities.", danglingElseUniqueAmbs);
            debugLogger.info("{} longest match ambiguities.", longestMatchUniqueAmbs);

            resultLogger.info(opStyleTopLevelAmbs + ";");
            resultLogger.info(danglingElseTopLevelAmbs + ";");
            resultLogger.info(longestMatchTopLevelAmbs + ";");
            resultLogger.info(opStyleUniqueAmbs + ";");
            resultLogger.info(danglingElseUniqueAmbs + ";");
            resultLogger.info(longestMatchUniqueAmbs + ";");

            // TODO count brackets
            countBrackets(ast, ptg, debugLogger, resultLogger);


            filesLogger.info(f.getAbsolutePath());
            filesParsed++;
        }

        long finalStates = pt.getPTgenerator().totalStates();
        long finalProcessedStates = pt.getPTgenerator().getProcessedStates();
        long finalProductionsUsed = parserAllFiles.numberOfProductionsUsedAllParses();

        long totalProductions = ptg.labels().size();
        long noOperatorAmbTotalProductions = noOperatorAmbPtg.labels().size();
        long noDanglingElseTotalProductions = noDanglingElsePtg.labels().size();
        long noLongestMatchProductions = noLongestMatchPtg.labels().size();
        long onlyOperatorAmbTotalProductions = onlyOperatorAmbPtg.labels().size();
        long onlyDanglingElseTotalProductions = onlyDanglingElsePtg.labels().size();
        long onlyLongestMatchProductions = onlyLongestMatchPtg.labels().size();
        long ambiguousGrammartotalProductions = ambiguousGrammarPtg.labels().size();

        long totalStates;
        long noOperatorAmbTotalStates;
        long noDanglingElseTotalStates;
        long noLongestMatchTotalStates;
        long onlyOperatorAmbTotalStates;
        long onlyDanglingElseTotalStates;
        long onlyLongestMatchTotalStates;
        long ambiguousGrammarTotalStates;

        if(!TABLECREATION) {
            totalStates = 0;
            noOperatorAmbTotalStates = 0;
            noDanglingElseTotalStates = 0;
            noLongestMatchTotalStates = 0;
            onlyOperatorAmbTotalStates = 0;
            onlyDanglingElseTotalStates = 0;
            onlyLongestMatchTotalStates = 0;
            ambiguousGrammarTotalStates = 0;

        } else {
            ptg.createCompleteTable();
            noOperatorAmbPtg.createCompleteTable();
            noDanglingElsePtg.createCompleteTable();
            noLongestMatchPtg.createCompleteTable();
            onlyOperatorAmbPtg.createCompleteTable();
            onlyDanglingElsePtg.createCompleteTable();
            onlyLongestMatchPtg.createCompleteTable();
            ambiguousGrammarPtg.createCompleteTable();


            totalStates = ptg.totalStates();
            noOperatorAmbTotalStates = noOperatorAmbPtg.totalStates();
            noDanglingElseTotalStates = noDanglingElsePtg.totalStates();
            noLongestMatchTotalStates = noLongestMatchPtg.totalStates();
            onlyOperatorAmbTotalStates = onlyOperatorAmbPtg.totalStates();
            onlyDanglingElseTotalStates = onlyDanglingElsePtg.totalStates();
            onlyLongestMatchTotalStates = onlyLongestMatchPtg.totalStates();
            ambiguousGrammarTotalStates = ambiguousGrammarPtg.totalStates();
        }


        debugLogger.info("");
        debugLogger.info("-------------------------------------");
        debugLogger.info("The grammar that solves all deep conflicts has {} productions and {} states.",
            totalProductions, totalStates);
        debugLogger.info("The grammar that solves all conflicts but operator-style has {} productions and {} states.",
            noOperatorAmbTotalProductions, noOperatorAmbTotalStates);
        debugLogger.info("The grammar that solves all conflicts but dangling else has {} productions and {} states.",
            noDanglingElseTotalProductions, noDanglingElseTotalStates);
        debugLogger.info("The grammar that solves all conflicts but longest-match has {} productions and {} states.",
            noLongestMatchProductions, noLongestMatchTotalStates);
        debugLogger.info("The grammar that solves only operator-style has {} productions and {} states.",
            onlyOperatorAmbTotalProductions, onlyOperatorAmbTotalStates);
        debugLogger.info("The grammar that solves only dangling else has {} productions and {} states.",
            onlyDanglingElseTotalProductions, onlyDanglingElseTotalStates);
        debugLogger.info("The grammar that solves only longest-match has {} productions and {} states.",
            onlyLongestMatchProductions, onlyLongestMatchTotalStates);
        debugLogger.info("The grammar that solves no deep conflicts has {} productions and {} states.",
            ambiguousGrammartotalProductions, ambiguousGrammarTotalStates);
        debugLogger.info("All programs visited {} states.", finalStates);
        debugLogger.info("processed {} states.", finalProcessedStates);
        debugLogger.info("and used {} productions.", finalProductionsUsed);
        debugLogger.info("Files parsed: {}", filesParsed);
        debugLogger.info("Files that did not parse: {}", oCamlFiles.size() - filesParsed - 1);
        debugLogger.info("-------------------------------------");

        resultLogger.info("\n\n\n" + totalProductions + ";" + totalStates + ";" + finalStates + ";"
            + finalProcessedStates + ";" + finalProductionsUsed);
        resultLogger.info("\n" + noOperatorAmbTotalProductions + ";" + noOperatorAmbTotalStates);
        resultLogger.info("\n" + noDanglingElseTotalProductions + ";" + noDanglingElseTotalStates);
        resultLogger.info("\n" + noLongestMatchProductions + ";" + noLongestMatchTotalStates);
        resultLogger.info("\n" + onlyOperatorAmbTotalProductions + ";" + onlyOperatorAmbTotalStates);
        resultLogger.info("\n" + onlyDanglingElseTotalProductions + ";" + onlyDanglingElseTotalStates);
        resultLogger.info("\n" + onlyLongestMatchProductions + ";" + onlyLongestMatchTotalStates);
        resultLogger.info("\n" + ambiguousGrammartotalProductions + ";" + ambiguousGrammarTotalStates);
    }

    /*
     * private static void resumeOCamlExperiment(File previousFiles) { String processedFiles; try { processedFiles =
     * FileUtils.readFileToString(previousFiles, Charsets.UTF_8); } catch(IOException e) {
     * debugLogger.info("Could not open file containing files processed previously.");
     * resultLogger.info("could not open file containing files processed previously"); e.printStackTrace(); return; }
     * 
     * org.apache.log4j.Logger filesLogger = org.apache.log4j.Logger.getLogger("filesLogger");
     * 
     * File oCamlDir = new File("test/OCaml/"); Collection<File> oCamlFiles = FileUtils.listFiles(oCamlDir, new String[]
     * { "ml" }, true);
     * 
     * final TermTreeFactory factory = new TermTreeFactory(new ParentTermFactory(tf)); File mainOcamlFile = new
     * File("normalizedGrammars/OCaml/normalized/OCaml-norm.aterm"); ParseTableGenerator ptg = new
     * ParseTableGenerator(mainOcamlFile, null, null, null, Lists.newArrayList("normalizedGrammars/OCaml"), false);
     * 
     * IStrategoTerm ptAterm, noOperatorAmbPTAterm, noDanglingElsePTAterm, noLongestMatchPTAterm; ParseTableGenerator
     * noOperatorAmbPtg, noDanglingElsePtg, noLongestMatchPtg; SGLR parserAllFiles, noOperatorAmbParser,
     * noDanglingElseParser, noLongestMatchParser; ParseTable pt, noOperatorAmbPT, noDanglingElsePT, noLongestMatchPT;
     * 
     * try { ptAterm = ptg.createDynamicTable(true, true, true, true);
     * 
     * noOperatorAmbPtg = new ParseTableGenerator(mainOcamlFile, null, null, null,
     * Lists.newArrayList("normalizedGrammars/OCaml"), false); noOperatorAmbPTAterm =
     * noOperatorAmbPtg.createDynamicTable(false, true, true, true);
     * 
     * noDanglingElsePtg = new ParseTableGenerator(mainOcamlFile, null, null, null,
     * Lists.newArrayList("normalizedGrammars/OCaml"), false); noDanglingElsePTAterm =
     * noDanglingElsePtg.createDynamicTable(true, false, true, true);
     * 
     * noLongestMatchPtg = new ParseTableGenerator(mainOcamlFile, null, null, null,
     * Lists.newArrayList("normalizedGrammars/OCaml"), false); noLongestMatchPTAterm =
     * noLongestMatchPtg.createDynamicTable(true, true, false, true);
     * 
     * pt = new ParseTable(ptAterm, tf, ptg.getGrammar()); noOperatorAmbPT = new ParseTable(noOperatorAmbPTAterm, tf,
     * noOperatorAmbPtg.getGrammar()); noDanglingElsePT = new ParseTable(noDanglingElsePTAterm, tf,
     * noDanglingElsePtg.getGrammar()); noLongestMatchPT = new ParseTable(noLongestMatchPTAterm, tf,
     * noLongestMatchPtg.getGrammar());
     * 
     * 
     * parserAllFiles = new SGLR(new TreeBuilder(factory), pt); noOperatorAmbParser = new SGLR(new TreeBuilder(factory),
     * noOperatorAmbPT); noDanglingElseParser = new SGLR(new TreeBuilder(factory), noDanglingElsePT);
     * noLongestMatchParser = new SGLR(new TreeBuilder(factory), noLongestMatchPT);
     * 
     * setParser(parserAllFiles);
     * 
     * } catch(Exception e) { debugLogger.info("Failed to create the parse table"); return; }
     * 
     * int i = 0;
     * 
     * for(File f : oCamlFiles) { // if testing, only use disamb.ml, if not, skip disamb.ml if(TESTING) {
     * if(!f.getName().equals(testingFile)) { continue; } } else { if(f.getName().equals(testingFile)) { continue; } }
     * 
     * if(processedFiles.contains(f.getAbsolutePath())) { continue; }
     * 
     * // FIXME the files below cause a stack overflow when imploding or makes the parser hang
     * if(checkProblematicFiles(f)) { continue; }
     * 
     * if(i != 0) { debugLogger.info(""); resultLogger.info("\n"); } i++;
     * 
     * debugLogger.info(f.getAbsolutePath()); resultLogger.info(f.getName() + ";");
     * 
     * long lineCount; String input; try { input = FileUtils.readFileToString(f, Charsets.UTF_8); lineCount =
     * getLineCount(f); } catch(IOException e) { debugLogger.info("Could not open file.");
     * resultLogger.info("could not open file"); e.printStackTrace(); continue; }
     * 
     * debugLogger.info("{} lines.", lineCount); resultLogger.info(lineCount + ";");
     * 
     * ParseTable individualPT; try { individualPT = new ParseTable(ptAterm, tf, ptg.getGrammar()); }
     * catch(InvalidParseTableException e) { debugLogger.info("Could not create individual parse table.");
     * resultLogger.info("could not create individual parse table;"); e.printStackTrace(); continue; }
     * 
     * SGLR individualParser = new SGLR(new TreeBuilder(factory), individualPT);
     * 
     * 
     * SGLRParseResult parseResult; try { parseResult = parserAllFiles.parse(input, f.getName(), "Start"); }
     * catch(Exception e) { debugLogger.info("Parsing failed with exception {}", e.getMessage());
     * resultLogger.info("parsing failed."); e.printStackTrace(); continue; }
     * 
     * SGLRParseResult individualParseResult; try { individualParseResult = individualParser.parse(input, f.getName(),
     * "Start"); } catch(Exception e) { debugLogger.info("Parsing failed with exception {}", e.getMessage());
     * resultLogger.info("parsing failed."); e.printStackTrace(); continue; }
     * 
     * SGLRParseResult noOperatorAmbResult; try { noOperatorAmbResult = noOperatorAmbParser.parse(input, f.getName(),
     * "Start"); } catch(Exception e) { debugLogger.info("Operator-style ambiguity parser failed with exception {}",
     * e.getMessage()); resultLogger.info("operator-style parsing failed."); e.printStackTrace(); continue; }
     * 
     * SGLRParseResult noDanglingElseResult; try { noDanglingElseResult = noDanglingElseParser.parse(input, f.getName(),
     * "Start"); } catch(Exception e) { debugLogger.info("Dangling else parser failed with exception {}",
     * e.getMessage()); resultLogger.info("dangling else parsing failed."); e.printStackTrace(); continue; }
     * 
     * SGLRParseResult noLongestMatchResult; try { noLongestMatchResult = noLongestMatchParser.parse(input, f.getName(),
     * "Start"); } catch(Exception e) { debugLogger.info("Longest match parser failed with exception {}",
     * e.getMessage()); resultLogger.info("longest match parsing failed."); e.printStackTrace(); continue; }
     * 
     * IStrategoTerm ast = (IStrategoTerm) parseResult.output; IStrategoTerm individualAst = (IStrategoTerm)
     * individualParseResult.output; IStrategoTerm noOperatorAmbAst = (IStrategoTerm) noOperatorAmbResult.output;
     * IStrategoTerm noDanglingElseAst = (IStrategoTerm) noDanglingElseResult.output; IStrategoTerm noLongestMatchAst =
     * (IStrategoTerm) noLongestMatchResult.output;
     * 
     * if(!ast.equals(individualAst)) {
     * debugLogger.info("AST from local parser is different from AST from global parser.");
     * resultLogger.info("local and global asts are different;"); continue; }
     * 
     * long ambs = parserAllFiles.getAmbiguitiesCount(); long opStyleAmbs = noOperatorAmbParser.getAmbiguitiesCount();
     * long danglingElseAmbs = noDanglingElseParser.getAmbiguitiesCount(); long longestMatchAmbs =
     * noLongestMatchParser.getAmbiguitiesCount();
     * 
     * if(ambs != 0) { debugLogger.info("Original input contains ambiguities");
     * resultLogger.info("input contains ambiguities;"); continue; }
     * 
     * if(opStyleAmbs != 0) { exportAmbiguities(f, noOperatorAmbAst, "operator-style"); }
     * 
     * if(danglingElseAmbs != 0) { exportAmbiguities(f, noDanglingElseAst, "dangling-else"); }
     * 
     * if(longestMatchAmbs != 0) { exportAmbiguities(f, noLongestMatchAst, "longest-match"); }
     * 
     * long nodeCount = getNodeCount(ast); long totalStates = individualPT.getPTgenerator().totalStates(); long
     * processedStates = individualPT.getPTgenerator().getProcessedStates(); debugLogger.info("{} ast nodes.",
     * nodeCount); debugLogger.info("Visited {} states.", totalStates); debugLogger.info("And processed {} states.",
     * processedStates);
     * 
     * resultLogger.info(nodeCount + ";"); resultLogger.info(totalStates + ";"); resultLogger.info(processedStates +
     * ";");
     * 
     * debugLogger.info("{} ambiguities.", ambs); debugLogger.info("{} operator-style ambiguities.", opStyleAmbs);
     * debugLogger.info("{} dangling else ambiguities.", danglingElseAmbs);
     * debugLogger.info("{} longest match ambiguities.", longestMatchAmbs);
     * 
     * resultLogger.info(opStyleAmbs + ";"); resultLogger.info(danglingElseAmbs + ";");
     * resultLogger.info(longestMatchAmbs + ";");
     * 
     * filesLogger.info(f.getAbsolutePath()); }
     * 
     * 
     * debugLogger.info("");
     * 
     * debugLogger.info("-----------------------------------------------------------------------------------------");
     * debugLogger.warn("PLEASE RE-RUN THE FULL EXPERIMENT TO GET THE CORRECT NUMBER OF FINAL AND PROCESSED STATES");
     * debugLogger.info("-----------------------------------------------------------------------------------------");
     * 
     * resultLogger
     * .info("\n\nPLEASE RE-RUN THE FULL EXPERIMENT TO GET THE CORRECT NUMBER OF FINAL AND PROCESSED STATES");
     * 
     * }
     */

    private static boolean checkProblematicFiles(File f, Logger debugLogger, org.apache.log4j.Logger resultLogger) {
        if(f.getPath().equals("test/OCaml/bucklescript/jscomp/test/gpr_1150.ml")) {
            debugLogger.info("SGLR error - too many longest match ambiguities to implode.");
            resultLogger.info("SGLR error - too many longest match ambiguities to implode.");
            return true;
        }

        if(f.getPath().equals("test/OCaml/bucklescript/jscomp/test/ocaml_typedtree_test.ml")) {
            debugLogger.info("SGLR error - parser time out.");
            resultLogger.info("SGLR error - parser time out.");
            return true;
        }

        if(f.getPath().equals("test/OCaml/tezos/src/node/shell/validator.ml")
            || f.getPath().equals("test/OCaml/tezos/src/proto/alpha/amendment.ml")
            || f.getPath().equals("test/OCaml/tezos/test/p2p/test_p2p_connection_pool.ml")
            || f.getPath().equals("test/OCaml/tezos/test/proto_alpha/test_endorsement.ml")
            || f.getPath().equals("test/OCaml/tezos/src/proto/alpha/contract_storage.ml")) {
            debugLogger.info("SGLR error - too many operator style ambiguities to implode.");
            resultLogger.info("SGLR error - too many operator style ambiguities to implode.");
            return true;
        }

        if(f.getPath().equals("test/OCaml/bucklescript/jscomp/test/string_interp_test.ml")
            || f.getPath().equals("test/OCaml/bucklescript/jscomp/test/chn_test.ml")
            || f.getPath().equals("test/OCaml/tezos/src/client/client_network.ml")) {
            debugLogger.info("SGLR error - failed to parse unicode characters.");
            resultLogger.info("SGLR error - failed to parse unicode characters.");
            return true;
        }

        if(f.getPath().equals("test/OCaml/bucklescript/jscomp/test/flow_parser_reg_test.ml")) {
            debugLogger.info("SGLR error - too long literals to implode.");
            resultLogger.info("SGLR error - too long literals to implode.");
            return true;
        }

        return false;
    }

    private static long exportAmbiguities(File f, IStrategoTerm ast, String kind) {
        // export ambiguities to logs/ambs/{path}/kind/name
        final Path filePath = Paths.get(f.toURI());
        final String projectPath = System.getProperty("user.dir");
        final Path basePath = Paths.get(projectPath, "test");

        final Path relativePath = basePath.relativize(filePath);

        File outputFile = new File("logs/ambiguities/" + relativePath.getParent() + "/" + kind + "/"
            + FilenameUtils.removeExtension(relativePath.getFileName().toString()) + "-ambs.txt");
        File parentDir = outputFile.getParentFile();
        if(!parentDir.exists()) {
            parentDir.mkdirs();
        }

        List<IStrategoTerm> ambs = collectTopLevelAmbs(ast);

        FileWriter out = null;
        try {
            out = new FileWriter(outputFile);
            // output ambiguity
            int i = 0;
            for(IStrategoTerm amb : ambs) {
                if(i != 0)
                    out.write("\n\n");
                out.write("Ambiguity " + ++i + ": \n");
                ImploderAttachment ia = ImploderAttachment.get(amb);
                out.write(ia.getLeftToken().getTokenizer().toString(ia.getLeftToken(), ia.getRightToken()));
            }
            out.close();
        } catch(IOException e) {
            System.err.println(e.getMessage());
        }

        return ambs.size();
    }

    private static void setParser(SGLR parser) {
        parser.setCompletionParse(false, Integer.MAX_VALUE);
        parser.setTimeout(30000);
    }

    private static long getNodeCount(IStrategoTerm ast) {
        int nc = 0;
        for(IStrategoTerm child : ast.getAllSubterms()) {
            nc += getNodeCount(child);
        }

        return nc + 1;
    }

    private static Set<IStrategoTerm> collectAmbs(IStrategoTerm ast) {
        Set<IStrategoTerm> ambs = Sets.newHashSet();

        if(ast instanceof IStrategoAppl) {
            IStrategoAppl amb = (IStrategoAppl) ast;
            if(amb.getConstructor().getName().equals("amb")) {
                ambs.add(ast);
            }
        }

        for(IStrategoTerm child : ast.getAllSubterms()) {
            ambs.addAll(collectAmbs(child));
        }

        return ambs;
    }

    private static List<IStrategoTerm> collectTopLevelAmbs(IStrategoTerm ast) {
        List<IStrategoTerm> ambs = Lists.newArrayList();

        if(ast instanceof IStrategoAppl) {
            IStrategoAppl amb = (IStrategoAppl) ast;
            if(amb.getConstructor().getName().equals("amb")) {
                ambs.add(ast);
                return ambs;
            }
        }

        for(IStrategoTerm child : ast.getAllSubterms()) {
            ambs.addAll(collectTopLevelAmbs(child));
        }

        return ambs;

    }

    private static IStrategoString prettyPrint(IStrategoTerm term) {
        org.strategoxt.lang.Context context = new org.strategoxt.lang.Context(tf);
        org.strategoxt.stratego_aterm.Main.init(context);
        term = aterm_escape_strings_0_0.instance.invoke(context, term);
        term = pp_aterm_box_0_0.instance.invoke(context, term);
        term = box2text_string_0_1.instance.invoke(context, term, tf.makeInt(120));
        return (IStrategoString) term;
    }

    /**
     * Count file rows.
     *
     * @param file
     *            file
     * @return file row count
     * @throws IOException
     */
    private static long getLineCount(File file) throws IOException {

        try(BufferedInputStream is = new BufferedInputStream(new FileInputStream(file), 1024)) {

            byte[] c = new byte[1024];
            boolean empty = true, lastEmpty = false;
            long count = 0;
            int read;
            while((read = is.read(c)) != -1) {
                for(int i = 0; i < read; i++) {
                    if(c[i] == '\n') {
                        count++;
                        lastEmpty = true;
                    } else if(lastEmpty) {
                        lastEmpty = false;
                    }
                }
                empty = false;
            }

            if(!empty) {
                if(count == 0) {
                    count = 1;
                } else if(!lastEmpty) {
                    count++;
                }
            }

            return count;
        }
    }

    private static void countBrackets(IStrategoTerm ast, ParseTableGenerator ptg, Logger debugLogger,
        org.apache.log4j.Logger resultLogger) throws Exception {
        List<ContextualProduction> conflictingProductions =
            Lists.newArrayList(ptg.getGrammar().contextual_prods.values());
        BiMap<ContextualProduction, UniqueConstructor> conflictingProdCons = HashBiMap.create();
        Map<IProduction, UniqueConstructor> allProdsCons = Maps.newHashMap();

        for(IProduction p : ptg.getGrammar().prods.values()) {
            for(IAttribute attr : ptg.getGrammar().prod_attrs.get(p)) {
                if(attr instanceof ConstructorAttribute) {
                    allProdsCons.put(p, new UniqueConstructor(p.leftHand(), (ConstructorAttribute) attr));
                }
            }
        }

        for(ContextualProduction p : conflictingProductions) {
            for(IAttribute attr : ptg.getGrammar().prod_attrs.get(p.getOrigProduction())) {
                if(attr instanceof ConstructorAttribute) {
                    Symbol lastOrigSymbol =
                        p.getOrigProduction().rightHand().get(p.getOrigProduction().rightHand().size() - 1);
                    // longest-match * lists might have the same constructor
                    if(ptg.getGrammar().longest_match_prods.get(lastOrigSymbol).contains(p.getOrigProduction())) {
                        Symbol lastSymbolContextualProduction = p.rightHand().get(p.rightHand().size() - 1);
                        if(lastSymbolContextualProduction instanceof ContextualSymbol) {
                            Symbol lastSymbol = ((ContextualSymbol) lastSymbolContextualProduction).getOrigSymbol();
                            if(lastSymbol.toString().contains("*-CF")) {
                                UniqueConstructor uc =
                                    new UniqueConstructor(p.leftHand(), (ConstructorAttribute) attr, true);
                                conflictingProdCons.put(p, uc);
                                continue;
                            }
                        }
                    }
                    UniqueConstructor uc = new UniqueConstructor(p.leftHand(), (ConstructorAttribute) attr);
                    conflictingProdCons.put(p, uc);
                }
            }
        }


        IdentityHashMap<IStrategoTerm, IStrategoTerm> bracketNodes = collectAllBrackets(ast);

        long disambBrackets =
            countDeepConflictsExplicitDisamb(ast, bracketNodes, allProdsCons, conflictingProdCons, ptg);
        long directConflicts = countDirectConflictsExplicitDisamb(bracketNodes, ptg);

        debugLogger.info("{} explicit disambiguation of deep priority conflicts.", disambBrackets);
        debugLogger.info("{} explicit disambiguation of direct conflicts.", directConflicts);
        debugLogger.info("{} brackets used only for readability.", bracketNodes.size());

        resultLogger.info(disambBrackets + ";");
        resultLogger.info(directConflicts + ";");
        resultLogger.info(bracketNodes.size() + ";");

    }

    private static long countDirectConflictsExplicitDisamb(IdentityHashMap<IStrategoTerm, IStrategoTerm> bracketNodes,
        ParseTableGenerator ptg) {
        long directConflictsCount = 0;
        List<IStrategoTerm> directConflictBrackets = Lists.newArrayList();

        for(IStrategoTerm node : bracketNodes.keySet()) {
            IStrategoTerm parent = ParentAttachment.get(node).getParent();
            if(parent != null) {
                int pos = 0;
                for(IStrategoTerm child : parent) {
                    if(child == node) {
                        break;
                    } else {
                        pos++;
                    }
                }
                // check if there's a priority that forbids node to be a child of parent at position pos
                if(node instanceof IStrategoAppl && parent instanceof IStrategoAppl) {
                    IStrategoAppl higherNode = (IStrategoAppl) parent;
                    IStrategoAppl lowerNode = (IStrategoAppl) node;
                    // assumes that the sort is Name-CF
                    boolean directConflict = checkPriority(ptg, pos, higherNode, lowerNode);

                    if(directConflict) {
                        directConflictBrackets.add(node);
                        directConflictsCount++;
                    }

                } else if(node instanceof IStrategoAppl && parent instanceof IStrategoList) {
                    if(ImploderAttachment.getSort(parent).contains("Arg")) {
                        if(checkArgPriority(ptg, (IStrategoAppl) node)) {
                            directConflictBrackets.add(node);
                            directConflictsCount++;
                        }
                    }
                }
            }
        }

        for(IStrategoTerm bracket : directConflictBrackets) {
            bracketNodes.remove(bracket);
        }

        return directConflictsCount;
    }

    private static boolean checkPriority(ParseTableGenerator ptg, int pos, IStrategoAppl higherNode,
        IStrategoAppl lowerNode) {
        Symbol higherSymbol = new ContextFreeSymbol(new Sort(ImploderAttachment.getSort(higherNode)));
        ConstructorAttribute higherCons = new ConstructorAttribute(higherNode.getConstructor().getName());

        Symbol lowerSymbol = new ContextFreeSymbol(new Sort(ImploderAttachment.getSort(lowerNode)));
        ConstructorAttribute lowerCons = new ConstructorAttribute(lowerNode.getConstructor().getName());

        IProduction higher = ptg.getGrammar().sort_cons_prods.get(new ProductionReference(higherSymbol, higherCons));
        IProduction lower = ptg.getGrammar().sort_cons_prods.get(new ProductionReference(lowerSymbol, lowerCons));

        if(higher == null || lower == null) {
            System.err
                .println("Could not find the productions based on the AST nodes to check if they violate priorities.");
        }

        // count position based on the position at production, not on AST position
        int normPos = 0;
        for(Symbol s : higher.rightHand()) {
            if(s instanceof ContextFreeSymbol && !s.name().equals("LAYOUT?-CF")) {
                pos--;
                if(pos < 0)
                    break;
            }
            normPos++;
        }

        IPriority priority = new Priority(higher, lower, false);

        boolean directConflict = ptg.getGrammar().priorities().get(priority).contains(normPos);
        return directConflict;
    }

    private static boolean checkArgPriority(ParseTableGenerator ptg, IStrategoAppl node) {
        Symbol higherSymbol = new ContextFreeSymbol(new Sort("Arg"));

        Symbol lowerSymbol = new ContextFreeSymbol(new Sort(ImploderAttachment.getSort(node)));
        ConstructorAttribute lowerCons = new ConstructorAttribute(node.getConstructor().getName());
        IProduction higher = null;
        // get Arg = Expr production
        for(IProduction p : ptg.getGrammar().symbol_prods.get(higherSymbol)) {
            if(p.rightHand().size() == 1 && p.rightHand().toString().contains("Expr")) {
                higher = p;
            }
        }

        if(higher == null) {
            return false;
        }

        IProduction lower = ptg.getGrammar().sort_cons_prods.get(new ProductionReference(lowerSymbol, lowerCons));

        IPriority priority = new Priority(higher, lower, false);

        boolean directConflict = ptg.getGrammar().priorities().get(priority).contains(0);
        return directConflict;
    }

    private static long countDeepConflictsExplicitDisamb(IStrategoTerm node,
        IdentityHashMap<IStrategoTerm, IStrategoTerm> bracketNodes, Map<IProduction, UniqueConstructor> allProdsCons,
        BiMap<ContextualProduction, UniqueConstructor> conflictingProdCons, ParseTableGenerator ptg) throws Exception {

        long disambBrackets = 0;


        // longest match
        if(node instanceof IStrategoList) {
            Symbol sort = new ContextFreeSymbol(new Sort(node.getAttachment(ImploderAttachment.TYPE).getSort()));
            for(Symbol longestMatchListSymbol : ptg.getGrammar().longest_match_prods.keySet()) {
                if(sort.toString().equals(longestMatchListSymbol.toString())) {
                    if(node.getSubtermCount() >= 2) {
                        for(int i = 0; i < node.getSubtermCount() - 1; i++) {
                            // get the contextual symbol for the list
                            Symbol iterCtxSymbol = null;
                            Symbol iterSymbol = ((ContextFreeSymbol) longestMatchListSymbol).getSymbol();
                            if(iterSymbol instanceof IterStarSymbol) {
                                iterSymbol = new IterSymbol(((IterStarSymbol) iterSymbol).getSymbol());
                            }
                            iterSymbol = new ContextFreeSymbol(iterSymbol);

                            for(IProduction p : ptg.getGrammar().symbol_prods.get(iterSymbol)) {
                                if(p.rightHand().size() > 1) {
                                    iterCtxSymbol = ptg.getGrammar().contextual_prods.get(p).rightHand().get(0);
                                }

                            }
                            if(iterCtxSymbol == null)
                                continue;
                            List<String> rightContexts =
                                getConstructorsFromRightMostContext((ContextualSymbol) iterCtxSymbol, allProdsCons);

                            if(checkRightMostContext(node.getSubterm(i), rightContexts)) {
                                removeRightMostBracketFromList(bracketNodes, node.getSubterm(i));
                                disambBrackets++;
                            }
                        }
                    }
                }
            }
            // check if list is longest-match



            // if a node that is not the last deeply matches a context
            // then there was explicit disambiguation by brackets


        }

        // if constructor has conflicts
        if(node instanceof IStrategoAppl) {
            String constructor = ((IStrategoAppl) node).getConstructor().getName();
            Symbol sort = new Sort(node.getAttachment(ImploderAttachment.TYPE).getSort());
            UniqueConstructor uniqueConstructor;
            // check for * list
            UniqueConstructor uniqueConstructorStarList =
                new UniqueConstructor(sort, new ConstructorAttribute(constructor), true);
            if(conflictingProdCons.containsValue(uniqueConstructorStarList)) {
                IStrategoTerm lastNode = node.getSubterm(node.getSubtermCount() - 1);
                if(lastNode.isList() && lastNode.getSubtermCount() == 0) {
                    uniqueConstructor = uniqueConstructorStarList;
                } else {
                    uniqueConstructor = new UniqueConstructor(sort, new ConstructorAttribute(constructor));
                }
            } else {
                uniqueConstructor = new UniqueConstructor(sort, new ConstructorAttribute(constructor));
            }

            if(conflictingProdCons.containsValue(uniqueConstructor)) {
                // TODO can we separate the kind of conflict being solved?
                ContextualProduction conflictingProduction = conflictingProdCons.inverse().get(uniqueConstructor);

                List<Integer> positionsWithContext = Lists.newArrayList();
                List<ContextualSymbol> contextualSymbols = Lists.newArrayList();
                int ASTpos = 0;
                for(Symbol s : conflictingProduction.rightHand()) {
                    if(s instanceof ContextualSymbol) {
                        // position to check for conflicts
                        positionsWithContext.add(ASTpos);
                        contextualSymbols.add((ContextualSymbol) s);
                        ASTpos++;
                    } else if(s instanceof ContextFreeSymbol && !s.name().equals("LAYOUT?-CF")) {
                        ASTpos++;
                    }
                }

                // check all positions with context, whether there
                // is a conflict with explicit brackets
                for(int i = 0; i < positionsWithContext.size(); i++) {
                    int position = positionsWithContext.get(i);
                    if(position >= node.getSubtermCount()) {
                        System.err.println("Position does not exist in the AST!");
                    } else {
                        IStrategoTerm nodeToCheck = node.getSubterm(position);


                        if(nodeToCheck.getSubtermCount() == 0)
                            continue;

                        List<String> rightContexts =
                            getConstructorsFromRightMostContext(contextualSymbols.get(i), allProdsCons);
                        if(!rightContexts.isEmpty()) {
                            IStrategoTerm rightmostChild = nodeToCheck.getSubterm(nodeToCheck.getSubtermCount() - 1);

                            if(checkRightMostContext(rightmostChild, rightContexts, node, position, ptg)) {
                                removeRightMostBracketFromList(bracketNodes, nodeToCheck);
                                disambBrackets++;
                            }
                        }

                        List<String> leftContexts =
                            getConstructorsFromLeftMostContext(contextualSymbols.get(i), allProdsCons);
                        if(!leftContexts.isEmpty()) {
                            IStrategoTerm leftmostChild = nodeToCheck.getSubterm(0);
                            if(checkLeftMostContext(leftmostChild, leftContexts, node, position, ptg)) {
                                removeLeftMostBracketFromList(bracketNodes, nodeToCheck);
                                disambBrackets++;
                            }
                        }
                    }
                }
            }
        }

        for(

        IStrategoTerm child : node) {
            disambBrackets +=
                countDeepConflictsExplicitDisamb(child, bracketNodes, allProdsCons, conflictingProdCons, ptg);
        }

        return disambBrackets;
    }

    private static void removeRightMostBracketFromList(IdentityHashMap<IStrategoTerm, IStrategoTerm> bracketNodes,
        IStrategoTerm term) throws Exception {
        if(ImploderAttachment.get(term).isBracket()) {
            bracketNodes.remove(term);
        } else if(term instanceof IStrategoAppl && isOCamlBracket(term)) {
            bracketNodes.remove(term);
        } else {
            if((term.getSubtermCount() - 1) < 0) {
                throw new Exception("Bracket could not be removed from the list.");
            }
            removeRightMostBracketFromList(bracketNodes, term.getSubterm(term.getSubtermCount() - 1));
        }
    }

    private static void removeLeftMostBracketFromList(IdentityHashMap<IStrategoTerm, IStrategoTerm> bracketNodes,
        IStrategoTerm term) {
        if(ImploderAttachment.get(term).isBracket()) {
            bracketNodes.remove(term);
        } else {
            removeLeftMostBracketFromList(bracketNodes, term.getSubterm(0));
        }
    }

    private static boolean checkRightMostContext(IStrategoTerm term, List<String> contexts, IStrategoTerm node,
        int position, ParseTableGenerator ptg) {
        if(term instanceof IStrategoAppl) {
            if(contexts.contains(((IStrategoAppl) term).getConstructor().getName())) {
                // parent needs to have higher priority than node, otherwise is a direct disambiguation
                IStrategoTerm parent = ParentAttachment.getParent(term);
                if(node instanceof IStrategoAppl && parent instanceof IStrategoAppl) {
                    if(checkPriority(ptg, position, (IStrategoAppl) node, (IStrategoAppl) parent)) {
                        return false;
                    }
                }
                return true;
            } else {
                if(term.getSubtermCount() > 0) {
                    return checkRightMostContext(term.getSubterm(term.getSubtermCount() - 1), contexts, node, position,
                        ptg);
                }
            }
        }

        return false;
    }

    private static boolean checkRightMostContext(IStrategoTerm term, List<String> contexts) {
        if(term instanceof IStrategoAppl) {
            if(contexts.contains(((IStrategoAppl) term).getConstructor().getName())) {
                return true;
            } else {
                if(term.getSubtermCount() > 0) {
                    return checkRightMostContext(term.getSubterm(term.getSubtermCount() - 1), contexts);
                }
            }
        }

        return false;
    }

    private static boolean checkLeftMostContext(IStrategoTerm term, List<String> contexts, IStrategoTerm node,
        int position, ParseTableGenerator ptg) {
        if(term instanceof IStrategoAppl) {
            if(contexts.contains(((IStrategoAppl) term).getConstructor().getName())) {
                // parent needs to have higher priority than node, otherwise is a direct disambiguation
                IStrategoTerm parent = ParentAttachment.getParent(term);
                if(node instanceof IStrategoAppl && parent instanceof IStrategoAppl) {
                    if(checkPriority(ptg, position, (IStrategoAppl) node, (IStrategoAppl) parent)) {
                        return false;
                    }
                }
                return true;
            } else {
                if(term.getSubtermCount() > 0) {
                    return checkRightMostContext(term.getSubterm(0), contexts, node, position, ptg);
                }
            }
        }

        return false;
    }

    private static List<String> getConstructorsFromRightMostContext(ContextualSymbol symbol,
        Map<IProduction, UniqueConstructor> allProdsCons) {
        List<String> result = Lists.newArrayList();

        for(Context ctx : symbol.getContexts()) {
            if(ctx.getType() == ContextType.DEEP && (ctx.getPosition().equals(ContextPosition.RIGHTMOST)
                || ctx.getPosition().equals(ContextPosition.LEFTANDRIGHTMOST))) {
                result.add(allProdsCons.get(ctx.getContext()).getCons().getConstructor());
            }
        }

        return result;
    }

    private static List<String> getConstructorsFromLeftMostContext(ContextualSymbol symbol,
        Map<IProduction, UniqueConstructor> allProdsCons) {
        List<String> result = Lists.newArrayList();

        for(Context ctx : symbol.getContexts()) {
            if(ctx.getType() == ContextType.DEEP && (ctx.getPosition().equals(ContextPosition.LEFTMOST)
                || ctx.getPosition().equals(ContextPosition.LEFTANDRIGHTMOST))) {
                result.add(allProdsCons.get(ctx.getContext()).getCons().getConstructor());
            }
        }

        return result;
    }

    private static IdentityHashMap<IStrategoTerm, IStrategoTerm> collectAllBrackets(IStrategoTerm node) {
        IdentityHashMap<IStrategoTerm, IStrategoTerm> brackets = Maps.newIdentityHashMap();

        if(node.getAttachment(ImploderAttachment.TYPE).isBracket()) {
            brackets.put(node, node);
        } else if(node instanceof IStrategoAppl) {
            // some brackets in OCaml have constructors because of optional semicolons or terms
            if(isOCamlBracket(node)) {
                brackets.put(node, node);
            }
        }

        for(IStrategoTerm child : node.getAllSubterms()) {
            brackets.putAll(collectAllBrackets(child));
        }

        return brackets;
    }

    private static boolean isOCamlBracket(IStrategoTerm node) {
        return ((IStrategoAppl) node).getConstructor().getName().equals("Bracket")
            || ((IStrategoAppl) node).getConstructor().getName().equals("Bracket2");
    }


}
