package xyz.teamnerds.nerdbot;

import javax.annotation.CheckForNull;
import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import xyz.teamnerds.wordgame.gen.client.api.GameApi;
import xyz.teamnerds.wordgame.gen.client.handler.ApiClient;


/**
 * Entry point to connect to the word game microservice
 * 
 * @author plee
 */
@Component
public class WordGameServiceFactory
{
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WordGameServiceFactory.class);
    
    @Value("${nerdbot.wordgame.basepath}")
    private String wordGameBasePath;
    
    @PostConstruct
    public void init()
    {
        LOGGER.info("WordGameBasePath=" + wordGameBasePath);
    }
    
    /**
     * Create a new api client to connect to the microservice.  
     * The return object is not thread safe!
     * @return the client used to connect, not thread safe!
     */
    @CheckForNull
    public GameApi createService()
    {
        if (wordGameBasePath == null || wordGameBasePath.length() == 0)
        {
            LOGGER.info("nerdbot.wordgame.basepath was not set");
            return null;
        }
        else
        {
            ApiClient apiClient = new ApiClient(wordGameBasePath, null, null, null);
            GameApi gameApi = new GameApi(apiClient);
            return gameApi;
        }
    }

}
