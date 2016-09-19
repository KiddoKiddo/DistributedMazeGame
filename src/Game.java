import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.LineBorder;

public class Game implements Serializable {

	private static final long serialVersionUID = 6148003629986301236L;

	private static final String TREASURE = "x";
	
	int N;
	int K;
	Map<String, Player> players;
	String[][] board;

	final JPanel gui = new JPanel(new BorderLayout(1, 2));
	JPanel mazeBoard;
	JPanel scoreBoard;
	JFrame frame;

	public Game(int N, int K) throws GameException {
		if (K > N * N) throw new GameException("Too many treasures");
		this.N = N;
		this.K = K;
		this.players = new HashMap<String, Player>();
		this.board = new String[N][N];
		generateTreasures(K);
	}

	public Game(int N, int K, Map<String, Player> p, String[][] b) throws GameException {
		this.N = N;
		this.K = K;
		this.players = p;
		this.board = b;
	}

	public Game(Game game) {
		this.N = game.getN();
		this.K = game.getK();
		this.players = game.getPlayers();
		this.board = game.getBoard();
	}

	/**
	 * Purpose of this method is to remove those crashed players from the board.
	 * The argument is the list of current active players
	 * 
	 * @param set
	 * @throws GameException
	 */
	public void updatePlayers(Set<String> currentPlayers) {
		for (String playerId : players.keySet()) {
			if (!currentPlayers.contains(playerId)) {
				removePlayer(playerId);
			}
		}

	}

	public Location randomEmptyLocation() {
		Random rand = new Random();
		int x, y;
		do {
			x = rand.nextInt(N);
			y = rand.nextInt(N);
		} while (board[x][y] != null); // Should not occupied by any player and treasures
		return new Location(x, y);
	}

	public void addPlayer(String playerId) {
		Location loc = randomEmptyLocation();

		board[loc.getX()][loc.getY()] = playerId;

		Player player = new Player(playerId, loc);
		players.put(playerId, player);
	}

	public void removePlayer(String playerId) {
		Player p = players.get(playerId);
		if(p == null) return; // Already remove
		
		board[p.getLocation().getX()][p.getLocation().getY()] = null;
		players.remove(playerId);
	}

	public boolean isOccupiedByPlayer(Location loc){
		return board[loc.getX()][loc.getY()] != null && ! board[loc.getX()][loc.getY()].equals(TREASURE);
	}
	
	public boolean isOccupiedByTreasure(Location loc){
		return board[loc.getX()][loc.getY()] != null && board[loc.getX()][loc.getY()].equals(TREASURE);
	}

	public boolean validLocation(Location loc) {
		return (loc.x >= 0 && loc.y >= 0 && loc.x < N && loc.y < N);
	}

	public boolean move(String playerId, int direction) {
		Player player = players.get(playerId);
		Location dest = new Location(player.getLocation());
		switch (direction) {
		case 1:
			dest.y--;
			break;
		case 2:
			dest.x++;
			break;
		case 3:
			dest.y++;
			break;
		case 4:
			dest.x--;
			break;
		default:
			break; // should not fall into this case
		}
		if (validLocation(dest) && ! isOccupiedByPlayer(dest)) {
			
			boolean foundTreasure = false;
			if(isOccupiedByTreasure(dest)) foundTreasure = true;
			
			// Move player
			board[player.getLocation().getX()][player.getLocation().getY()] = null;
			board[dest.getX()][dest.getY()] = player.getId();
			player.moveTo(dest);
			
			// Increase score and generate treasure
			if(foundTreasure){
				System.out.println("Player "+playerId+" got a TREASURE!!!");
				player.increaseScore();
				generateTreasures(1);
			}
			
			return true;
		}
		return false;
	}

	public void generateTreasures(int total) {
		int count = 0;
		while(count < total){
			Location treasure = randomEmptyLocation();
			board[treasure.getX()][treasure.getY()] = TREASURE;
			count++;
		}
	}

	public void initBoard(String currentPlayerID){
		System.out.println("Init Board...");
		scoreBoard = new JPanel();
		scoreBoard.setBorder(new LineBorder(Color.BLACK));
		
		String[] scoreColumns = new String[] {"Player ID", "Score"};
		Object[][] scoreTable = new Object[players.size()][2];
		
		JTable table = new JTable(scoreTable, scoreColumns);
        JScrollPane tableBoard = new JScrollPane(table);
        table.setPreferredScrollableViewportSize(new Dimension(N*15, N*30));
        table.setFillsViewportHeight(true);
        
        scoreBoard.add(tableBoard);  
		gui.add(scoreBoard, BorderLayout.WEST);
		
		mazeBoard = new JPanel(new GridLayout(N,N));
		mazeBoard.setBorder(new LineBorder(Color.BLACK));
		for (int i = 0; i < N; i++){
        	for (int j = 0; j < N; j++){
        		JButton b = new JButton();
        		b.setPreferredSize(new Dimension(30, 30));
                b.setBackground(Color.WHITE);
                mazeBoard.add(b);
        	}
        }
		gui.add(mazeBoard);
		
		frame = new JFrame(currentPlayerID);
        frame.add(gui);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationByPlatform(true);
        frame.pack();
        frame.setMinimumSize(frame.getSize());
        frame.setVisible(true);
	}
	
	public void updateBoard(int role){
		System.out.println("Updating Board...");
		String[] scoreColumns = new String[] {"Player ID", "Score"};
		Object[][] scoreTable = new Object[players.size()][2];
		int row = 0;
		for (String playerId : players.keySet()){
        	scoreTable[row][0] = players.get(playerId).id;
        	scoreTable[row][1] = players.get(playerId).score;
        	row++;
        }
		
        JTable table = new JTable(scoreTable, scoreColumns);
        JScrollPane tableBoard = new JScrollPane(table);
        table.setPreferredScrollableViewportSize(new Dimension(N*15, N*30));
        table.setFillsViewportHeight(true);
        scoreBoard.removeAll();
        scoreBoard.add(tableBoard);
        scoreBoard.revalidate();
        scoreBoard.repaint();
        
        mazeBoard.removeAll();
        for (int i = 0; i < N; i++){
        	for (int j = 0; j < N; j++){
        		JButton b = new JButton(board[i][j]);
        		b.setPreferredSize(new Dimension(30, 30));
                b.setBackground(Color.WHITE);
                mazeBoard.add(b);
        	}
        }
        mazeBoard.revalidate();
        mazeBoard.repaint();
	}

	public int getN() {
		return N;
	}

	public void setN(int n) {
		N = n;
	}

	public int getK() {
		return K;
	}

	public void setK(int k) {
		K = k;
	}

	public Map<String, Player> getPlayers() {
		return players;
	}

	public void setPlayers(Map<String, Player> players) {
		this.players = players;
	}

	public String[][] getBoard() {
		return board;
	}

	public void setBoard(String[][] board) {
		this.board = board;
	}
	
	public void printScore(){
		StringBuilder sb = new StringBuilder();
		sb.append("Score: ");
		for(String id : players.keySet()){
			sb.append(id+"("+players.get(id).getScore()+") ");
		}
		System.out.println(sb.toString());
	}
	
	public void printBoard(){
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<board.length; i++){
			sb.append(".");
			for(int j=0; j<board[0].length; j++){
				sb.append(String.format("%2s", board[i][j]==null? " " : board[i][j])+".");
			}
			sb.append("\r\n");
		}
		System.out.print(sb.toString());
	}

}
