package agent;

import java.util.HashMap;

/**
 * Created by deepal on 3/5/15.
 */
public class Cache {
    static volatile HashMap<String, Integer> neighbours;
    static volatile HashMap<String, Node> fileCache;
}
