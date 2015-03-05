package agent;

import java.util.HashMap;

/**
 * Created by deepal on 3/4/15.
 */
public class Starter {


    public Starter(){
        Cache.neighbours= new HashMap<Integer, String>();
        Cache.fileCache= new HashMap<String, Node>();
        String BSIP = "127.0.0.1";
    }

    public static void main(String[] args) {


    }
}
