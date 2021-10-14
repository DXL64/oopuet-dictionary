public class Word {
    private final String wordTarget;
    private final String wordExplain;

    Word(String wordTarget, String wordExplain) {
        this.wordTarget = wordTarget;
        this.wordExplain = wordExplain;
    }

    public String getWordTarget() {
        return wordTarget;
    }

    public String getWordExplain() {
        return wordExplain;
    }
}
