package xyz.teamnerds.nerdbot.slack;

import java.io.IOException;
import java.util.ArrayList;
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

import com.slack.api.Slack;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import xyz.teamnerds.nerdbot.WordGameServiceFactory;
import xyz.teamnerds.nerdbot.api.WordGameActionHandler;
import xyz.teamnerds.nerdbot.api.WordGameHelpAction;
import xyz.teamnerds.nerdbot.api.WordGameShowGameScoreAction;
import xyz.teamnerds.nerdbot.api.WordGameShowUserScoreAction;
import xyz.teamnerds.nerdbot.api.WordGameSubmitAnswerAction;
import xyz.teamnerds.wordgame.gen.client.api.GameApi;
import xyz.teamnerds.wordgame.gen.client.model.GameAnswerRecord;
import xyz.teamnerds.wordgame.gen.client.model.GameInfo;
import xyz.teamnerds.wordgame.gen.client.model.GameScoreInfo;
import xyz.teamnerds.wordgame.gen.client.model.SubmitAnswerRequest;
import xyz.teamnerds.wordgame.gen.client.model.SubmitAnswerResponse;
import xyz.teamnerds.wordgame.gen.client.model.UserLeaderboardInfo;
import xyz.teamnerds.wordgame.gen.client.model.UserScoreInfo;


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
                
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("Congrats <@%s>, you said the hidden words of the day!  Say `@homerbot gamehelp` for more info.  Words=%s", acceptedAnswersString));
                sendMessage(action.getChannelId(), sb.toString());
            }
            
        }
        catch (IOException ex)
        {
            LOGGER.warn("Failed to connect to Word Game API", ex);
        }
    }
    
    @Override
    public void handleShowGameScoreAction(@Nonnull WordGameShowGameScoreAction action)
    {
        LOGGER.info(getClass().getSimpleName() + " handleShowGameScoreAction " + action);
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
    
    @Override
    public void handleShowUserScoreAction(@Nonnull WordGameShowUserScoreAction action)
    {
        LOGGER.info(getClass().getSimpleName() + " handleShowUserScoreAction " + action);
        GameApi gameApi = wordGameServiceFactory.createService();
        if (gameApi == null)
        {
            return;
        }
        
        List<UserScoreInfo> topUsers = null;
        try
        {
            UserLeaderboardInfo userLeaderboardInfo = gameApi.getUserLeaderboards();
            if (userLeaderboardInfo != null)
            {
                topUsers = userLeaderboardInfo.getTopUsers();
            }
            
        }
        catch (IOException ex)
        {
            LOGGER.warn("Failed to connect to Word Game API", ex);
        }
        
        if (topUsers == null)
        {
            LOGGER.warn("Failed to get Top Users, null response");
            return;
        }
        
        // Possible no one has played any game yet
        if (topUsers.isEmpty())
        {
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<topUsers.size(); i++)
        {
            UserScoreInfo userScoreInfo = topUsers.get(i);
            switch (i)
            {
            case 0:
                sb.append(String.format(":first_place_medal: <@%s> has all time score of %s points\n", userScoreInfo.getExternalUserId(), userScoreInfo.getTotalScore()));
                break;
            case 1:
                sb.append(String.format(":second_place_medal: <@%s> has all time score of %s points\n", userScoreInfo.getExternalUserId(), userScoreInfo.getTotalScore()));
                break;
            case 2:
                sb.append(String.format(":third_place_medal: <@%s> has all time score of %s points\n", userScoreInfo.getExternalUserId(), userScoreInfo.getTotalScore()));
                break;
            default:
                sb.append(String.format(":medal: <@%s> has all time score of %s points\n", userScoreInfo.getExternalUserId(), userScoreInfo.getTotalScore()));
                break;
            }
            
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
        String headerText = String.format("*Word Game*\nToday the hidden works are compose of these characters: *%s*\n\n", currentGameLetters);
        String detailText = "*How to Play*\n You will score points if you say any of the hidden words.  All the hidden words are composed of the above characters only.  For example, if the characters are *ABCDEFG*, the hidden words will be cab, fad, deaf, etc...\n";
        String bullet1 = "- The characters can only be used once unless there are duplicates\n";
        String bullet2 = "- Longer word scores more points\n";
        String bullet3 = "- Say `@homerbot gamehelp` to display this message again\n";
        String bullet4 = "- Say `@homerbot gamescore` to display the current game scores\n";
        String bullet5 = "- Say `@homerbot userscore` to display the all time user scores\n";
        
        StringBuilder sb = new StringBuilder();
        sb.append(headerText);
        sb.append(detailText);
        sb.append(bullet1);
        sb.append(bullet2);
        sb.append(bullet3);
        sb.append(bullet4);
        sb.append(bullet5);
        
        sendMessage(channelId, sb.toString());
    }
    
    private void sendMessage(@Nonnull String channelId, @Nonnull String text)
    {
        Slack slack = Slack.getInstance();
        String token = new SlackConfiguration().getToken();
        try
        {
            ChatPostMessageResponse response = slack.methods(token)
                    .chatPostMessage(req -> req.channel(channelId).text(text));
            if (response == null)
            {
                LOGGER.warn("Failed to send slack message, null response?");
            }
            else if (response.isOk())
            {
                LOGGER.info("Slack message sent to channel " + channelId);
            }
            else
            {
                LOGGER.warn("Failed to send slack message, response=" + response);
            }
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
