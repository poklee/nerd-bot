package xyz.teamnerds.nerdbot;

import xyz.teamnerds.wordgame.gen.client.api.GameApi;
import xyz.teamnerds.wordgame.gen.client.handler.ApiClient;


/**
 * Entry point to connect to the word game microservice
 * 
 * @author plee
 */
public class WordGameServiceFactory
{
    
    /**
     * Create a new api client to connect to the microservice.  
     * The return object is not thread safe!
     * @return the client used to connect, not thread safe!
     */
    public static GameApi createService()
    {
        String basePath = "http://127.0.0.1:8080/v0";
        ApiClient apiClient = new ApiClient(basePath, null, null, null);
        GameApi gameApi = new GameApi(apiClient);
        return gameApi;
    }

}
