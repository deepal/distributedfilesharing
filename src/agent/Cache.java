package agent;

import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * Created by deepal on 3/5/15.
 */
public class Cache {
    static volatile HashMap<String, Integer> neighbours;
    static volatile HashMap<String, HashSet<String>> fileCache;
    static HashMap<String, String> queryCache;
    static HashMap<String, HashSet<String>> myFiles;
    static String BSIP = "127.0.0.1";
    static int BSPORT = 55555;
    static int HOP_COUNT = 10;
    static String NODE_IP = "10.8.108.35";
    static int NODE_PORT = (new Random()).nextInt(65535-1024)+1024;
    static String NODE_USER = "node_A";
    static Semaphore semAgent;
    static Semaphore semService;
    static Semaphore semPrint;
}
