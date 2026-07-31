    import java.awt.BorderLayout;
    import java.awt.Color;
    import java.awt.Font;
    import java.awt.Point;
    import java.awt.event.KeyEvent;
    import java.awt.event.KeyListener;
    import java.awt.event.MouseAdapter;
    import java.awt.event.MouseEvent;
    import java.io.BufferedReader;
    import java.io.File;
    import java.io.FileInputStream;
    import java.io.FileNotFoundException;
    import java.io.FileOutputStream;
    import java.io.IOException;
    import java.io.InputStreamReader;
    import java.io.OutputStream;
    import java.io.OutputStreamWriter;
    import java.io.PrintWriter;
    import java.io.UnsupportedEncodingException;
    import java.text.Collator;
    import java.util.ArrayList;
    import java.util.Collections;
    import java.util.HashSet;
    import java.util.List;
    import java.util.Locale;
    import java.util.Set;
    import javax.swing.BorderFactory;
    import javax.swing.JFrame;
    import javax.swing.JList;
    import javax.swing.JPanel;
    import javax.swing.JPopupMenu;
    import javax.swing.JTextArea;
    import javax.swing.ListSelectionModel;
    import javax.swing.SwingUtilities;
    import javax.swing.UIManager;
    import javax.swing.UnsupportedLookAndFeelException;
    import javax.swing.text.BadLocationException;
    
    
    public class WordAssist {
    public class SuggestionPanel {
        private final JList<String> list;
        private final JPopupMenu popupMenu;
        private final String subWord;
        private final int insertionPosition;
        private String dictionaryFilePath = "French_Dictionary.txt";
        private int numberOfWordsInList = 10;
        private Color colorOfList = Color.decode("#FBFEC3");  // Light-Yellow (default)
        private Color colorOfListText = Color.decode("#000000");  // Black (default)
        private String listFontName = Font.SANS_SERIF;
        private int listFontStyle = Font.PLAIN;
        private int listFontSize = 11;
        private Font listFont = new Font(listFontName, listFontStyle, listFontSize);
        private Locale locale = Locale.FRANCE;
        public String characterEncoding = "UTF-8";
        public SuggestionPanel(JTextArea textarea, int position, String subWord, Point location) {
            this.insertionPosition = position;
            this.subWord = subWord;
            popupMenu = new JPopupMenu();
            popupMenu.removeAll();
            popupMenu.setOpaque(false);
            popupMenu.setBorder(null);
            popupMenu.add(list = createSuggestionList(position, subWord), BorderLayout.CENTER);
            popupMenu.show(textarea, location.x, textarea.getBaseline(0, 0) + location.y);
        }
        public void hide() {
            popupMenu.setVisible(false);
            if (suggestion == this) {
                suggestion = null;
            }
        }
        private JList<String> createSuggestionList(final int position, final String subWord) {
            String[] data = searchForWord(dictionaryFilePath, subWord + "*", numberOfWordsInList);
            if (data.length == 0) {
                data = new String[2];
                data[0] = " : Unknown Word : ";
                data[1] = "Add To Dictionary";
            }
            JList<String> assistList = new JList<>(data);
            assistList.setFont(new Font(listFontName, listFontStyle, listFontSize));
            assistList.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
            assistList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            assistList.setBackground(colorOfList);
            assistList.setForeground(colorOfListText);
            if (data.length == 2 && data[0].equalsIgnoreCase("unknown word:")) {
                assistList.setSelectedIndex(1);
            }
            else {
                assistList.setSelectedIndex(0);
            }
            assistList.addMouseListener(new MouseAdapter() {
                @Override
                @SuppressWarnings("Convert2Lambda")
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 1 && list.getSelectedValue().equalsIgnoreCase("add to dictionary")) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                addToDictionary(dictionaryFilePath, subWord, characterEncoding, locale);
                            }
                        });
                        hideSuggestion();
                        textarea.requestFocus();
                    }
                    if (e.getClickCount() == 2) {
                        insertSelection();
                        hideSuggestion();
                        textarea.requestFocus();
                    }
                }
            });
            return assistList;
        }
        /**
         * Adds the supplied word to the supplied Dictionary text file but only
         * if it doesn't already exist. The dictionary text file must be
         * formated in such a manner that each line of that file must contain
         * only one word.
         *
         * @param dictionaryPath    (String) The path and file name to the
         *                          Dictionary text file.<br>
         *
         * @param wordToAdd         (String) The word to add to dictionary.<br>
         *
         * @param characterEncoding (String) The Character encoding to use for Dictionary:<pre>
         *
         *         Example:  "UTF-8"</pre><br>
         *
         * See:
         * <b>https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html</b>
         * for all the supported Encodings.<br>
         *
         * @param locale            (Locale) The Locale of the dictionary file.
         *                          This is also important for sorting so that
         *                          it too is done according to the proper locale.<pre>
         *         Example: Locale.FRANCE or Locale.US;</pre>
         *
         */
        public void addToDictionary(String dictionaryPath, String wordToAdd, String characterEncoding, Locale locale) {
            if (dictionaryPath.trim().equals("") || wordToAdd.trim().equals("")) {
                return;
            }
            wordToAdd = wordToAdd.trim();
            String savePath = new File(dictionaryPath).getAbsolutePath();
            savePath = savePath.substring(0, savePath.lastIndexOf(File.separator))
                    + File.separator + "tmpDictFile.txt";
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(dictionaryPath), characterEncoding))) {
                OutputStream os = new FileOutputStream(savePath);
                // PrintWriter writer = new PrintWriter(savePath)) {
                try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, characterEncoding))) {
                    // PrintWriter writer = new PrintWriter(savePath)) {
                    String addwordFirstChar = wordToAdd.substring(0, 1);
                    Collator collator = Collator.getInstance(locale);
                    collator.setStrength(Collator.PRIMARY);
                    List<String> wordList = new ArrayList<>();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.equals("")) {
                            continue;
                        }
                        String firstChar = line.substring(0, 1);
                        if (firstChar.equals(firstChar.toUpperCase())
                                && addwordFirstChar.equals(addwordFirstChar.toUpperCase())
                                && collator.equals(firstChar, addwordFirstChar)) {
                            wordList.clear();
                            wordList.add(wordToAdd);
                            wordList.add(line);
                            // Add to uppercase section of file...
                            while ((line = reader.readLine()) != null || !firstChar.equals(firstChar.toUpperCase())) {
                                firstChar = line.substring(0, 1);
                                if (firstChar.equals(firstChar.toUpperCase())
                                        && !collator.equals(firstChar, addwordFirstChar)) {
                                    break;
                                }
                                wordList.add(line);
                            }
                            Set<String> strSet = new HashSet<>(wordList);
                            wordList.clear();
                            wordList.addAll(strSet);
                            Collections.sort(wordList, collator);
                            for (String wrds : wordList) {
                                writer.println(wrds);
                            }
                            writer.println(line);
                        }
                        else if (firstChar.equals(firstChar.toLowerCase())
                                && addwordFirstChar.equals(addwordFirstChar.toUpperCase())
                                && collator.equals(firstChar, addwordFirstChar.toLowerCase())) {
                            wordList.clear();
                            if (!wordList.contains(wordToAdd.toLowerCase())) {
                                wordList.add(wordToAdd.toLowerCase());
                            }
                            wordList.add(line.toLowerCase());
                            // Add to lowercase section of file...
                            while ((line = reader.readLine()) != null) {
                                firstChar = line.substring(0, 1);
                                if (collator.equals(firstChar, addwordFirstChar.toLowerCase())) {
                                    break;
                                }
                                wordList.add(line);
                            }
                            Set<String> strSet = new HashSet<>(wordList);
                            wordList.clear();
                            wordList.addAll(strSet);
                            Collections.sort(wordList, collator);
                            for (String wrds : wordList) {
                                writer.println(wrds);
                            }
                            writer.println(line);
                        }
                        else if (firstChar.equals(firstChar.toLowerCase())
                                && addwordFirstChar.equals(addwordFirstChar.toLowerCase())
                                && collator.equals(firstChar, addwordFirstChar)) {
                            wordList.clear();
                            if (!wordList.contains(wordToAdd)) {
                                wordList.add(wordToAdd);
                            }
                            wordList.add(line);
                            // Add to dictionary file...
                            while ((line = reader.readLine()) != null || !firstChar.equals(firstChar.toUpperCase())) {
                                firstChar = line.substring(0, 1);
                                if (!collator.equals(firstChar, addwordFirstChar)) {
                                    break;
                                }
                                wordList.add(line);
                            }
                            Set<String> strSet = new HashSet<>(wordList);
                            wordList.clear();
                            wordList.addAll(strSet);
                            Collections.sort(wordList, collator);
                            for (String wrds : wordList) {
                                writer.println(wrds);
                            }
                            writer.println(line);
                        }
                        else {
                            writer.println(line);
                        }
                    }
                }
            }
            catch (FileNotFoundException ex) {
                System.err.println(ex);
            }
            catch (UnsupportedEncodingException ex) {
                System.err.println(ex);
            }
            catch (IOException ex) {
                System.err.println(ex);
            }
            if (new File(savePath).exists()) {
                if (new File(dictionaryPath).delete()) {
                    if (!new File(savePath).renameTo(new File(dictionaryPath))) {
                        System.err.println("Could not add the word: " + wordToAdd
                                + " to Dictionary!");
                    }
                }
            }
        }
        @SuppressWarnings({"CallToPrintStackTrace", "Convert2Lambda"})
        public boolean insertSelection() {
            if (list.getSelectedValue() != null) {
                try {
                    if (list.getSelectedValue().equalsIgnoreCase("add to dictionary")) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                addToDictionary(dictionaryFilePath, subWord, characterEncoding, locale);
                            }
                        });
                        hideSuggestion();
                        textarea.requestFocus();
                        return true;
                    }
                    else {
                        final String selectedSuggestion = list.getSelectedValue().substring(subWord.length());
                        textarea.getDocument().insertString(insertionPosition, selectedSuggestion, null);
                        return true;
                    }
                }
                catch (BadLocationException e1) {
                    e1.printStackTrace();
                }
                hideSuggestion();
            }
            return false;
        }
        public void moveUp() {
            int index = Math.min(list.getSelectedIndex() - 1, 0);
            selectIndex(index);
        }
        public void moveDown() {
            int index = Math.min(list.getSelectedIndex() + 1, list.getModel().getSize() - 1);
            selectIndex(index);
        }
        @SuppressWarnings("Convert2Lambda")
        private void selectIndex(int index) {
            final int position = textarea.getCaretPosition();
            list.setSelectedIndex(index);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    textarea.setCaretPosition(position);
                }
            });
        }
        public String[] searchForWord(String dictionaryFilePath, String searchCriteria, int numberOfWordsToReturn) {
            // This method ignores letter case!
            List<String> foundList = new ArrayList<>();  // To hold all found words.
            // Convert the supplied criteria string to a Regular Expression 
            // for the String#matches() method located in the 'while' loop.
            String regEx = searchCriteria.replace("?", ".").replace("-", ".").replace("*", ".*?").toLowerCase();
            // 'Try With Resources' use here to auto-close the reader.
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(dictionaryFilePath), characterEncoding))) {
                String line = "";
                int counter = 0;
                while ((line = reader.readLine()) != null) {
                    line = line.trim().toLowerCase();
                    if (line.matches(regEx)) {
                        counter++;
                        foundList.add(line);  // There's a match...add to the List.
                        if (counter == numberOfWordsToReturn) {
                            break;
                        }
                    }
                }
            }
            // catch Exceptions (if any).
            catch (FileNotFoundException ex) {
                System.err.println(ex);
            }
            catch (IOException ex) {
                System.err.println(ex);
            }
            return foundList.toArray(new String[0]);  // Return the Array.
        }
        public String getDictionaryFilePath() {
            return dictionaryFilePath;
        }
        public void setDictionaryFilePath(String dictionaryFilePath) {
            this.dictionaryFilePath = dictionaryFilePath;
        }
        public int getNumberOfWordsInList() {
            return numberOfWordsInList;
        }
        public void setNumberOfWordsInList(int numberOfWordsInList) {
            this.numberOfWordsInList = numberOfWordsInList;
        }
        public Color getColorOfList() {
            return colorOfList;
        }
        public void setColorOfList(Color colorOfList) {
            this.colorOfList = colorOfList;
        }
        public Color getColorOfListText() {
            return colorOfListText;
        }
        public void setColorOfListText(Color colorOfListText) {
            this.colorOfListText = colorOfListText;
        }
        public String getListFontName() {
            return listFontName;
        }
        public Font getListFont() {
            return listFont;
        }
        public void setListFont(Font listFont) {
            this.listFont = listFont;
            this.listFontName = listFont.getName();
            this.listFontStyle = listFont.getStyle();
            this.listFontSize = listFont.getSize();
        }
        public void setListFontName(String listFontName) {
            this.listFontName = listFontName;
        }
        public int getListFontStyle() {
            return listFontStyle;
        }
        public void setListFontStyle(int listFontStyle) {
            this.listFontStyle = listFontStyle;
        }
        public int getListFontSize() {
            return listFontSize;
        }
        public void setListFontSize(int listFontSize) {
            this.listFontSize = listFontSize;
        }
        public String getCharacterEncoding() {
            return characterEncoding;
        }
        public void setCharacterEncoding(String characterEncoding) {
            this.characterEncoding = characterEncoding;
        }
        public Locale getLocale() {
            return locale;
        }
        public void setLocale(Locale locale) {
            this.locale = locale;
        }
    }
    private SuggestionPanel suggestion;
    private JTextArea textarea;
    @SuppressWarnings("Convert2Lambda")
    protected void showSuggestionLater() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                showSuggestion();
            }
        });
    }
    @SuppressWarnings({"CallToPrintStackTrace", "Convert2Lambda"})
    protected void showSuggestion() {
        hideSuggestion();
        final int position = textarea.getCaretPosition();
        Point location;
        try {
            location = textarea.modelToView(position).getLocation();
        }
        catch (BadLocationException e2) {
            e2.printStackTrace();
            return;
        }
        String text = textarea.getText();
        int start = Math.max(0, position - 1);
        while (start > 0) {
            if (!Character.isWhitespace(text.charAt(start))) {
                start--;
            }
            else {
                start++;
                break;
            }
        }
        if (start > position) {
            return;
        }
        final String subWord = text.substring(start, position);
        if (subWord.length() < 2) {
            return;
        }
        suggestion = new SuggestionPanel(textarea, position, subWord, location);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                textarea.requestFocusInWindow();
            }
        });
    }
    private void hideSuggestion() {
        if (suggestion != null) {
            suggestion.hide();
        }
    }
    protected void initUI() {
        final JFrame frame = new JFrame();
        frame.setTitle("Word Assist");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel(new BorderLayout());
        textarea = new JTextArea(24, 80);
        textarea.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        textarea.addKeyListener(new KeyListener() {
            @Override
            @SuppressWarnings("Convert2Lambda")
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    if (suggestion != null) {
                        if (suggestion.insertSelection()) {
                            e.consume();
                            final int position = textarea.getCaretPosition();
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                @SuppressWarnings("CallToPrintStackTrace")
                                public void run() {
                                    try {
                                        textarea.getDocument().remove(position - 1, 1);
                                    }
                                    catch (BadLocationException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN && suggestion != null) {
                    suggestion.moveDown();
                }
                else if (e.getKeyCode() == KeyEvent.VK_UP && suggestion != null) {
                    suggestion.moveUp();
                }
                else if (Character.isLetterOrDigit(e.getKeyChar())) {
                    showSuggestionLater();
                }
                else if (Character.isWhitespace(e.getKeyChar())) {
                    hideSuggestion();
                }
            }
            @Override
            public void keyPressed(KeyEvent e) {
            }
        });
        panel.add(textarea, BorderLayout.CENTER);
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
    }
    @SuppressWarnings({"CallToPrintStackTrace", "Convert2Lambda"})
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new WordAssist().initUI();
            }
        });
    }
    }