package responses;

import java.util.ArrayList;

/**
 * Created by deepal on 3/5/15.
 */
public class FileSearchResponse {

    public ArrayList<String> filePaths;
    public ArrayList<String> cachedLocations;

    public FileSearchResponse(){}


    public FileSearchResponse(ArrayList<String> filePaths, ArrayList<String> cachedLocations){
        this.filePaths = filePaths;
        this.cachedLocations = cachedLocations;
    }
}
