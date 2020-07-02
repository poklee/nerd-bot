package xyz.teamnerds.nerdbot.slack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.slack.api.app_backend.events.EventTypeExtractor;
import com.slack.api.app_backend.events.EventTypeExtractorImpl;
import com.slack.api.app_backend.events.payload.AppMentionPayload;
import com.slack.api.app_backend.events.payload.MessagePayload;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.RichTextBlock;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.RichTextElement;
import com.slack.api.model.block.element.RichTextSectionElement;
import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.model.event.MessageEvent;
import com.slack.api.util.json.GsonFactory;

import xyz.teamnerds.nerdbot.api.WordGameActionHandler;
import xyz.teamnerds.nerdbot.api.WordGameHelpAction;
import xyz.teamnerds.nerdbot.api.WordGameShowLeaderboardsAction;
import xyz.teamnerds.nerdbot.api.WordGameSubmitAnswerAction;

@Service
public class SlackWordGameEventHandler implements SlackWebhookEventHandler
{

    private static final Logger LOGGER = LoggerFactory.getLogger(SlackWordGameEventHandler.class);
    
    @Autowired
    private TaskExecutor taskExecutor;
    
    @Autowired
    private WordGameActionHandler wordGameActionHandler;
    
    
    
    
    
    @Override
    public void handlePayload(String contentBody)
    {
        if (taskExecutor == null)
        {
            LOGGER.error("Failed to auto wire TaskExecutor");
            return;
        }

        taskExecutor.execute(() ->
        {
            handlePayloadInBackground(contentBody);
        });
    }
    
    private void handlePayloadInBackground(String contentBody)
    {
        LOGGER.info("SlackWordGameEventHandler handlePayload in thread " + Thread.currentThread().getName());

        EventTypeExtractor eventTypeExtractor = new EventTypeExtractorImpl();
        String eventType = eventTypeExtractor.extractEventType(contentBody);

        if (MessageEvent.TYPE_NAME.equals(eventType))
        {
            Gson gson = GsonFactory.createSnakeCase();
            MessagePayload payload = gson.fromJson(contentBody, MessagePayload.class);
            if (payload != null)
            {
                handleMessagePayload(payload);
            }
        }
        else if (AppMentionEvent.TYPE_NAME.equals(eventType))
        {
            // Handle app mentions here
            Gson gson = GsonFactory.createSnakeCase();
            AppMentionPayload payload = gson.fromJson(contentBody, AppMentionPayload.class);
            if (payload != null && payload.getEvent() != null)
            {
                handleAppMentionPayload(payload);
            }
        }
    }
    
    
    private void handleMessagePayload(@Nonnull MessagePayload payload)
    {
        if (payload.getEvent() == null)
        {
            return;
        }
        
        MessageEvent messageEvent = payload.getEvent();
        if (messageEvent == null)
        {
            return;
        }
        
        String userId = messageEvent.getUser();
        if (userId == null)
        {
            return;
        }
        
        List<LayoutBlock> blocks = messageEvent.getBlocks();
        if (blocks == null)
        {
            return;
        }
        
        Collection<String> allWords = parseWordsFromLayoutBlocks(blocks);
        if (allWords != null && !allWords.isEmpty())
        {
            List<String> uniqueWords = allWords.stream().distinct().collect(Collectors.toList());
            WordGameSubmitAnswerAction action = WordGameSubmitAnswerAction.builder()
                    .userId(userId)
                    .words(uniqueWords)
                    .build();
            
            wordGameActionHandler.handleSubmitAnswerAction(action);
        }
    }
    
    private void handleAppMentionPayload(@Nonnull AppMentionPayload payload)
    {
        AppMentionEvent appMentionEvent = payload.getEvent();
        if (appMentionEvent == null)
        {
            return;
        }
        
        String channelId = appMentionEvent.getChannel();
        if (channelId == null)
        {
            return;
        }
        
        String userId = appMentionEvent.getUser();
        if (userId == null)
        {
            return;
        }
        
        String text = appMentionEvent.getText();
        Set<String> words = parseWordsFromText(text).stream()
                .map(str -> str.toLowerCase())
                .collect(Collectors.toSet());
        
        if (words.contains("gamehelp"))
        {
            WordGameHelpAction action = WordGameHelpAction.builder()
                    .userId(userId)
                    .channelId(channelId)
                    .build();
            
            wordGameActionHandler.handleHelpAction(action);
        }
        else if (words.contains("gamescore"))
        {
            WordGameShowLeaderboardsAction action = WordGameShowLeaderboardsAction.builder()
                    .userId(userId)
                    .channelId(channelId)
                    .build();
            
            wordGameActionHandler.handleShowLeaderboardsAction(action);
        }
        
        
    }
    
    @Nonnull
    private List<String> parseWordsFromLayoutBlocks(@Nonnull List<LayoutBlock> layoutBlocks)
    {
        // Parse message to build the user karma map
        List<String> results = new ArrayList<>();
        for (LayoutBlock layoutBlock : layoutBlocks)
        {
            if (layoutBlock instanceof RichTextBlock)
            {
                RichTextBlock richTextBlock = (RichTextBlock) layoutBlock;
                for (BlockElement blockElement : richTextBlock.getElements())
                {
                    if (blockElement instanceof RichTextSectionElement)
                    {
                        RichTextSectionElement richTextSectionElement = (RichTextSectionElement) blockElement;
                        for (RichTextElement richTextElement : richTextSectionElement.getElements())
                        {
                            if (richTextElement instanceof RichTextSectionElement.Text)
                            {
                                RichTextSectionElement.Text richTextSectionElementText = (RichTextSectionElement.Text) richTextElement;
                                String text = richTextSectionElementText.getText();
                                results.addAll(parseWordsFromText(text));
                            }
                        }
                    }
                }
            }
        }
        
        return results;
    }
    
    @Nonnull
    private List<String> parseWordsFromText(@CheckForNull String text)
    {
        if (text == null)
        {
            return Collections.emptyList();
        }
        
        List<String> results = new ArrayList<>();
        String[] parts = text.split("\\W");
        for (String part : parts)
        {
            part = part.trim();
            if (part.length() > 0)
            {
                results.add(part);
            }
        }
        return results;
    }

}
