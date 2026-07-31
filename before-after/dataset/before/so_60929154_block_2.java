    import java.awt.BorderLayout;
    import java.awt.Color;
    import java.awt.Point;
    import java.awt.event.KeyEvent;
    import java.awt.event.KeyListener;
    import java.awt.event.MouseAdapter;
    import java.awt.event.MouseEvent;
    import java.io.BufferedReader;
    import java.io.FileNotFoundException;
    import java.io.FileReader;
    import java.io.IOException;
    import java.util.ArrayList;
    import java.util.List;
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
            private JList list;
            private JPopupMenu popupMenu;
            private String subWord;
            private final int insertionPosition;
            private final String dictionaryFilePath = "Dictionary.txt";
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
            private JList createSuggestionList(final int position, final String subWord) {
                String[] data = searchForWord(dictionaryFilePath, subWord + "*", 10);
                JList list = new JList(data);
                list.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
                list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                list.setBackground(Color.decode("#FBFEC3")); //light-yellow
                list.setSelectedIndex(0);
                list.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            insertSelection();
                            textarea.requestFocus();
                            hideSuggestion();
                        }
                    }
                });
                return list;
            }
            public boolean insertSelection() {
                if (list.getSelectedValue() != null) {
                    try {
                        final String selectedSuggestion = ((String) list.getSelectedValue()).substring(subWord.length());
                        textarea.getDocument().insertString(insertionPosition, selectedSuggestion, null);
                        return true;
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
                try (BufferedReader reader = new BufferedReader(new FileReader(dictionaryFilePath))) {
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
        }
        private SuggestionPanel suggestion;
        private JTextArea textarea;
        protected void showSuggestionLater() {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    showSuggestion();
                }
            });
        }
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
                public void keyTyped(KeyEvent e) {
                    if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                        if (suggestion != null) {
                            if (suggestion.insertSelection()) {
                                e.consume();
                                final int position = textarea.getCaretPosition();
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
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