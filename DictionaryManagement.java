import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class DictionaryManagement {
    private static final String CHARSET_NAME = "UTF-8";
    private final Dictionary dictionary = new Dictionary();

    public void insertFromCommandline() {
        Scanner scanner = new Scanner(System.in, CHARSET_NAME);
        System.out.println("Type the number of words you want to add to dictionary:");
        int numberOrWords = Integer.parseInt(scanner.nextLine());
        System.out.println("Start adding new word");
        for (int i = 0; i < numberOrWords; i++) {
            System.out.println("Type the word target: ");
            String wordTarget =  scanner.nextLine();
            System.out.println("Type the word explain: ");
            String wordExplain =  scanner.nextLine();
            dictionary.addNewWord(wordTarget, wordExplain);
        }

        System.out.println("END");
        scanner.close();
    }

    public void insertFromFile() {
        try {
            File file = new File("dictionary.txt");
            Scanner scanner = new Scanner(file, CHARSET_NAME);
            while(scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String field[] = line.split(": ");
                dictionary.addNewWord(field[0], field[1]);
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println(e);
        }

    }
    public void showAllWords() {
        System.out.println("No   | English       | Vietnamese");
        int numberOrWords = dictionary.numberOrWords();
        for (int i = 0; i < numberOrWords; i++) {
            System.out.print(i + 1);
            System.out.println("    " + "| " + dictionary.getWordTarget(i) + "  | " + dictionary.getWordExplain(i));
        }
    }

    /**
     * Look up a word in dictionary.
     * Input from commandline
     */
    public Integer dictionaryLookup(String key) {
        int n = dictionary.words.size() - 1;
        int left = 0;
        int right = n - 1;
        int res = -1;
        while(left <= right) {
            int mid = (left + right) / 2;
            if(dictionary.words.get(mid).getWordTarget().compareTo(key) == 0) {
                res = mid;
            }
            else if(dictionary.words.get(mid).getWordTarget().compareTo(key) < 0)
                right = mid - 1;
            else
                left = mid + 1;
        }
        return res;
    }

    /**
     * Search a word in dictionary.
     * Input from commandline
     */
    public ArrayList<String> dictionarySearcher(String key) {
        ArrayList<String> arr = new ArrayList<String> ();
        int temp = dictionaryLookup(key);
        for(int i = temp; i <= temp + 10; i++) {
            //arr.add(new dictionary.words.get(i).getWordTarget());
        }
        return arr;
    }
    public void dictionaryBasic() {
        insertFromCommandline();
        showAllWords();
        dictionary.QuickSort(dictionary.getWords(), 0, dictionary.words.size()-1);
        showAllWords();
    }

    public void dictionaryAdvanced() {
        insertFromFile();
        showAllWords();
        
    }

    public Dictionary getDictionary() {
        return dictionary;
    }
    public static void main(String[] args) {
        DictionaryManagement dictionaryManagement = new DictionaryManagement();
        dictionaryManagement.dictionaryAdvanced();
        System.out.println("---------------------------");
        System.out.println(dictionaryManagement.dictionaryLookup("hello"));
        System.out.println(dictionaryManagement.getDictionary().getWordExplain(dictionaryManagement.dictionaryLookup("hello")));
    }
}
