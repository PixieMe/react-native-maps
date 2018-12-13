package com.airbnb.android.react.maps;

import android.content.Context;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class AirMapUrlTile extends AirMapFeature
{
	private static final int BUFFER_SIZE = 16 * 1024;

	class AIRMapUrlTileProvider implements TileProvider
	{
		private int width;
		private int height;
		private String urlTemplate;

		public AIRMapUrlTileProvider(
			int width,
			int height,
			String urlTemplate
		)
		{
			this.width = width;
			this.height = height;
			this.urlTemplate = urlTemplate;
		}

		@Override
		public final Tile getTile(
			int x,
			int y,
			int zoom
		)
		{
			URL tileUrl = getTileUrl(x, y, zoom);
			if (tileUrl == null)
			{
				return NO_TILE;
			}
			else
			{
				InputStream urlStream = null;
				ByteArrayOutputStream outputStream = null;
				try
				{
					urlStream = tileUrl.openStream();
					outputStream = new ByteArrayOutputStream();

					int nRead;
					byte[] buffer = new byte[BUFFER_SIZE];

					while ((nRead = urlStream.read(buffer)) != -1)
					{
						outputStream.write(buffer, 0, nRead);
					}

					return new Tile(this.width, this.height, outputStream.toByteArray());
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				finally
				{
					if (urlStream != null) try { urlStream.close(); } catch (Exception ignored) {}
					if (outputStream != null) try { outputStream.close(); } catch (Exception ignored) {}
				}

				return null;
			}
		}

		public URL getTileUrl(
			int x,
			int y,
			int zoom
		)
		{

			String s = this.urlTemplate.replace("{x}", Integer.toString(x))
			                           .replace("{y}", Integer.toString(y))
			                           .replace("{z}", Integer.toString(zoom));
			URL url = null;

			if (AirMapUrlTile.this.maximumZ > 0 && zoom > maximumZ)
			{
				return url;
			}

			if (AirMapUrlTile.this.minimumZ > 0 && zoom < minimumZ)
			{
				return url;
			}

			try
			{
				url = new URL(s);
			}
			catch (MalformedURLException e)
			{
				throw new AssertionError(e);
			}
			return url;
		}

		public void setUrlTemplate(String urlTemplate)
		{
			this.urlTemplate = urlTemplate;
		}
	}

	private TileOverlayOptions tileOverlayOptions;
	private TileOverlay tileOverlay;
	private AIRMapUrlTileProvider tileProvider;

	private String urlTemplate;
	private float zIndex;
	private float maximumZ;
	private float minimumZ;

	public AirMapUrlTile(Context context)
	{
		super(context);
	}

	public void setUrlTemplate(String urlTemplate)
	{
		this.urlTemplate = urlTemplate;
		if (tileProvider != null)
		{
			tileProvider.setUrlTemplate(urlTemplate);
		}
		if (tileOverlay != null)
		{
			tileOverlay.clearTileCache();
		}
	}

	public void setZIndex(float zIndex)
	{
		this.zIndex = zIndex;
		if (tileOverlay != null)
		{
			tileOverlay.setZIndex(zIndex);
		}
	}

	public void setMaximumZ(float maximumZ)
	{
		this.maximumZ = maximumZ;
		if (tileOverlay != null)
		{
			tileOverlay.clearTileCache();
		}
	}

	public void setMinimumZ(float minimumZ)
	{
		this.minimumZ = minimumZ;
		if (tileOverlay != null)
		{
			tileOverlay.clearTileCache();
		}
	}

	public TileOverlayOptions getTileOverlayOptions()
	{
		if (tileOverlayOptions == null)
		{
			tileOverlayOptions = createTileOverlayOptions();
		}
		return tileOverlayOptions;
	}

	private TileOverlayOptions createTileOverlayOptions()
	{
		TileOverlayOptions options = new TileOverlayOptions();
		options.zIndex(zIndex);
		this.tileProvider = new AIRMapUrlTileProvider(256, 256, this.urlTemplate);
		options.tileProvider(this.tileProvider);
		return options;
	}

	@Override
	public Object getFeature()
	{
		return tileOverlay;
	}

	@Override
	public void addToMap(GoogleMap map)
	{
		this.tileOverlay = map.addTileOverlay(getTileOverlayOptions());
	}

	@Override
	public void removeFromMap(GoogleMap map)
	{
		tileOverlay.remove();
	}
}
