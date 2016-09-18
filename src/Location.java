import java.io.Serializable;

public class Location implements Serializable{
	
	private static final long serialVersionUID = -2382852890508845098L;
	
	int x, y;
	
	@Override
	public String toString() {
		return "[x=" + x + ", y=" + y + "]";
	}
	public Location(Location p){
		this.x = p.getX();
		this.y = p.getY();
	}
	public Location(int x, int y) {
		this.x = x;
		this.y = y;
	}
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
}
