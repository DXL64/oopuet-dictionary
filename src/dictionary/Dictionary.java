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
    public void changeWord(String key, String mean) {
        key = lowerCase(key);
        mean = lowerCase(mean);
        words.replace(words.get(key), mean);
    }
    public int numberOrWords() {
        return words.size();
    }
}
