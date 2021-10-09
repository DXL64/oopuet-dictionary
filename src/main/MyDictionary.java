import java.io.IOException;
import com.knziha.plod.dictionary.mdict;

public class MyDictionary {
    public static void main(String[] args) throws IOException {
        System.out.println("START: ");
        String key = "hello";
        mdict md = new mdict("D:\\workspace\\git\\djun100\\mdict-java\\res\\Oxford Advanced Learner_s Dictionary\\Oxford Advanced Learner_s Dictionary.mdx");
        int search_result = md.lookUp(key, true);//true means to match strictly

        for (int i = -100; i < 100; i++) {
            System.out.println(md.getEntryAt(search_result + i));
        }

        System.out.println(search_result);
        if(search_result != -1) {
            String html_contents = md.getRecordAt(search_result);
            String entry_name = md.getEntryAt(search_result);
            System.out.println(html_contents);
            System.out.println(md.processStyleSheet(html_contents));
            System.out.println(entry_name);
        }
        System.out.println("PRINT ALL CONTENTS:");
        System.out.println("END.");
    }
}
