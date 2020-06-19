package xyz.teamnerds.nerdbot.slack;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;
import com.slack.api.Slack;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;

import xyz.teamnerds.nerdbot.api.HelpAction;
import xyz.teamnerds.nerdbot.api.KarmaReadAction;
import xyz.teamnerds.nerdbot.api.KarmaReadRankingAction;
import xyz.teamnerds.nerdbot.api.KarmaUpdateAction;
import xyz.teamnerds.nerdbot.api.NerdBotActionHandler;
import xyz.teamnerds.nerdbot.dao.KarmaDatastore;

@Component
public class SlackNerdBotActionHandler implements NerdBotActionHandler
{

	private static final Logger LOGGER = LoggerFactory.getLogger(SlackNerdBotActionHandler.class);
	
	@Autowired
	private KarmaDatastore karmaDatastore;
	
	@Override
	public void handleHelpAction(HelpAction action)
	{
		LOGGER.info("handleHelpAction " + action);
		
		String helpBlockJson = "[{\"type\":\"section\",\"text\":{\"type\":\"mrkdwn\",\"text\":\"Give kuddos to your teammates with karma points, they are like Bravo points, but with no actual value!  Use *@user++* or *@user--* after mentioning a user to give out points. \"}},{\"type\":\"divider\"},{\"type\":\"section\",\"text\":{\"type\":\"mrkdwn\",\"text\":\"*Example Usage*\"}},{\"type\":\"context\",\"elements\":[{\"type\":\"mrkdwn\",\"text\":\"*POK*: Thanks @Andrea ++ for QAing our bad code\"}]},{\"type\":\"context\",\"elements\":[{\"type\":\"mrkdwn\",\"text\":\"*HOMERBOT*: @Andrea karma increase to 100\"}]},{\"type\":\"context\",\"elements\":[{\"type\":\"mrkdwn\",\"text\":\"*POK*: Stop breaking the buid @Sony --\"}]},{\"type\":\"context\",\"elements\":[{\"type\":\"mrkdwn\",\"text\":\"*HOMERBOT*: @Sony karma decrease to -1\"}]},{\"type\":\"divider\"},{\"type\":\"section\",\"text\":{\"type\":\"mrkdwn\",\"text\":\"*More Plus/Minus equals More Good/Bad!*\"}},{\"type\":\"context\",\"elements\":[{\"type\":\"mrkdwn\",\"text\":\"*YOU*: Thanks @Pok++++++++++++++ for building this bot\"}]},{\"type\":\"context\",\"elements\":[{\"type\":\"mrkdwn\",\"text\":\"*HOMERBOT*: @Pok your karma increase to over 9000\"}]}]";
		sendMessageWithJson(action.getChannelId(), helpBlockJson);
	}
	
	@Override
	public void handleKarmaReadRankingAction(KarmaReadRankingAction action)
	{
		LOGGER.info("handleKarmaReadRankingAction " + action);
		@Nonnull String channelId = action.getChannelId();
		sendMessage(channelId, "TODO");
	}

	@Override
	public void handleKarmaReadAction(KarmaReadAction action)
	{
		LOGGER.info("handleKarmaReadAction " + action);
		String userId = action.getUserId();
		String channelId = action.getChannelId();
		
		Preconditions.checkNotNull(userId);
		Preconditions.checkNotNull(channelId);
		Integer karma = null;
		try
		{
			karma = karmaDatastore.getKarmaForUser(userId);
		}
		catch (IOException ex)
		{
			LOGGER.warn("Failed to read karma from datastore", ex);
		}
		
		int karmaIntValue = karma == null ? 0 : karma.intValue();
		String message = String.format("<@%1$s> your karma is %2$d", userId, karmaIntValue);
		Preconditions.checkNotNull(message);
		sendMessage(channelId, message);
	}

	@Override
	public void handleKarmaUpdateAction(KarmaUpdateAction action)
	{
		LOGGER.info("handleKarmaUpdateAction " + action);
		String userId = action.getUserId();
		int karma = action.getKarmaAmount();
		String channelId = action.getChannelId();
		Preconditions.checkNotNull(userId);
		Preconditions.checkNotNull(channelId);

		Integer newAmount = null;
		try
		{
			newAmount = karmaDatastore.incrementKarmaForUser(userId, karma);
		}
		catch (IOException ex)
		{
			LOGGER.warn("Failed to write karma to datastore", ex);
		}
		
		if (newAmount != null)
		{
			String increaseOrDecrease = karma > 0 ? "increased" : "decreased";
			String message = String.format("<@%1$s> your karma %2$s to %3$d", userId, increaseOrDecrease, newAmount);
			Preconditions.checkNotNull(message);
			sendMessage(channelId, message);
		}
	}

	
	private void sendMessage(@Nonnull String channel, @Nonnull String text)
	{
		Slack slack = Slack.getInstance();
		String token = new SlackConfiguration().getToken();
		try
		{
			
			ChatPostMessageResponse response = slack.methods(token)
					.chatPostMessage(req -> req.channel(channel).text(text));
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
	
	private void sendMessageWithJson(String channel, String json)
	{
		Slack slack = Slack.getInstance();
		String token = new SlackConfiguration().getToken();
		try
		{
			
			ChatPostMessageResponse response = slack.methods(token)
					.chatPostMessage(req -> req.channel(channel).blocksAsString(json));
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
