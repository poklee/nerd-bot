package xyz.teamnerds.nerdbot.api;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;


/**
 * Event that indicates a user is asking for help
 * @author plee
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode
public class HelpAction implements NerdBotAction
{
	private String userId;
	
	private String channelId;

}
