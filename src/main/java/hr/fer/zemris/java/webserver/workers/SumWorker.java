package hr.fer.zemris.java.webserver.workers;

import hr.fer.zemris.java.custom.scripting.exec.SmartScriptEngine;
import hr.fer.zemris.java.webserver.IWebWorker;
import hr.fer.zemris.java.webserver.RequestContext;

/**
 * Razred koji implementira sučelje {@link IWebWorker}. Primjerak razreda
 * koristi se za sumiranje parametara koje je korisnik posalo na poslužitelj.
 * Jedini uvjet je da je prvi parametar pod ključem {@value #FIRST_PARAM_NAME},
 * a drugi parametar pod ključem {@value #SECOND_PARAM_NAME}. Primjerak će po
 * dohvatu parametara i izračunu rezultata generiranje html dokumenta delegirati
 * primjerku razreda {@link SmartScriptEngine} koji će isparsirati datoteku
 * {@value #SCRIPT_PATH}.
 * <p>
 * Napomena: ako se predaju parametri koji se ne mogu parsirati metodom
 * {@link Integer#parseInt(String)} ili se neki parametar ne zada, defaultne
 * vrijednosti su {@value #FIRST_PARAM_NAME} = {@value #FIRST_PARAM_DEFAULT} i
 * {@value #SECOND_PARAM_NAME} = {@value #SECOND_PARAM_DEFAULT}
 * <p>
 * 
 * @see IWebWorker
 * @see SmartScriptEngine
 * 
 * @author Davor Češljaš
 */
public class SumWorker implements IWebWorker {

	/** Konstanta koja predstavlja ključ prvog argumenta zbrajanja */
	private static final String FIRST_PARAM_NAME = "a";

	/**
	 * Konstanta koja predstavlja predpostavljenu vrijednost prvog argumenta
	 * zbrajanja
	 */
	private static final int FIRST_PARAM_DEFAULT = 1;

	/** Konstanta koja predstavlja ključ drugog argumenta zbrajanja */
	private static final String SECOND_PARAM_NAME = "b";

	/**
	 * Konstanta koja predstavlja predpostavljenu vrijednost drugog argumenta
	 * zbrajanja
	 */
	private static final int SECOND_PARAM_DEFAULT = 2;

	/** Konstanta koja predstavlja ključ rezultata zbrajanja */
	private static final String SUM_KEY = "zbroj";

	/**
	 * Konstanta koja predstavlja putanju do skripte koja se izvodi nakon
	 * dohvata rezultata
	 */
	private static final String SCRIPT_PATH = "/private/calc.smscr";

	@Override
	public void processRequest(RequestContext context) throws Exception {
		int firstArgument = getOrDefault(FIRST_PARAM_NAME, FIRST_PARAM_DEFAULT, context);
		int secondArgument = getOrDefault(SECOND_PARAM_NAME, SECOND_PARAM_DEFAULT, context);

		context.setTemporaryParameter(FIRST_PARAM_NAME, String.valueOf(firstArgument));
		context.setTemporaryParameter(SECOND_PARAM_NAME, String.valueOf(secondArgument));
		context.setTemporaryParameter(SUM_KEY, String.valueOf(firstArgument + secondArgument));

		context.getDispatcher().dispatchRequest(SCRIPT_PATH);
	}

	/**
	 * Pomoćna metoda koja iz predanog primjerka razreda {@link RequestContext}
	 * dohvaća parametar pod predanim ključem <b>paramName</b>, ukoliko u
	 * parametrima ne postoji takav ključ ili ukoliko se vrijednost ne može
	 * parsirati metodom {@link Integer#parseInt(String)} metoda vraća
	 * <b>defaultValue</b>. Ukoliko je dohvat uspio metoda vraća parsiranu
	 * vrijednost.
	 *
	 * @param paramName
	 *            ključ parametra koji se traži
	 * @param defaultValue
	 *            predpostavljena vrijednost parametra ukoliko parsiranje ne uspije
	 * @param context
	 *            primjerak razreda {@link RequestContext} iz kojeg se dohvaćaju parametri
	 * @return parsirana vrijednosti ili <b>defaultValue</b>
	 */
	private int getOrDefault(String paramName, int defaultValue, RequestContext context) {
		int value = defaultValue;

		String paramValue = context.getParameter(paramName);
		if (paramValue != null) {
			try {
				value = Integer.parseInt(paramValue);
			} catch (NumberFormatException ignorable) {
			}
		}

		return value;
	}
}
