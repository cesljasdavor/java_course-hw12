package hr.fer.zemris.java.webserver.workers;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import hr.fer.zemris.java.webserver.IWebWorker;
import hr.fer.zemris.java.webserver.RequestContext;

/**
 * Razred koji implementira sučelje {@link IWebWorker}. Primjerak razred generira poruku,
 * koja je <b>html</b> dokument, unutar koje se prvo ispisuje poruka "Hello!!!",
 * nakon čega slijedi vrijeme i datum slanja odgovora. Potom ukoliko je korisnik
 * ponudio parametar sa ključem "name" ispisuje koliko ime ima slova, a u
 * suprotnom ispisuje poruku da nije poslano ime.
 * 
 * @see IWebWorker
 * 
 * @author Davor Češljaš
 */
public class HelloWorker implements IWebWorker {

	@Override
	public void processRequest(RequestContext context) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date now = new Date();
		context.setMimeType("text/html");
		String name = context.getParameter("name");
		try {
			context.setFullContent(false);
			context.write("<html><body>");
			context.write("<h1>Hello!!!</h1>");
			context.write("<p>Now is: " + sdf.format(now) + "</p>");
			if (name == null || name.trim().isEmpty()) {
				context.write("<p>You did not send me your name!</p>");
			} else {
				context.write("<p>Your name has " + name.trim().length() + " letters.</p>");
			}
			context.write("</body></html>");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}