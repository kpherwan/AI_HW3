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

    public String getPredicate() {
        return predicate;
    }

    public boolean isNegativeLiteral() {
        return isNegativeLiteral;
    }

    public void addToArguments(Argument arg) {
        arguments.add(arg);
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
}

/*PredicateInfo: All occurrences of a predicate in the KB*/
class PredicateInfo {
    private List<Literal> positiveLiterals;
    private List<Literal> negativeLiterals;
    private List<Clause> clausesContainingPositivePredicate;
    private List<Clause> clausesContainingNegativePredicate;

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
    public static void main(String[] args) {
        Scanner scanner = null;
        List<Literal> queryLiterals = new ArrayList<>();
        Map<String, PredicateInfo> predicateMap = new HashMap<>();

        try {
            //scanner = new Scanner(new File("src/work/externalInput.txt"));
            scanner = new Scanner(new File("input4.txt"));
            int noOfQueries = Integer.parseInt(scanner.nextLine());

            for (int i=0; i<noOfQueries; i++) {
                queryLiterals.add(new Literal(scanner.nextLine()));
            }

            int noOfSentences = Integer.parseInt(scanner.nextLine());
            for (int i=0; i<noOfSentences; i++) {
                processSentence(scanner.nextLine(), predicateMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

                }
                clause.addToPositiveLiterals(conclusion);

                String[] premises = twoParts[0].split("&");
                for(String premise: premises) {
                    clause.addToNegativeLiterals(new Literal(premise));
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
