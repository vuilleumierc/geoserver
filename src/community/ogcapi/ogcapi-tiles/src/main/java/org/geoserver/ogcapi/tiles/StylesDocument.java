/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.tiles;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.ogcapi.StyleDocument;
import org.geoserver.ows.util.ResponseUtils;
import org.geowebcache.layer.TileLayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Contains the list of styles for a given collection */
@JsonPropertyOrder({"styles", "links"})
public class StylesDocument extends AbstractDocument {
    private static final String REL_MAP = "map";
    private final TileLayer tileLayer;

    private final String tileLayerId;

    public StylesDocument(TileLayer tileLayer) {
        this.tileLayer = tileLayer;
        this.tileLayerId = TilesService.getTileLayerId(tileLayer);

        addSelfLinks("ogc/tiles/collections/" + ResponseUtils.urlEncode(tileLayerId) + "/styles");
    }

    public List<StyleDocument> getStyles() {
        return getStyleInfos().stream().map(this::toDocument).collect(Collectors.toList());
    }

    private StyleDocument toDocument(StyleInfo s) {

        StyleDocument result;
        if (s != null) {
            result = new StyleDocument(s);
        } else {
            // layer group and GWC native layer case
            result =
                    new StyleDocument(
                            StyleDocument.DEFAULT_STYLE_NAME, "Default style for " + tileLayerId);
        }

        // are the map tiles at all?
        if (!tileLayer.getMimeTypes().stream().allMatch(mt -> mt.isVector())) {
            result.addLinksFor(
                    "ogc/tiles/collections/"
                            + ResponseUtils.urlEncode(tileLayerId)
                            + "/styles/map/tiles",
                    TilesDocument.class,
                    "Tilesets list for "
                            + tileLayerId
                            + " with style "
                            + s.getName()
                            + ", represented as ",
                    "tilesets",
                    null,
                    TiledCollectionDocument.REL_TILESETS_MAP);
        }

        return result;
    }

    private List<StyleInfo> getStyleInfos() {
        if (tileLayer instanceof GeoServerTileLayer) {
            PublishedInfo published = ((GeoServerTileLayer) tileLayer).getPublishedInfo();
            if (published instanceof LayerInfo) {
                List<StyleInfo> result = new ArrayList<>();
                LayerInfo layer = (LayerInfo) published;
                result.addAll(layer.getStyles());
                StyleInfo defaultStyle = layer.getDefaultStyle();
                if (!result.contains(defaultStyle)) result.add(defaultStyle);
                return result;
            }
        }
        // layer groups and native tile layers do not have a named style right now
        return Collections.emptyList();
    }

    @JsonIgnore
    public TileLayer getTileLayer() {
        return this.tileLayer;
    }

    @JsonIgnore
    public String getTileLayerId() {
        return tileLayerId;
    }
}
