package cz.lastaapps.black;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;

@TargetApi(24)
public class MyTileService  extends android.service.quicksettings.TileService {

    @Override
    public void onTileAdded() {
        Tile tile = getQsTile();
        tile.setState(Tile.STATE_INACTIVE);
        tile.setIcon(Icon.createWithResource(this,
                R.drawable.icon));
        tile.setLabel(getString(R.string.tile_name));
        tile.setContentDescription(
                "shows black over whole screen"/*getString(R.string.tile_content_description)*/);
        tile.updateTile();
    }

    @Override
    public void onStartListening() {
        Tile tile = getQsTile();
        int newState = MainActivity.isMyServiceRunning(FloatingService.class) ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
        tile.setState(newState);
        tile.updateTile();
    }

    public void onStopListening() {}

    @Override
    public void onClick() {
        Tile tile = getQsTile();
        int newState = tile.getState() == Tile.STATE_ACTIVE ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE;
        tile.setState(newState);

        Intent intent = new Intent(this, FloatingService.class);

        if (tile.getState() == Tile.STATE_ACTIVE) {
            startService(intent);
        } else if (tile.getState() == Tile.STATE_INACTIVE) {
            stopService(intent);
        }

        tile.updateTile();
    }
}
