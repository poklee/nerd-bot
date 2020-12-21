package xyz.teamnerds.nerdbot.api;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Event when karma change occurs
 * 
 * @author plee
 *
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode
public class KarmaUpdateAction implements NerdBotAction
{
    private String userId;

    private String channelId;

    private int karmaAmount;

}
