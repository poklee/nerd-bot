package xyz.teamnerds.nerdbot.slack;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

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

import xyz.teamnerds.nerdbot.api.HelpAction;
import xyz.teamnerds.nerdbot.api.KarmaReadAction;
import xyz.teamnerds.nerdbot.api.KarmaUpdateAction;
import xyz.teamnerds.nerdbot.api.NerdBotActionHandler;

@Service
public class SlackWebhookEventHandlerImpl implements SlackWebhookEventHandler
{

	private Logger LOGGER = LoggerFactory.getLogger(SlackWebhookEventHandlerImpl.class);

	@Autowired
	private NerdBotActionHandler nerdBotActionHandler;

	@Autowired
	private TaskExecutor taskExecutor;

	@Override
	public void handlePayload(String contentBody)
	{
		if (taskExecutor == null)
		{
			LOGGER.error("Failed to auto wire TaskExecutor");
			return;
		}
		
		taskExecutor.execute(() -> {
			routeEvents(contentBody);
		});
	}
	
	private void routeEvents(String contentBody)
	{
		LOGGER.info("Routing events in thread " + Thread.currentThread().getName());
		
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
			if (payload != null)
			{
				handleAppMentionPayload(payload);
			}
		}
	}
	
	private void handleAppMentionPayload(@Nonnull AppMentionPayload payload)
	{
		AppMentionEvent messageEvent = payload.getEvent();
		if (messageEvent == null)
		{
			return;
		}
		
		
		LOGGER.info("handleAppMentionPayload text=" + messageEvent.getText());
		String text = messageEvent.getText();
		String user = messageEvent.getUser();
		String channel = messageEvent.getChannel();
		if (text != null && user != null && channel != null)
		{
			if (text.toLowerCase().contains("karma"))
			{
				KarmaReadAction action = KarmaReadAction.builder()
						.userId(user)
						.channelId(channel)
						.build();
				
				nerdBotActionHandler.handleKarmaReadAction(action);
			}
			else if (text.toLowerCase().contains("help"))
			{
				HelpAction action = HelpAction.builder()
						.userId(user)
						.channelId(channel)
						.build();
				
				nerdBotActionHandler.handleHelpAction(action);
			}
		}
	}

	public void handleMessagePayload(@Nonnull MessagePayload payload)
	{
		MessageEvent messageEvent = payload.getEvent();
		if (messageEvent == null || messageEvent.getBlocks() == null)
		{
			return;
		}
		
		LOGGER.info("handleMessagePayload text=" + messageEvent.getText());
		Map<String, Integer> userKarmaMap = new HashMap<>();

		// Parse message to build the user karma map
		for (LayoutBlock layoutBlock : messageEvent.getBlocks())
		{
			if (layoutBlock instanceof RichTextBlock)
			{
				RichTextBlock richTextBlock = (RichTextBlock) layoutBlock;
				for (BlockElement blockElement : richTextBlock.getElements())
				{
					if (blockElement instanceof RichTextSectionElement)
					{
						RichTextSectionElement richTextSectionElement = (RichTextSectionElement) blockElement;
						Stack<RichTextElement> stack = new Stack<>();
						for (RichTextElement richTextElement : richTextSectionElement.getElements())
						{
							if (richTextElement instanceof RichTextSectionElement.User)
							{
								// Nothing special needed
							}
							else if (richTextElement instanceof RichTextSectionElement.Text)
							{
								// See if there is karma points
								RichTextSectionElement.Text richTextSectionElementText = (RichTextSectionElement.Text) richTextElement;
								int karmaPoints = countKarmaPoints(richTextSectionElementText.getText());
								if (karmaPoints != 0 && stack.isEmpty() == false)
								{
									RichTextElement lastElement = stack.peek();
									if (lastElement instanceof RichTextSectionElement.User)
									{
										RichTextSectionElement.User user = (RichTextSectionElement.User) lastElement;
										String userId = user.getUserId();
										userKarmaMap.put(userId, karmaPoints);
									}
								}
							}
							stack.push(richTextElement);
						}
					}
				}
			}
		}

		for (Map.Entry<String, Integer> entry : userKarmaMap.entrySet())
		{
			String userId = entry.getKey();
			Integer karma = entry.getValue();
			if (userId != null)
			{
				KarmaUpdateAction action = KarmaUpdateAction.builder()
						.userId(userId)
						.channelId(messageEvent.getChannel())
						.karmaAmount(karma)
						.build();
				
				nerdBotActionHandler.handleKarmaUpdateAction(action);
			}
		}
	}

	private int countKarmaPoints(String text)
	{
		if (text == null)
		{
			return 0;
		}

		int count = 0;
		for (int i = 0; i < text.length(); i++)
		{
			char ch = text.charAt(i);
			if (Character.isWhitespace(ch))
			{
				// ignore
			}
			else if (ch == '+')
			{
				count++;
			}
			else if (ch == '-')
			{
				count--;
			}
			else
			{
				// no more plus or minus
				break;
			}
		}

		if (count >= 2)
		{
			return count - 1;
		}
		else if (count <= -2)
		{
			return count + 1;
		}
		else
		{
			return 0;
		}
	}


}
