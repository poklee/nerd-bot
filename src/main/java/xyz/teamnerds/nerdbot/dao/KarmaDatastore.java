package xyz.teamnerds.nerdbot.dao;

import java.io.IOException;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * The interface to the datastore
 */
public interface KarmaDatastore
{
	@CheckForNull
	public Integer getKarmaForUser(@Nonnull String userId) throws IOException;
	
	public int incrementKarmaForUser(@Nonnull String userId, int amount) throws IOException;

	/**
	 * Get the top karma rankings
	 *
	 * @return a sorted list of top karma users
	 * @throws IOException if datastore cannot read the info
	 */
	@Nonnull
	public List<UserKarma> getUserKarmaRankings() throws IOException;
}
