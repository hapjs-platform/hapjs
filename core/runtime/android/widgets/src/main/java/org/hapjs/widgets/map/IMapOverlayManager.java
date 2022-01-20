/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.map;

import android.view.View;
import java.util.List;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.widgets.map.model.BaseMapMarker;
import org.hapjs.widgets.map.model.HybridLatLng;
import org.hapjs.widgets.map.model.MapMarker;

public abstract class IMapOverlayManager {

    public abstract void setMarkerExtraTapListener(MapProxy.OnMarkerTapListener listener);

    public abstract void setCalloutTapListener(MapProxy.OnCalloutTapListener listener);

    public abstract void setOnControlClickListener(MapProxy.OnControlTapListener listener);

    public abstract void onMapRelease();

    public abstract void setMarkers(String markerContent);

    public abstract void setPolylines(String polylineContent);

    public abstract void setPolygons(String polygonContent);

    public abstract void setCircles(String circleContent);

    public abstract void setGrounds(String groundContent);

    public abstract void setControls(String controls);

    public abstract void setGeolocationMarkers(List<MapMarker> markers);

    public abstract void removeGeolocationMarkers();

    public abstract void addCustomMarkerView(View markView, BaseMapMarker baseMapMarker);

    public abstract void removeCustomMarkerView(View markView);

    public abstract void setHeatmapLayer(String heatMapLayerContent, RenderEventCallback callback);

    public abstract void hideShowingByClickCalloutMarkers();

    public abstract void translateMarker(
            int markerId,
            HybridLatLng destination,
            boolean autoRotate,
            int rotate,
            int duration,
            final MapProxy.OnAnimationEndListener listener,
            MapProxy.OnRetCallbackListener retCallbackListener);
}
