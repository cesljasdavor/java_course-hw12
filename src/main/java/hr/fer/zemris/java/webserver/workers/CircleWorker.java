package hr.fer.zemris.java.webserver.workers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import hr.fer.zemris.java.webserver.IWebWorker;
import hr.fer.zemris.java.webserver.RequestContext;

/**
 * Razred koji implementira sučelje {@link IWebWorker}. Primjerak razred generira sliku
 * plavog kruga upisanu u kvadrat veličine 200x200 piksela. Primjerak razreda sliku
 * generira dinamički (ne dohvaća je iz memorije). Za izradu slike koristi se
 * primjerak razreda {@link BufferedImage}.
 * 
 * @see IWebWorker
 * @see BufferedImage
 * 
 * @author Davor Češljaš
 */
public class CircleWorker implements IWebWorker {

	/** Konstanta koja predstavlja širinu slike */
	private static final int CIRCLE_WIDTH = 200;

	/** Konstanta koja predstavlja visinu slike */
	private static final int CIRCLE_HEIGHT = 200;

	/**
	 * Konstanta koja predstavlja poziciju na x-osi od koje se na slici kreće
	 * generirati krug
	 */
	private static final int CIRCLE_X = 0;

	/**
	 * Konstanta koja predstavlja poziciju na y-osi od koje se na slici kreće
	 * generirati krug
	 */
	private static final int CIRCLE_Y = 0;

	/** Konstanta koja predstavlja ekstenziju slike */
	private static final String IMAGE_EXTENSION = "png";

	/** Konstanta koja predstavlja mime-tip slike */
	private static final String IMAGE_MIME_TYPE = "image/png";

	@Override
	public void processRequest(RequestContext context) throws Exception {
		BufferedImage bim = new BufferedImage(CIRCLE_WIDTH, CIRCLE_HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g = bim.createGraphics();

		g.setColor(Color.BLUE);
		g.fillOval(CIRCLE_X, CIRCLE_Y, CIRCLE_WIDTH, CIRCLE_HEIGHT);
		g.dispose();

		sendImage(bim, context);
	}

	/**
	 * Pomoćna metoda koja se koristi za slanje slike kao odgovor na korisnikov
	 * upit. Metoda prvo koristi metodu
	 * {@link ImageIO#write(java.awt.image.RenderedImage, String, java.io.OutputStream)}
	 * čime se slika pretvara u niz okteta, a potom te oktete pozivom metode
	 * {@link RequestContext#write(byte[])} šalje korisniku
	 *
	 * @param bim
	 *            primjerak razreda {@link BufferedImage} koji predstavlja sliku
	 *            koja se šalje
	 * @param context
	 *            primjerak razreda {@link RequestContext} kojemu se delegira
	 *            samo slanje slike, pozivom njegove metode
	 *            {@link RequestContext#write(byte[])}
	 */
	private void sendImage(BufferedImage bim, RequestContext context) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			ImageIO.write(bim, IMAGE_EXTENSION, bos);

			context.setMimeType(IMAGE_MIME_TYPE);
			context.write(bos.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
