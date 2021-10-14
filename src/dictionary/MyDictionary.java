package dictionary;

import java.io.IOException;
import java.util.ArrayList;

public class MyDictionary {
    public static void main(String[] args) throws IOException {
        System.out.println("START: " + "\n");
        String key = "hell";

        MultiDictionary dict = new MultiDictionary("res\\mdxs.txt");
        System.out.println("TEST: " + "\n");


        System.out.println("LOOK UP TEST: " + "\n");
        ArrayList<String> wordExplains = dict.lookUp(key);
        for (int i = 0; i < wordExplains.size(); i++) {
            System.out.println(wordExplains.get(i));
        }

        System.out.println("SEARCHER TEST: " + "\n");
        ArrayList<String> wordTargets = dict.searcher(key);
        for (int i = 0; i < wordTargets.size(); i++) {
            System.out.println(wordTargets.get(i));
        }

        System.out.println("ADJACENT TEST: " + "\n");
        ArrayList<String> words = dict.adjacentWord(key, 10);
        for (int i = 0; i < words.size(); i++) {
            System.out.println(words.get(i));
        }
        System.out.println("END." + "\n");
    }
}
