package presentacio;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;

public class Table extends JPanel {
	private JTable table;

	public Table() {

		JScrollPane scrollPane = new JScrollPane();
		add(scrollPane);

		table = new JTable();
		scrollPane.setViewportView(table);

	}

	public JTable getTable() {
		return table;
	}
}
