package xyz.teamnerds.nerdbot.api;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Event when a user ask for his/her karma
 * 
 * @author plee
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode
public class KarmaReadAction implements NerdBotAction
{

    private String userId;

    private String channelId;
}
