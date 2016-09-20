import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Assuming that we have a RMI registry running on the same machine with Tracker
 * 
 */
public class Tracker extends UnicastRemoteObject implements TrackerInterface {
	private static final long serialVersionUID = 4262700063105105025L;
	private static int RETRY = 10;
	
	public int port, N, K;
	/*
	 * List of RMI naming of all GameNode 
	 * We use playerId as RMI naming for each GameNode
	 * 
	 * This list needs not tally with the actual current players in the game
	 * Some inactive nodes may not be removed from this list yet
	 */
	CopyOnWriteArrayList<GameNodeInterface> nodes;

	public Tracker(int port, int N, int K) throws RemoteException {
		this.port = port;
		this.N = N;
		this.K = K;
		nodes = new CopyOnWriteArrayList<GameNodeInterface>();
	}

	public static void main(String[] args)
			throws AccessException, RemoteException, NotBoundException, AlreadyBoundException, UnknownHostException {
		if (args.length != 3) {
			printHelp();
			System.exit(0);
		}
		// Parse all arguments
		int port = Integer.parseInt(args[0]);
		int N = Integer.parseInt(args[1]);
		int K = Integer.parseInt(args[2]);

		// Get IP address of current machine
		String ip = InetAddress.getLocalHost().getHostAddress();
		String trackerNaming = "Tracker";
		String address = "rmi://" + ip + ":" + port + "/" + trackerNaming;

		// Register the tracker to rmiregistry
		TrackerInterface stub = new Tracker(port, N, K);
		Registry registry = null;
		try {
			/*
			 * By default, port is 1099. Otherwise, port is as arguments.
			 */
			registry = LocateRegistry.getRegistry(port);
			
			// Retry because rmiregistry is slow in my laptop,
			// Hence I retry to start the Tracker a few times
			boolean ok = false;
			while(RETRY != 0  && !ok){
				try{
					registry.bind(trackerNaming, stub);
					ok = true;
					System.out.println("\nSuccessfully register Tracker at [" + address + "]");
					break;
				} catch (RemoteException e){
					try { Thread.sleep(4000); } catch (InterruptedException e1) { e1.printStackTrace(); }
				}
				
				System.out.print(RETRY+"...");
				RETRY--;
			}


		} catch (AlreadyBoundException e) {
			System.out.println("AlreadyBoundException: Retry to register the Tracker");

			registry.unbind(trackerNaming);
			registry.bind(trackerNaming, stub);

			System.out.println("Successfully register Tracker at [" + address + "]");

		}

		// To remove inactive one
		TimerTask task = (new TimerTask() {
			@Override
			public void run() {
				try {
					String currentPlayers = "";
					List<GameNodeInterface> removedNode = new ArrayList<GameNodeInterface>();
					
					// Remove inactive node
					Iterator<GameNodeInterface> it = stub.getNodes().iterator();
					while(it.hasNext()){
						GameNodeInterface node = it.next();
						try{
							node.ping();
							currentPlayers+=(node.getPlayerId()+" ");
						} catch (RemoteException e) {
							removedNode.add(node);
						}
					}
					stub.getNodes().removeAll(removedNode);
					System.out.println("Current players: "+currentPlayers);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
		
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(task, 0, 2000);
	}

	public int getN() throws RemoteException {
		return N;
	}

	public int getK() throws RemoteException {
		return K;
	}

	public synchronized CopyOnWriteArrayList<GameNodeInterface> getNodes() throws RemoteException {
		return nodes;
	}
	
	public synchronized boolean addNode(GameNodeInterface node) throws RemoteException, AlreadyBoundException {
		String nodeName = node.getPlayerId();
		boolean isPrimary = false;
		
		if(nodes.size() == 0){
			isPrimary = true;
		}else if(nodes.contains(nodeName)){
			throw new RemoteException("There is already a player with the same id. Exit!!!");
		}
		// Add to 'nodes'
		nodes.add(node);
		
		// The playerId should be unique hence do not expect the error here
//		Registry registry = LocateRegistry.getRegistry(port);
//		registry.bind(nodeName, node);

		System.out.println("Successfully register node [" + nodeName + "]");
		return isPrimary;
	}

	@Override
	public void removeInactiveNodes() throws RemoteException {
		int count = 0;
		Iterator<GameNodeInterface> it = nodes.iterator();
		while(it.hasNext()){
			GameNodeInterface node = it.next();
			try{
				node.ping();
			} catch (RemoteException e) {
				it.remove();
				count++;
			}
		}
		System.out.println("Total: " + count + " nodes removed");
	}
	
	private static void printHelp() {
		System.out.println("java Tracker [port-number] [N] [K]");
	}
}
