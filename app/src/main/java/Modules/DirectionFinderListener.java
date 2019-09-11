package Modules;

import java.util.List;

public interface DirectionFinderListener {
    void onCameraMoveStarted(int reason);

    void onDirectionFinderStart();
    void onDirectionFinderSuccess(List<Route> route);
}
