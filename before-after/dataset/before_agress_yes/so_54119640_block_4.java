    import java.awt.event.*;
    import javax.swing.*;
    import javax.swing.event.*;
    import javax.swing.plaf.*;
    import javax.swing.table.*;
    import javax.swing.plaf.basic.BasicTableHeaderUI;
    public class SorterChooserTableHeaderUI extends BasicTableHeaderUI {
        protected JTableHeader header;
        public class MouseInputHandler extends BasicTableHeaderUI.MouseInputHandler {
            public void mouseClicked (MouseEvent ev) {
                if (!header.isEnabled())
                    return;
                // Here's the original predicate
                // if (ev.getClickCount() % 2 == 1 && SwingUtilities.isLeftMouseButton(ev)) {
                if (ev.getClickCount() % 2 == 1 && ev.getModifiersEx() == 0 && SwingUtilities.isLeftMouseButton(ev)) {
                    JTable table = header.getTable();
                    RowSorter sorter;
                    if (table != null && (sorter = table.getRowSorter()) != null) {
                        int columnIndex = header.columnAtPoint(ev.getPoint());
                        if (columnIndex != -1) {
                            columnIndex = table.convertColumnIndexToModel(columnIndex);
                            sorter.toggleSortOrder(columnIndex);
                        }
                    }
                }
            }
        }
        @Override
        protected MouseInputListener createMouseInputListener () {
            return new MouseInputHandler();
        }
        public void installUI (JComponent c) {
            header = (JTableHeader) c;
            super.installUI(c);
        }
    }