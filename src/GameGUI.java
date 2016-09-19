import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.Serializable;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

public class GameGUI extends JFrame implements Serializable {

	private static final long serialVersionUID = -2915642945247071751L;

	int N, K;

	final JPanel panel = new JPanel(new BorderLayout(1, 2));
	JTable table;
	JTable maze;
	
	private int CELL_SIZE = 20;

	GameGUI(int N, int K, String playerId) {
		System.out.println("Init Board...");

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
		setTableCellSize(maze, CELL_SIZE);

		JScrollPane mazeBoard = new JScrollPane(maze);
		panel.add(mazeBoard);

		/**
		 *  JFrame
		 */
		setTitle(playerId);
		add(panel);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationByPlatform(true);
		pack();
		setMinimumSize(this.getSize());
		setVisible(true);
	}

	private void setTableCellSize(JTable table, int size) {
		// Row
		table.setRowHeight(size);
		
		// Column
		for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
			TableColumn column = table.getColumnModel().getColumn(i);
			column.setPreferredWidth(size);
		}
	}
	
	public void updateBoard(Map<String, Player> players, String[][] board){
		/**
		 * Score board
		 */
		
		
		/**
		 *  Maze board
		 */
		DefaultTableModel tableModel = (DefaultTableModel) maze.getModel();
	    for (int i = 0; i < board.length; i++) {
	    	tableModel.removeRow(i);
	        tableModel.addRow(board[i]);
	    }
	    maze.setModel(tableModel);
	}

	// public void updateBoard(int role){
	// System.out.println("Updating Board...");
	// String[] scoreColumns = new String[] {"Player ID", "Score"};
	// Object[][] scoreTable = new Object[players.size()][2];
	// int row = 0;
	// for (String playerId : players.keySet()){
	// scoreTable[row][0] = players.get(playerId).id;
	// scoreTable[row][1] = players.get(playerId).score;
	// row++;
	// }
	//
	// JTable table = new JTable(scoreTable, scoreColumns);
	// JScrollPane tableBoard = new JScrollPane(table);
	// table.setPreferredScrollableViewportSize(new Dimension(N*15, N*30));
	// table.setFillsViewportHeight(true);
	// scoreBoard.removeAll();
	// scoreBoard.add(tableBoard);
	// scoreBoard.revalidate();
	// scoreBoard.repaint();
	//
	// mazeBoard.removeAll();
	// for (int i = 0; i < N; i++){
	// for (int j = 0; j < N; j++){
	// JButton b = new JButton(board[i][j]);
	// b.setPreferredSize(new Dimension(30, 30));
	// b.setBackground(Color.WHITE);
	// mazeBoard.add(b);
	// }
	// }
	// mazeBoard.revalidate();
	// mazeBoard.repaint();
	// }
}
