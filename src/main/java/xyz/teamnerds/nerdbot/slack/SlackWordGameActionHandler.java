package xyz.teamnerds.nerdbot.slack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Comparators;
import com.slack.api.Slack;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import xyz.teamnerds.nerdbot.WordGameServiceFactory;
import xyz.teamnerds.nerdbot.api.WordGameActionHandler;
import xyz.teamnerds.nerdbot.api.WordGameHelpAction;
import xyz.teamnerds.nerdbot.api.WordGameShowLeaderboardsAction;
import xyz.teamnerds.nerdbot.api.WordGameSubmitAnswerAction;
import xyz.teamnerds.wordgame.gen.client.api.GameApi;
import xyz.teamnerds.wordgame.gen.client.model.GameAnswerRecord;
import xyz.teamnerds.wordgame.gen.client.model.GameInfo;
import xyz.teamnerds.wordgame.gen.client.model.GameScoreInfo;
import xyz.teamnerds.wordgame.gen.client.model.SubmitAnswerRequest;
import xyz.teamnerds.wordgame.gen.client.model.SubmitAnswerResponse;


@Component
public class SlackWordGameActionHandler implements WordGameActionHandler
{
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SlackWordGameActionHandler.class);

    @Autowired
    private WordGameServiceFactory wordGameServiceFactory;
    
    @Override
    public void handleHelpAction(@Nonnull WordGameHelpAction action)
    {
        LOGGER.info(getClass().getSimpleName() + " handleHelpAction " + action);
        String channelId = action.getChannelId();
        if (channelId == null)
        {
            return;
        }
        
        GameApi gameApi = wordGameServiceFactory.createService();
        if (gameApi == null)
        {
            return;
        }
        try
        {
            GameInfo currentGame = gameApi.getDailyGameInfo();
            if (currentGame == null)
            {
                LOGGER.info("No daily game found, cannot submit answer");
                return;
            }
            
            String currentGameLetters = currentGame.getId().toUpperCase();
            sendHelpMessage(channelId, currentGameLetters);
        }
        catch (IOException ex)
        {
            LOGGER.warn("Failed to connect to Word Game API", ex);
        }
    }
    
    @Override
    public void handleSubmitAnswerAction(@Nonnull WordGameSubmitAnswerAction action)
    {
        LOGGER.info(getClass().getSimpleName() + " handleSubmitAnswerAction " + action);
        GameApi gameApi = wordGameServiceFactory.createService();
        if (gameApi == null)
        {
            return;
        }
        try
        {
            GameInfo currentGame = gameApi.getDailyGameInfo();
            if (currentGame == null)
            {
                LOGGER.info("No daily game found, cannot submit answer");
                return;
            }
            
            String gameId = currentGame.getId();
            String userId = action.getUserId();
            
            SubmitAnswerRequest submitAnswerRequest = new SubmitAnswerRequest();
            submitAnswerRequest.setAnswers(action.getWords());
            submitAnswerRequest.setUser(userId);
    
            SubmitAnswerResponse submitAnswerResponse = gameApi.submitAnswer(gameId, submitAnswerRequest);
            List<String> acceptedAnswers = submitAnswerResponse.getAcceptedAnswers();
            if (acceptedAnswers == null || acceptedAnswers.isEmpty())
            {
                LOGGER.info("Got 0 words.");
            }
            else
            {
                String acceptedAnswersString = acceptedAnswers.stream().collect(Collectors.joining(","));
                LOGGER.info("Got " + acceptedAnswers.size() + " words.  List=" + acceptedAnswersString);
            }
            
        }
        catch (IOException ex)
        {
            LOGGER.warn("Failed to connect to Word Game API", ex);
        }
    }
    
    @Override
    public void handleShowLeaderboardsAction(@Nonnull WordGameShowLeaderboardsAction action)
    {
        LOGGER.info(getClass().getSimpleName() + " handleShowLeaderboardsAction " + action);
        GameApi gameApi = wordGameServiceFactory.createService();
        if (gameApi == null)
        {
            return;
        }
        
        List<GameAnswerRecord> gameAnswerRecords = null;
        String gameId = null;
        try
        {
            GameInfo currentGame = gameApi.getDailyGameInfo();
            gameId = currentGame.getId();
            GameScoreInfo gameScoreInfo = gameApi.getLeaderboardInfo(currentGame.getId());
            gameAnswerRecords = gameScoreInfo.getAnswers();
        }
        catch (IOException ex)
        {
            LOGGER.warn("Failed to connect to Word Game API", ex);
        }
        
        if (gameAnswerRecords == null)
        {
            return;
        }
        
        MultiValuedMap<String, GameAnswerRecord> userGameAnswerRecords = new ArrayListValuedHashMap<String, GameAnswerRecord>();
        for (GameAnswerRecord gameAnswerRecord : gameAnswerRecords)
        {
            userGameAnswerRecords.put(gameAnswerRecord.getExternalUserId(), gameAnswerRecord);
        }


        List<UserLeaderboardEntry> leaderboards = new ArrayList<>();
        for (String externalUserId : userGameAnswerRecords.keySet())
        {
            List<GameAnswerRecord> records = userGameAnswerRecords.get(externalUserId)
                    .stream()
                    .collect(Collectors.toList());
            
            int totalScore = records.stream()
                    .map(r -> r.getScore())
                    .collect(Collectors.summingInt(score -> score.intValue()));
            
            UserLeaderboardEntry userLeaderboardEntry = UserLeaderboardEntry.builder()
                    .externalUserId(externalUserId)
                    .gameAnswerRecords(records)
                    .totalScore(totalScore)
                    .build();
            
            leaderboards.add(userLeaderboardEntry);
        }
        
        leaderboards = leaderboards
                .stream()
                .sorted(Comparator.comparing(UserLeaderboardEntry::getTotalScore))
                .collect(Collectors.toList());
        
        StringBuilder sb = new StringBuilder();
        sb.append("Today's game score for game id: ").append(gameId).append("\n");
        sb.append("Users have found a total of ").append(gameAnswerRecords.size()).append(" words\n");
        for (int i=0; i<leaderboards.size(); i++)
        {
            
            switch (i)
            {
                case 0:
                    sb.append(":first_place_medal:");
                    break;
                case 1:
                    sb.append(":first_place_medal:");
                    break;
                case 2:
                    sb.append(":first_place_medal:");
                    break;
                default:
                    break;
            }
            
            UserLeaderboardEntry userLeaderboardEntry = leaderboards.get(i);
            sb.append(" <@").append(userLeaderboardEntry.getExternalUserId()).append("> scored ").append(userLeaderboardEntry.getTotalScore()).append(" points.  ");
            for (GameAnswerRecord gameAnswerRecord : userLeaderboardEntry.getGameAnswerRecords())
            {
                sb.append(gameAnswerRecord.getWord()).append("(").append(gameAnswerRecord.getScore()).append(")").append(" ");
            }
            sb.append("\n");
        }
        
        sendMessage(action.getChannelId(), sb.toString());
    }
    
    @Builder
    @Getter
    @ToString
    private static class UserLeaderboardEntry
    {
        private String externalUserId;
        private int totalScore;
        private List<GameAnswerRecord> gameAnswerRecords;
    }
    
    private void sendHelpMessage(@Nonnull String channelId, @Nonnull String currentGameLetters)
    {
        String json = String.format("{\"blocks\":[{\"type\":\"section\",\"text\":{\"type\":\"mrkdwn\",\"text\":\"Today the hidden words are composed of these characters: *%s*\"}},{\"type\":\"divider\"},{\"type\":\"section\",\"text\":{\"type\":\"mrkdwn\",\"text\":\"*How to Play*: Anytime you say one of the hidden word of the days, you get points!  The more you chat, the more points you can possibly earn.  The hidden words of day are made up of the same characters. For example if the characters are *N,I,S,R,E,U,G,Y*, the hidden words will be all words that uses those combo of characters.  Example words are: guy, sir, sing, singer, etc.  \"}},{\"type\":\"context\",\"elements\":[{\"type\":\"mrkdwn\",\"text\":\"*Detail Explaination:*\"},{\"type\":\"mrkdwn\",\"text\":\"* Each character can only be used once unless there are multiple characters in the list.\\n* The longer words are worth more points\\n* A valid word is at least 3 characters long\"}]},{\"type\":\"divider\"},{\"type\":\"section\",\"text\":{\"type\":\"mrkdwn\",\"text\":\"*Other Commands*\\n@homerbot gamehelp\\n@homerbot gamescore - shows score for today's game\\n@homerbot gameleaderboards - show the all time leaders in points\"}}]}", currentGameLetters);
        sendMessage(channelId, json);
    }
    
    private void sendMessage(@Nonnull String channelId, @Nonnull String text)
    {
        Slack slack = Slack.getInstance();
        String token = new SlackConfiguration().getToken();
        try
        {
            ChatPostMessageResponse response = slack.methods(token)
                    .chatPostMessage(req -> req.channel(channelId).text(text));
            LOGGER.info("ChatPostMessageResponse = " + response);
        }
        catch (Exception ex)
        {
            LOGGER.error("Failed to send slack message.", ex);
        }
        finally
        {

        }
    }

}
