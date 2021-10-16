import java.util.ArrayList;

public class Dictionary {
    ArrayList<Word> words = new ArrayList<>();

    public void swapWord(Word a, Word b) {
        Word tmp = new Word(a.getWordTarget(), a.getWordExplain());
        a.setWordTarget(b.getWordTarget());
        a.setWordExplain(b.getWordExplain());
        b.setWordTarget(tmp.getWordTarget());
        b.setWordExplain(tmp.getWordExplain());
    }
    public void QuickSort(ArrayList<Word> a, int left, int right) {
        if(left >= right) return;
        int mid = (left + right) / 2;
        String pivot = a.get(mid).getWordTarget();
        int i = left;
        int j = right;
        while(i <= j) {
            while(a.get(i).getWordTarget().compareTo(pivot) < 0) i++;
            while(a.get(j).getWordTarget().compareTo(pivot) > 0) j--;
            if(i <= j) {
                swapWord(a.get(i), a.get(j));
                i++;
                j--;
            }
        }
        if(left < j) {
            QuickSort(a, left, j);
        }
        if(right > i) {
            QuickSort(a, i, right);
        }
    }

    public void addNewWord(String wordTarget, String wordExplain) {
        words.add(new Word(wordTarget, wordExplain));
        int n = words.size() - 1;
        int left = 0;
        int right = n - 1;
        if(left >= right) right = -1;
        while(left < right) {
            int mid = (left + right) / 2;
            if(words.get(mid).getWordTarget().compareTo(words.get(n).getWordTarget()) < 0) {
                left = mid + 1;
            }
            else
                right = mid;
        }
        for(int i = n; i > Math.max(right+1,0); i--)
            swapWord(words.get(i), words.get(i-1));
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

    public ArrayList<Word> getWords() {
        return words;
    }
}