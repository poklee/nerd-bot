package xyz.teamnerds.nerdbot.dao;

import java.io.IOException;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

public interface KarmaDatastore
{
	@CheckForNull
	public Integer getKarmaForUser(@Nonnull String userId) throws IOException;
	
	public int incrementKarmaForUser(@Nonnull String userId, int amount) throws IOException;
}
