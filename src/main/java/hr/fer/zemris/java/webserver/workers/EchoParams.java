package hr.fer.zemris.java.webserver.workers;

import java.io.IOException;

import hr.fer.zemris.java.webserver.IWebWorker;
import hr.fer.zemris.java.webserver.RequestContext;

/**
 * Razred koji implementira sučelje {@link IWebWorker}. Primjerak razreda
 * generira poruku u stilu <b>html</b> tablice, koja sadrži sve parametre koje
 * je korisnik predao kroz zahtjev. Ukoliko korisnik ne preda parametre ispisuje
 * se prazan <b>html</b> dokument. Čitava poruka generira se dinamički
 */
public class EchoParams implements IWebWorker {

	/**
	 * Konstanta koje predstavlja head-tag <b>html</b> dokumenta koji se šalje.
	 * Unutar ovog taga definiran je stil tablice
	 */
	private static final String TABLE_HEAD = "<head><style> td, th { border: 1px solid #0275d8; } </style></head>";

	@Override
	public void processRequest(RequestContext context) throws Exception {
		try {
			context.setFullContent(false);

			context.write("<html>" + TABLE_HEAD + "<body>"
					+ "<table><tr><th>Naziv parametra</th><th>Vrijednost parametra</th></tr>");
			for (String paramName : context.getParameterNames()) {
				context.write(
						String.format("<tr><th>%s</th><th>%s</th></tr>", paramName, context.getParameter(paramName)));
			}
			context.write("</table></body></html>");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
