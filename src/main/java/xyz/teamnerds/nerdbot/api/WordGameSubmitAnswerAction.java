package xyz.teamnerds.nerdbot.api;

import java.util.List;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
@EqualsAndHashCode
public class WordGameSubmitAnswerAction implements NerdBotAction
{

    private String userId;
    private List<String> words;
}
