import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Parent;
import org.kordamp.bootstrapfx.BootstrapFX;


import javax.swing.*;
import java.io.File;
import java.util.ArrayList;

import static com.teamdev.jxbrowser.engine.RenderingMode.HARDWARE_ACCELERATED;


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


    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("DefaultDictionary.fxml"));
        primaryStage.setTitle("Dictionary");
        Scene scene = new Scene(root, 750, 477);
        scene.getStylesheets().add("styles.css");
        primaryStage.setScene(scene);
        // Closing the engine when stage is about to close
        primaryStage.show();

    }
    public static void main(String[] args){
        launch(args);
    }
    public void dd_onChangeTextFieldFindWord(){
        ArrayList<String> searcherOfInput = getSearcherFromDefaultDictionary(dd_textFieldFindWord.getText());
        dd_listViewSearcher.getItems().clear();
        dd_listViewSearcher.getItems().addAll(searcherOfInput);
    }
    private ArrayList<String> getSearcherFromDefaultDictionary(String textInput){
        // call searcher default
        ArrayList<String> result = new ArrayList<>(10);
        for(int i = 1; i <= (int)(Math.random() * 10); ++i) result.add("du");
        return result;
    }
    public void dd_onPushItemOfListViewSearcher(){
        Object wordChoosen = dd_listViewSearcher.getSelectionModel().getSelectedItem();
        dd_textFieldFindWord.setText((String) wordChoosen);
    }
    public void dd_onClickFindIcon(){
        dd_wordTarget = dd_textFieldFindWord.getText();
        Word result = getWordResultFromDefaultDictionary(dd_wordTarget);
        if(result == null){
            // message cant not find this word
            return;
        }
        dd_refreshToStart();
        dd_listViewShowWord.getItems().add(result.getWordTarget() + ": " + result.getWordExplain());
    }
    private Word getWordResultFromDefaultDictionary(String textInput){
        // call lookup for DefaultDict
        return new Word("hello", "xin chao");
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
        ArrayList<String> result = new ArrayList<>(10);
        for(int i = 1; i <= (int)(Math.random() * 10); ++i) result.add("du");
        return result;
    }
    public void dd_onClickButtonDeleteWord(){
        deleteWordInDefaultDict(dd_wordTarget);
        dd_refreshToStart();
    }
    private void deleteWordInDefaultDict(String target){
        // call delete word
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
    }
    public void dd_onClickListenIcon(){
        // speak(dd_wordTarget)
    }
    private void dd_refreshToStart(){
        dd_textFieldFindWord.setText("");
        dd_listViewShowWord.getItems().clear();
        dd_listViewSearcher.getItems().clear();
        dd_vBoxChangeThisWord.setVisible(false);
        dd_wordTarget = "";
    }


    public void md_onChangeTextFieldFindWord(){
        ArrayList<String> searcherOfInput = getSearcherFromMdictDictionary(md_textFieldFindWord.getText());
        md_listViewSearcher.getItems().clear();
        md_listViewSearcher.getItems().addAll(searcherOfInput);
    }
    private ArrayList<String> getSearcherFromMdictDictionary(String textInput){
        // call searcher default
        ArrayList<String> result = new ArrayList<>(10);
        for(int i = 0; i <= (int)(Math.random() * 10); ++i) result.add("du");
        return result;
    }
    public void md_onPushItemOfListViewSearcher(){
        Object wordChoosen = md_listViewSearcher.getSelectionModel().getSelectedItem();
        md_textFieldFindWord.setText((String) wordChoosen);
    }
    public void md_onClickFindIcon(){
        md_wordTarget = md_textFieldFindWord.getText();
        String result = getWordResultFromMdictDictionary(md_wordTarget);
        if(result == null){
            // message cant not find this word
            return;
        }
        md_listViewSearcher.getItems().clear();
        md_textFieldFindWord.setText("");
        // change file HTML and load HTML
        File HTML = new File("./oopuet-dictionary/index.html");
        md_webView.getEngine().load(HTML.toURI().toString());
    }
    private String getWordResultFromMdictDictionary(String textInput){
        // call lookup for Mdict with currentDictOfMultiDict
        return new String("dupham");
    }
    public void md_onClickDeleteIcon(){
        md_textFieldFindWord.setText("");
        md_listViewSearcher.getItems().clear();
    }
    public void md_onClickListenIcon(){
        // speak(md_wordTarget)
    }
    public void gg_changeTabToGoogleTranslate(){
        gg_webView.getEngine().load("https://translate.google.com/?sl=auto&tl=vi&op=translate");
        gg_webView.getEngine().load("https://translate.google.com/?sl=auto&tl=vi&op=translate");
    }
}
