package hr.fer.zemris.java.webserver;

/**
 * Sučelje koje predstavlja jednog radnika na poslužitelju oblikovanom razredom
 * {@link SmartHttpServer}. Posao ovog radnika je izvršiti generiranje odgovora
 * na zahtjev korisnika. Kontakt sa klijenotom te trenutno stanje odgovora može
 * se dohvatiti iz primjerka razreda {@link RequestContext} koji se predaje
 * jedinoj metodi {@link #processRequest(RequestContext)}.
 * <p>
 * Napomena: sučelje sadrži samo jednu metodu, te je time povoljno za pisanje
 * lambda-izraza
 * </p>
 * 
 * @see SmartHttpServer
 * @see RequestContext
 * 
 * @author Davor Češljaš
 */
public interface IWebWorker {

	/**
	 * Metoda koja procesira zahtjev korisnika. Metodi se predaje jedan
	 * parametar, koji je primjerak razreda {@link RequestContext}, a pomoću
	 * kojeg se vrši generiranje odgovora. Budući da isti primjerak sadrži i
	 * metode za pisanje odgovora u okviru metoda
	 * {@link RequestContext#write(byte[])} i
	 * {@link RequestContext#write(String)}, korisnika se navodi na korištenje
	 * tih metoda za ispis generiranog sadržaja
	 *
	 * @param context
	 *            primjerak razreda {@link RequestContext} koji se koristi za
	 *            dohvat trenutnog stanja odgovora te za slanje odgovora
	 * @throws Exception
	 *             iznimka koja se može dogoditi prilikom generiranja odgovora
	 */
	public void processRequest(RequestContext context) throws Exception;
}
