package com.airbnb.android.react.maps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class AirMapLocalTile extends AirMapFeature {

    class AIRMapLocalTileProvider implements TileProvider {
        private int maxZoom;
        private static final int MEM_MAX_SIZE = 8;
        private static final int BUFFER_SIZE = 16 * 1024;
        private int tileSize;
        private String pathTemplate;


        public AIRMapLocalTileProvider(int tileSizet, String pathTemplate, int maxZoom) {
            this.tileSize = tileSizet;
            setPathTemplate(pathTemplate);
            this.maxZoom = maxZoom;
        }

        @Override
        public Tile getTile(int x, int y, int zoom) {
            byte[] image = readClosestTileImage(x, y, zoom);

            if (image == null) {
                return TileProvider.NO_TILE;
            }

            return new Tile(this.tileSize, this.tileSize, image);
        }

        public void setPathTemplate(String pathTemplate) {
            this.pathTemplate = getContext().getFilesDir() + "/" + pathTemplate;
        }

        public void setTileSize(int tileSize) {
            this.tileSize = tileSize;
        }

        /**
         * Reads a tile image for this zoom level if one exists, or the closest at a higher zoom level
         */
        private byte[] readClosestTileImage(int x, int y, int zoom) {
            int xCoord = x;
            int yCoord = y;
            int zCoord = zoom;

            InputStream in = null;
            ByteArrayOutputStream buffer = null;
            File file = new File(getTileFilename(xCoord, yCoord, zCoord));

            // Find the closest tile to this zoom
            while (!file.exists() && zCoord > maxZoom)
            {
              xCoord /= 2;
              yCoord /= 2;
              zCoord = zCoord - 1;
              file = new File(getTileFilename(xCoord, yCoord, zCoord));
            }

            // Finished searching without finding a tile file at any zoom level
            if (!file.exists()) {
                return null;
            }

            byte[] tileImage = readTileImage(xCoord, yCoord, zCoord);

            // If the loaded tile isn't for the zoom requested then rescale it
            if (zCoord != zoom) {
                Bitmap sourceBitmap = BitmapFactory.decodeByteArray(tileImage, 0, tileImage.length);
                Bitmap rescaledBitmap = getRescaledTileBitmap(sourceBitmap, x, y, zoom, zCoord);
                sourceBitmap.recycle();

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                rescaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                tileImage = stream.toByteArray();
                rescaledBitmap.recycle();
            }

            return tileImage;
        }

        private byte[] readTileImage(int x, int y, int zoom) {
            InputStream in = null;
            ByteArrayOutputStream buffer = null;
            File file = new File(getTileFilename(x, y, zoom));

            // Finished searching without finding a tile file at any zoom level
            if (!file.exists()) {
                return null;
            }

            try {
                in = new FileInputStream(file);
                buffer = new ByteArrayOutputStream();

                int nRead;
                byte[] data = new byte[BUFFER_SIZE];

                while ((nRead = in.read(data, 0, BUFFER_SIZE)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                return buffer.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                return null;
            } finally {
                if (in != null) try { in.close(); } catch (Exception ignored) {}
                if (buffer != null) try { buffer.close(); } catch (Exception ignored) {}
            }
        }

        private String getTileFilename(int x, int y, int zoom) {
            String s = this.pathTemplate
                    .replace("{x}", Integer.toString(x))
                    .replace("{y}", Integer.toString(y))
                    .replace("{z}", Integer.toString(zoom));
            return s;
        }

        private Bitmap getRescaledTileBitmap(Bitmap image, int targetX, int targetY, int targetZ, int sourceZ) {
            int zSteps = targetZ - sourceZ;
            int relation = (int) Math.pow(2, zSteps);
            int cropSize = (this.tileSize / relation);
            int cropX = (targetX % relation) * (this.tileSize / relation);
            int cropY = (targetY % relation) * (this.tileSize / relation);
            int scaleSize = (relation <= MEM_MAX_SIZE) ? tileSize * relation : tileSize * MEM_MAX_SIZE;
            Bitmap croppedBitmap = Bitmap.createBitmap(image, cropX, cropY, cropSize, cropSize);
            Bitmap scaled = Bitmap.createScaledBitmap(croppedBitmap, scaleSize, scaleSize, true);
            croppedBitmap.recycle();
            return scaled;
        }
    }

    private TileOverlayOptions tileOverlayOptions;
    private TileOverlay tileOverlay;
    private AirMapLocalTile.AIRMapLocalTileProvider tileProvider;

    private String pathTemplate;
    private float tileSize;
    private float zIndex;
    private int maxZoom;

    public AirMapLocalTile(Context context) {
        super(context);
    }

    public void setPathTemplate(String pathTemplate) {
        this.pathTemplate = pathTemplate;
        if (tileProvider != null) {
            tileProvider.setPathTemplate(pathTemplate);
        }
        if (tileOverlay != null) {
            tileOverlay.clearTileCache();
        }
    }

    public void setZIndex(float zIndex) {
        this.zIndex = zIndex;
        if (tileOverlay != null) {
            tileOverlay.setZIndex(zIndex);
        }
    }

    public void setMaxZoom(int maxZoom) {
        this.maxZoom = maxZoom;
    }

    public void setTileSize(float tileSize) {
        this.tileSize = tileSize;
        if (tileProvider != null) {
            tileProvider.setTileSize((int)tileSize);
        }
    }
    public TileOverlayOptions getTileOverlayOptions() {
        if (tileOverlayOptions == null) {
            tileOverlayOptions = createTileOverlayOptions();
        }
        return tileOverlayOptions;
    }

    private TileOverlayOptions createTileOverlayOptions() {
        TileOverlayOptions options = new TileOverlayOptions();
        options.zIndex(zIndex);
        this.tileProvider = new AirMapLocalTile.AIRMapLocalTileProvider((int)this.tileSize, this.pathTemplate, this.maxZoom);
        options.tileProvider(this.tileProvider);
        options.fadeIn(true);
        return options;
    }

    @Override
    public Object getFeature() {
        return tileOverlay;
    }

    @Override
    public void addToMap(GoogleMap map) {
        this.tileOverlay = map.addTileOverlay(getTileOverlayOptions());
    }

    @Override
    public void removeFromMap(GoogleMap map) {
        tileOverlay.remove();
    }
}
