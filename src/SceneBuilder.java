import dictionary.DictionaryApplication;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Parent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.io.FileOutputStream;
import java.util.Locale;

import com.voicerss.tts.AudioCodec;
import com.voicerss.tts.AudioFormat;
import com.voicerss.tts.Languages;
import com.voicerss.tts.VoiceParameters;
import com.voicerss.tts.VoiceProvider;


public class SceneBuilder extends Application {
    @FXML
    public TextField dd_textFieldFindWord;
    public TextField dd_textFieldAddEnglish;
    public TextField dd_textFieldAddVietnamese;
    public TextField dd_textFieldChangeMeanOfWord;
    public Button dd_buttonSubmitAddNewWord;
    public Button dd_buttonSubmitChangeWord;
    public Button dd_buttonDeleteThisWord;
    public Button dd_buttonChangeThisWord;
    public ListView dd_listViewSearcher;
    public ListView dd_listViewShowWord;
    public VBox dd_vBoxAddNewWord;
    public VBox dd_vBoxChangeThisWord;
    public WebView md_webView;
    public TextField md_textFieldFindWord;
    public ListView md_listViewSearcher;
    public SplitMenuButton md_splitMenuButtonChooseDictionary;
    public WebView gg_webView;

    private int currentDictOfMultiDict = 0;
    private ArrayList<MenuItem> md_nameOfDict = new ArrayList<MenuItem>();
    private static final int maxSizeOfMenuDict = 19;
    private String dd_wordTarget;
    private String md_wordTarget;

    private DictionaryApplication dictapp = new DictionaryApplication();

    public SceneBuilder() throws IOException { }

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


    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(new File("D:/workspace/git/oopuet-dictionary/src/res/style/DefaultDictionary.fxml").toURI().toURL());
        Parent root = loader.load();
        //Parent root = FXMLLoader.load(getClass().getResource("./src/app/DefaultDictionary.fxml"));
        primaryStage.setTitle("Dictionary");
        Scene scene = new Scene(root, 750, 477);
        scene.getStylesheets().add("res/style/styles.css");
        primaryStage.setScene(scene);
        // Closing the engine when stage is about to close
        primaryStage.show();
    }

    public static void main(String[] args){
        launch(args);
    }

    public void dd_onChangeTextFieldFindWord() {
        dd_wordTarget = dd_textFieldFindWord.getText();
        ArrayList<String> searcherOfInput = getSearcherFromDefaultDictionary(dd_wordTarget);
        dd_listViewSearcher.getItems().clear();
        dd_listViewSearcher.getItems().addAll(searcherOfInput);
    }

    /**
     * Call searcher in default dicionary.
     * @param textInput
     * @return
     */
    private ArrayList<String> getSearcherFromDefaultDictionary(String textInput){
        return dictapp.getDdict().adjacentWord(textInput);
    }

    public void dd_onPushItemOfListViewSearcher(){
        Object wordChoosen = dd_listViewSearcher.getSelectionModel().getSelectedItem();
        dd_textFieldFindWord.setText((String) wordChoosen);
    }

    public void dd_onClickFindIcon(){
        dd_wordTarget = dd_textFieldFindWord.getText();
        String temp = dd_wordTarget;
        String result = getWordResultFromDefaultDictionary(dd_wordTarget);
        if(result == null){
            String fuzzy = dictapp.getDdict().FuzzySearch(temp);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Error...");
            alert.setHeaderText("This word is not existed");
            if (fuzzy == null) {
                alert.setContentText("Try again!");
            }
            else alert.setContentText("Did you mean " + fuzzy + ": " + dictapp.getDdict().lookup(fuzzy) + "?");
            alert.show();

        }
        dd_refreshToStart();
        dd_listViewShowWord.getItems().add(temp + ": " + result);
    }

    /**
     * Call lookup for DefaultDictionary.
     * @param textInput
     * @return
     */
    private String getWordResultFromDefaultDictionary(String textInput){
        return dictapp.getDdict().lookup(textInput);
    }

    public void dd_onClickDeleteIcon(){
        dd_textFieldFindWord.setText("");
        dd_listViewSearcher.getItems().clear();
    }
    public void dd_onClickButtonAddNewWord(){
        dd_vBoxAddNewWord.setVisible(!dd_vBoxAddNewWord.isVisible());
    }
    public void dd_onClickButtonShowAllWords(){
        dd_refreshToStart();
        dd_listViewShowWord.getItems().addAll(getAllWordsFromDefaultDict());
    }
    private ArrayList<String> getAllWordsFromDefaultDict(){
        // call show all word default
        return dictapp.getDdict().showAllWords();
    }
    public void dd_onClickButtonDeleteWord(){
        deleteWordInDefaultDict(dd_wordTarget);
        dd_refreshToStart();
    }
    private void deleteWordInDefaultDict(String target){
        // call delete word
        dictapp.getDdict().getDictionary().removeWord(target);
    }
    public void dd_onClickButtonChangeWord(){
        dd_vBoxChangeThisWord.setVisible(!dd_vBoxChangeThisWord.isVisible());
    }
    public void dd_onClickButtonSubmitAddNewWord(){
        String target = dd_textFieldAddEnglish.getText();
        String explain = dd_textFieldAddVietnamese.getText();
        if(target == "" || explain == ""){
            // throw message error
            return;
        }

        // if(have target in dict) throw message error

        // else
        Word newWord = new Word(target, explain);
        addNewWordtoDefaultDictionary(newWord);
        dd_textFieldAddEnglish.setText("");
        dd_textFieldAddVietnamese.setText("");
    }
    public void dd_onClickButtonSubmitChangeThisWord(){
        deleteWordInDefaultDict(dd_wordTarget);
        Word newWord = new Word(dd_wordTarget, dd_textFieldChangeMeanOfWord.getText());
        addNewWordtoDefaultDictionary(newWord);
        dd_refreshToStart();
        dd_textFieldChangeMeanOfWord.setText("");

    }
    private void addNewWordtoDefaultDictionary(Word word){
        // add word to dictionary
        if (!dictapp.getDdict().getDictionary().addNewWord(word.getWordTarget(), word.getWordExplain())) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Error...");
            alert.setHeaderText("This word is existed");
            alert.setContentText("You can't add this word to your dictionary!");
            alert.show();
        }
    }
    public void dd_onClickListenIcon() throws Exception {
        VoiceProvider tts = new VoiceProvider("5cfb298cb08e4d09b7106d7e6dddbb4b");
        System.out.println("Word target on md_onClickListenIcon(): " + md_wordTarget);
        VoiceParameters params = new VoiceParameters(dd_wordTarget.toLowerCase(Locale.ROOT), Languages.English_UnitedStates);
        params.setCodec(AudioCodec.WAV);
        params.setFormat(AudioFormat.Format_44KHZ.AF_44khz_16bit_stereo);
        params.setBase64(false);
        params.setSSML(false);
        params.setRate(0);

        byte[] voice = tts.speech(params);

        FileOutputStream fos = new FileOutputStream("voice.wav");
        fos.write(voice, 0, voice.length);
        fos.flush();
        fos.close();

        String path = "D:\\workspace\\git\\oopuet-dictionary\\voice.wav";
        Media media = new Media(new File(path).toURI().toURL().toString());
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setAutoPlay(true);
    }
    private void dd_refreshToStart(){
        dd_textFieldFindWord.setText("");
        dd_listViewShowWord.getItems().clear();
        dd_listViewSearcher.getItems().clear();
        dd_vBoxChangeThisWord.setVisible(false);
        dd_wordTarget = "";
    }


    public void md_onChangeTextFieldFindWord(){
        md_wordTarget = md_textFieldFindWord.getText();
        ArrayList<String> searcherOfInput = getSearcherFromMdictDictionary(md_wordTarget);
        md_listViewSearcher.getItems().clear();
        md_listViewSearcher.getItems().addAll(searcherOfInput);
    }
    private ArrayList<String> getSearcherFromMdictDictionary(String textInput){
        // call searcher default
        return dictapp.getMdict().searcher(textInput);
    }
    public void md_onPushItemOfListViewSearcher(){
        Object wordChoosen = md_listViewSearcher.getSelectionModel().getSelectedItem();
        md_textFieldFindWord.setText((String) wordChoosen);
    }

    public void md_onClickFindIcon() throws IOException {
        md_wordTarget = md_textFieldFindWord.getText();
        String result = getWordResultFromMdictDictionary(md_wordTarget);
        if(result == null){
            // message cant not find this word
            return;
        }
        md_listViewSearcher.getItems().clear();
        md_textFieldFindWord.setText("");

        md_webView.getEngine().loadContent(result);
    }

    public void md_onClickFindAnotherDict() throws IOException {
        String result = getWordResultFromMdictDictionary(md_wordTarget);
        if(result == null){
            // message cant not find this word
            return;
        }
        md_listViewSearcher.getItems().clear();
        md_textFieldFindWord.setText("");

        md_webView.getEngine().loadContent(result);
    }

    private String getWordResultFromMdictDictionary(String textInput) throws IOException {
        // call lookup for Mdict with currentDictOfMultiDict
        ArrayList<String> result = dictapp.getMdict().lookUp(textInput);
        return result.get(currentDictOfMultiDict);
    }
    public void md_onClickDeleteIcon(){
        md_textFieldFindWord.setText("");
        md_listViewSearcher.getItems().clear();
    }
    public void md_onClickListenIcon() throws Exception {
        // speak(md_wordTarget)
        VoiceProvider tts = new VoiceProvider("5cfb298cb08e4d09b7106d7e6dddbb4b");
        System.out.println("Word target on md_onClickListenIcon(): " + md_wordTarget);
        VoiceParameters params = new VoiceParameters(md_wordTarget.toLowerCase(Locale.ROOT), Languages.English_UnitedStates);
        params.setCodec(AudioCodec.WAV);
        params.setFormat(AudioFormat.Format_44KHZ.AF_44khz_16bit_stereo);
        params.setBase64(false);
        params.setSSML(false);
        params.setRate(0);

        byte[] voice = tts.speech(params);

        FileOutputStream fos = new FileOutputStream("voice.wav");
        fos.write(voice, 0, voice.length);
        fos.flush();
        fos.close();

        String path = "D:\\workspace\\git\\oopuet-dictionary\\voice.wav";
        Media media = new Media(new File(path).toURI().toURL().toString());
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setAutoPlay(true);
    }
    public void gg_changeTabToGoogleTranslate(){
        gg_webView.getEngine().load("https://translate.google.com/?sl=auto&tl=vi&op=translate");
        gg_webView.getEngine().load("https://translate.google.com/?sl=auto&tl=vi&op=translate");
    }

    public void md_checkonSelectMultiDictionary(){
        for (final MenuItem item : md_splitMenuButtonChooseDictionary.getItems()) {
            item.setOnAction((event) -> {
                int idDict = Integer.parseInt(item.getId().substring(4,5));
                md_splitMenuButtonChooseDictionary.setText(item.getText());
                currentDictOfMultiDict = idDict;
                try {
                    md_onClickFindAnotherDict();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            });
        }
    }
    public void gg_changeTabToMultiDictionary(){
        md_checkonSelectMultiDictionary();
    }
}
