import java.util.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Scanner;

import org.json.simple.*;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.process.Morphology;


public class Depper {
	public static HeadFinder headfinder;
	public static TreeFactory tree_factory;

	public static Tree readTreeFromString(String parseStr){
		//read in the input into a Tree data structure
		TreeReader treeReader = new PennTreeReader(new StringReader(parseStr), tree_factory);
		Tree inputTree = null;
		try{
			inputTree = treeReader.readTree();
			
		}catch(IOException e){
			e.printStackTrace();
		}
		return inputTree;
	}
	public static String readFile(String filename) throws FileNotFoundException { 
		File file = new File(filename);
		return new Scanner(file).useDelimiter("\\Z").next();
	}



    /**
     * Needs to match sem/lib/core2jsent.py
     *
     * @param deps
     * @param parse
     * @param leaves
     * @return
     */
    public static JSONObject makeJSent(String parseStr, List<TypedDependency> deps, Tree parse, ArrayList<Tree> leaves) {
        HashMap<Integer,List<TypedDependency>> map = new HashMap();

        JSONArray jDeps = new JSONArray();

        for (TypedDependency d : deps) {
            JSONArray jDep = new JSONArray();
            int di = d.dep().index()-1;
            int gi = d.gov().index()-1;
            jDep.add(d.reln().getShortName());
            jDep.add(di);
            jDep.add(gi);
            jDeps.add(jDep);
        }

        JSONArray jToks = new JSONArray();
        for (Tree leaf : leaves) {
            JSONArray tok = new JSONArray();
            String surface = leaf.label().value();
            String posTag = leaf.parent(parse).label().value();
            String lemma = Morphology.lemmaStatic(surface, posTag, true);

            tok.add(surface); // surface word form
            tok.add(lemma); // supposed to be lemma
            tok.add(posTag); // POS tag
//            tok.add(null); // NER tag
            jToks.add(tok);
        }

        JSONObject jsent = new JSONObject();
        jsent.put("deps", jDeps);
        jsent.put("tokens", jToks);
        jsent.put("parse", parseStr);
        return jsent;
    }

	public static void printCoNLL(List<TypedDependency> deps, Tree parse, List<Tree> leaves) {
//		HashMap<Integer,TypedDependency> map = new HashMap();
		HashMap<Integer,List<TypedDependency>> map = new HashMap();
		for (TypedDependency d : deps) {
			int i = d.dep().index();
//			assert ! map.containsKey(d.dep().index()) : d.dep() + " " + map.get(d.dep().index());
			if (! map.containsKey(i)) {
				map.put(i, new ArrayList());

			}
			map.get(i).add(d);
		}
		int i=0;
		for (Tree L : leaves) {
			i++;
			boolean hasBeenCollapsed = !map.containsKey(i);
			if (map.containsKey(i)) {
				for (TypedDependency d : map.get(i)) {
					System.out.printf("%d\t%s\t_\t%s\t_\t_", i, L.yield(), L.parent(parse).label().value());
					System.out.printf("\t%s\t%s\t_\t_\n", d.gov().index(), d.reln() );				
				}
//				TypedDependency d = map.get(i);
			} else {
				System.out.printf("%d\t%s\t_\t%s\t_\t_", i, L.yield(), L.parent(parse).label().value());
				System.out.printf("\t0\t_\t_\t_\n");
			}
		}
//		for (TypedDependency d : deps) {
//			if (d == null) continue;
//			System.out.println("*** " + d);
////			System.out.println("*** " + d.dep().yield());
//			String s1,s2;
//			s1 = String.format("%d\t%s\t_\t%s\t_\t_", d.dep().index(), d.dep().yield(), "_");
//			s2 = String.format("\t%s\t%s\t_\t_", d.gov().index(), d.reln());
//			System.out.print(s1);
//			System.out.print(s2);
//			System.out.println("");
//		}
	}



	public static void main(String args[]) throws Exception {

//		headfinder = new CollinsHeadFinder();
		tree_factory = new LabeledScoredTreeFactory();
		 
		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
		
		for (String arg : args) {
		}
		String line;
		BufferedReader brIn = new BufferedReader(new InputStreamReader(System.in));
		while ( (line=brIn.readLine()) != null) {
			if (line.trim().equals("")) continue;
            String[] parts = line.trim().split("\t");
            String parseStr = parts[parts.length-1];
			Tree parse = readTreeFromString(parseStr);
			GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);

			Collection<TypedDependency> deps1;
            deps1 = gs.typedDependenciesCCprocessed(true);
			ArrayList<TypedDependency> deps = new ArrayList();
            deps.addAll(deps1);

//			printCoNLL(deps, parse, parse.getLeaves());
//            System.out.println("PARSE\t" + line.trim());

//            for (TypedDependency d : deps) System.out.println(d);

            ArrayList<Tree> leaves = new ArrayList();
            leaves.addAll(parse.getLeaves());

//            System.out.println(leaves2);
//            for (int i=0; i < leaves2.size(); i++)
//                System.out.printf("%d %s\n", i, leaves2.get(i).label().value());
//		TypedDependency[] deps = new TypedDependency[deps1.size() + 1000];
//		for (TypedDependency d : deps1)
//			deps[ d.dep().index() - 1 ] = d;
//		printCoNLL(deps);


            JSONObject jsent = makeJSent(parseStr, deps, parse, leaves);

            ArrayList<String> tokens = new ArrayList();
            for (Tree l : leaves) {
                tokens.add( l.label().value());
            }
            String tokensStr = join(tokens, " ");

            ArrayList<String> fields = new ArrayList();
            for (int i=0; i < parts.length-1; i++) {
                fields.add(parts[i]);
            }
            fields.add(tokensStr);
            fields.add(jsent.toJSONString());
            System.out.println(join(fields, "\t"));
		}
    }

    public static String join(AbstractCollection<String> s, String delimiter) {
        if (s == null || s.isEmpty()) return "";
        Iterator<String> iter = s.iterator();
        StringBuilder builder = new StringBuilder(iter.next());
        while( iter.hasNext() )
        {
            builder.append(delimiter).append(iter.next());
        }
        return builder.toString();
    }

		

}
