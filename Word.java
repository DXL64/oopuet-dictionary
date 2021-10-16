public class Word {
    private String wordTarget;
    private String wordExplain;

    Word(String wordTarget, String wordExplain) {
        this.wordTarget = wordTarget;
        this.wordExplain = wordExplain;
    }

    public void setWordExplain(String wordExplain) {
        this.wordExplain = wordExplain;
    }

    public void setWordTarget(String wordTarget) {
        this.wordTarget = wordTarget;
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

