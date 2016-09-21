import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Vector;

public interface GameNodeInterface extends Remote{
	
	/*----------------------------------------------------------
		This is SERVER side functions
	----------------------------------------------------------*/
	
	void addNewNode(GameNodeInterface node) throws RemoteException;
	
	boolean move(GameNodeInterface node,  int direction) throws RemoteException;
	
	/*----------------------------------------------------------
		This is CLIENT side functions
	----------------------------------------------------------*/
	
	String getPlayerId() throws RemoteException;
	
	GameNodeInterface getPrimaryServer() throws RemoteException;
	
	GameNodeInterface getBackupServer() throws RemoteException;
	
	Map<String, GameNodeInterface> getNodes() throws RemoteException;
	
	Game getGame() throws RemoteException;
	
	void ping() throws RemoteException;

	void updateAll(GameNodeInterface primary, GameNodeInterface backup, Game game, Map<String, GameNodeInterface> nodes) throws RemoteException;
	
	void updatePrimaryServer(GameNodeInterface node) throws RemoteException;

	void updateBackUpServer(GameNodeInterface node) throws RemoteException;

	void updateGame(Game game) throws RemoteException;

	void updateNodes(Map<String, GameNodeInterface> nodes) throws RemoteException;

}
