/*
 * Framebuffer.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.util.vnc;

import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import poker.common.Rect;

/**
 * The Framebuffer maintains a single screen of pixels and is able to apply RFB
 * protocol messages to modify the screen upon recieving a framebuffer update
 * message from a VNC server.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class Framebuffer implements ImageProducer
{

	private boolean					fb_dbg_info		= false;
	
	private boolean					fb_dbg		= false;

	/** RFB Protocol adapter */
	private RfbProtocol				rfb;

	/** pixel data (we assume 8 bpp) */
	private Rect					rect;

	/** origin for pixel operations */
	private int						xorig		= 0, yorig = 0;

	/** color for fill operations */
	private int						color;

	/** color model */
	private ColorModel				colorModel;

	/** image consumer */
	private ImageConsumer			consumer	= null;

	/** listeners for framebuffer updates */
	private List<FbUpdateListener>	updListeners;

	/** left x coord of update bounds */
	private int						updateBoundsX;

	/** top y coord of update bounds */
	private int						updateBoundsY;

	/** width of update bounds */
	private int						updateBoundsW;

	/** height of update bounds */
	private int						updateBoundsH;

	/** whether to request VNC update */
	private boolean					doUpdate	= true;


	/**
	 * Constructor.
	 * 
	 * @param rfb
	 *            protocol adapter
	 */
	public Framebuffer(RfbProtocol rfb)
	{
		this.rfb = rfb;
		this.colorModel = new DirectColorModel(8, 7, (7 << 3), (3 << 6));
		this.updListeners = new LinkedList<FbUpdateListener>();
	}


	/**
	 * Initialize the framebuffer.
	 * 
	 * @param fbWidth
	 *            pixel width of frame buffer
	 * @param fbHeight
	 *            pixel height of frame buffer
	 */
	public void init(int fbWidth, int fbHeight)
	{
		this.rect = new Rect(fbWidth, fbHeight);
	}


	public String stamp()
	{
		return Long.toString(System.currentTimeMillis());
	}


	/**
	 * Perform a framebuffer update.
	 * 
	 * @throws IOException
	 */
	public void doUpdate() throws IOException
	{
		if (doUpdate)
		{
			rfb.readFramebufferUpdateMsg();

			if (fb_dbg_info)
				System.out.printf(
						"DBG: doUpdate(): head end; %d blocks (%s)\n",
						rfb.numRectUpdates, stamp());
			clearBounds();

			synchronized (rect)
			{
				for (int i = 0; i < rfb.numRectUpdates; i++)
				{
					if (fb_dbg)
						System.out.printf(
								"DBG: doUpdate(): begin read block %d (%s)\n",
								i + 1, stamp());
					rfb.readFbUpdRectHeaderMsg();
					if (fb_dbg)
						System.out
								.printf(
										"DBG: doUpdate(): end read block %d: %d x %d @ %d, %d (%s)\n",
										i + 1, rfb.rectW, rfb.rectH, rfb.rectX,
										rfb.rectY, stamp());
					updateBounds(rfb.rectX, rfb.rectY, rfb.rectW, rfb.rectH);

					switch (rfb.rectEncoding) {

					case RfbProtocol.EncodingRaw: {
						if (fb_dbg)
							System.out
									.printf(
											"DBG: doUpdate(): raw begin, block %d: %d x %d @ %d, %d (%s)\n",
											i + 1, rfb.rectW, rfb.rectH,
											rfb.rectX, rfb.rectY, stamp());
						readRawRect(rfb.rectX, rfb.rectY, rfb.rectW, rfb.rectH);
						if (fb_dbg)
							System.out
									.printf(
											"DBG: doUpdate(): raw end, block %d (%s)\n",
											i + 1, stamp());
						break;
					}

					case RfbProtocol.EncodingCopyRect: {
						if (fb_dbg)
							System.out
									.printf(
											"DBG: doUpdate(): copy begin, block %d: %d x %d @ %d, %d D %d, %d (%s)\n",
											i + 1, rfb.rectW, rfb.rectH,
											rfb.copyRectX, rfb.copyRectY,
											rfb.rectX - rfb.copyRectX,
											rfb.rectY - rfb.copyRectY, stamp());
						rfb.readCopyRectSrcMsg();
						copyRect(rfb.copyRectX, rfb.copyRectY, rfb.rectW,
								rfb.rectH, rfb.rectX - rfb.copyRectX, rfb.rectY
										- rfb.copyRectY);
						if (fb_dbg)
							System.out
									.printf(
											"DBG: doUpdate(): copy end, block %d (%s)\n",
											i + 1, stamp());
						break;
					}

					case RfbProtocol.EncodingRRE: {
						if (fb_dbg)
							System.out
									.printf(
											"DBG: doUpdate(): rre begin, block %d (%s)\n",
											i + 1, stamp());
						int numSubrects = rfb.readInt();
						int bg = rfb.readByte();
						int pix, x, y, w, h;
						translate(rfb.rectX, rfb.rectY);
						color = bg;
						fillRect(0, 0, rfb.rectW, rfb.rectH);
						for (int j = 0; j < numSubrects; j++)
						{
							pix = rfb.readByte();
							x = rfb.readUnsignedShort();
							y = rfb.readUnsignedShort();
							w = rfb.readUnsignedShort();
							h = rfb.readUnsignedShort();
							color = pix;
							fillRect(x, y, w, h);
						}
						translate(-rfb.rectX, -rfb.rectY);
						if (fb_dbg)
							System.out
									.printf(
											"DBG: doUpdate(): rre end, block %d (%s)\n",
											i + 1, stamp());
						break;
					}

					case RfbProtocol.EncodingCoRRE: {
						if (fb_dbg)
							System.out
									.printf(
											"DBG: doUpdate(): corre begin, block %d (%s)\n",
											i + 1, stamp());
						int numSubrects = rfb.readInt();
						int bg = rfb.readByte();
						int pix, x, y, w, h;
						translate(rfb.rectX, rfb.rectY);
						color = bg;
						fillRect(0, 0, rfb.rectW, rfb.rectH);
						for (int j = 0; j < numSubrects; j++)
						{
							pix = rfb.readByte();
							x = rfb.readByte();
							y = rfb.readByte();
							w = rfb.readByte();
							h = rfb.readByte();
							color = pix;
							fillRect(x, y, w, h);
						}
						translate(-rfb.rectX, -rfb.rectY);
						if (fb_dbg)
							System.out
									.printf(
											"DBG: doUpdate(): corre end, block %d (%s)\n",
											i + 1, stamp());
						break;
					}

					case RfbProtocol.EncodingHextile: {
						if (fb_dbg)
							System.out
									.printf(
											"DBG: doUpdate(): hextile begin, block %d: %d x %d @ %d, %d (%s)\n",
											i + 1, rfb.rectW, rfb.rectH,
											rfb.rectX, rfb.rectY, stamp());

						int bg = 0, fg = 0, sx, sy, sw, sh;

						int n = 0;

						// LOOP TY OVER Y AREA BY 16
						for (int ty = rfb.rectY; ty < rfb.rectY + rfb.rectH; ty += 16)
						{
							// LOOP TX OVER X AREA BY 16
							for (int tx = rfb.rectX; tx < rfb.rectX + rfb.rectW; tx += 16)
							{
								int tw = 16, th = 16;

								// GET TILE WIDTH/HEIGHT
								if (rfb.rectX + rfb.rectW - tx < 16)
									tw = rfb.rectX + rfb.rectW - tx;
								if (rfb.rectY + rfb.rectH - ty < 16)
									th = rfb.rectY + rfb.rectH - ty;

								// GET SUBENCODING
								int subenc = rfb.readByte();

								// IF RAW ENCODING, DO RAW IN TILE
								if ((subenc & RfbProtocol.HextileRaw) != 0)
								{
									readRawRect(tx, ty, tw, th);
									continue;
								}

								// IF BG SPEC, READ IT
								if ((subenc & RfbProtocol.HextileBackgroundSpecified) != 0)
									bg = rfb.readByte();

								// SET BG COLOR (0 DEFAULT) AND FILL TILE
								color = bg;
								fillRect(tx, ty, tw, th);

								// IF FG SPEC, READ IT
								if ((subenc & RfbProtocol.HextileForegroundSpecified) != 0)
									fg = rfb.readByte();

								// IF NO SUBRECTS, GO TO NEXT TILE
								if ((subenc & RfbProtocol.HextileAnySubrects) == 0)
									continue;

								// ELSE, READ NUM SUBRECTS
								int numSubrects = rfb.readByte();

								n += numSubrects;

								// GO TO TILE TOP,LEFT
								translate(tx, ty);

								// IF COLORED SUBRECTS
								if ((subenc & RfbProtocol.HextileSubrectsColoured) != 0)
								{
									// FOR EACH SUBRECT
									for (int j = 0; j < numSubrects; j++)
									{
										// READ FB (BYTE), X, Y, W, H (NIBS)
										fg = rfb.readByte();
										int b1 = rfb.readByte();
										int b2 = rfb.readByte();
										sx = b1 >> 4;
										sy = b1 & 0xf;
										sw = (b2 >> 4) + 1;
										sh = (b2 & 0xf) + 1;

										// COLOR SUBRECT
										color = fg;
										fillRect(sx, sy, sw, sh);
									}
								}
								else
								{
									// IF NOT COLORED SUBRECTS, SET LAST KNOWN
									// FG
									// COLOR
									color = fg;

									// FOR EACH SUBRECT
									for (int j = 0; j < numSubrects; j++)
									{
										// READ X, Y, W, H (NIBS)
										int b1 = rfb.readByte();
										int b2 = rfb.readByte();
										sx = b1 >> 4;
										sy = b1 & 0xf;
										sw = (b2 >> 4) + 1;
										sh = (b2 & 0xf) + 1;

										// FILL SUBRECT
										fillRect(sx, sy, sw, sh);
									}
								}

								// UNTRANSLATE FROM TILE
								translate(-tx, -ty);
							}
						}
						if (fb_dbg)
							System.out
									.printf(
											"DBG: doUpdate(): hextile end (n=%d), block %d (%s)\n",
											n, i + 1, stamp());

						break;
					}

					default:
						throw new IOException(rfb.getHost() + ":"
								+ rfb.getPort()
								+ " - unknown rectangle encoding "
								+ rfb.rectEncoding);
					}

					if (fb_dbg)
						System.out.printf(
								"DBG: doUpdate(): end block %d (%s)\n", i + 1,
								stamp());
				}

				for (FbUpdateListener fbul : updListeners)
					fbul.updated(updateBoundsX, updateBoundsY, updateBoundsW,
							updateBoundsH);

			}
		}
	}


	private void clearBounds()
	{
		updateBoundsX = -1;
		updateBoundsY = -1;
		updateBoundsW = -1;
		updateBoundsH = -1;
	}


	private void updateBounds(int x, int y, int w, int h)
	{
		if (updateBoundsX == -1 || x < updateBoundsX)
			updateBoundsX = x;
		if (updateBoundsY == -1 || y < updateBoundsY)
			updateBoundsY = y;
		int x2 = x + w;
		int y2 = y + h;
		if (updateBoundsW == -1 || x2 > (updateBoundsX + updateBoundsW))
			updateBoundsW = x2 - updateBoundsX;
		if (updateBoundsH == -1 || y2 > (updateBoundsY + updateBoundsH))
			updateBoundsH = y2 - updateBoundsY;
	}


	private void readRawRect(int x, int y, int w, int h) throws IOException
	{
		if (!(new Rect(w, h, rfb.getInputStream())).drawOnto(rect, x, y, false))
			throw new IOException("readRawRect(): out of bounds");
		newPixels(x, y, w, h);
	}


	/**
	 * Translate the pixel operation origin.
	 * 
	 * @param dx
	 *            x distance
	 * @param dy
	 *            y distance
	 */
	private void translate(int dx, int dy)
	{
		xorig += dx;
		yorig += dy;
	}


	/**
	 * Fill a rectangle with the current color.
	 * 
	 * @param x
	 *            left position of rectangle
	 * @param y
	 *            top position of rectangle
	 * @param w
	 *            width of rectangle
	 * @param h
	 *            height of rectangle
	 */
	private void fillRect(int x, int y, int w, int h)
	{
		x += xorig;
		y += yorig;

		rect.sub(x, y, w, h, false).fill((byte) color);
		newPixels(x, y, w, h);
	}


	/**
	 * Copy a rectangle of pixels onscreen.
	 * 
	 * @param x
	 *            origin x position
	 * @param y
	 *            origin y position
	 * @param w
	 *            width of rectangle
	 * @param h
	 *            height of rectangle
	 * @param dx
	 *            x distance
	 * @param dy
	 *            y distance
	 */
	private void copyRect(int x, int y, int w, int h, int dx, int dy)
	{
		Rect r1 = rect.sub(x, y, w, h, false);
		Rect r2 = rect.sub(x + dx, y + dy, w, h, false);
		r1.copyInto(r2);
		newPixels(x + dx, y + dy, w, h);
	}


	/**
	 * New pixels arrived in the given rectangle. Inform the image consumer.
	 * 
	 * @param x
	 *            left position
	 * @param y
	 *            top position
	 * @param w
	 *            width
	 * @param h
	 *            height
	 */
	public void newPixels(int x, int y, int w, int h)
	{
		if (consumer != null)
		{
			consumer.setPixels(x, y, w, h, colorModel, rect.getPixels(),
					getWidth() * y + x, getWidth());
			consumer.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
		}
	}


	/**
	 * @return height of framebuffer
	 */
	public int getHeight()
	{
		return rfb.fbHeight;
	}


	/**
	 * @return width of framebuffer
	 */
	public int getWidth()
	{
		return rfb.fbWidth;
	}


	/**
	 * @see java.awt.image.ImageProducer#addConsumer(java.awt.image.ImageConsumer)
	 */
	public void addConsumer(ImageConsumer ic)
	{
		if (consumer == ic)
			return;

		if (consumer != null)
			consumer.imageComplete(ImageConsumer.IMAGEERROR);

		consumer = ic;
		ic.setDimensions(getWidth(), getHeight());
		ic.setColorModel(colorModel);
		ic.setHints(ImageConsumer.RANDOMPIXELORDER);
		ic.setPixels(0, 0, getWidth(), getHeight(), colorModel, rect
				.getPixels(), 0, getWidth());
		ic.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
	}


	/**
	 * @see java.awt.image.ImageProducer#isConsumer(java.awt.image.ImageConsumer)
	 */
	public boolean isConsumer(ImageConsumer ic)
	{
		return (consumer == ic);
	}


	/**
	 * @see java.awt.image.ImageProducer#removeConsumer(java.awt.image.ImageConsumer)
	 */
	public void removeConsumer(ImageConsumer ic)
	{
		if (consumer == ic)
			consumer = null;
	}


	/**
	 * @see java.awt.image.ImageProducer#startProduction(java.awt.image.ImageConsumer)
	 */
	public void startProduction(ImageConsumer ic)
	{
		addConsumer(ic);
	}


	/**
	 * @see java.awt.image.ImageProducer#requestTopDownLeftRightResend(java.awt.image.ImageConsumer)
	 */
	public void requestTopDownLeftRightResend(ImageConsumer ic)
	{
	}


	public void addUpdateListener(FbUpdateListener fbul)
	{
		if (fbul != null)
			updListeners.add(fbul);
	}


	public void removeUpdateListener(FbUpdateListener fbul)
	{
		if (fbul != null)
			updListeners.remove(fbul);
	}


	public byte getPixel(int x, int y)
	{
		return rect.getPixel(x, y);
	}


	public void DBG_toggleUpdate()
	{
		doUpdate = !doUpdate;
		System.out.println("update is now " + (doUpdate ? "on" : "off"));
	}


	public void write(String fname) throws IOException
	{
		rect.write(fname);
	}


	public Rect getRect()
	{
		return this.rect;
	}


	public void getUpdateBounds(int[] b)
	{
		b[0] = updateBoundsX;
		b[1] = updateBoundsY;
		b[2] = updateBoundsW;
		b[3] = updateBoundsH;
	}
}
