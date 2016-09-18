import java.awt.Point;
import java.io.Serializable;

public class Player implements Serializable {
	
	private static final long serialVersionUID = -5708078649704981318L;
	
	String id;
	Location location;
	public int score;
	
	public Player(String id, Location location) {
		this.id = id;
		this.location = location;
		this.score = 0;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Location getLocation() {
		return location;
	}
	public void moveTo(Location location) {
		this.location = location;
	}
	public int getScore() {
		return score;
	}
	public void setScore(int score) {
		this.score = score;
	}
	public void increaseScore(){
		this.score++;
	}
}
