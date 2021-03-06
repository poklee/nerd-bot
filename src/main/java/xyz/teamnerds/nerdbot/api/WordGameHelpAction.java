package xyz.teamnerds.nerdbot.api;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
@EqualsAndHashCode
public class WordGameHelpAction implements NerdBotAction
{

    private String userId;
    
    private String channelId;
    
}
