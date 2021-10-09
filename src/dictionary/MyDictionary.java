package dictionary;

import java.io.IOException;
import java.util.ArrayList;

public class MyDictionary {
    public static void main(String[] args) throws IOException {
        System.out.println("START: " + "\n");
        String key = "hello";

        MultiDictionary dict = new MultiDictionary("res\\mdxs.txt");

        System.out.println("LOOK UP TEST: " + "\n");
        ArrayList<String> wordExplains = dict.lookUp(key);
        for (int i = 0; i < wordExplains.size(); i++) {
            System.out.println(wordExplains.get(i));
        }

        System.out.println("LOOK UP TEST: " + "\n");
        ArrayList<String> wordTargets = dict.searcher(key, -10);
        for (int i = 0; i < 10; i++) {
            System.out.println(wordTargets.get(i));
        }
        System.out.println("END." + "\n");
    }
}
