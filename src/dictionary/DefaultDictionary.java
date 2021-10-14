package dictionary;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Set;


public class DefaultDictionary {
    private static final String CHARSET_NAME = "UTF-8";
    private static Dictionary dictionary = new Dictionary();

    public Dictionary getDictionary() {
        return dictionary;
    }
    public void insertFromFile() {
        try {
            File file = new File("./src/dictionary/dictionary.txt");
            Scanner scanner = new Scanner(file, CHARSET_NAME);
            while(scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String field[] = line.split(": ");
                dictionary.getWords().put(field[0], field[1]);
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println(e);
        }
    }

    public ArrayList<String> showAllWords() {
        ArrayList<String> result = new ArrayList<>();
        Set<String> keySet = dictionary.words.keySet();
        for(String key : keySet)
            result.add(key + ": " + dictionary.words.get(key));
        return result;
    }

    public String lookup(String key) {
        key = dictionary.lowerCase(key);
        return dictionary.words.get(key);
    }
    public ArrayList<String> adjacentWord(String key) {
        key = dictionary.lowerCase(key);
        ArrayList<String> list = new ArrayList<String>();
        list.add(key);
        int cnt = 9;
        String tmp = key;
        while (cnt != 0) {
            cnt--;
            try {
                tmp = dictionary.words.higherKey(tmp);
            }
            catch (NullPointerException e) {
                break;
            }
            if(tmp != null) {
                list.add(tmp);
            }
        }
        return list;
    }
    public int LevenshteinDistance(String a, String b) {
        int n = a.length();
        int m = b.length();
        a = " " + a;
        b = " " + b;
        int cost;
        int[][] dp = new int[n+1][m+1];
        dp[0][0] = 0;
        for(int i = 1; i <= n; i++)
            dp[i][0] = i;
        for(int i = 1; i <= m; i++)
            dp[0][i] = i;
        for(int i = 1; i <= n; i++) {
            for(int j = 1; j <= m; j++) {
                if(a.charAt(i) == b.charAt(j)) {
                    cost = 0;
                } else cost = 1;
                dp[i][j] = Math.min(dp[i - 1][j] + 1, Math.min(dp[i][j-1] + 1, dp[i-1][j-1] + cost));
            }
        }
        return dp[n][m];
    }
    public String FuzzySearch(String key) {
        key = dictionary.lowerCase(key);
        Set<String> keySet = dictionary.words.keySet();
        String res = "";
        int LevenshteinMin = Integer.MAX_VALUE;
        for(String tmp : keySet) {
            int valueOfLD = LevenshteinDistance(key, tmp);
            if(valueOfLD < LevenshteinMin) {
                LevenshteinMin = valueOfLD;
                res = tmp;
            }
        }

        if(LevenshteinMin <= 3)
            return res;
        else
            return null;
    }
    public static void main(String[] args) {
        DefaultDictionary Ex = new DefaultDictionary();
        dictionary.addNewWord("Hello","aa");
        dictionary.addNewWord("Hell","bb");
        Ex.showAllWords();
        System.out.println(Ex.adjacentWord("Helo"));
        System.out.println(Ex.FuzzySearch("Helo")) ;
    }
}
