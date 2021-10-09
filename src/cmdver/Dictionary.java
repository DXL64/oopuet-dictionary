package cmdver;

import java.util.ArrayList;

public class Dictionary {
    ArrayList<Word> words = new ArrayList<>();

    public void addNewWord(String wordTarget, String wordExplain) {
        words.add(new Word(wordTarget, wordExplain));
    }

    public int numberOrWords() {
        return words.size();
    }

    public String getWordTarget(int index) {
        return words.get(index).getWordTarget();
    }

    public String getWordExplain(int index) {
        return words.get(index).getWordExplain();
    }
}