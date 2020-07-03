package xyz.teamnerds.nerdbot.api;

import javax.annotation.Nonnull;

public interface WordGameActionHandler
{

    public void handleHelpAction(@Nonnull WordGameHelpAction action);
    
    public void handleSubmitAnswerAction(@Nonnull WordGameSubmitAnswerAction action);
    
    public void handleShowGameScoreAction(@Nonnull WordGameShowGameScoreAction action);
    
    public void handleShowUserScoreAction(@Nonnull WordGameShowUserScoreAction action);
    
}
