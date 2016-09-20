import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.Serializable;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

public class GameGUI extends JFrame implements Serializable {

	private static final long serialVersionUID = -2915642945247071751L;

	int N, K;
	String playerId;

	JTable table;
	JTable maze;
	
	private int CELL_SIZE = 20;

	GameGUI(int N, int K, String playerId) {
		UIManager.put("Table.gridColor", new ColorUIResource(Color.gray));
		JPanel panel = new JPanel(new BorderLayout());

		/**
		 * Score board
		 */
		String[] headers = new String[] { "Player ID", "Score" };

		table = new JTable() {
			private static final long serialVersionUID = 3739426267786323567L;
			public boolean isCellEditable(int nRow, int nCol) {
				return false;
			}
		};
		table.setPreferredScrollableViewportSize(new Dimension(150, (N-1)*CELL_SIZE));
		table.setFillsViewportHeight(true);
		table.setRowHeight(CELL_SIZE);
		
		DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
		tableModel.setColumnIdentifiers(headers);

		JScrollPane tableBoard = new JScrollPane(table);
		panel.add(tableBoard, BorderLayout.WEST);

		/**
		 *  Maze board
		 */
		maze = new JTable(N, N) {
			private static final long serialVersionUID = 3739426267786323567L;
			public boolean isCellEditable(int nRow, int nCol) {
				return false;
			}
		};
		maze.setPreferredScrollableViewportSize(new Dimension(N*CELL_SIZE, N*CELL_SIZE));
		maze.setFillsViewportHeight(true);
		maze.setTableHeader(null);
		maze.setFocusable(false);
		maze.setRowSelectionAllowed(false);
		setTableCellSize(maze, CELL_SIZE);

		JScrollPane mazeBoard = new JScrollPane(maze);
		panel.add(mazeBoard);

		/**
		 *  JFrame
		 */
		add(panel);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationByPlatform(true);
		pack();
		setMinimumSize(getSize());
		setResizable(false);
		setVisible(true);
		
		this.playerId = playerId;
	}

	private void setTableCellSize(JTable table, int size) {
		// Row
		table.setRowHeight(size);
		
		// Set align
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		
		// Column
		for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
			TableColumn column = table.getColumnModel().getColumn(i);
			column.setPreferredWidth(size);
			column.setCellRenderer( centerRenderer );
		}
	}
	
	public synchronized void updateBoard(int role, Map<String, Player> players, String[][] board){
		/**
		 * Update role in the title
		 */
		setTitle(playerId + (role == 1? " ** PRIMARY ** " : role == 2? " ** BACKUP ** " : ""));
		
		/**
		 * Score board
		 */
		DefaultTableModel score = (DefaultTableModel) table.getModel();
		score.setRowCount(0); // Clear
		for	(String playerId : players.keySet()) {
			Player p = players.get(playerId);
	        score.addRow(new String[]{p.getId(), String.valueOf(p.getScore())});
	    }
	    score.fireTableDataChanged();
		
		/**
		 *  Maze board
		 */
		DefaultTableModel mazeModel = (DefaultTableModel) maze.getModel();
		mazeModel.setRowCount(0); // Clear
		for	(int i = 0; i < board.length; i++) {
	        mazeModel.addRow(board[i]);
	    }
	    mazeModel.fireTableDataChanged();
	}
}
