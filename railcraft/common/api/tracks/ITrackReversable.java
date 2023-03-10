package railcraft.common.api.tracks;

/**
 * Implementing this interface will allow your track to be direction specific.
 *
 * And so long as you inherit from TrackInstanceBase it will automatically be
 * reversable via the Crowbar.
 *
 * @author CovertJaguar <railcraft.wikispaces.com>
 */
public interface ITrackReversable extends ITrackInstance
{

    public boolean isReversed();

    public void setReversed(boolean reversed);
}
