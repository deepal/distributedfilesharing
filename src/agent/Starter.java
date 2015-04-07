package agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Semaphore;

/**
 * Created by deepal on 3/4/15.
 */
public class Starter {

    public Starter() {

        Cache.myFiles = new HashMap<String, HashSet<String>>();
        Cache.neighbours = new HashMap<String, Integer>();
        Cache.fileCache = new HashMap<String, HashSet<String>>();
        Cache.queryCache = new HashMap<String, String>();
        Cache.semAgent = new Semaphore(0);
        Cache.semService = new Semaphore(1);
        Cache.semPrint = new Semaphore(1);

        Service agentService = new Service();
        Agent agentClient = new Agent();

        Thread serviceThread = new Thread(agentService);
        Thread clientThread = new Thread(agentClient);

        serviceThread.start();
        clientThread.start();
    }

    public static void main(String[] args) {
        new Starter();
    }
}
