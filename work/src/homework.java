import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class homework {
    // minimum one second
    private static final double TOTAL_TIME_FOR_QUERY = 1000;
    private static final double TOTAL_ATTEMPTS = 150;
    private static final String NOT_INFERRED = "FALSE";
    private static final String INFERRED = "TRUE";

    public static void main(String[] args) {
        double startTime = System.currentTimeMillis();
        Scanner scanner = null;
        List<Literal> queryLiterals = new ArrayList<>();
        Map<String, PredicateInfo> predicateMap = new HashMap<>();

        try {
            scanner = new Scanner(new File("../../input.txt"));
            //scanner = new Scanner(new File("input.txt"));
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
                Map<String, PredicateInfo> predicateMapClone = new HashMap<>();
                cloneMap(predicateMap, predicateMapClone);
                boolean askResult = ask(predicateMapClone, queryLiteral);
                results.add(askResult);
                //System.out.println("Result " + askResult);
                //System.out.println();
            }
            addToOutputFile(results);
            double currTime = System.currentTimeMillis();
            System.out.println("Time taken: " + ((currTime - startTime)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void cloneMap(Map<String,PredicateInfo> predicateMap, Map<String,PredicateInfo> predicateMapClone) {
        for (Map.Entry<String,PredicateInfo> entry : predicateMap.entrySet()) {
            predicateMapClone.put(entry.getKey(),
                    new PredicateInfo(entry.getValue()));
        }
    }

    private static boolean ask(Map<String,PredicateInfo> predicateMap, Literal queryLiteral) {
        double startTime = System.currentTimeMillis();
        int noOfAttempts = 0;
        Literal negatedQueryLiteral = queryLiteral.negateLiteral();
        String predicate = negatedQueryLiteral.getPredicate();

        PredicateInfo predicateInfo = predicateMap.get(predicate);
        Clause queryClause = new Clause(negatedQueryLiteral);
        addClauseToMap(queryClause, predicateMap);
        Queue<Clause> queue = new LinkedList<>();
        Clause newClause;

        if (predicateInfo != null) {
             queue.addAll(resolve(queryClause, predicateMap));
            do {
                if(queue == null || queue.isEmpty()/* || !newClause.isValid()*/) {
                    return false;
                }

                newClause = queue.poll();

                if (newClause.isEmpty()) {
                    return true;
                }

                //newClause.printClause();
                double currTime = System.currentTimeMillis();
                noOfAttempts++;
                if ((currTime - startTime) > TOTAL_TIME_FOR_QUERY && noOfAttempts > TOTAL_ATTEMPTS) {
                    System.out.println("TIMEOUT");
                    return false;
                }
                int exitCode = addClauseToMap(newClause, predicateMap);
                if (exitCode == 0) {
                    queue.addAll(resolve(newClause, predicateMap));
                }

            }while (true);
        }
        else {
            return false;
        }
    }

    private static List<Clause> resolve(Clause clause, Map<String,PredicateInfo> predicateMap) {
        List<Substitution> subList1 = resolveAgainstLiterals(clause, predicateMap);
        List<Clause> resultClauses = new ArrayList<>();
        if (!subList1.isEmpty()) {
            for(Substitution substitution: subList1) {
                Clause resolvedClause = new Clause();
                resolvedClause.addListToPositiveLiterals(getLiteralsPostUnification(substitution, clause.getPositiveLiterals()));
                resolvedClause.addListToNegativeLiterals(getLiteralsPostUnification(substitution, clause.getNegativeLiterals()));
                resultClauses.add(resolvedClause);
            }
            return resultClauses;
        }

        List<Substitution> subList = resolveAgainstClauses(clause, predicateMap);
        if (!subList.isEmpty()) {
            for(Substitution sub: subList) {
                Clause resolvedClause = new Clause();
                resolvedClause.addListToPositiveLiterals(getLiteralsPostUnification(sub, sub.clause1.getPositiveLiterals()));
                resolvedClause.addListToPositiveLiterals(getLiteralsPostUnification(sub, sub.clause2.getPositiveLiterals()));

                resolvedClause.addListToNegativeLiterals(getLiteralsPostUnification(sub, sub.clause1.getNegativeLiterals()));
                resolvedClause.addListToNegativeLiterals(getLiteralsPostUnification(sub, sub.clause2.getNegativeLiterals()));
                resultClauses.add(resolvedClause);
            }
        }
        return resultClauses;
    }

    private static Set<Literal> getLiteralsPostUnification(Substitution sub, Set<Literal> literals) {
        Set<Literal> result = new HashSet<>();
        int resolvedCount = 0;
        for (Literal literal: literals) {
            if ((literal.equals(sub.literal1) || literal.equals(sub.literal2)) && resolvedCount < 2) {
                resolvedCount++;
                continue;
            }
            else if (sub.map != null && !sub.map.isEmpty()){
                List<Argument> newArgs = new ArrayList<>();
                for(Argument arg: literal.getArguments()) {
                    if (sub.map.get(arg) != null) {
                        Argument substitutedArg = new Argument(sub.map.get(arg).getTerm());
                        newArgs.add(substitutedArg);
                    }
                    else {
                        newArgs.add(arg);
                    }
                }
                result.add(new Literal(literal.getPredicate(), newArgs, literal.isNegativeLiteral()));
            }
            else {
                result.add(literal);
            }
        }
        return result;
    }

    private static List<Substitution> resolveAgainstClauses(Clause inputClause, Map<String,PredicateInfo> predicateMap) {
        Set<Literal> positiveLiterals = inputClause.getPositiveLiterals();
        List<Substitution> substitutions = new ArrayList<>();

        for(Literal originalLiteral: positiveLiterals) {
            String predicate = originalLiteral.getPredicate();
            PredicateInfo predicateInfo = predicateMap.get(predicate);

            if (predicateInfo != null) {
                List<Clause> clausesContainingNegativePredicate = predicateInfo.getClausesContainingNegativePredicate();
                for(Clause iterClause: clausesContainingNegativePredicate) {
                    Literal negativeLiteral = iterClause.getNegativeLiterals().stream()
                            .filter(literal -> literal.getPredicate().equals(originalLiteral.getPredicate())).findFirst().get();
                    if (canLiteralsResolve(negativeLiteral, originalLiteral)) {
                        Map<Argument, Argument> map = unify(originalLiteral, negativeLiteral);
                        if (map != null) {
                            substitutions.add(new Substitution(map, inputClause, iterClause, negativeLiteral, originalLiteral));
                        }
                    }
                }
            }
        }

        Set<Literal> negativeLiterals = inputClause.getNegativeLiterals();

        for(Literal originalLiteral: negativeLiterals) {
            String predicate = originalLiteral.getPredicate();
            PredicateInfo predicateInfo = predicateMap.get(predicate);

            if (predicateInfo != null) {
                List<Clause> clausesContainingPositivePredicate = predicateInfo.getClausesContainingPositivePredicate();
                for(Clause iterClause: clausesContainingPositivePredicate) {
                    Literal positiveLiteral = iterClause.getPositiveLiterals().stream()
                            .filter(literal -> literal.getPredicate().equals(originalLiteral.getPredicate())).findFirst().get();
                    if (canLiteralsResolve(positiveLiteral, originalLiteral)) {
                        Map<Argument, Argument> map = unify(originalLiteral, positiveLiteral);
                        if (map != null) {
                            substitutions.add(new Substitution(map, inputClause, iterClause, positiveLiteral, originalLiteral));
                        }
                    }
                }
            }
        }
        return substitutions;
    }


    private static List<Substitution> resolveAgainstLiterals(Clause clause, Map<String,PredicateInfo> predicateMap) {

        Set<Literal> positiveLiterals = clause.getPositiveLiterals();
        List<Substitution> substitutionList = new ArrayList<>();

        for(Literal originalLiteral: positiveLiterals) {
            String predicate = originalLiteral.getPredicate();
            PredicateInfo predicateInfo = predicateMap.get(predicate);

            if (predicateInfo != null) {
                List<Literal> negativeLiteralList = predicateInfo.getNegativeLiterals();
                for(Literal negativeLiteral: negativeLiteralList) {
                    Map<Argument, Argument> substitutionMap = unify(negativeLiteral, originalLiteral);
                    if (substitutionMap != null) {
                        substitutionList.add(new Substitution(substitutionMap, null, null, negativeLiteral, originalLiteral));
                    }
                    else if (canLiteralsNullify(negativeLiteral, originalLiteral)) {
                        substitutionList.add(new Substitution(null, null, null, negativeLiteral, originalLiteral));
                    }
                }
            }
        }

        Set<Literal> negativeLiterals;
        negativeLiterals = clause.getNegativeLiterals();

        for(Literal originalLiteral: negativeLiterals) {
            String predicate = originalLiteral.getPredicate();
            PredicateInfo predicateInfo = predicateMap.get(predicate);

            if (predicateInfo != null) {
                List<Literal> positiveLiteralsList = predicateInfo.getPositiveLiterals();
                for(Literal positiveLiteral: positiveLiteralsList) {
                    Map<Argument, Argument> substitutionMap = unify(positiveLiteral, originalLiteral);
                    if (substitutionMap != null) {
                        substitutionList.add(new Substitution(substitutionMap, null, null, positiveLiteral, originalLiteral));
                    }
                    if (canLiteralsNullify(positiveLiteral, originalLiteral)) {
                        substitutionList.add(new Substitution(null, null, null, positiveLiteral, originalLiteral));
                    }
                }
            }
        }

        return substitutionList;
    }

    /*Exactly alike literals for A OR ~A*/
    private static boolean canLiteralsNullify(Literal literal1, Literal literal2) {
        if (literal1.getPredicate().equals(literal2.getPredicate()) && literal1.isNegativeLiteral() != literal2.isNegativeLiteral()) {
            for(int i = 0; i < literal1.getArguments().size(); i++) {
                if (!literal1.getArguments().get(i).equals(literal2.getArguments().get(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean canLiteralsResolve(Literal literal1, Literal literal2) {
        Argument arg1, arg2;
        if (literal1.getPredicate().equals(literal2.getPredicate())) {
            for(int i = 0; i < literal1.getArguments().size(); i++) {
                arg1 = literal1.getArguments().get(i);
                arg2 = literal2.getArguments().get(i);

                // diff constants
                if(!arg1.isVariable() && !arg2.isVariable() && !arg1.getTerm().equals(arg2.getTerm())) {
                    return false;
                }
            }
            return true;
        }
        return false;
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
                addClauseToMap(clause, predicateMap);
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

    private static int addClauseToMap(Clause clause, Map<String,PredicateInfo> predicateMap) {
        Set<Literal> positiveLiterals = clause.getPositiveLiterals();
        Set<Literal> negativeLiterals = clause.getNegativeLiterals();
        Set<String> distinctPredicates = new HashSet<>();

        for(Literal positiveLiteral: positiveLiterals) {
            PredicateInfo predicateInfo = predicateMap.get(positiveLiteral.getPredicate());
            if (predicateInfo == null) {
                predicateInfo = new PredicateInfo();
                predicateMap.put(positiveLiteral.getPredicate(), predicateInfo);
            }
            if (!distinctPredicates.contains(positiveLiteral.getPredicate())) {
                if (predicateInfo.getClausesContainingPositivePredicate().contains(clause)) {
                    return 1;
                }
                if (predicateInfo.getPositiveLiterals().contains(positiveLiteral)) {
                    return 1;
                }
                predicateInfo.addClauseContainingPositivePredicate(clause);
                distinctPredicates.add(positiveLiteral.getPredicate());
            }
        }

        distinctPredicates = new HashSet<>();
        for(Literal negativeLiteral: negativeLiterals) {
            PredicateInfo predicateInfo = predicateMap.get(negativeLiteral.getPredicate());
            if (predicateInfo == null) {
                predicateInfo = new PredicateInfo();
                predicateMap.put(negativeLiteral.getPredicate(), predicateInfo);
            }
            if (!distinctPredicates.contains(negativeLiteral.getPredicate())) {
                if (predicateInfo.getClausesContainingNegativePredicate().contains(clause)) {
                    return 1;
                }
                if (predicateInfo.getNegativeLiterals().contains(negativeLiteral)) {
                    return 1;
                }
                predicateInfo.addClauseContainingNegativePredicate(clause);
                distinctPredicates.add(negativeLiteral.getPredicate());
            }
        }
        return 0;
    }


    private static void addToOutputFile(List<Boolean> results) {

        try {
            FileWriter fw = new FileWriter("output.txt");
            boolean result;
            boolean isLast = false;

            for(int i=0; i<results.size(); i++) {
                result = results.get(i);
                isLast = ( i == (results.size() - 1));
                if(result) {
                    fw.write(INFERRED + (isLast ? "" : "\n"));
                }
                else {
                    fw.write(NOT_INFERRED + (isLast ? "" : "\n"));
                }
            }

            fw.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Argument {
    private boolean isVariable;
    private String term;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Argument argument = (Argument) o;
        return isVariable == argument.isVariable && term.equals(argument.term);
    }

    public boolean isVariable() {
        return isVariable;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isVariable, term);
    }

    public String getTerm() {
        return term;
    }

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

        if(argument.isVariable() && this.isVariable()) {
            substitutions.put(argument, this);
            return substitutions;
        }
        return null;
    }
}


class Literal {
    private String predicate;
    private List<Argument> arguments;
    private boolean isNegativeLiteral;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Literal literal = (Literal) o;

        if (this.arguments.size() != literal.arguments.size()) {
            return false;
        }

        for (int i=0; i<this.arguments.size(); i++) {
            if(!this.arguments.get(i).equals(literal.arguments.get(i))) {
                return false;
            }
        }
        return isNegativeLiteral == literal.isNegativeLiteral && Objects.equals(predicate, literal.predicate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicate, arguments, isNegativeLiteral);
    }

    public void setNegativeLiteral(boolean negativeLiteral) {
        isNegativeLiteral = negativeLiteral;
    }

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

    public void printLiteral() {
        if (this.isNegativeLiteral) {
            System.out.print("~");
        }
        System.out.print(this.predicate + "(");

        for(Argument arg: this.getArguments()) {
            System.out.print(arg.getTerm() + ",");
        }

        System.out.print(") V ");
    }
}

/*A Clause: A disjunction of literals*/
class Clause {
    private Set<Literal> positiveLiterals;
    private Set<Literal> negativeLiterals;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Clause clause = (Clause) o;
        if ((this.getNegativeLiterals().size() != clause.getNegativeLiterals().size()) ||
                (this.getPositiveLiterals().size() != clause.getPositiveLiterals().size())) {
            return false;
        }

        for(Literal literal: this.getNegativeLiterals()) {
            if (!clause.getNegativeLiterals().contains(literal)) {
                return false;
            }
        }

        for(Literal literal: this.getPositiveLiterals()) {
            if (!clause.getPositiveLiterals().contains(literal)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(positiveLiterals, negativeLiterals);
    }

    Clause() {
        positiveLiterals = new HashSet<>();
        negativeLiterals = new HashSet<>();
    }

    public Clause(Literal literal) {
        positiveLiterals = new HashSet<>();
        negativeLiterals = new HashSet<>();

        if(literal.isNegativeLiteral()) {
            addToNegativeLiterals(literal);
        }
        else {
            addToPositiveLiterals(literal);
        }
    }

    public void addToNegativeLiterals(Literal literal) {
        literal.setNegativeLiteral(true);
        negativeLiterals.add(literal);
    }

    public void addToPositiveLiterals(Literal literal) {
        literal.setNegativeLiteral(false);
        positiveLiterals.add(literal);
    }

    public void addListToPositiveLiterals(Set<Literal> literals) {
        positiveLiterals.addAll(literals);
    }

    public void addListToNegativeLiterals(Set<Literal> literals) {
        negativeLiterals.addAll(literals);
    }

    public Set<Literal> getPositiveLiterals() {
        return positiveLiterals;
    }

    public Set<Literal> getNegativeLiterals() {
        return negativeLiterals;
    }

    public static Clause emptyClause() {
        return new Clause();
    }

    public boolean isEmpty() {
        return positiveLiterals.size() == 0 && negativeLiterals.size() == 0;
    }

    public void printClause() {
        for (Literal negativeLiteral: this.getNegativeLiterals()) {
            negativeLiteral.printLiteral();
        }

        for (Literal positiveLiteral: this.getPositiveLiterals()) {
            positiveLiteral.printLiteral();
        }
        System.out.println("");
    }

    public boolean isValid() {
        Set<Literal> literalSet = new HashSet<>();
        for(Literal literal: this.getPositiveLiterals()) {
            literalSet.add(literal);
        }
        for(Literal literal: this.getNegativeLiterals()) {
            literalSet.add(literal);
        }
        return ((this.getPositiveLiterals().size() + this.getNegativeLiterals().size()) == literalSet.size());
    }
}

class Substitution {
    Map<Argument, Argument> map;
    Clause clause1;
    Clause clause2;
    Literal literal1;
    Literal literal2;

    public Substitution(Map<Argument, Argument> map, Clause clause1, Clause clause2, Literal literal1, Literal literal2) {
        this.map = map;
        this.clause1 = clause1;
        this.clause2 = clause2;
        this.literal1 = literal1;
        this.literal2 = literal2;
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

    PredicateInfo(PredicateInfo predicateInfoInput) {
        positiveLiterals = new ArrayList<>(predicateInfoInput.positiveLiterals);
        negativeLiterals = new ArrayList<>(predicateInfoInput.negativeLiterals);
        clausesContainingPositivePredicate = new ArrayList<>(predicateInfoInput.clausesContainingPositivePredicate);
        clausesContainingNegativePredicate = new ArrayList<>(predicateInfoInput.clausesContainingNegativePredicate);
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

    public boolean contains(Clause inputClause) {
        for(Clause currentClause: clausesContainingPositivePredicate) {
            if(currentClause.equals(inputClause))
                return true;
        }
        for(Clause currentClause: clausesContainingNegativePredicate) {
            if(currentClause.equals(inputClause))
                return true;
        }
        return false;
    }
}

