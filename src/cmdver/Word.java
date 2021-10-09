package cmdver;

public class Word {
    private final String wordTarget;
    private final String wordExplain;

    Word(String wordTarget, String wordExplain) {
        this.wordTarget = wordTarget;
        this.wordExplain = wordExplain;
    }


    /**
     * Getter method for {@code wordTarget}.
     * @return word target {@code String}
     */
    public String getWordTarget() {
        return wordTarget;
    }

    /**
     * Getter method for {@code wordExplain}.
     * @return word explain {@code String}
     */
    public String getWordExplain() {
        return wordExplain;
    }
}