package dictionary;

import cmdver.Word;

import java.util.TreeMap;

public class Dictionary {
    TreeMap<String, String> words = new TreeMap<String, String>();
    public String lowerCase(String word) {
        String s = word.toLowerCase();
        return s;
    }
    public void addNewWord(String wordTarget, String wordExplain) {
        wordTarget = lowerCase(wordTarget);
        wordExplain = lowerCase(wordExplain);
        words.put(wordTarget, wordExplain);
    }

    public void removeWord(String key) {
        words.remove(key);
    }

    public int numberOrWords() {
        return words.size();
    }

    /*
    public String getWordTarget(int index) {
        return words.get(index);
    }

    public String getWordExplain(int index) {
        return words.get(index;
    }
     */
}
