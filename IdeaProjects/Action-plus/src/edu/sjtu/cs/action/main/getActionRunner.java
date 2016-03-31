package edu.sjtu.cs.action.main;

import edu.sjtu.cs.action.knowledgebase.ProbaseClient;
import edu.sjtu.cs.action.knowledgebase.Wordnet;
import edu.sjtu.cs.action.util.Pair;
import edu.sjtu.cs.action.util.Action;
import edu.sjtu.cs.action.util.Parser;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by xinyu on 16-3-30.
 * This class extract action instance from parsed and postagged
 * bing news.
 */
public class getActionRunner implements Runnable{
    final private static String OUTPUT_URL = "dat/action_occurrence/";
    final private static String BING_NEWS_URL = "dat/news/bing_news_sliced/";
    final private static String BING_NEWS_PARSED_URL = "dat/news/bing_news_sliced_parsed/";
    final private static String BING_NEWS_POSTAG_URL = "dat/bews/bing_news_sliced_postag/";
    private static String[] pronList = {"he","she","him","her","i","me","you","they","them","his","mine","its","theirs","yours"};
    private  ProbaseClient pb;
    private static Wordnet wn;
    private static List<Set<String>> inflectionList;
    private static HashSet<String> inflectionSet;
    private static HashSet<String> dummyVerbs;
    private static List<String[]> actionList;
    private static HashSet<String> verbTagSet;
    private static List<HashSet<String>> synList;
    private static HashSet<String> synsetSet;
    private int part;

    public getActionRunner( int part, ProbaseClient pb, Wordnet wn,
                                List<String[]> al,  List<Set<String>> il,
                                HashSet<String> is,	HashSet<String> dummyVerbSet, HashSet<String> verbTagSet,
                                List<HashSet<String>> synList, HashSet<String> synsetSet){
        this.pb = pb;
        this.wn = wn;
        this.dummyVerbs = dummyVerbSet;
        this.verbTagSet = verbTagSet;
        this.actionList = al;
        this.inflectionList = il;
        this.inflectionSet = is;
        this.part = part;
        this.synList = synList;
        this.synsetSet = synsetSet;
    }

    public void test(String document)throws Exception{
        Parser parser = new Parser();
        List<List<String>> parsingResult = parser.dependencyParseDocument(document);
        List<String> parsed = parsingResult.get(0);
        List<String> postag = parsingResult.get(1);
        for(int sentenceIdx = 0; sentenceIdx < parsed.size(); ++sentenceIdx) {
            String extracted = extractActionFromSentence(parsed.get(sentenceIdx), postag.get(sentenceIdx));
            System.out.println(sentenceIdx + ":" + extracted);
        }
    }

    private String extractActionFromSentence(String parsed, String postag)throws Exception{
        String[] posStr = postag.trim().replaceAll("\\[|\\]","").split(",\\s+");
        String[] parsedStr = parsed.trim().split("\\)");
        List<Set<Pair>> dependencyTree = new ArrayList<>();
        for(int i = 0; i < posStr.length + 1; ++i){
            dependencyTree.add(new HashSet<>());
        }

        for(String parsedPart : parsedStr ) {
            String dep = parsedPart.substring(0, parsedPart.indexOf("(")).trim();
            String content = parsedPart.substring(parsedPart.indexOf("(") + 1).trim();
            String[] sp = content.split(",\\s+");
            int left = Integer.parseInt(sp[0].substring(sp[0].lastIndexOf("-") + 1));
            int right = Integer.parseInt(sp[1].substring(sp[1].lastIndexOf("-") + 1));
            Set<Pair> set = dependencyTree.get(left);
            set.add(new Pair(dep,right));
        }
        int cnt = 0;
//        for(Set<Pair> set : dependencyTree){
//            System.out.print(cnt + "\t");
//            for(Pair p : set){
//                System.out.print(p + " ");
//            }
//            System.out.println();
//            cnt++;
//        }

        /*
         * Initialize verbIds list and tokens list.
         * verbIds list stores indeces of verbs, tokens list stores tokens
         * in the original sentence, in the original order.
         */
        cnt = 1;
        List<Integer> verbIds = new ArrayList<Integer>();
        List<String> tokens = new ArrayList<String>();
        tokens.add("ROOT");
        for(String pos : posStr){
            pos = pos.trim();
            String sp[] = pos.split("/");
            String token = sp[0];
            String TAG = sp[1];
            if(verbTagSet.contains(TAG) && !dummyVerbs.contains(token)){
                verbIds.add(cnt);
            }
            tokens.add(token);
            cnt++;
        }

        /*
         * Search actions in the document.
         */
        List<Action> resultActionList = searchAction(tokens, dependencyTree, verbIds);
        Object[] output = writeActionAll(resultActionList, 0);
        String toWrite = (String) output[ 0 ];
        return toWrite;
    }

    public void run(){
        File fileToWrite = new File(OUTPUT_URL + part + ".txt"); try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(fileToWrite));
            BufferedReader parsedNewsReader = new BufferedReader(new FileReader(BING_NEWS_PARSED_URL + "bing_news_parsed_" + part + ".txt"));
            BufferedReader postagNewsReader = new BufferedReader(new FileReader(BING_NEWS_POSTAG_URL + "bing_news_pos_" + part + ".txt"));
            String line = null;
            int foundNumber = 0; // cnt = -1, which means the first news is numbered as 0
            int newsIdx = -1;
            int oldIdx = -1;
            while((line = parsedNewsReader.readLine())!=null){
                newsIdx = Integer.parseInt(line.split("\t")[0]);
                String parsed = line.split("\t")[1];
                line = postagNewsReader.readLine();
                String postag = line.split("\t")[1];
                if(newsIdx % 1000 == 0){
                    System.out.println("part:" + part +" read:" + newsIdx + " found:" + foundNumber);
                }
                String[] posStr = postag.replaceAll("\\[|\\]","").split(",\\s+");
                List<Set<Pair>> dependencyTree = new ArrayList<Set<Pair>>();
                for(int i = 0; i < posStr.length + 1; ++i){
                    dependencyTree.add(new HashSet<Pair>());
                }

                for(String parsedPart : parsed.split("\\) ")){
                    String dep = parsedPart.substring(0, parsedPart.indexOf("("));
                    String content = parsedPart.substring(parsedPart.indexOf("(") + 1);
                    String[] sp = content.split(",\\s+");
                    int left = Integer.parseInt(sp[0].substring(sp[0].lastIndexOf("-") + 1));
                    int right = Integer.parseInt(sp[1].substring(sp[1].lastIndexOf("-") + 1));
                    Set<Pair> set = dependencyTree.get(left);
                    set.add(new Pair(dep,right));
                }

				/*
				 * Display dependency tree
				 */
//				int cnt = 0;
//				for(Set<Pair> set : dependencyTree){
//					System.out.print(cnt + "\t");
//					for(Pair p : set){
//						System.out.print(p + " ");
//					}
//					System.out.println();
//					cnt++;
//				}

				/*
				 * Initialize verbIds list and tokens list.
				 * verbIds list stores indeces of verbs, tokens list stores tokens
				 * in the original sentence, in the original order.
				 */
                int cnt = 1;
                List<Integer> verbIds = new ArrayList<Integer>();
                List<String> tokens = new ArrayList<String>();
                tokens.add("ROOT");
                for(String pos : posStr){
                    pos = pos.trim();
                    String sp[] = pos.split("/");
                    String token = sp[0];
                    String TAG = sp[1];
                    if(verbTagSet.contains(TAG) && !dummyVerbs.contains(token)){
                        verbIds.add(cnt);
                    }
                    tokens.add(token);
                    cnt++;
                }

				/*
				 * Search actions in the document.
				 */
                String toWrite = "";
                List<Action> resultActionList = searchAction(tokens, dependencyTree, verbIds);
                Object[] output = writeActionAll(resultActionList, foundNumber);
                toWrite = (String) output[ 0 ];
                foundNumber = (int) output[ 1 ];

                if(toWrite.length() != 0){
                    if(oldIdx == newsIdx){
                        bw.append(toWrite);
                    }else{
                        bw.newLine();
                        bw.append(newsIdx + toWrite);
                        oldIdx = newsIdx;
                    }
                    bw.flush();
                }
            }

            parsedNewsReader.close();
            postagNewsReader.close();
            bw.close();
            pb.disconnect();
        }
        catch(Exception e){
            e.printStackTrace();
            System.out.println(e);
        }
    }

    private Object[] writeActionAll(List<Action> resultActionList, int foundNumber)throws Exception{
        String toWrite = "";
        for(Action ac : resultActionList){
            foundNumber++;
            toWrite += "\t" + ac.toString();
        }
        Object[] result = new Object[2];
        result[0] = toWrite;
        result[1] = foundNumber;
        return result;
    }

    private Object[] writeActionForKnown(List<Action> resultActionList, int foundNumber)throws Exception{
        String toWrite = "";
        for(Action ac : resultActionList){
            String subj = ac.getSubj();
            String verb = ac.getVerb();
            String obj = ac.getObj();

            if(!inflectionSet.contains(verb) && !synsetSet.contains(verb)){
                continue;
            }

            for(int i = 0; i < actionList.size(); ++i){
                String[] action = actionList.get(i);
                Set<String> inflect = inflectionList.get(i);
                HashSet<String> synSet = synList.get(i);
                if(inflect.contains(verb) || synSet.contains(verb)){
                    //System.out.println(action[0] + "-" + subj);
                    if(pb.isPair(action[0], subj) && pb.isPair(action[2], obj)){
                        foundNumber ++;
                        toWrite += "\t" + action[0] + "_" + action[1] + "_" + action[2];
                        toWrite += "(" + ac.toString() + ")";
                    }
                }
            }
        }

        Object[] result = new Object[2];
        result[ 0 ] = toWrite;
        result[ 1 ] = foundNumber;
        return result;
    }

    /*
     * This method search actions in the document, it starts searching from
     * each verb stored in the verbIds list.
     */
    private List<Action> searchAction(List<String> tokens, List<Set<Pair>> dependencyTree, List<Integer> verbIds) throws Exception{
        List<Action> output = new ArrayList<Action>();
        for(Integer id : verbIds){
            output.addAll(searchActionForVerb(tokens,dependencyTree,id));
        }
        return output;
    }


    /*
     * This method search action given a certain verbId, it might find multiple actions given a single verb.
     */
    private List<Action> searchActionForVerb(List<String> tokens, List<Set<Pair>> dependencyTree, int verbId) throws Exception{
        String verb= tokens.get(verbId);
        Set<Pair> verbChildrenSet = dependencyTree.get(verbId);
        List<Action> output = new ArrayList<Action>();

		/*
		 * Traverse children of the verb node, search for subjects and objects.
		 */
        List<Integer> subjs = new ArrayList<Integer>();
        List<Integer> objs = new ArrayList<Integer>();
        boolean isPassive = false;
        List<Integer> buffer = new ArrayList<Integer>();

        for(Pair verbRelatedPair : verbChildrenSet){
            if(verbRelatedPair.getDep().equals("nsubj")){
                int id = verbRelatedPair.getPos();
                subjs.add(id);
            }else if(verbRelatedPair.getDep().equals("nmod")){
                int id = verbRelatedPair.getPos();
                buffer.add(id);
            }else if(verbRelatedPair.getDep().equals("nsubjpass")){
                int id = verbRelatedPair.getPos();
                isPassive = true;
                objs.add(id);
            }else if(verbRelatedPair.getDep().equals("dobj")){
                int id = verbRelatedPair.getPos();
                objs.add(id);
            }
        }

        if(isPassive) {
            subjs.addAll(buffer);
        }
        for(Integer subj : subjs){
            String subject = getSubTree(dependencyTree, tokens, subj);
            for(Integer obj : objs){
                String object= getSubTree(dependencyTree, tokens, obj);
                if(subject.equals("") || object.equals("")){
                    continue;
                }
                Action tmp = new Action(subject, verb, object);
                output.add(tmp);
            }
        }
        return output;
    }

    /*
     * This method search subtree of a given id, and output the longest probase entity as a string
     */
    private String getSubTree(List<Set<Pair>> dependencyTree, List<String> tokens, int id) throws Exception {

        String head = wn.stemNounFirst( tokens.get(id) );
        String result = head;
        Set<Pair> subTree = dependencyTree.get(id);
        for (Pair subTreePair : subTree) {
            String dep = subTreePair.getDep();
            if (dep.equals("nmod")) {
                Integer subHead = subTreePair.getPos();
                Set<Pair> subSubTree = dependencyTree.get(subHead);
                for (Pair subSubPair : subSubTree) {
                    if (subSubPair.getDep().equals("case")) {
                        if (tokens.get(subSubPair.getPos()).equals("of")) {
                            result = tokens.get(subTreePair.getPos());
                            return result;
                        }
                    }
                }
            }
            if (dep.equals("compound") || dep.equals("amod")) {
                String modifier = tokens.get(subTreePair.getPos());
                if (pb.isProbaseEntity(modifier + " " + head)) {
                    result = modifier + " " + head;
                }
            }
            if(dep.equals("case")) {
                if(tokens.get(subTreePair.getPos()).equals("in")) {
                    return "";
                }
            }
        }
        return result;
    }
}
