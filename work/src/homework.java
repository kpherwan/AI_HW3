import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

class Argument {
    private boolean isVariable;
    private String term;

    Argument(String argumentString) {
        if(Character.isUpperCase(argumentString.charAt(0))) {
            isVariable = false;
        }
        else {
            isVariable = true;
        }
        term = argumentString.trim();
    }

    public Map<Argument, Argument> unify(Argument argument, Map<Argument, Argument> substitutions) {
        if (this.isVariable) {
            return unifyVariable(argument, substitutions);
        }
        return unifyConstant(argument, substitutions);
    }

    private Map<Argument, Argument> unifyConstant(Argument argument, Map<Argument, Argument> substitutions) {
        // both are same constants
        if (this.term.equals(argument.term)) {
            return substitutions;
        }

        // if 'this' is constant and argument is a variable then we can unify
        if (argument.isVariable) {
            return argument.unifyVariable(this, substitutions);
        }
        return null;
    }

    private Map<Argument, Argument> unifyVariable(Argument argument, Map<Argument, Argument> substitutions) {
        // both are same variables
        if (this.term.equals(argument.term)) {
            return substitutions;
        }

        // has this variable already found a substitution before
        // if yes unify previous substitution with the new possible one
        if (substitutions.get(this.term) != null) {
            Argument previousSubstitution = substitutions.get(this.term);
            return previousSubstitution.unify(argument, substitutions);
        }

        if(!argument.isVariable) {
            substitutions.put(this, argument);
            return substitutions;
        }
        return null;
    }
}


class Literal {
    private String predicate;
    private List<Argument> arguments;
    private boolean isNegativeLiteral;

    Literal(String literalString) {
        arguments = new ArrayList<>();
        literalString = literalString.trim();
        if(literalString.charAt(0) == '~') {
            this.isNegativeLiteral = true;
        }
        this.predicate = literalString.substring(isNegativeLiteral ? 1 : 0, literalString.indexOf('(')).trim();
        String allTerms = literalString.substring(literalString.indexOf('(') + 1, literalString.indexOf(')'));

        String argumentStrings[] = allTerms.split(",");
        for(String argument: argumentStrings) {
            addToArguments(new Argument(argument));
        }
    }

    public Literal(String predicate, List<Argument> arguments, boolean isNegativeLiteral) {
        this.predicate = predicate;
        this.arguments = arguments;
        this.isNegativeLiteral = isNegativeLiteral;
    }

    public List<Argument> getArguments() {
        return arguments;
    }

    public String getPredicate() {
        return predicate;
    }

    public boolean isNegativeLiteral() {
        return isNegativeLiteral;
    }

    public void addToArguments(Argument arg) {
        arguments.add(arg);
    }

    public Literal negateLiteral() {
        return new Literal(this.predicate, this.arguments, !isNegativeLiteral);
    }
}

/*A Clause: A disjunction of literals*/
class Clause {
    private List<Literal> positiveLiterals;
    private List<Literal> negativeLiterals;

    Clause() {
        positiveLiterals = new ArrayList<>();
        negativeLiterals = new ArrayList<>();
    }

    public Clause(Literal literal) {
        positiveLiterals = new ArrayList<>();
        negativeLiterals = new ArrayList<>();

        if(literal.isNegativeLiteral()) {
            addToNegativeLiterals(literal);
        }
        else {
            addToPositiveLiterals(literal);
        }
    }

    public void addToNegativeLiterals(Literal literal) {
        negativeLiterals.add(literal);
    }

    public void addToPositiveLiterals(Literal literal) {
        positiveLiterals.add(literal);
    }

    public List<Literal> getPositiveLiterals() {
        return positiveLiterals;
    }

    public List<Literal> getNegativeLiterals() {
        return negativeLiterals;
    }

    public static Clause emptyClause() {
        return new Clause();
    }

    public boolean isEmpty() {
        return positiveLiterals.size() == 0 && negativeLiterals.size() == 0;
    }
}

class Substitution {
    Map<Argument, Argument> map;
    Clause clause1;
    Clause clause2;

    public Substitution(Map<Argument, Argument> map, Clause clause1, Clause clause2) {
        this.map = map;
        this.clause1 = clause1;
        this.clause2 = clause2;
    }
}

/*PredicateInfo: All occurrences of a predicate in the KB*/
class PredicateInfo {
    private List<Literal> positiveLiterals;
    private List<Literal> negativeLiterals;
    private List<Clause> clausesContainingPositivePredicate;
    private List<Clause> clausesContainingNegativePredicate;

    public List<Literal> getPositiveLiterals() {
        return positiveLiterals;
    }

    public List<Literal> getNegativeLiterals() {
        return negativeLiterals;
    }

    public List<Clause> getClausesContainingPositivePredicate() {
        return clausesContainingPositivePredicate;
    }

    public List<Clause> getClausesContainingNegativePredicate() {
        return clausesContainingNegativePredicate;
    }

    PredicateInfo() {
        positiveLiterals = new ArrayList<>();
        negativeLiterals = new ArrayList<>();
        clausesContainingPositivePredicate = new ArrayList<>();
        clausesContainingNegativePredicate = new ArrayList<>();
    }

    public void addToNegativeLiterals(Literal literal) {
        negativeLiterals.add(literal);
    }

    public void addToPositiveLiterals(Literal literal) {
        positiveLiterals.add(literal);
    }

    public void addClauseContainingPositivePredicate(Clause clause) {
        clausesContainingPositivePredicate.add(clause);
    }

    public void addClauseContainingNegativePredicate(Clause clause) {
        clausesContainingNegativePredicate.add(clause);
    }
}

public class homework {
    private static final double TOTAL_TIME_FOR_QUERY = 30000;
    private static final String NOT_INFERRED = "FALSE";
    private static final String INFERRED = "TRUE";

    public static void main(String[] args) {
        Scanner scanner = null;
        List<Literal> queryLiterals = new ArrayList<>();
        Map<String, PredicateInfo> predicateMap = new HashMap<>();

        try {
            //scanner = new Scanner(new File("src/work/externalInput.txt"));
            scanner = new Scanner(new File("input2.txt"));
            int noOfQueries = Integer.parseInt(scanner.nextLine());
            List<Boolean> results = new ArrayList<>(noOfQueries);

            for (int i=0; i<noOfQueries; i++) {
                queryLiterals.add(new Literal(scanner.nextLine()));
            }

            int noOfSentences = Integer.parseInt(scanner.nextLine());
            for (int i=0; i<noOfSentences; i++) {
                processSentence(scanner.nextLine(), predicateMap);
            }

            for(Literal queryLiteral: queryLiterals) {
                Literal negatedQueryLiteral = queryLiteral.negateLiteral();
                results.add(ask(predicateMap, negatedQueryLiteral));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean ask(Map<String,PredicateInfo> predicateMap, Literal negatedQueryLiteral) {
        double startTime = System.currentTimeMillis();
        String predicate = negatedQueryLiteral.getPredicate();
        PredicateInfo predicateInfo = predicateMap.get(predicate);
        Clause queryClause = new Clause(negatedQueryLiteral);

        if (predicateInfo != null) {
            Clause newClause = resolve(queryClause, predicateMap);
            do {
                if (newClause.isEmpty()) {
                    return true;
                }
                double currTime = System.currentTimeMillis();
                if ((currTime - startTime) > TOTAL_TIME_FOR_QUERY) {
                    return false;
                }
                tell(predicateMap, newClause);
                newClause = resolve(newClause, predicateMap);
            }while (true);
        }
        else {
            return false;
        }
    }

    private static Clause resolve(Clause clause, Map<String,PredicateInfo> predicateMap) {
        Map<Argument, Argument> map = resolveAgainstLiterals(clause, predicateMap);
        if (map != null) {
            System.out.println("todo");
        }

        Substitution sub = resolveAgainstClauses(clause, predicateMap);
        if (sub != null) {
            System.out.println("todo");
        }
        return null;
    }

    private static Substitution resolveAgainstClauses(Clause inputClause, Map<String,PredicateInfo> predicateMap) {
        List<Literal> positiveLiterals = inputClause.getPositiveLiterals();

        for(Literal originalLiteral: positiveLiterals) {
            String predicate = originalLiteral.getPredicate();
            PredicateInfo predicateInfo = predicateMap.get(predicate);

            if (predicateInfo != null) {
                List<Clause> clausesContainingNegativePredicate = predicateInfo.getClausesContainingNegativePredicate();
                for(Clause iterClause: clausesContainingNegativePredicate) {
                    Literal negativeLiteral = iterClause.getNegativeLiterals().stream()
                            .filter(literal -> literal.getPredicate().equals(originalLiteral.getPredicate())).findFirst().get();
                    Map<Argument, Argument> map = unify(negativeLiteral, originalLiteral);
                    if (map != null) {
                        return new Substitution(map, inputClause, iterClause);
                    }
                }
            }
        }

        List<Literal> negativeLiterals = inputClause.getNegativeLiterals();

        for(Literal originalLiteral: negativeLiterals) {
            String predicate = originalLiteral.getPredicate();
            PredicateInfo predicateInfo = predicateMap.get(predicate);

            if (predicateInfo != null) {
                List<Clause> clausesContainingPositivePredicate = predicateInfo.getClausesContainingPositivePredicate();
                for(Clause iterClause: clausesContainingPositivePredicate) {
                    Literal positiveLiteral = iterClause.getPositiveLiterals().stream()
                            .filter(literal -> literal.getPredicate().equals(originalLiteral.getPredicate())).findFirst().get();
                    Map<Argument, Argument> map = unify(positiveLiteral, originalLiteral);
                    if (map != null) {
                        return new Substitution(map, inputClause, iterClause);
                    }
                }
            }
        }
        return null;
    }

    private static void tell(Map<String,PredicateInfo> predicateMap, Clause newClause) {
    }

    private static Map<Argument, Argument> resolveAgainstLiterals(Clause clause, Map<String,PredicateInfo> predicateMap) {

        List<Literal> positiveLiterals = clause.getPositiveLiterals();

        for(Literal originalLiteral: positiveLiterals) {
            String predicate = originalLiteral.getPredicate();
            PredicateInfo predicateInfo = predicateMap.get(predicate);

            if (predicateInfo != null) {
                List<Literal> negativeLiteralList = predicateInfo.getNegativeLiterals();
                for(Literal negativeLiteral: negativeLiteralList) {
                    Map<Argument, Argument> substitutions = unify(negativeLiteral, originalLiteral);
                    if (substitutions != null) {
                        return substitutions;
                    }
                }
            }
        }

        List<Literal> negativeLiterals;
        negativeLiterals = clause.getNegativeLiterals();

        for(Literal originalLiteral: negativeLiterals) {
            String predicate = originalLiteral.getPredicate();
            PredicateInfo predicateInfo = predicateMap.get(predicate);

            if (predicateInfo != null) {
                List<Literal> positiveLiteralsList = predicateInfo.getPositiveLiterals();
                for(Literal positiveLiteral: positiveLiteralsList) {
                    Map<Argument, Argument> substitutions = unify(positiveLiteral, originalLiteral);
                    if (substitutions != null) {
                        return substitutions;
                    }
                }
            }
        }

        return null;
    }

    /* Returns substitutions for remaining *//*
    private static Map<String, String> resolve(Literal inputLiteral, Map<String,PredicateInfo> predicateMap) {
        String predicate = inputLiteral.getPredicate();
        PredicateInfo predicateInfo = predicateMap.get(predicate);

        if (predicateInfo == null) {
            return null;
        }
        List<Literal> literalList = inputLiteral.isNegativeLiteral() ? predicateInfo.getPositiveLiterals() : predicateInfo.getNegativeLiterals();
        for(Literal literal: literalList) {
            if(canUnify(literal, inputLiteral)) {
                  return unify(literal, inputLiteral);
            }
        }
        return null;
    }*/

    private static Map<Argument, Argument> unify(Literal literal1, Literal literal2) {
        Map<Argument, Argument> substitutions = new HashMap<>();
        List<Argument> list1 = literal1.getArguments();
        List<Argument> list2 = literal2.getArguments();

        for(int i = 0; i < list1.size(); i++) {
            substitutions = list1.get(i).unify(list2.get(i), substitutions);
            if(substitutions == null) {
                return null;
            }
        }
        return substitutions;
    }


    private static void processSentence(String sentence, Map<String, PredicateInfo> predicateMap) {
        if (sentence.contains("=>")) {
            String[] twoParts = sentence.split("=>");
            if (twoParts.length > 2) {
                System.out.println("OOPS SOMETHING WENT WRONG GIRLLL");
                System.exit(1);
            }
            else {
                Literal conclusion = new Literal(twoParts[1]);
                Clause clause = new Clause();
                if (conclusion.isNegativeLiteral()) {
                    clause.addToNegativeLiterals(conclusion);
                }
                else {
                    clause.addToPositiveLiterals(conclusion);
                }

                String[] premises = twoParts[0].split("&");
                Literal premiseLiteral;
                for(String premise: premises) {
                    premiseLiteral = new Literal(premise);
                    if (premiseLiteral.isNegativeLiteral()) {
                        clause.addToPositiveLiterals(premiseLiteral);
                    }
                    else {
                        clause.addToNegativeLiterals(premiseLiteral);
                    }
                }
                addPredicatesOfClauseToMap(clause, predicateMap);
            }
        }
        else {
            Literal literal = new Literal(sentence);
            String predicate = literal.getPredicate();
            PredicateInfo predicateInfo = predicateMap.get(predicate);

            if (predicateInfo == null) {
                predicateInfo = new PredicateInfo();
                predicateMap.put(predicate, predicateInfo);
            }

            if (literal.isNegativeLiteral()) {
                predicateInfo.addToNegativeLiterals(literal);
            }
            else {
                predicateInfo.addToPositiveLiterals(literal);
            }
        }
    }

    private static void addPredicatesOfClauseToMap(Clause clause, Map<String,PredicateInfo> predicateMap) {
        List<Literal> positiveLiterals = clause.getPositiveLiterals();
        List<Literal> negativeLiterals = clause.getNegativeLiterals();

        for(Literal positiveLiteral: positiveLiterals) {
            PredicateInfo predicateInfo = predicateMap.get(positiveLiteral.getPredicate());
            if (predicateInfo == null) {
                predicateInfo = new PredicateInfo();
                predicateMap.put(positiveLiteral.getPredicate(), predicateInfo);
            }
            predicateInfo.addClauseContainingPositivePredicate(clause);
        }

        for(Literal negativeLiteral: negativeLiterals) {
            PredicateInfo predicateInfo = predicateMap.get(negativeLiteral.getPredicate());
            if (predicateInfo == null) {
                predicateInfo = new PredicateInfo();
                predicateMap.put(negativeLiteral.getPredicate(), predicateInfo);
            }
            predicateInfo.addClauseContainingNegativePredicate(clause);
        }
    }


    private static void addToOutputFile() {

        try {
            FileWriter fw = new FileWriter("output.txt");

            int i=0;
            fw.write("FAIL" + "\n");
            fw.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
