import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REMEMBER TO DELETE // Debug LINE BEFORE SUBMITTIN
 * Notes (REMEMBER TO DELETE BEFORE SUBMITTING):
 * - How to detect crashed NORMAL node?
 * 		+ When one of the servers fails, it ping all nodes (to generate the new servers) 
 * 		(and conveniently remove those inactive).
 * 		+ Periodically remove inactive nodes.
 * - How to detect crashed BACKUP?
 * 		+ After PRIMNARY server 'move' or 'addNewNode', it updates current 'game' and 'nodes' to the BACKUP 
 * 		--> know whether it fails
 * - How to detect crashed PRIMARY?
 * 		+ When the server receives 'move' or 'addNewNode' request is BACKUP
 *
 */
public class GameNode extends UnicastRemoteObject implements GameNodeInterface {

	private static final long serialVersionUID = -3637465977534510865L;
	
	private static final boolean invokeUI = true;
	private static final boolean printBoardAndScore = false;

	public String playerId;
	// 1: primaryServer, 2: backupServe, 0: normalNode (used in GUI to indicate whether it is servers)
	int role;
	
	// Flag to resovle new request ('move', 'addNewNode') coming while handling PRIMARY crash 
	boolean handlingPRIMARYCrash = false;
	boolean handlingBACKUPCrash = false;

	GameNodeInterface primaryServer;
	GameNodeInterface backupServer;
	
	GameGUI gui;
	
	/**
	 *  Data below need to be synchronously updated
	 *  The version holding by PRIMARY, BACKUP should be the main reference
	 */
	public Game game;
	public Map<String, GameNodeInterface> nodes;

	/*
	 * This thread can be OPTIONALY added to GUI (if have time) 
	 */
	public static Thread input, output; 
	
	@Override
	public String toString() {
		return " [" + playerId + " (" + role + ")] ";
	}
	
	public GameNode(String playerId) throws RemoteException{
		this.playerId = playerId;
	}

	@Override
	public void setupFirstGameNode(String playerId, int N, int K) throws GameException, RemoteException{
		this.role = 1;
		
		// Assign itself to be primary server
		this.primaryServer = this;
		
		// Init list of node
		this.nodes = new ConcurrentHashMap<String, GameNodeInterface>();
		this.nodes.put(playerId, this);
		
		// Init Game (put behind as it has some delay)
		this.game = new Game(N, K);
		this.game.addPlayer(playerId); 
		
		// Start input thread
		this.startInputThread();
				
		// Start GUI Game
		if (invokeUI){
			gui = new GameGUI(N, K, playerId);
			gui.updateBoard(role, game.getPlayers(), game.getBoard());
		}
		if(printBoardAndScore){
			game.printScore();
			game.printBoard(); // Debug
		}
		
		// For PRIMARY to check for inactive nodes
		setupCleanNodesTask();
	}

	@Override
	public void setupNormalGameNode(String playerId, GameNodeInterface otherNode) throws RemoteException {
		this.role = 0;
		
		while(otherNode.getPrimaryServer()==null || otherNode.getNodes()==null || otherNode.getGame()==null){
			// Wait a bit for the other node set up
			try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }
		}
		// Copy remote reference of servers, temporarily use 'nodes'
		this.primaryServer = otherNode.getPrimaryServer();
		this.backupServer = otherNode.getBackupServer();
		this.nodes = otherNode.getNodes();
		this.game = new Game(otherNode.getGame());
		
		// New node request primary server to join, 
		// (info about servers on otherNode maybe out-of-date)
		try{
			primaryServer.addNewNode(this);
		} catch(RemoteException e) {
			System.out.println("Failed to request PRIMARY ('setupNormalGameNode'). Contact BACKUP.");
			try {
				backupServer.addNewNode(this);
				
			} catch (RemoteException e1) {
				System.out.println("Failed to contact both servers ('setupNormalGameNode'). Contact other up-to-date nodes");
				
				// Either get servers info from other nodes or set itself as PRIMARY,...
				handleCannotReachBothServers();
				
				// Retry with new PRIMARY
				primaryServer.addNewNode(this);
			}
		}
		// Start input thread
		this.startInputThread();
		
		// Start GUI Game
		if (invokeUI){
			gui = new GameGUI(game.getN(), game.getK(), playerId);
			gui.updateBoard(role, game.getPlayers(), game.getBoard());
		}
		if(printBoardAndScore){
			game.printScore();
			game.printBoard(); // Debug
		}
	}

	/*----------------------------------------------------------
	 		This is SERVER side functions
	 ----------------------------------------------------------*/
	
	/**
	 * This method should be invoked by setupNormalGameNode()
	 */
	@Override
	public synchronized void addNewNode(GameNodeInterface node) throws RemoteException{
		// If BACKUP server receive request, hence PRIMARY server failed
		if(equals(this, backupServer)){
			handlePrimaryCrash();
		}
		
		String id = node.getPlayerId();
		
		// Update info for server synchronously as this method is synchronously
		game.addPlayer(id); 
		nodes.put(id, node);
		
		System.out.println("--- Player ["+id+"] joined.");
		System.out.println("--- Current players: "+nodes.keySet());
		
		// Create BACKUP server (if necessary, in case null or fail to ping)
		try{
			if(backupServer == null) throw new RemoteException();
			backupServer.ping();
		} catch (RemoteException e){
			backupServer = node;
			System.out.println("--- Player ["+id+"] is BACKUP server.");
		}
		// Update info from PRIMARY server (this) to node: 
		// 'primaryServer', 'backupServer', 'nodes', 'game'
		// Update 'nodes', 'game' to BACKUP
		updateNodeAndBackup(node);
		
		// Update board after resolve other user's request to move
		if (invokeUI && gui != null){
			gui.updateBoard(role, game.getPlayers(), game.getBoard());
		}
		if(printBoardAndScore){
			game.printScore();
			game.printBoard(); // Debug
		}
	}
	/**
	 * This method should be invoked by requestMove()
	 * This method is not synchronous because, multiple players can move at the same time.
	 */
	@Override
	public boolean move(GameNodeInterface node, int direction) throws RemoteException{
		boolean ok = false;
		
		// If BACKUP server receive request, hence PRIMARY server failed
		if(equals(this, backupServer)){
			handlePrimaryCrash();
		}
		String id = node.getPlayerId();
		
		// Resolve move
		if(direction == 0){
			// Do nothing
			ok = true;
		} else if(direction == 9){
			// Remove from game
			game.removePlayer(id); 
			
			// Remove from nodes
			nodes.remove(id);
			
			ok = true;
		} else {
			ok = game.move(id, direction); 
		}
		
		System.out.println("--- Move ["+id+"] with ["+direction+"] --- "+ (ok ? "OK" : "CANNOT MOVE") + " (up-to-date): " + nodes.keySet());
		
		// Update list of players in 'game'
		game.updatePlayers(nodes.keySet()); 
		
		// Check BACKUP health
		if(backupServer != null){
			try{
				backupServer.ping();
			} catch(RemoteException e) {
				// Handle backup crash
				handleBackupCrash();
			}
		}
		// Update info from PRIMARY server (this) to node: 
		// 'primaryServer', 'backupServer', 'nodes', 'game'
		// Update 'nodes', 'game' to BACKUP
		updateNodeAndBackup(node);
		
		if (invokeUI && gui != null){
			gui.updateBoard(role, game.getPlayers(), game.getBoard());
		}
		if(printBoardAndScore){
			game.printScore();
			game.printBoard(); // Debug
		}
		
		return ok;
	}
	/**
	 * Update info for requesting node and BACKUP after PRIMARY operation
	 * Note: This is private method for PRIMARY
	 */
	private void updateNodeAndBackup(GameNodeInterface node) throws RemoteException {
		if( ! equals(node, primaryServer) ){
			node.updateAll(primaryServer, backupServer, game, nodes);
		}
		
		if( ! equals(node, backupServer) && backupServer != null){
			backupServer.updateGame(game);
			backupServer.updateNodes(nodes);
		}
	}

	private void handlePrimaryCrash() throws RemoteException{
		if( ! handlingPRIMARYCrash){
			handlingPRIMARYCrash = true;
			
			System.out.println("--- Handle PRIMARY crash.");
			// Update itself as PRIMARY
			updatePrimaryServer(this);
			setupCleanNodesTask();
			
			// Create new BACKUP server
			handleBackupCrash();
			
			handlingPRIMARYCrash = false;
		}
	}
	
	private void handleBackupCrash() throws RemoteException {
		if (!handlingBACKUPCrash) {
			handlingBACKUPCrash = true;
			
			System.out.println("--- Handle BACKUP crash.");
			GameNodeInterface newBackup = findActiveNode();
			if (newBackup != null) {
				updateBackUpServer(newBackup);
				newBackup.updateAll(primaryServer, backupServer, game, nodes);
			} else {
				System.out.println("Cannot find new BACKUP server (all nodes inactive except for the current one.)");
				System.out.println("Wait for the new node"); // Bottle neck
																// (a(killed),b,c(joined))
				backupServer = null;
			}
			handlingBACKUPCrash = false;
		}
	}
	/**
	 * This is to setup periodically ping to nodes to remove inactive ones
	 * Meant for PRIMARY only
	 */
	private void setupCleanNodesTask() {
		TimerTask task = (new TimerTask() {
			@Override
			public void run() {
				Iterator<Map.Entry<String, GameNodeInterface>> nodeIterator = nodes.entrySet().iterator();
				while (nodeIterator.hasNext()) {
					
					Entry<String, GameNodeInterface> entry = nodeIterator.next();
					GameNodeInterface node = entry.getValue();
					String playerId = entry.getKey();
					
					try {
						node.ping();
					} catch (RemoteException e) {
						// Remove node from the 'nodes'
						nodeIterator.remove();
						
						// Remove from 'game'
						game.removePlayer(playerId);
					}
				}
			}
		});

		Timer timer = new Timer();
		timer.scheduleAtFixedRate(task, 0, 700);
	}
	/*----------------------------------------------------------
			This is CLIENT side functions
	----------------------------------------------------------*/
	public void requestMove(int direction) throws RemoteException {
		System.out.println("\r\nMove "+direction+" - (slate): " + nodes.keySet());
		boolean ok = false;
		try {
			ok = primaryServer.move(this, direction);
			
		} catch (RemoteException e) {
			System.out.println("Failed to request PRIMARY ('requestMove'). Contact BACKUP.");
			try {
				if(backupServer != null){
					ok = backupServer.move(this, direction);
				}
			} catch (RemoteException e1) {
				System.out.println("Failed to contact both servers ('requestMove'). Contact other up-to-date nodes");
				
				// Either get servers info from other nodes or set itself as PRIMARY,...
				handleCannotReachBothServers();
				
				// Retry with new PRIMARY
				ok = primaryServer.move(this, direction);
			}
		}
		if(!ok){
			System.out.println("!!!Cannot move!!!");
		}
		// Update board in requesting user
		if(invokeUI && gui != null){
			gui.updateBoard(role, game.getPlayers(), game.getBoard());
		}
		
		if(printBoardAndScore){
			game.printScore();
			game.printBoard(); // Debug
		}
		
		// Player quit game
		if(ok && direction == 9){
			System.out.println("Player quit game!!!");
			// Removes the remote object, obj, from the RMI runtime
			UnicastRemoteObject.unexportObject(this, true);
			System.exit(0);
		}
	}
	
	private void handleCannotReachBothServers() throws RemoteException {
		// This case can be due to two cases:
		// 1. This node does not have latest servers info
		// 2. Both servers down before handling one of it (no 'move' or 'addNewNode')
		GameNodeInterface n = findActiveNodeWithHealthySevers();
		
		if( n !=null ){
			updatePrimaryServer(n.getPrimaryServer());
			updateBackUpServer(n.getBackupServer());
		} else {
			System.out.println("No other up-to-date nodes");
			
			// Made the first node in the list to be PRIMARY
			// BACKUP are created within this handler too
			handlePrimaryCrash();
			
		}
	}
	
	@Override
	public String getPlayerId() throws RemoteException{
		return playerId;
	}
	@Override
	public GameNodeInterface getPrimaryServer() throws RemoteException{
		return primaryServer;
	}
	@Override
	public GameNodeInterface getBackupServer() throws RemoteException{
		return backupServer;
	}
	@Override
	public Map<String, GameNodeInterface> getNodes() throws RemoteException{
		return nodes;
	}
	@Override
	public Game getGame() throws RemoteException{
		return game;
	}
	@Override
	public void ping() throws RemoteException{
		return;
	}
	@Override
	public void updateAll(GameNodeInterface primary, GameNodeInterface backup, Game game, Map<String, GameNodeInterface> nodes) throws RemoteException{
		updatePrimaryServer(primary);
		updateBackUpServer(backup);
		updateGame(game);
		updateNodes(nodes);
	}
	@Override
	public void updatePrimaryServer(GameNodeInterface primary) throws RemoteException {
		primaryServer = primary;
		if ( equals(this, primary) ) {
			System.out.println("PRIMARY server: ***YOU*** ");
			role = 1;
		}else{
			System.out.println("PRIMARY server ["+primaryServer.getPlayerId()+"]");
		}
	}
	@Override	
	public void updateBackUpServer(GameNodeInterface backup) throws RemoteException {
		backupServer = backup;
		if ( equals(this, backup) ) {
			System.out.println("BACKUP server: ***YOU*** ");
			role = 2;
		}else{
			System.out.println("BACKUP server ["+backupServer.getPlayerId()+"]");
		}
	}
	@Override
	public void updateGame(Game game) throws RemoteException {
		this.game.setBoard(game.getBoard());
		this.game.setPlayers(game.getPlayers());
	}
	@Override
	public void updateNodes(Map<String, GameNodeInterface> nodes) throws RemoteException {
		this.nodes = nodes;
	}
	
	/**
	 * This method is invoked within PRIMARY only
	 * It needs to be synchronous so that it can remove node from 'nodes'
	 */
	private synchronized GameNodeInterface findActiveNode(){
		Iterator<Map.Entry<String, GameNodeInterface>> nodeIterator = nodes.entrySet().iterator();
		while (nodeIterator.hasNext()) {
			
			Entry<String, GameNodeInterface> entry = nodeIterator.next();
			GameNodeInterface node = entry.getValue();
			String playerId = entry.getKey();
			
			if(equals(this, node)) continue; // Skip itself
			
			try {
				node.ping();
				return node;
			} catch (RemoteException e) {
				// Remove node from the 'nodes'
				nodeIterator.remove();
				
				// Remove from 'game'
				game.removePlayer(playerId); 
			}
		}
		return null;
	}
	/**
	 * 'checkServerHealth': To check servers info of this active node is up-to-date
	 */
	private synchronized GameNodeInterface findActiveNodeWithHealthySevers(){
		Iterator<Map.Entry<String, GameNodeInterface>> nodeIterator = nodes.entrySet().iterator();
		while (nodeIterator.hasNext()) {
			
			Entry<String, GameNodeInterface> entry = nodeIterator.next();
			GameNodeInterface node = entry.getValue();
			String playerId = entry.getKey();
			
			if(equals(this, node)) continue; // Skip itself
			
			try {
				// Check weather node still active
				try {
					node.ping();
				} catch (RemoteException e) {
					// Remove node from the 'nodes'
					nodeIterator.remove();
					
					// Remove from 'game'
					game.removePlayer(playerId); 
					continue;
				}
				// Check servers' health
				node.getPrimaryServer().ping();
				node.getBackupServer().ping();
				System.out.println("Both servers in ["+node.getPlayerId()+"] healthy.");
				return node;
			} catch (RemoteException e) {
				// Do not remove node here
			}
		}
		return null;
	}
	
	private void startInputThread(){
		input = new Thread() {
			@Override
	        public void run() {
				try {
					getInput();
				} catch (IOException e) {
					e.printStackTrace();
				}
	        }

		};
		input.start();
	}
	
	private void getInput() throws IOException{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String direction = null;
        while (true) {
        	while (!br.ready()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) { }
			}
			direction = br.readLine();
            if( !direction.equals("0") && !direction.equals("1") 
            	&& !direction.equals("2") && !direction.equals("3") 
            	&& !direction.equals("4") && !direction.equals("9"))
            {
            	System.out.println("Invalid input. ["+direction+"]");
            }else{
                try {
					requestMove(Integer.parseInt(direction));
				} catch (NumberFormatException e){
					System.out.println("Invalid input. ["+direction+"]");
				}
            }
        }
	}
	
	private static boolean equals(GameNodeInterface a, GameNodeInterface b) {
		if (a == null || b == null) return false;
		return a.hashCode() == b.hashCode();
	}
	
	/*----------------------------------------------------------
				MAIN
	----------------------------------------------------------*/
	public static void main(String[] args) throws Exception{
		System.out.println("---TWO---");
		if (args.length != 3) {
			printHelp();
			System.exit(0);
		}
		// Parse all arguments
		String ip = args[0];
		int port = Integer.parseInt(args[1]);
		String playerId = args[2];
		
		// Validate playerId
		if(playerId.length() != 2){
			System.out.println("playerId is not 2 characters. Exit!!!");
			System.exit(0);
		}

		System.out.println("Starting a game node ["+playerId+"].");
		long start = System.currentTimeMillis();

		// Connect to the Tracker
		Registry registry = null;
		try {
			registry = LocateRegistry.getRegistry(ip, port);
			TrackerInterface tracker = (TrackerInterface) registry.lookup("Tracker");

			GameNodeInterface newNode = new GameNode(playerId);
			
			// This 'addNode' method is synchronous to prevent 
			// 2 first nodes contacting at the same time, become 2 PRIMARY
			boolean isPrimary = tracker.addNode(newNode);
			
			/*
			 *  First game node joins
			 */
			if (isPrimary) {
				System.out.println("This is the first node, PRIMARY server");
				
				int N = tracker.getN();
				int K = tracker.getK();
				newNode.setupFirstGameNode(playerId, N, K);
			}
			/*
			 *  Subsequent nodes join
			 */
			else {
				// Ping all the nodes to find an active one
				boolean found = false;
				for(GameNodeInterface node : tracker.getNodes()){
					try {
						node.ping(); // ping all nodes to gather list of inactive nodes
						
						if(!found){
							found = true;
							if(!found) System.out.print("Contacting...["+node.getPlayerId()+"]...OK");
							// Only use the first found node
							newNode.setupNormalGameNode(playerId, node); 
							System.out.println("Joined the game. ["+ (System.currentTimeMillis()-start) + "ms] ");
							break;
						}
					} catch (RemoteException e) {
						if(!found) System.out.println("Retrying to contact other nodes...");
					}
				}
				if(!found){
					System.out.println("ERROR: Cannot find any current active players. Exit!!!");
					System.exit(0); // Debug
				}
			}
		} catch (RemoteException e) {
			e.printStackTrace();
			System.exit(0);
			
		} catch (NotBoundException e) {
			e.printStackTrace();
			System.exit(0);
			
		} catch (AlreadyBoundException e) {
			e.printStackTrace();
			System.exit(0);
			
		} catch (GameException e){
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	private static void printHelp() {
		System.out.println("java Game [IP-address] [port-number] [player-id]");
	}

}
