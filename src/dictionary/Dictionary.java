package dictionary;



import java.util.TreeMap;

public class Dictionary {
    TreeMap<String, String> words = new TreeMap<String, String>();

    public TreeMap<String, String> getWords() {
        return words;
    }

    public String lowerCase(String word) {
        String s = word.toLowerCase();
        return s;
    }
    public boolean addNewWord(String wordTarget, String wordExplain) {
        wordTarget = lowerCase(wordTarget);
        wordExplain = lowerCase(wordExplain);
        if(words.get(wordTarget) != null) {
            words.put(wordTarget, wordExplain);
            return true;
        } else {
            return false;
        }
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
