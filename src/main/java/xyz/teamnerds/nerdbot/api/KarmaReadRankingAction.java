package xyz.teamnerds.nerdbot.api;

import javax.annotation.Nonnull;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * 
 * @author plee
 *
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode
public class KarmaReadRankingAction implements NerdBotAction
{
	
	/**
	 * The channel id this action is being requested in
	 */
	@lombok.NonNull
	@Nonnull
	private String channelId;
	
	
	

}
