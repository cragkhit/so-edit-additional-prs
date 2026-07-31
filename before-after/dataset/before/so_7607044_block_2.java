    public class JIDCellEditor extends AbstractCellEditor implements TableCellEditor {
    
        private JComboBox jComboBox = new JComboBox();
    
        @Override
        public Object getCellEditorValue() {
            return jComboBox.getSelectedItem();
        }
    
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            Vector vector = new Vector();
            vector.add(0);
            for (int i = 0; i < table.getRowCount(); i++) {
                if (!vector.contains(table.getValueAt(i, 0)) && table.getValueAt(i, 3).toString().equals("Sheep")) {
                    vector.add(table.getValueAt(i, 0));
                }
            }
            vector.remove(table.getValueAt(row, 0));
            jComboBox = new JComboBox(vector);
            jComboBox.setSelectedItem(value);
            jComboBox.addItemListener(new ItemListener() {
    
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        stopCellEditing();
                    }
                }
            });
            return jComboBox;
        }
    }