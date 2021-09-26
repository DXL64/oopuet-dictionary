import java.io.File;
import java.io.FileNotFoundException;
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
                String field[] = line.split("   ");
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

    public void dictionaryLookup() {
        System.out.println("START LOOP UP: ");
        // while () {
            
        // }

    }

    public void dictionaryBasic() {
        insertFromCommandline();
        showAllWords();
    }

    public void dictionaryAdvanced() {
        insertFromFile();
        showAllWords();
        dictionaryLookup();
    }

    public static void main(String[] args) {
        DictionaryManagement dictionaryManagement = new DictionaryManagement();
        dictionaryManagement.dictionaryBasic();
    }
}
