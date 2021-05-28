/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.tiles;

import static org.geoserver.ows.util.ResponseUtils.appendPath;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wms.WMS;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.meta.TileJSON;
import org.geowebcache.mime.MimeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Tileset extends AbstractDocument {

    public static final String TILE_REL = "item";
    private final String styleId;
    private final String tileMatrixId;

    /** The type of tileset, according to the spec */
    public enum DataType {
        map,
        vector,
        coverage
    }

    String tileMatrixSetURI;
    String tileMatrixSetDefinition;
    DataType dataType;
    private final String gridSubsetId;

    List<TileMatrixSetLimit> tileMatrixSetLimits = new ArrayList<>();

    public Tileset(
            WMS wms,
            TileLayer tileLayer,
            DataType dataType,
            String tileMatrixId,
            boolean addDetails) {
        this(wms, tileLayer, dataType, tileMatrixId, null, addDetails);
    }

    public Tileset(
            WMS wms,
            TileLayer tileLayer,
            DataType dataType,
            String tileMatrixId,
            String styleId,
            boolean addDetails) {
        GridSubset gridSubset = tileLayer.getGridSubset(tileMatrixId);
        this.gridSubsetId = tileMatrixId;
        this.dataType = dataType;
        this.styleId = styleId;
        this.tileMatrixId = tileMatrixId;
        String baseURL = APIRequestInfo.get().getBaseURL();

        // TODO: link definition to local URL, but if the matrix is a well known one,
        // use the well known URI for tileMatrixSetURI instead
        this.tileMatrixSetURI =
                ResponseUtils.buildURL(
                        baseURL,
                        "ogc/tiles/tileMatrixSets/"
                                + ResponseUtils.urlEncode(gridSubset.getGridSet().getName()),
                        null,
                        URLMangler.URLType.SERVICE);
        this.tileMatrixSetDefinition = this.tileMatrixSetURI;

        if (!gridSubset.fullGridSetCoverage() && addDetails) {
            String[] levelNames = gridSubset.getGridNames();
            long[][] wmtsLimits = gridSubset.getWMTSCoverages();

            for (int i = 0; i < levelNames.length; i++) {
                TileMatrixSetLimit limit =
                        new TileMatrixSetLimit(
                                levelNames[i],
                                wmtsLimits[i][1],
                                wmtsLimits[i][3],
                                wmtsLimits[i][0],
                                wmtsLimits[i][2]);
                tileMatrixSetLimits.add(limit);
            }
        }

        // go for the links
        this.id =
                tileLayer instanceof GeoServerTileLayer
                        ? ((GeoServerTileLayer) tileLayer).getContextualName()
                        : tileLayer.getName();
        if (dataType == DataType.vector) {
            addSelfLinks("ogc/tiles/collections/" + id + "/tiles/" + tileMatrixId);
        } else if (dataType == DataType.map) {
            if (styleId == null) {
                addSelfLinks("ogc/tiles/collections/" + id + "/map/tiles/" + tileMatrixId);
            } else {
                addSelfLinks("ogc/tiles/collections/" + id + "/styles/" + styleId + "/map/tiles/" + tileMatrixId);
            }
        } else {
            throw new IllegalArgumentException("Cannot handle data type: " + dataType);
        }

        if (addDetails) {
            // links depend on the data type
            List<MimeType> tileTypes = tileLayer.getMimeTypes();
            if (dataType == DataType.vector) {
                tileTypes.stream()
                        .filter(mt -> mt.isVector())
                        .collect(Collectors.toList())
                        .forEach(
                                dataFormat ->
                                        addTilesLinkForFormat(
                                                this.id,
                                                baseURL,
                                                dataFormat.getFormat(),
                                                appendPath(
                                                        "/tiles/",
                                                        tileMatrixId,
                                                        "/{tileMatrix}/{tileRow}/{tileCol}"),
                                                TILE_REL));

                // tileJSON
                addLinksFor(
                        "ogc/tiles/collections/" + id + "/tiles/" + tileMatrixId + "/metadata",
                        TileJSON.class,
                        "Tiles metadata as ",
                        "metadata",
                        (m, l) -> l.setTemplated(true),
                        "describedBy");
            } else if (dataType == DataType.map) {
                List<MimeType> imageFormats =
                        tileTypes.stream()
                                .filter(mt -> !mt.isVector())
                                .collect(Collectors.toList());
                String base = styleId != null ? "/styles/" + styleId + "/map/tiles/" : "/map/tiles/";
                imageFormats.forEach(
                        imageFormat ->
                                addTilesLinkForFormat(
                                        this.id,
                                        baseURL,
                                        imageFormat.getFormat(),
                                        appendPath(
                                                base,
                                                tileMatrixId,
                                                "/{tileMatrix}/{tileRow}/{tileCol}"),
                                        TILE_REL));

                // add the info links (might be needed only for maps, but we always have a style
                // so...)
                wms.getAvailableFeatureInfoFormats()
                        .forEach(
                                infoFormat ->
                                        addTilesLinkForFormat(
                                                this.id,
                                                baseURL,
                                                infoFormat,
                                                appendPath(
                                                        base,
                                                        tileMatrixId,
                                                        "/{tileMatrix}/{tileRow}/{tileCol}/info"),
                                                "info"));

                // tileJSON
                addLinksFor(
                        "ogc/tiles/collections/" + id + base + tileMatrixId + "/metadata",
                        TileJSON.class,
                        "Tiles metadata as ",
                        "metadata",
                        (m, l) -> {
                            l.setTemplated(true);
                            l.setHref(l.getHref() + "&tileFormat={tileFormat}");
                        },
                        "describedBy");
            } else {
                throw new IllegalArgumentException(
                        "Tiles of this type are not yet supported: " + dataType);
            }
        }
    }

    public String getTileMatrixSetURI() {
        return tileMatrixSetURI;
    }

    public void setTileMatrixSetURI(String tileMatrixSetURI) {
        this.tileMatrixSetURI = tileMatrixSetURI;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<TileMatrixSetLimit> getTileMatrixSetLimits() {
        return tileMatrixSetLimits;
    }

    public void setTileMatrixSetLimits(List<TileMatrixSetLimit> tileMatrixSetLimits) {
        this.tileMatrixSetLimits = tileMatrixSetLimits;
    }

    public String getTileMatrixSetDefinition() {
        return tileMatrixSetDefinition;
    }

    public void setTileMatrixSetDefinition(String tileMatrixSetDefinition) {
        this.tileMatrixSetDefinition = tileMatrixSetDefinition;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    @JsonIgnore
    public String getGridSubsetId() {
        return gridSubsetId;
    }

    @Override
    public String toString() {
        return "Tileset{"
                + "tileMatrixSetURI='"
                + tileMatrixSetURI
                + '\''
                + ", tileMatrixSetDefinition='"
                + tileMatrixSetDefinition
                + '\''
                + ", dataType="
                + dataType
                + ", tileMatrixSetLimits="
                + tileMatrixSetLimits
                + '}';
    }

    protected final void addTilesLinkForFormat(
            String layerName, String baseURL, String format, String path, String rel) {
        String apiUrl =
                ResponseUtils.buildURL(
                        baseURL,
                        "ogc/tiles/collections/" + ResponseUtils.urlEncode(layerName) + path,
                        Collections.singletonMap("f", format),
                        URLMangler.URLType.SERVICE);
        Link link = new Link(apiUrl, rel, format, layerName + " tiles as " + format);
        link.setTemplated(true);
        addLink(link);
    }

    @JsonIgnore
    public String getStyleId() {
        return styleId;
    }
    
    @JsonIgnore
    public String getTileMatrixId() {
        return tileMatrixId;
    }
}
