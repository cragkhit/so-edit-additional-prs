public class AutoCompleteTextArea extends JTextArea implements FocusListener, DocumentListener {
	private static final long serialVersionUID = 1L;
	private AutoCompleteRecommendationsWindow autoCompleteWindow;
	private List<String> words;
	public AutoCompleteTextArea(List<String> words) {
		super();
		init(words);
	}
	public AutoCompleteTextArea(List<String> words, int rows, int columns) {
		super(rows, columns);
		init(words);
	}
	public AutoCompleteTextArea(List<String> words, String text, int rows, int columns) {
		super(text, rows, columns);
		init(words);
	}
	public AutoCompleteTextArea(List<String> words, String text) {
		super(text);
		init(words);
	}
	private void init(List<String> words) {
		this.words = words;
		autoCompleteWindow = new AutoCompleteRecommendationsWindow(this);
		getDocument().addDocumentListener(this);
		addFocusListener(this);
	}
	private void attemptAutoCompleteFromRemoval(DocumentEvent e) throws BadLocationException {
		if (e.getLength() != 1)
			return;
		int pos = e.getOffset();
		String content = getText(0, pos + 1);
		int startPosition = getStartingPositionOfTheWord(pos - 1, content);
		if (pos - startPosition < 3) {
			autoCompleteWindow.setVisible(false);
			return;
		}
		String prefix = content.substring(startPosition + 1);
		showRecommendations(prefix.trim());
	}
	private int getStartingPositionOfTheWord(int offset, String content) {
		int index;
		for (index = offset; index >= 0; index--) {
			if (!Character.isLetter(content.charAt(index))) {
				break;
			}
		}
		return index;
	}
	private void showRecommendations(String prefix) {
		List<String> recommendations = getAutoCompleteRecommendationsForPrefix(prefix);
		if (!recommendations.isEmpty()) {
			autoCompleteWindow.showRecommendations(prefix, recommendations);
			SwingUtilities.invokeLater(() -> autoCompleteWindow.setVisible(true));
		} else {
			autoCompleteWindow.setVisible(false);
		}
	}
	private List<String> getAutoCompleteRecommendationsForPrefix(String prefix) {
		int n = Collections.binarySearch(words, prefix);
		List<String> result = new ArrayList<>();
		if (n < 0 && -n <= words.size()) {
			int indexOfMatchedWord = -n - 1;
			String match = words.get(indexOfMatchedWord);
			if (!match.startsWith(prefix))
				return result;
			result.add(match);
			for (int i = indexOfMatchedWord + 1; i < words.size(); i++) {
				match = words.get(i);
				if (match.startsWith(prefix)) {
					result.add(match);
				} else
					break;
			}
			for (int i = indexOfMatchedWord - 1; i >= 0; i--) {
				match = words.get(i);
				if (match.startsWith(prefix)) {
					result.add(match);
				} else
					break;
			}
		}
		return result.stream().sorted(Comparator.naturalOrder()).distinct().collect(Collectors.toList());
	}
	private void attemptAutoComplete(DocumentEvent ev) throws BadLocationException {
		if (ev.getLength() != 1) {
			return;
		}
		int pos = ev.getOffset();
		String content = getText(0, pos + 1);
		int startPosition = getStartingPositionOfTheWord(pos, content);
		if (pos - startPosition < 2) {
			autoCompleteWindow.setVisible(false);
			return;
		}
		String prefix = content.substring(startPosition + 1);
		showRecommendations(prefix);
	}
	@Override
	public void focusGained(FocusEvent e) {
	}
	@Override
	public void focusLost(FocusEvent e) {
		autoCompleteWindow.setVisible(false);
	}
	@Override
	public void insertUpdate(DocumentEvent e) {
		try {
			attemptAutoComplete(e);
		} catch (BadLocationException e1) {
		}
	}
	@Override
	public void removeUpdate(DocumentEvent e) {
		try {
			attemptAutoCompleteFromRemoval(e);
		} catch (BadLocationException e1) {
		}
	}
	@Override
	public void changedUpdate(DocumentEvent e) {
	}
	private class AutoCompleteRecommendationsWindow extends JWindow implements KeyListener, MouseListener {
		private static final long serialVersionUID = 2646960455879045456L;
		private JTextArea area;
		private JList<String> wordList;
		private String prefix;
		private AutoCompleteRecommendationsWindow(JTextArea area) {
			super();
			this.area = area;
			setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
			wordList = new JList<>();
			wordList.addMouseListener(this);
			wordList.setModel(new DefaultListModel<>());
			area.addKeyListener(this);
			getContentPane().setLayout(new BorderLayout());
			JScrollPane sp = new JScrollPane(wordList);
			getContentPane().add(sp, BorderLayout.CENTER);
			pack();
		}
		public void showRecommendations(String prefix, List<String> recommendations) {
			this.prefix = prefix;
			wordList.setVisibleRowCount(Math.min(10, recommendations.size()));
			String longestWord = recommendations.stream().max(Comparator.comparingInt(String::length)).get();
			wordList.setPrototypeCellValue(longestWord + "S"); // Some extra space
			showRecommendationsInWordList(recommendations);
			wordList.setSelectedIndex(0);
			revalidate();
			pack();
		}
		private void showRecommendationsInWordList(List<String> recommendations) {
			DefaultListModel<String> model = (DefaultListModel<String>) wordList.getModel();
			model.removeAllElements();
			recommendations.forEach(model::addElement);
		}
		@Override
		public void setVisible(boolean b) {
			try {
				setLocation(getCursorLocation());
			} catch (BadLocationException e) {
				return;
			}
			super.setVisible(b);
		}
		private Point getCursorLocation() throws BadLocationException {
			Rectangle rect = area.modelToView(Math.max(0, area.getCaretPosition() - 1));
			if (rect == null) // In a test environment is null
				throw new BadLocationException("", 0);
			Point coordinates = rect.getLocation();
			coordinates.x += 10;
			coordinates.y += area.getFontMetrics(area.getFont()).getHeight();
			SwingUtilities.convertPointToScreen(coordinates, area);
			return coordinates;
		}
		@Override
		public void keyTyped(KeyEvent e) {
		}
		@Override
		public void keyPressed(KeyEvent e) {
			if (!isVisible())
				return;
			if (isDownArrowKey(e) || isUpArrowKey(e)) {
				String value = wordList.getSelectedValue();
				if (isDownArrowKey(e) && wordList.getSelectedIndex() < wordList.getModel().getSize() - 1) {
					value = wordList.getModel().getElementAt((wordList.getSelectedIndex() + 1));
				} else if (isUpArrowKey(e) && wordList.getSelectedIndex() > 0) {
					value = wordList.getModel().getElementAt((wordList.getSelectedIndex() - 1));
				}
				wordList.setSelectedValue(value, true);
				e.consume();
			} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				addSelectedWordAndClose();
				e.consume();
			} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				setVisible(false);
			}
		}
		private void addSelectedWordAndClose() {
			int position = area.getCaretPosition();
			area.insert((wordList.getSelectedValue() + " ").substring(prefix.length()), position);
			setVisible(false);
		}
		@Override
		public void keyReleased(KeyEvent e) {
		}
		private boolean isDownArrowKey(KeyEvent e) {
			return e.getKeyCode() == KeyEvent.VK_DOWN;
		}
		private boolean isUpArrowKey(KeyEvent e) {
			return e.getKeyCode() == KeyEvent.VK_UP;
		}
		@Override
		public void mouseClicked(MouseEvent e) {
		}
		@Override
		public void mousePressed(MouseEvent e) {
		}
		@Override
		public void mouseReleased(MouseEvent e) {
			addSelectedWordAndClose();
		}
		@Override
		public void mouseEntered(MouseEvent e) {
		}
		@Override
		public void mouseExited(MouseEvent e) {
		}
	}
}
 - It takes a `List<String>` in constructor which represents your dictionary. Be careful that the dictionary **must be sorted** since it uses binary search to find the recommendation words for the auto-complete.
 - It allows you to navigate recommended words with arrow keys <kbd>?</kbd> and <kbd>?</kbd>
The demo:
public class Demo {
	public static void main(String[] args) throws Exception {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Auto complete Demo");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLayout(new BorderLayout());
			List<String> dictionary = Arrays.asList("computer", "canada", "controversial", "conversation", "construct");
			Collections.sort(dictionary);
			AutoCompleteTextArea autoCompleteTextArea = new AutoCompleteTextArea(dictionary, 20, 50);
			autoCompleteTextArea.setFont(new JLabel().getFont()); // Windows laf text area font does not look good
			frame.add(autoCompleteTextArea);
			frame.pack();
			frame.setLocationByPlatform(true);
			frame.setVisible(true);
		});
	}
}
**Preview:**
[![preview][1]][1]
  [1]: https://i.stack.imgur.com/Q8ArB.gif