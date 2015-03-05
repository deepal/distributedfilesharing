package responses;

/**
 * Created by deepal on 3/5/15.
 */
public class FileSearchResponse {

    public String[] filePaths;
    public String[] cachedLocations;

    public FileSearchResponse(String[] filePaths, String[] cachedLocations){
        this.filePaths = filePaths;
        this.cachedLocations = cachedLocations;
    }
}
