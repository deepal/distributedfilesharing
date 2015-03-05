package agent;

/**
 * Created by deepal on 3/4/15.
 */
public class Node {
    public int port;
    public String ip;
    public String username;

    public Node(String ip, int port){
        this.ip = ip;
        this.port = port;
    }
}
