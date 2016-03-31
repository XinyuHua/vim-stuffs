package edu.sjtu.cs.action.util;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.parser.lexparser.*;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
/**
 * Created by xinyu on 16-3-30.
 */

public class Parser {
    private static LexicalizedParser lp;
    private static TreebankLanguagePack tlp;
    private static GrammaticalStructureFactory gsf;

    public static void main(String[] args)throws Exception{
        String document  = "United States invaded Iraq.";

        Parser p = new Parser();
        List<List<String>> out = dependencyParseDocument(document);
        List<String> dep = out.get(0);
        List<String> postag = out.get(1);
        for(String s : dep){
            System.out.println(s);
        }
        System.out.println("----------");
        for(String s : postag){
            System.out.println(s);
        }
    }

    public Parser()throws Exception{
        lp = LexicalizedParser.loadModel(
                "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz",
                "-maxLength", "80", "-retainTmpSubcategories");
        tlp = new PennTreebankLanguagePack();
        gsf = tlp.grammaticalStructureFactory();
    }

    public static List<List<String>> dependencyParseDocument(String document) throws Exception{
        StringReader sr = new StringReader(document);
        List<String> parsed_result = new ArrayList<>();
        List<String> postag_result = new ArrayList<>();
        for(List<HasWord> sentence : new DocumentPreprocessor(sr)){

            String[] query = new String[sentence.size()];
            for(int i = 0; i < sentence.size(); ++i){
                query[ i ] = sentence.get(i).word();
            }
            if(isGoodSentence(query)){
                Tree parse = lp.apply(sentence);
                String tmp = "";
                GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
                Collection<TypedDependency> tdl = gs.typedDependencies();
                postag_result.add(parse.taggedYield().toString());
                for(TypedDependency td : tdl){
                    //System.out.println(td);
                    tmp += td.toString() + " ";
                }
                parsed_result.add(tmp);
            }
        }

        List<List<String>> result = new ArrayList<List<String>>();
        result.add(parsed_result);
        result.add(postag_result);

        return result;
    }

    private static boolean isGoodSentence(String[] query)throws Exception{
        try{
            int length = query.length;
            //condition 1: length between 5 and 29 tokens
            if ( length <= 3 || length > 30){
                return false;
            }

            //condition 2: starts with capital letter; ends with .
            if (query[0].charAt(0) < 'A' || query[0].charAt(0) > 'Z' || !query[length - 1].equals(".")){
                return false;
            }

            int badWords = 0, noisyCharacters = 0, numbers = 0;

            for (int i = 0; i < length; i++)
            {
                String currentToken = query[ i ];
                boolean isBad = true;
                for(int j = 0; j < currentToken.length(); j++){
                    if(currentToken.charAt(j) >= 'A' && currentToken.charAt(j) <= 'Z' || currentToken.charAt(j) >= 'a' && currentToken.charAt(j) <= 'z')
                        isBad = false;
                    else if (!(currentToken.charAt(j) >= '0' && currentToken.charAt(j) <= '9'
                            || currentToken.charAt(j) == ',' || currentToken.charAt(j) == '.'
                            || currentToken.charAt(j) == '-' || currentToken.charAt(j) == '\"')){
                        noisyCharacters++;
                    }

                    if (currentToken.charAt(j) >= '0' && currentToken.charAt(j) <= '9') numbers++;
                }
                if (currentToken.length() == 1
                        && (currentToken.charAt(0) >= 'A' && currentToken.charAt(0) <= 'Z'
                        || currentToken.charAt(0) >= 'a' && currentToken.charAt(0) <= 'z')
                        && !(currentToken.charAt(0) == 'a' || currentToken.charAt(0) == 'A' || currentToken.charAt(0) == 'I'))
                    isBad = true;
                if(isBad) badWords++;
            }
            // condition 3: characters other than letters,numbers,',','.',' ','-','"' <=6
            if (noisyCharacters > 6){
                return false;
            }
            //condition 4: pure non-letter words + wrong single-letter words <=4
            if (badWords > 4){
                return false;
            }
            //condition 5: number characters should not exceed 10
            if (numbers > 10){
                return false;
            }

            //condition 6: More than half words with upper captital
            int capCount = 0;
            for(String token : query){
                if(token.length() == 1){
                    continue;
                }

                if(token.charAt(0) >= 'A' && token.charAt(0) <= 'Z' && token.charAt(1) >= 'a' && token.charAt(1) <= 'z'){
                    capCount++;
                }
            }

//            if (1.0 * capCount / length > 0.5){
//                return false;
//            }

            //condition 7: Special char
            for(String token : query){
                if(token.contains("/") || token.contains("\\")){
                    return false;
                }
            }

        }
        catch (Exception e1){
            return false;
        }
        return true;
    }
}
