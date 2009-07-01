package poker.server.log;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Date;

import poker.common.Rect;


public class LogEntry
{

	private int id;
	
	private String type;
	
	private String src;
	
	private String msg;
	
	private Rect img;
	
	private Date date;
	
	
	/**
	 * Constructor.
	 * 
	 * @param id
	 * @param type
	 * @param src
	 * @param msg
	 * @param img
	 */
	public LogEntry(int id, String type, String src, String msg, Rect img)
	{
		this.date = LogServerImpl.now();
		this.id = id;
		this.type = type;
		this.src = src;
		this.msg = msg;
		this.img = img;
	}


	/**
	 * Write the message entry to the output stream.
	 * 
	 * @param stream
	 */
	public void writeTo(OutputStream stream)
	{
		OutputStreamWriter osw = new OutputStreamWriter(stream);
		PrintWriter pw = new PrintWriter(osw);
		
		pw.printf("%d:%s:%s\n%%%%%s%%%%", id, type, src, msg);
		if (img != null)
			pw.printf("%s", img.printHex(80));
		pw.printf("%%%%\n");
		pw.flush();
	}

}
