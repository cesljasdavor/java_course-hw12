package hr.fer.zemris.java.webserver;

/**
 * Sučelje koje predstavlja pošiljatelja odgovora. Razredi koji implementiraju
 * ovo sučelje obavezni su poslati neku vrstu odgovora klijentu koji komunicira
 * sa poslužiteljem oblikovanim razredom {@link SmartHttpServer} ili izazvati
 * bilo koju iznimku, ukoliko nisu u mogućnosti poslati odgovor klijentu
 * internetskog-poslužitelja
 * 
 * <p>
 * Napomena: ovo sučelje je funkcionalno sučelje i time se može koristiti u
 * lambda-izrazima
 * </p>
 * 
 * @see SmartHttpServer
 * 
 * @author Davor Češljaš
 */
public interface IDispatcher {

	/**
	 * Metoda koja šalje odgovor na klijentov zahtjev. Metoda prima samo jedan
	 * parametar, kako bi dretva koja obrađuje zahtjev znala gdje treba tražiti
	 * odgovor na predani zahtjev
	 *
	 * @param urlPath
	 *            putanja do zahtjevanog resursa
	 * @throws Exception
	 *             iznimka koja se izaziva ukoliko nije moguće poslati odgovor klijentu
	 */
	void dispatchRequest(String urlPath) throws Exception;
}
