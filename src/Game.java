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
	
	int N, K;
	Map<String, Player> players;
	String[][] board;

	public Game(int N, int K) throws GameException {
		if (K > N * N) throw new GameException("Too many treasures");
		this.N = N;
		this.K = K;
		this.players = new HashMap<String, Player>();
		this.board = new String[N][N];
		generateTreasures(K);
	}

	public Game(Game game) {
		this.N = game.getN();
		this.K = game.getK();
		this.players = game.getPlayers();
		this.board = game.getBoard();
	}

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
			
			boolean foundTreasure = isOccupiedByTreasure(dest);
			
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
