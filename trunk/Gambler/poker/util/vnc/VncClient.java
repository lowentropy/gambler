/*
 * VncClient.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.util.vnc;

import java.awt.event.KeyEvent;
import java.io.IOException;

import poker.common.DesCipher;

/**
 * A VncClient is a wrapper around an active VNC connection which allows two
 * modes: automatic and manual. In automatic mode, the screen is continually
 * updated by polling the server. In Manual mode, updates must be requeseted and
 * manually recieved.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class VncClient
{

	/** supported encodings */
	private static int[] encodings = { RfbProtocol.EncodingCopyRect,
			RfbProtocol.EncodingCoRRE, RfbProtocol.EncodingHextile,
			RfbProtocol.EncodingRaw, RfbProtocol.EncodingRRE };

	/** interactive mode; process clicks */
	public static final int INTERACTIVE = 1;

	/** debug mode; alternative input method */
	public static final int DEBUG = 2;

	/** automatic update mode */
	public static final int AUTOMATIC = 1;

	/** manual update mode */
	public static final int MANUAL = 2;

	/** whether mouse events are sent directly to VNC */
	public int inputMode = INTERACTIVE;

	/** which update mode client is in */
	public int updateMode = AUTOMATIC;

	/** whether automatic updater is running */
	private boolean running = false;

	/** hostname of server */
	private String host;

	/** server port */
	private int port;

	/** password for authentication */
	private String pass;

	/** protocol class */
	private RfbProtocol rfb;

	/** framebuffer */
	private Framebuffer fb;

	/** updater thread */
	private VncThread updThread;

	/** error which stopped thread */
	private IOException stopErr;

	/** whether waiting for first screen update */
	private boolean firstUpdate = true;

	/** whether handshake protocol is complete */
	private boolean handshakeDone;

	/** mouse button states */
	private boolean[] mouseBtns = new boolean[8];

	/** counter for screenshots */
	private int imgCount = 1;

	/** pixel buffer, for debug mode */
	private byte[] pbuf = null;

	/** buffer width and height */
	private int bufW, bufH;

	/** current foreground color */
	private byte fgColor = (byte) 0;

	/** array used for debugging */
	private int[] rectX1 = new int[3];

	/** array used for debugging */
	private int[] rectY1 = new int[3];

	/** array used for debugging */
	private int[] rectX2 = new int[3];

	/** array used for debugging */
	private int[] rectY2 = new int[3];

	/** rectangle outline colors */
	private byte[] rectC = new byte[] { (byte) 0x7, (byte) 0, (byte) 0xc0 };

	/** whether to produce debugging output */
	private boolean debug = true;

	/**
	 * Constructor.
	 * 
	 * @param host
	 *            hostname
	 * @param port
	 *            port
	 * @param pass
	 *            password
	 */
	public VncClient()
	{
		this.rfb = new RfbProtocol();
		this.fb = new Framebuffer(this.rfb);
	}

	/**
	 * Start connection with RFB server.
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("deprecation")
	public void start() throws IOException
	{
		if (running)
			stop();

		boolean auth = false;
		handshakeDone = false;

		// infinite login retries
		while (!auth)
		{
			// open connection and get version
			rfb.open(host, port);
			rfb.readProtocolVersionMsg();
			dbg("Server version: " + rfb.serverMajor + "." + rfb.serverMinor);

			// respond with version, wait for auth request
			rfb.writeProtocolVersionMsg(3, 3);
			rfb.readAuthenticationMsg();

			// authenticate
			switch (rfb.authScheme)
			{

			// no auth: done
			case RfbProtocol.NoAuth:
				auth = true;
				break;

			// basic auth: send password key
			case RfbProtocol.VNCAuth:

				// read challenge
				rfb.readVNCChallengeMsg();
				byte[] key = new byte[8];
				pass.getBytes(0, pass.length(), key, 0);
				for (int i = pass.length(); i < 8; i++)
					key[i] = (byte) 0;

				// cipher with DES
				DesCipher des = new DesCipher(key);
				des.encrypt(rfb.challenge, 0, rfb.challenge, 0);
				des.encrypt(rfb.challenge, 8, rfb.challenge, 8);

				// respond to challenge and wait for response
				rfb.writeVNCChallengeResponseMsg(rfb.challenge);
				rfb.readAuthenticationResponseMsg();

				switch (rfb.authStatus)
				{

				// auth okay: done
				case RfbProtocol.AuthOK:
					auth = true;
					break;

				// tossed off server: error
				case RfbProtocol.AuthTooMany:
					throw new IOException("too many failed authorizations");

				// failed: retry
				case RfbProtocol.AuthFailed:
					break;

				// invalid protocol
				default:
					throw new IOException(
							"unknown authorization response code: "
									+ rfb.authStatus);
				}
				break;

			// unknown auth scheme: error
			default:
				throw new IOException("unknown authorization scheme: "
						+ rfb.authScheme);
			}

			// auth is done so break
			if (auth)
				break;

			// close connection before retrying
			rfb.close();
		}

		// initialize connection parameters
		rfb.writeClientInitMsg(false);
		rfb.readServerInitMsg();
		dbg("Desktop name is " + rfb.name);
		dbg("Desktop size is " + rfb.fbWidth + " x " + rfb.fbHeight);

		// initialize buffer and pixel format (fixed 8bpp)
		fb.init(rfb.fbWidth, rfb.fbHeight);
		rfb.writeSetEncodingsMsg(encodings);
		
		// rfb.writeSetPixelFormatMsg(8, 8, false, true, 7, 7, 3, 0, 3, 6);

		// mark status
		handshakeDone = true;
		stopErr = null;
		running = true;
		updThread = new VncThread();

		// if auto-update enabled, start thread
		if (updateMode == AUTOMATIC)
			updThread.start();
	}

	/**
	 * Stop updates and close connection.
	 * 
	 * @throws IOException
	 */
	public void stop() throws IOException
	{
		if (!running)
			return;

		running = false;
		dbg("Stopping...");

		updThread.stop = true;
		while (!updThread.done)
			Thread.yield();

		dbg(" done.");

		rfb.close();
		if (stopErr != null)
			throw stopErr;
	}

	public Framebuffer getFramebuffer()
	{
		return fb;
	}

	/**
	 * Error occurred in thread. Store it and re-throw it when the connection is
	 * closed.
	 * 
	 * @param e
	 *            error
	 */
	private void threadError(IOException e)
	{
		stopErr = e;
	}

	/**
	 * Send an update request to the server for the whole screen.
	 * 
	 * @throws IOException
	 */
	public void requestUpdate() throws IOException
	{
		requestUpdate(0, 0, rfb.fbWidth, rfb.fbHeight);
	}

	/**
	 * Send an update request to the server.
	 * 
	 * @param x
	 *            left bound of update rectangle
	 * @param y
	 *            top bound of update rectangle
	 * @param w
	 *            width of update rectangle
	 * @param h
	 *            height of update rectangle
	 * @throws IOException
	 */
	public void requestUpdate(int x, int y, int w, int h) throws IOException
	{
		rfb.writeFramebufferUpdateRequestMsg(x, y, w, h, !firstUpdate);
		firstUpdate = false;
	}

	/**
	 * Wait for and process messages until an update message is recieved.
	 * Process it, then return.
	 * 
	 * @throws IOException
	 */
	public void recieveUpdate() throws IOException
	{
		// System.out.printf("DBG: recieveUpdate(): start (%s)\n", stamp());
		while (readMessage() != RfbProtocol.FrameBufferUpdate)
			;
		// System.out.printf("DBG: recieveUpdate(): end (%s)\n", stamp());
	}
	
	public boolean tryUpdate() throws IOException
	{
		if (!rfb.messagesWaiting())
		{
//			System.out.println("try: none waiting");
			return false;
		}
		else
		{
			boolean b = (readMessage() == RfbProtocol.FrameBufferUpdate);
			return b;
		}
	}

	public String stamp()
	{
		return Long.toString(System.currentTimeMillis());
	}

	
	/**
	 * Perform single refresh of all pixels; private mode.
	 * 
	 * @throws IOException
	 */
	private void updateFramebuffer() throws IOException
	{
		if (inputMode == INTERACTIVE)
		{
			rfb.writeFramebufferUpdateRequestMsg(0, 0, rfb.fbWidth,
					rfb.fbHeight, !firstUpdate);
			firstUpdate = false;
		}
		else
			firstUpdate = true;

		while (readMessage() != RfbProtocol.FrameBufferUpdate)
			;
	}

	/**
	 * Wait for, read, and process a message from the server.
	 * 
	 * @return message type which was processed
	 * @throws IOException
	 */
	private int readMessage() throws IOException
	{
//		System.out.printf("DBG: readMessage(): start (%s)\n", stamp());
		
		int msgType = rfb.readServerMessageType();
		
//		System.out.printf("DBG: readMessage(): data begin (%s)\n", stamp());
		
		switch (msgType)
		{
		case RfbProtocol.FrameBufferUpdate:
			fb.doUpdate();
			break;
		case RfbProtocol.ServerCutText:
			rfb.readServerCutTextMsg();
			dbg("New cut text:\n----------\n%s\n----------", rfb.cutText);
			break;
		case RfbProtocol.Bell:
			dbg("!!!BELL!!!");
			break;
		default:
			dbg("Unhandled message type: " + msgType);
		}

//		System.out.printf("DBG: readMessage(): end (%s)\n", stamp());

		return msgType;
	}

	/**
	 * The mouse moved; send a message.
	 * 
	 * @param x
	 *            new x location
	 * @param y
	 *            new y location
	 * @throws IOException
	 */
	public void mouseMoved(int x, int y) throws IOException
	{
		if (running)
			rfb.writePointerEventMsg(mouseBtns, x, y);
	}

	/**
	 * The mouse button went down; send a message.
	 * 
	 * @param btn
	 *            button which is down
	 * @param x
	 *            x location
	 * @param y
	 *            y location
	 * @throws IOException
	 */
	public void mouseDown(int btn, int x, int y) throws IOException
	{
		mouseBtns[btn] = false;
		if (running)
			mouseMoved(x, y);
		try
		{
			Thread.sleep(500);
		}
		catch (InterruptedException e)
		{
		}
		mouseBtns[btn] = true;
		if (running)
			mouseMoved(x, y);
	}

	/**
	 * The mouse button went up; send a message.
	 * 
	 * @param btn
	 *            button which is up
	 * @param x
	 *            x location
	 * @param y
	 *            y location
	 * @throws IOException
	 */
	public void mouseUp(int btn, int x, int y) throws IOException
	{
		mouseBtns[btn] = false;
		if (running)
			mouseMoved(x, y);
	}

	/**
	 * The mouse button was clicked; send a message.
	 * 
	 * @param btn
	 *            button which was clicked
	 * @param x
	 *            x location
	 * @param y
	 *            y location
	 * @throws IOException
	 */
	public void mouseClick(int btn, int x, int y) throws IOException
	{
		mouseDown(btn, x, y);
		mouseUp(btn, x, y);
	}
	

	public void mouseClickDeep(int b, int x, int y) throws IOException
	{
		mouseDown(b, x, y);
		try
		{
			Thread.sleep(500);
		}
		catch (InterruptedException e)
		{
		}
		mouseUp(b, x, y);
	}

	/**
	 * A key event occured; send a message.
	 * 
	 * @param e
	 *            event
	 * @throws IOException
	 */
	public void keyEvent(KeyEvent e) throws IOException
	{
		if (running)
			this.rfb.writeKeyEventMsg(e);
	}

	/**
	 * A key was typed; send a message.
	 * 
	 * @param c
	 *            character which was typed
	 * @throws IOException 
	 */
	public void keyType(char c) throws IOException
	{
		if (running)
			this.rfb.typeChar(c);
	}

	/**
	 * VNC Thread continually update framebuffer.
	 * 
	 * @author Nathan Matthews <lowentropy@gmail.com>
	 */
	private class VncThread extends Thread
	{

		public boolean stop;

		public boolean done;

		public VncThread()
		{
			super();
			stop = false;
			done = true;
		}

		/** @see java.lang.Runnable#run() */
		public void run()
		{
			System.out.println("Running automatic VNC update.");
			done = false;
			try
			{
				while (!stop)
					updateFramebuffer();
			} catch (IOException e)
			{
				done = true;
				stop = true;
				threadError(e);
			}
			done = true;
		}
	}

	/**
	 * @return whether handshake is done
	 */
	public boolean handshakeDone()
	{
		return handshakeDone;
	}

	/**
	 * Alternate mouse click behavior
	 * 
	 * @param btn
	 *            button which was clicked
	 * @param x
	 *            x location
	 * @param y
	 *            y location
	 */
	public void altMouseClick(int btn, int x, int y)
	{
		if (btn == 0)
		{
			fgColor = fb.getPixel(x, y);
		}
		else if (btn == 2)
		{
			// if (pbuf != null)
			// fb.setRect(x, y, bufW, bufH, pbuf);
		}
	}

	/**
	 * Alternate mouse-down behavior.
	 * 
	 * @param btn
	 *            button which is now down
	 * @param x
	 *            x location
	 * @param y
	 *            y location
	 */
	public void altMouseDown(int btn, int x, int y)
	{
		rectX1[btn] = x;
		rectY1[btn] = y;
		dbg("%d down @ (%d, %d)", btn, x, y);
	}

	/**
	 * Alternate mouse-up behavior.
	 * 
	 * @param btn
	 *            button which is now up
	 * @param x
	 *            x location
	 * @param y
	 *            y location
	 */
	public void altMouseUp(int btn, int x, int y)
	{
		rectX2[btn] = x;
		rectY2[btn] = y;
		dbg("%d up   @ (%d, %d)", btn, x, y);
		if (rectX1[btn] != x || rectY1[btn] != y)
		{
			// dbg(" rect");
			// int x1 = (rectX1[btn] < x) ? rectX1[btn] : x;
			// int y1 = (rectY1[btn] < y) ? rectY1[btn] : y;
			// int x2 = (rectX1[btn] > x) ? rectX1[btn] : x;
			// int y2 = (rectY1[btn] > y) ? rectY1[btn] : y;
			// fb.drawOutline(rectC[btn], x1 - 1, y1 - 1, x2 + 1, y2 + 1);
		}
	}

	/**
	 * Set the image count for screenshot names.
	 * 
	 * @param cnt
	 *            screen image count
	 */
	public void setImageCount(int cnt)
	{
		imgCount = cnt;
	}

	/**
	 * Save a current snapshot.
	 * 
	 * @throws IOException
	 */
	public void saveImage() throws IOException
	{
		String fname = "screenshot-" + (imgCount++) + ".img";
		dbg(fname + " saved.");
		fb.write(fname);

	}

	/**
	 * Connect to the given host with params.
	 * 
	 * @param host
	 *            hostname
	 * @param port
	 *            port on host
	 * @param pass
	 *            password to use
	 * @throws IOException
	 */
	public void connect(String host, int port, char[] pass) throws IOException
	{
		if (running)
			stop();
		this.host = host;
		this.port = port;
		this.pass = (pass.length < 8) ? new String(pass) : new String(pass, 0,
				8);
		start();
	}

	/**
	 * Produce debugging output.
	 * 
	 * @param s
	 *            format string to print
	 * @param args
	 *            arguments to format string
	 */
	private void dbg(String s, Object... args)
	{
		if (debug)
			System.out.printf(s + "\n", args);
	}
	
	public void getUpdateBounds(int[] b)
	{
		fb.getUpdateBounds(b);
	}

	public int getPort()
	{
		return this.port;
	}

	public void clearButtons()
	{
		for (int i = 0; i < mouseBtns.length; i++)
			mouseBtns[i] = false;
	}

}
