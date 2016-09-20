import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

public interface TrackerInterface extends Remote {
	
	int getN() throws RemoteException;
	
	int getK() throws RemoteException;
	
	CopyOnWriteArrayList<GameNodeInterface> getNodes() throws RemoteException;
	
	boolean addNode(GameNodeInterface node) throws RemoteException, AlreadyBoundException;

	void removeInactiveNodes() throws RemoteException;

}