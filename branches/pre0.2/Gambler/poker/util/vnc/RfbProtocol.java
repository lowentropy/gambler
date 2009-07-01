/*
 * RfbProtocol.java
 * 
 * Copyright (C) 2005 Nathan Matthews <lowentropy@gmail.com>
 * All rights reserved.
 */

package poker.util.vnc;

import java.awt.Event;
import java.awt.event.KeyEvent;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;


/**
 * Sends and recieves Remote FrameBuffer (RFB) protocol messages from a server
 * connected on a socket.
 * 
 * @author Nathan Matthews <lowentropy@gmail.com>
 */
public class RfbProtocol
{

	/** server message types */
	public static final int		FrameBufferUpdate	= 0,
			SetColorMapEntries = 1, Bell = 2, ServerCutText = 3;

	/** client message types */
	public static final int		SetPixelFormat		= 0,
			FixColorMapEntries = 1, SetEncodings = 2,
			FramebufferUpdateRequest = 3, KeyEvent = 4, PointerEvent = 5,
			ClientCutText = 6;

	/** RFB-defined encoding types */
	public static final int		EncodingRaw			= 0, EncodingCopyRect = 1,
			EncodingRRE = 2, EncodingCoRRE = 4, EncodingHextile = 5;

	/** Hextile flags */
	public static final int		HextileRaw			= (1 << 0),
			HextileBackgroundSpecified = (1 << 1),
			HextileForegroundSpecified = (1 << 2),
			HextileAnySubrects = (1 << 3), HextileSubrectsColoured = (1 << 4);

	/** RFB-defined keycodes for less common ASCII keys */
	public static final int		KEY_BackSpace		= 0xff08, KEY_Tab = 0xff09,
			KEY_Enter = 0xff0d, KEY_Escape = 0xff1b, KEY_Insert = 0xff63,
			KEY_Delete = 0xffff, KEY_Home = 0xff50, KEY_End = 0xff57,
			KEY_PageUp = 0xff55, KEY_PageDown = 0xff56, KEY_Left = 0xff51,
			KEY_Up = 0xff52, KEY_Right = 0xff53, KEY_Down = 0xff54,
			KEY_F1 = 0xffbe, KEY_F2 = 0xffbf, KEY_F3 = 0xffc0, KEY_F4 = 0xffc1,
			KEY_F5 = 0xffc2, KEY_F6 = 0xffc3, KEY_F7 = 0xffc4, KEY_F8 = 0xffc5,
			KEY_F9 = 0xffc6, KEY_F10 = 0xffc7, KEY_F11 = 0xffc8,
			KEY_F12 = 0xffc9, KEY_LeftShift = 0xffe1, KEY_RightShift = 0xffe2,
			KEY_LeftControl = 0xffe3, KEY_RightControl = 0xffe4,
			KEY_LeftMeta = 0xffe7, KEY_RightMeta = 0xffe8,
			KEY_LeftAlt = 0xffe9, KEY_RightAlt = 0xffea;

	/** size of read-buffer for network socket */
	private static final int	BUF_SIZE			= 16384;

	/** hostname to connect to */
	private String				host;

	/** port on which vnc server is running */
	private int					port;

	/** socket over which communications are made */
	private Socket				sock;

	/** stream to read incoming server messages from */
	private DataInputStream		is;

	/** stream to write outgoing client messages to */
	private OutputStream		os;

	/** whether connection is open */
	private boolean				open;


	/**
	 * Constructor.
	 * 
	 * @param host
	 *            hostname
	 * @param port
	 *            port number
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	public RfbProtocol()
	{
		this.open = false;
	}


	public void open(String host, int port) throws IOException
	{
		if (open)
			throw new IOException("protocol already connected - " + this.host
					+ ":" + this.port);
		this.open = true;
		this.host = host;
		this.port = port;

		this.sock = new Socket(host, port);
		this.is = new DataInputStream(new BufferedInputStream(
				sock.getInputStream(), BUF_SIZE));
		this.os = sock.getOutputStream();
	}


	/**
	 * Close the connection to the server.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException
	{
		sock.close();
		this.open = false;
	}


	/** major version number reported by server */
	public int	serverMajor;

	/** minor version number reported by server */
	public int	serverMinor;


	/**
	 * Reads server message of type ProtocolVersion.
	 * 
	 * @throws IOException
	 */
	public void readProtocolVersionMsg() throws IOException
	{
		byte[] b = new byte[12];
		is.readFully(b);

		if ((b[0] != 'R') || (b[1] != 'F') || (b[2] != 'B') || (b[3] != ' ')
				|| (b[4] < '0') || (b[4] > '9') || (b[5] < '0') || (b[5] > '9')
				|| (b[6] < '0') || (b[6] > '9') || (b[7] != '.')
				|| (b[8] < '0') || (b[8] > '9') || (b[9] < '0') || (b[9] > '9')
				|| (b[10] < '0') || (b[10] > '9') || (b[11] != '\n'))
		{
			throw new IOException(host + ":" + port
					+ " is not a valid RFB server");
		}

		serverMajor = (b[4] - '0') * 100 + (b[5] - '0') * 10 + (b[6] - '0');
		serverMinor = (b[8] - '0') * 100 + (b[9] - '0') * 10 + (b[10] - '0');
	}


	/**
	 * Writes client message of type ProtocolVersion.
	 * 
	 * @param major
	 *            client major version number
	 * @param minor
	 *            client minor version number
	 * @throws IOException
	 */
	public void writeProtocolVersionMsg(int major, int minor)
			throws IOException
	{
		byte[] b = new byte[12];
		b[0] = (byte) 'R';
		b[1] = (byte) 'F';
		b[2] = (byte) 'B';
		b[3] = (byte) ' ';
		b[4] = (byte) '0';
		b[5] = (byte) '0';
		b[6] = (byte) ((int) '0' + major);
		b[7] = (byte) '.';
		b[8] = (byte) '0';
		b[9] = (byte) '0';
		b[10] = (byte) ((int) '0' + minor);
		b[11] = (byte) '\n';
		os.write(b);
	}


	/** possible reported authentication schemes */
	public static final int	ConnFailed	= 0, NoAuth = 1, VNCAuth = 2;

	/** authentication scheme used by server */
	public int				authScheme;


	/**
	 * Reads a server message of type Authentication.
	 * 
	 * @throws IOException
	 */
	public void readAuthenticationMsg() throws IOException
	{
		int scheme = is.readInt();

		switch (scheme) {

		case ConnFailed:
			throw new IOException(readString());

		case NoAuth:
		case VNCAuth:
			authScheme = scheme;
			break;

		default:
			throw new IOException(host + ":" + port
					+ " reported unknown authentication scheme: " + authScheme);
		}
	}


	/** challenge value sent by server */
	public byte[]	challenge;


	/**
	 * Reads a server message of type VNC Challenge.
	 * 
	 * @throws IOException
	 */
	public void readVNCChallengeMsg() throws IOException
	{
		challenge = new byte[16];
		is.readFully(challenge);
	}


	/**
	 * Writes a client message of type VNC Challenge Response.
	 * 
	 * @param response
	 *            16-byte response to challenge
	 * @throws IOException
	 */
	public void writeVNCChallengeResponseMsg(byte[] response)
			throws IOException
	{
		os.write(response);
	}


	/** possible reponses to authentication attempt */
	public static final int	AuthOK	= 0, AuthFailed = 1, AuthTooMany = 2;

	/** authentication status response from server */
	public int				authStatus;


	/**
	 * Reads a server message of the type Authentication Response.
	 * 
	 * @throws IOException
	 */
	public void readAuthenticationResponseMsg() throws IOException
	{
		authStatus = is.readInt();
	}


	/**
	 * Writes a client message of the type Client Initialisation.
	 * 
	 * @param shared
	 *            whether to share desktop with other users, or boot them
	 * @throws IOException
	 */
	public void writeClientInitMsg(boolean shared) throws IOException
	{
		os.write((byte) (shared ? 1 : 0));
	}


	/** width of framebuffer */
	public int		fbWidth;

	/** height of framebuffer */
	public int		fbHeight;

	/** bits-per-pixel */
	public int		bpp;

	/** color depth (bits) */
	public int		depth;

	/** whether multi-byte values are big-endian */
	public boolean	bigEndian;

	/** whether true-color is used (as opposed to color-map) */
	public boolean	trueColor;

	/** maximum red pixel value */
	public int		rMax;

	/** maximum gren pixel value */
	public int		gMax;

	/** maximum blue pixel value */
	public int		bMax;

	/** number of right-shifts to obtain red value in pixel */
	public int		rShift;

	/** number of right-shifts to obtain green value in pixel */
	public int		gShift;

	/** number of right-shifts to obtain blue value in pixel */
	public int		bShift;

	/** name of connected desktop */
	public String	name;


	/**
	 * Reads server message of the type Server Initialisation.
	 * 
	 * @throws IOException
	 */
	public void readServerInitMsg() throws IOException
	{
		fbWidth = is.readUnsignedShort();
		fbHeight = is.readUnsignedShort();
		bpp = is.readUnsignedByte();
		depth = is.readUnsignedByte();
		bigEndian = (is.readUnsignedByte() != 0);
		trueColor = (is.readUnsignedByte() != 0);
		rMax = is.readUnsignedShort();
		gMax = is.readUnsignedShort();
		bMax = is.readUnsignedShort();
		rShift = is.readUnsignedByte();
		gShift = is.readUnsignedByte();
		bShift = is.readUnsignedByte();
		is.read(new byte[3]);
		name = readString();
	}


	/**
	 * Read the message type of the next message on the input stream.
	 * 
	 * @return message type
	 * @throws IOException
	 */
	public int readServerMessageType() throws IOException
	{
		int t = is.read();
		return t;
	}


	/** number of updated rectangles from a single framebuffer update message */
	public int	numRectUpdates;


	/**
	 * Reads a server message of the type Framebuffer Update.
	 * 
	 * @throws IOException
	 */
	public void readFramebufferUpdateMsg() throws IOException
	{
		is.readByte();
		numRectUpdates = is.readUnsignedShort();
	}


	/** X coordinate of update rectangle */
	public int	rectX;

	/** Y coordinate of update rectangle */
	public int	rectY;

	/** width of update rectangle */
	public int	rectW;

	/** height of update rectangle */
	public int	rectH;

	/** encoding type of update rectangle */
	public int	rectEncoding;


	/**
	 * Reads a server message of the type Framebuffer Update Rectangle Header.
	 * 
	 * @throws IOException
	 */
	public void readFbUpdRectHeaderMsg() throws IOException
	{
		rectX = is.readUnsignedShort();
		rectY = is.readUnsignedShort();
		rectW = is.readUnsignedShort();
		rectH = is.readUnsignedShort();
		rectEncoding = is.readInt();

		if ((rectX + rectW > fbWidth) || (rectY + rectH > fbHeight))
		{
			throw new IOException(host + ":" + port
					+ " - Update rectangle too large (" + rectX + ", " + rectY
					+ ") @ (" + rectW + " x " + rectH + ")");
		}
	}


	/** for copy rectangle encoding, the source X coordinate to copy from */
	public int	copyRectX;

	/** for copy rectangle encoding, the source Y coordinate to copy from */
	public int	copyRectY;


	/**
	 * Reads a server message of the type Copy Rectangle Source
	 * 
	 * @throws IOException
	 */
	public void readCopyRectSrcMsg() throws IOException
	{
		copyRectX = is.readUnsignedShort();
		copyRectY = is.readUnsignedShort();
	}


	/** cut text sent from server */
	public String	cutText;


	/**
	 * Reads a server message of the type Server Cut Text.
	 * 
	 * @throws IOException
	 */
	public void readServerCutTextMsg() throws IOException
	{
		is.read(new byte[3]);
		cutText = readString();
	}


	/**
	 * Writes a client message of the type Framebuffer Update Request.
	 * 
	 * @param x
	 *            X coordinate of rectangle to refresh
	 * @param y
	 *            Y coordinate of rectangle to refresh
	 * @param w
	 *            width of rectangle to refresh
	 * @param h
	 *            height of rectangle to refresh
	 * @param incr
	 *            true if incremental update is allowed, false if contents of
	 *            rectangle are to be sent in full
	 * @throws IOException
	 */
	public void writeFramebufferUpdateRequestMsg(int x, int y, int w, int h,
			boolean incr) throws IOException
	{
		byte[] b = new byte[10];

		b[0] = (byte) FramebufferUpdateRequest;
		b[1] = (byte) (incr ? 1 : 0);
		b[2] = (byte) ((x >> 8) & 0xff);
		b[3] = (byte) (x & 0xff);
		b[4] = (byte) ((y >> 8) & 0xff);
		b[5] = (byte) (y & 0xff);
		b[6] = (byte) ((w >> 8) & 0xff);
		b[7] = (byte) (w & 0xff);
		b[8] = (byte) ((h >> 8) & 0xff);
		b[9] = (byte) (h & 0xff);
		
		os.write(b);
		os.flush();
	}


	/**
	 * Writes a client message of the type Set Pixel Format.
	 * 
	 * @param bpp
	 *            bits-per-pixel
	 * @param depth
	 *            color depth
	 * @param bigEndian
	 *            whether pixel values are big-endian
	 * @param trueColor
	 *            whether to use true color (vs. color-map)
	 * @param rMax
	 *            maximum red value
	 * @param gMax
	 *            maximum green value
	 * @param bMax
	 *            maximum blue value
	 * @param rShift #
	 *            of shifts to get red value
	 * @param gShift #
	 *            of shifts to get green value
	 * @param bShift #
	 *            of shifts to get blue value
	 * @throws IOException
	 */
	public void writeSetPixelFormatMsg(int bpp, int depth, boolean bigEndian,
			boolean trueColor, int rMax, int gMax, int bMax, int rShift,
			int gShift, int bShift) throws IOException
	{
		byte[] b = new byte[20];

		b[0] = (byte) SetPixelFormat;
		b[4] = (byte) bpp;
		b[5] = (byte) depth;
		b[6] = (byte) (bigEndian ? 1 : 0);
		b[7] = (byte) (trueColor ? 1 : 0);
		b[8] = (byte) ((rMax >> 8) & 0xff);
		b[9] = (byte) (rMax & 0xff);
		b[10] = (byte) ((gMax >> 8) & 0xff);
		b[11] = (byte) (gMax & 0xff);
		b[12] = (byte) ((bMax >> 8) & 0xff);
		b[13] = (byte) (bMax & 0xff);
		b[14] = (byte) rShift;
		b[15] = (byte) gShift;
		b[16] = (byte) bShift;

		os.write(b);
		os.flush();
	}


	/**
	 * Writes a client message of type Fix Color Map Entries.
	 * 
	 * @param first
	 *            first color value
	 * @param num
	 *            number of colors
	 * @param red
	 *            red colors
	 * @param green
	 *            green colors
	 * @param blue
	 *            blue colors
	 * @throws IOException
	 */
	public void writeFixColorMapEntriesMsg(int first, int num, int[] red,
			int[] green, int[] blue) throws IOException
	{
		byte[] b = new byte[6 + (num * 6)];

		b[0] = (byte) FixColorMapEntries;
		b[2] = (byte) ((first >> 8) & 0xff);
		b[3] = (byte) (first & 0xff);
		b[4] = (byte) ((num >> 8) & 0xff);
		b[5] = (byte) (num & 0xff);

		for (int i = 0; i < num; i++)
		{
			b[6 + i * 6] = (byte) ((red[i] >> 8) & 0xff);
			b[6 + i * 6 + 1] = (byte) (red[i] & 0xff);
			b[6 + i * 6 + 2] = (byte) ((green[i] >> 8) & 0xff);
			b[6 + i * 6 + 3] = (byte) (green[i] & 0xff);
			b[6 + i * 6 + 4] = (byte) ((blue[i] >> 8) & 0xff);
			b[6 + i * 6 + 5] = (byte) (blue[i] & 0xff);
		}

		os.write(b);
		os.flush();
	}


	/**
	 * Writes a client message of type Set Encodings.
	 * 
	 * @param encs
	 *            supported encodings
	 * @throws IOException
	 */
	public void writeSetEncodingsMsg(int[] encs) throws IOException
	{
		int len = encs.length;
		byte[] b = new byte[4 + (4 * len)];

		b[0] = (byte) SetEncodings;
		b[2] = (byte) ((len >> 8) & 0xff);
		b[3] = (byte) (len & 0xff);

		for (int i = 0; i < len; i++)
		{
			b[4 + 4 * i] = (byte) ((encs[i] >> 24) & 0xff);
			b[5 + 4 * i] = (byte) ((encs[i] >> 16) & 0xff);
			b[6 + 4 * i] = (byte) ((encs[i] >> 8) & 0xff);
			b[7 + 4 * i] = (byte) (encs[i] & 0xff);
		}

		os.write(b);
		os.flush();
	}


	/**
	 * Writes a client message of type Client Cut Text.
	 * 
	 * @param text
	 *            client's cut text
	 * @throws IOException
	 */
	public void writeClientCutTextMsg(String text) throws IOException
	{
		byte[] b = new byte[8 + text.length()];

		b[0] = (byte) ClientCutText;
		b[4] = (byte) ((text.length() >> 24) & 0xff);
		b[5] = (byte) ((text.length() >> 16) & 0xff);
		b[6] = (byte) ((text.length() >> 8) & 0xff);
		b[7] = (byte) (text.length() & 0xff);

		text.getBytes(0, text.length(), b, 8);

		os.write(b);
		os.flush();
	}


	/**
	 * Write a client message of type Pointer Event.
	 * 
	 * @param btns
	 *            whether each button is down.
	 * @throws IOException
	 */
	public void writePointerEventMsg(boolean[] btns, int x, int y)
			throws IOException
	{
		byte[] b = new byte[6];

		int mask = 0;
		for (int i = 7; i >= 0; i--)
			mask = (mask << 1) | (btns[i] ? 1 : 0);

		b[0] = (byte) PointerEvent;
		b[1] = (byte) mask;
		b[2] = (byte) ((x >> 8) & 0xff);
		b[3] = (byte) (x & 0xff);
		b[4] = (byte) ((y >> 8) & 0xff);
		b[5] = (byte) (y & 0xff);

		os.write(b);
		os.flush();
	}


	private void writeKeyEvent(int key, boolean down) throws IOException
	{
		eventBuf[eventBufLen++] = (byte) KeyEvent;
		eventBuf[eventBufLen++] = (byte) (down ? 1 : 0);
		eventBuf[eventBufLen++] = (byte) 0;
		eventBuf[eventBufLen++] = (byte) 0;
		eventBuf[eventBufLen++] = (byte) ((key >> 24) & 0xff);
		eventBuf[eventBufLen++] = (byte) ((key >> 16) & 0xff);
		eventBuf[eventBufLen++] = (byte) ((key >> 8) & 0xff);
		eventBuf[eventBufLen++] = (byte) (key & 0xff);
	}


	public void typeChar(char c) throws IOException
	{
		writeKeyMsg((int) c, true);
		try
		{
			Thread.sleep(50);
		}
		catch (InterruptedException e)
		{
		}
		writeKeyMsg((int) c, false);
	}


	private void writeKeyMsg(int key, boolean down) throws IOException
	{
		eventBufLen = 0;
		writeKeyEvent(key, down);
		os.write(eventBuf, 0, eventBufLen);
		os.flush();
	}


	public void writeKeyEventMsg(KeyEvent evt) throws IOException
	{
		int key = evt.getKeyCode();
		boolean down = false;

		if ((evt.getID() == Event.KEY_PRESS)
				|| (evt.getID() == Event.KEY_ACTION))
			down = true;

		if ((evt.getID() == Event.KEY_ACTION)
				|| (evt.getID() == Event.KEY_ACTION_RELEASE))
		{
			switch (key) {
			case Event.HOME:
				key = 0xff50;
				break;
			case Event.LEFT:
				key = 0xff51;
				break;
			case Event.UP:
				key = 0xff52;
				break;
			case Event.RIGHT:
				key = 0xff53;
				break;
			case Event.DOWN:
				key = 0xff54;
				break;
			case Event.PGUP:
				key = 0xff55;
				break;
			case Event.PGDN:
				key = 0xff56;
				break;
			case Event.END:
				key = 0xff57;
				break;
			case Event.F1:
				key = 0xffbe;
				break;
			case Event.F2:
				key = 0xffbf;
				break;
			case Event.F3:
				key = 0xffc0;
				break;
			case Event.F4:
				key = 0xffc1;
				break;
			case Event.F5:
				key = 0xffc2;
				break;
			case Event.F6:
				key = 0xffc3;
				break;
			case Event.F7:
				key = 0xffc4;
				break;
			case Event.F8:
				key = 0xffc5;
				break;
			case Event.F9:
				key = 0xffc6;
				break;
			case Event.F10:
				key = 0xffc7;
				break;
			case Event.F11:
				key = 0xffc8;
				break;
			case Event.F12:
				key = 0xffc9;
				break;
			default:
				return;
			}
		}
		else
		{
			if (key < 32)
			{
				if ((evt.getModifiers() & Event.CTRL_MASK) != 0)
				{
					key += 96;
					if (key == 127) // CTRL-_
						key = 95;
				}
				else
				{
					switch (key) {
					case 8:
						key = 0xff08;
						break;
					case 9:
						key = 0xff09;
						break;
					case 10:
						key = 0xff0d;
						break;
					case 27:
						key = 0xff1b;
						break;
					}
				}

			}
			else if (key < 256)
			{
				if (key == 127)
					key = 0xffff;
			}
			else
			{
				if ((key < 0xff00) || (key > 0xffff))
					return;
			}
		}

		eventBufLen = 0;
		writeModifierKeyEvents(evt.getModifiers());
		writeKeyEvent(key, down);

		if (!down)
		{
			writeModifierKeyEvents(0);
		}

		os.write(eventBuf, 0, eventBufLen);
		os.flush();
	}


	private int		eventBufLen;

	private byte[]	eventBuf	= new byte[72];

	int				oldModifiers;


	void writeModifierKeyEvents(int newModifiers) throws IOException
	{
		if ((newModifiers & Event.CTRL_MASK) != (oldModifiers & Event.CTRL_MASK))
			writeKeyEvent(0xffe3, (newModifiers & Event.CTRL_MASK) != 0);

		if ((newModifiers & Event.SHIFT_MASK) != (oldModifiers & Event.SHIFT_MASK))
			writeKeyEvent(0xffe1, (newModifiers & Event.SHIFT_MASK) != 0);

		if ((newModifiers & Event.META_MASK) != (oldModifiers & Event.META_MASK))
			writeKeyEvent(0xffe7, (newModifiers & Event.META_MASK) != 0);

		if ((newModifiers & Event.ALT_MASK) != (oldModifiers & Event.ALT_MASK))
			writeKeyEvent(0xffe9, (newModifiers & Event.ALT_MASK) != 0);

		oldModifiers = newModifiers;
	}


	/**
	 * Read a number of bytes into a buffer.
	 * 
	 * @param buf
	 *            byte buffer
	 * @param offset
	 *            offset of first read byte
	 * @param num
	 *            number of bytes to read
	 * @throws IOException
	 */
	public void readBytes(byte[] buf, int offset, int num) throws IOException
	{
		is.readFully(buf, offset, num);
	}


	/**
	 * @return single integer from input stream
	 * @throws IOException
	 */
	public int readInt() throws IOException
	{
		return is.readInt();
	}


	/**
	 * @return single byte from input stream
	 * @throws IOException
	 */
	public int readByte() throws IOException
	{
		return is.read();
	}


	/**
	 * @return unsigned short value from input stream
	 * @throws IOException
	 */
	public int readUnsignedShort() throws IOException
	{
		return is.readUnsignedShort();
	}


	/**
	 * Reads a string prefixed by a four-byte length variable.
	 * 
	 * @return string in messageF
	 * @throws IOException
	 */
	private String readString() throws IOException
	{
		int strlen = is.readInt();
		byte[] b = new byte[strlen];
		is.readFully(b);
		return new String(b);
	}


	/**
	 * @return hostname
	 */
	public String getHost()
	{
		return host;
	}


	/**
	 * @return port number
	 */
	public int getPort()
	{
		return port;
	}


	public void dbgStream() throws IOException
	{
		int read = 0;
		System.out.println("STREAM DEBUG:");
		while (true)
		{
			System.out.print(is.read() + ",");
			read++;
			if (read > 50)
				break;
		}
	}


	public DataInputStream getInputStream()
	{
		return is;
	}


	public boolean messagesWaiting() throws IOException
	{
		return is.available() > 0;
	}
}
