package dictionary;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

import com.knziha.plod.dictionary.mdict;

public class MultiDictionary {
    private ArrayList<mdict> mdxs = new ArrayList<mdict>();
    public static final String CHARSET_NAME = "UTF-8";
    public static final String NOT_EXIST = "<h2>THIS DICTIONARY DON'T HAVE THIS WORD</h2>";

    /**
     * MultiDictionary constructor.
     * @param filepath includes all file paths of mdict file
     */
    public MultiDictionary(String filepath) throws IOException {
        try {
            File file = new File(filepath);
            Scanner scanner = new Scanner(file, CHARSET_NAME);
            while(scanner.hasNextLine()) {
                String line = scanner.nextLine();
                mdxs.add(new mdict(line));
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println(e);
        }
    }

    /**
     * Look up a word in all dictionary in {@code mdxs}.
     * @param key input word
     * @return {@code ArrayList<String>} html contents of input word
     */
    public ArrayList<String> lookUp(String key) throws IOException {
        ArrayList<String> htmlContents = new ArrayList<>();
        for (int i = 0; i < mdxs.size(); i++) {
            int search_result = mdxs.get(i).lookUp(key, true);
            if (search_result != -1) {
                htmlContents.add(mdxs.get(i).getRecordAt(search_result));
            } else {
                htmlContents.add(NOT_EXIST);
            }
        }
        return htmlContents;
    }

    /**
     * Searcher method
     * @param key target word
     * @return {@code ArrayList<String>} word that has same prefix
     */
    public ArrayList<String> searcher(String key) {
        return searcher(key,50);
    }


    /**
     * Searcher method with limit.
     * @param key target word
     * @param limit numbers of word after input word
     * @return {@code ArrayList<String>} word that has same prefix
     */
    public ArrayList<String> searcher(String key, int limit) {
        ArrayList<String> entryNames = new ArrayList<>();

        int search_result = mdxs.get(0).lookUp(key, false);
        for (int i = 0; i <= limit; i++) {
            String temp = mdxs.get(0).getEntryAt(search_result + i);
            if (temp.toLowerCase(Locale.ROOT).contains(key)) {
                entryNames.add(temp);
            } else {
                break;
            }
        }
        return entryNames;
    }


    /**
     * adjacentWord method.
     * @param key input word
     * @param n
     * {@code n > 0} numbers of word after input word
     * {@code n < 0} numbers of word before input word
     * @return {@code ArrayList<String>} n words after the input word
     */
    public ArrayList<String> adjacentWord(String key, int n) throws IOException {
        ArrayList<String> entryNames = new ArrayList<>();

        int search_result = mdxs.get(0).lookUp(key, false);

        for (int i = 1; i <= Math.abs(n); i++) {

            if (n > 0) {
                entryNames.add(mdxs.get(0).getEntryAt(search_result + i));
            } else {
                entryNames.add(mdxs.get(0).getEntryAt(search_result - i));
            }
        }

        return entryNames;
    }

    public ArrayList<String> nameOfDicionary() {
        ArrayList<String> dictName = new ArrayList<>();
        for (int i = 0; i < mdxs.size(); i++) {
            dictName.add(mdxs.get(i)._Dictionary_Name);
        }
        return dictName;
    }
}
