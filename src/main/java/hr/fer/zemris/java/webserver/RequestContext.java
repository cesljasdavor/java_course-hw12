package hr.fer.zemris.java.webserver;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Razred koji predstavlja kontekst korisnikova zahtjeva koji je poslao. Razred
 * sadržava opće informacije o korisnikovu zahtjevu. Razred se također koristi
 * kako bi se na ispravan način formirao odgovor na korisnikov zahtjev. Samo
 * neke od informacija koje primjerak ovog razreda sadrži su:
 * <ul>
 * <li>skup znakova kojim treba enkodirati odgovor</li>
 * <li>status koji se veže uz odgovor</li>
 * <li>tekst za gore navedeni status</li>
 * <li>korisnikovi parametri</li>
 * <li>mime-tip koji se šalje korisniku</li> ...
 * </ul>
 * Razred se koristi u okviru rada poslužitelja koji je modeliran razredom
 * {@link SmartHttpServer}
 * 
 * @see SmartHttpServer
 * 
 * @author Davor Češljaš
 */
public class RequestContext {

	/**
	 * Konstanta koja predstavlja predpostavljeni skup znakova za enkodiranje
	 */
	private static final String DEFAULT_ENCODING = "UTF-8";

	/**
	 * Konstanta koja predstavlja skup znakova koji se enkodira zaglavlje
	 * odgovora
	 */
	private static final Charset HEADER_CHARSET = StandardCharsets.ISO_8859_1;

	/** Konstanta koja predstavlja predpostavljeni statusni kod odgovora */
	private static final int DEFAULT_STATUS_CODE = 200;

	/**
	 * Konstanta koja predstavlja predpostavljeni tekst uz statusni kod odgovora
	 */
	private static final String DEFAULT_STATUS_TEXT = "OK";

	/** Konstanta koja predstavlja predpostavljeni mime-tip odgovora */
	private static final String DEFAULT_MIME_TYPE = "text/html";

	/** Konstanta koja predstavlja prazan niz znakova */
	private static final String EMPTY = "";

	/** Konstanta koja predstavlja oznaku za novi redak unutar HTTP protokola */
	private static final String REQUEST_NEWLINE = "\r\n";

	/**
	 * Članska varijabla koja predstavlja izlazni tok okteta u koji je potrebno
	 * pisati
	 */
	private OutputStream outputStream;

	/**
	 * Članska varijabla koja predstavlja skup znakova kojim treba enkodirati
	 * odgovor
	 */
	private Charset charset;

	/**
	 * Članska varijabla koja predstavlja tekstualnu reprezentaiju koda s kojim
	 * treba enkodirati odgovor
	 */
	private String encoding = DEFAULT_ENCODING;

	/** Članska varijabla koja predstavlja statusni kod odgovora */
	private int statusCode = DEFAULT_STATUS_CODE;

	/** Članska varijabla koja predstavlja tekst uz statusni kod odgovora */
	private String statusText = DEFAULT_STATUS_TEXT;

	/** Članska varijabla koja predstavlja mime-tip odgovora */
	private String mimeType = DEFAULT_MIME_TYPE;

	/**
	 * Članska varijabla koja predstavlja implementaciju sučelja
	 * {@link IDispatcher} koja koristi ovaj primjerak razreda
	 */
	private IDispatcher dispatcher;

	/**
	 * Članska varijabla koja predstavlja {@link Map} kojoj su ključevi nazivi,
	 * a vrijednosti vrijednosti uz naziv parametara koje je korisnik poslao
	 */
	private Map<String, String> parameters;

	/**
	 * Članska varijabla koja predstavlja {@link Map} kojoj su ključevi nazivi,
	 * a vrijednosti vrijednosti uz naziv privremenih parametara koji su se
	 * izgenerirali u odgovoru
	 */
	private Map<String, String> temporaryParameters;

	/**
	 * Članska varijabla koja predstavlja {@link Map} kojoj su ključevi nazivi,
	 * a vrijednosti vrijednosti uz naziv stalnih parametara koji se koriste uz
	 * cookie tehnologiju, te može biti da su izgenerirani u prijašnjim sesijama
	 * klijenta i ovog poslužitelja modeliranog razredom {@link SmartHttpServer}
	 */
	private Map<String, String> persistentParameters;

	/**
	 * Članska varijabla koja predstavlja {@link List} cookija modeliranih
	 * razredom {@link RCCookie}, a koji se šalju korisniku (nejgovom
	 * pregledniku)
	 */
	private List<RCCookie> outputCookies;

	/**
	 * Članska varijabla koja indicira je li zaglavlje odgovora izgenerirano.
	 */
	private boolean headerGenerated;

	/**
	 * Članska varijabla koja indicira šalju li se svi podaci odgovora odjednom.
	 * O vrijednosti ove varijable ovisi hoće li se u zaglavlju slati
	 * "Content-Length: " parametar
	 */
	private boolean fullContent = true;

	/**
	 * Konstruktor koji inicijalizira primjerak ovog razreda. Unutar
	 * konstruktora reference na sve predane parametre spremaju kao članske
	 * varijable. Ukoliko se kao parametar <b>outputStream</b> preda
	 * <code>null</code> baca se iznimka {@link IllegalArgumentException}
	 *
	 * @param temporaryParameters
	 *            varijabla koja predstavlja {@link Map} kojoj su ključevi
	 *            nazivi, a vrijednosti vrijednosti uz naziv privremenih
	 *            parametara koji su se izgenerirali u odgovoru
	 * @param dispatcher
	 *            varijabla koja predstavlja implementaciju sučelja
	 *            {@link IDispatcher} koja koristi ovaj primjerak razreda
	 * @param outputStream
	 *            Članska varijabla koja predstavlja izlazni tok okteta u koji
	 *            je potrebno pisati
	 * @param parameters
	 *            varijabla koja predstavlja {@link Map} kojoj su ključevi
	 *            nazivi, a vrijednosti vrijednosti uz naziv parametara koje je
	 *            korisnik poslao
	 * @param persistentParameters
	 *            varijabla koja predstavlja {@link Map} kojoj su ključevi
	 *            nazivi, a vrijednosti vrijednosti uz naziv stalnih parametara
	 *            koji se koriste uz cookie tehnologiju, te može biti da su
	 *            izgenerirani u prijašnjim sesijama klijenta i ovog
	 *            poslužitelja modeliranog razredom {@link SmartHttpServer}
	 * @param outputCookies
	 *            varijabla koja predstavlja {@link List} cookija modeliranih
	 *            razredom {@link RCCookie}, a koji se šalju korisniku (nejgovom
	 *            pregledniku)
	 * @throws IllegalArgumentException
	 *             ukoliko se kao parametar <b>outputStream</b> preda
	 *             <code>null</code>
	 */
	public RequestContext(Map<String, String> temporaryParameters, IDispatcher dispatcher, OutputStream outputStream,
			Map<String, String> parameters, Map<String, String> persistentParameters, List<RCCookie> outputCookies) {
		if (outputStream == null) {
			throw new IllegalArgumentException("Izlazni tok podataka ne može biti null");
		}

		this.outputStream = outputStream;
		this.parameters = Collections.unmodifiableMap(parameters != null ? parameters : new HashMap<>());
		this.persistentParameters = persistentParameters != null ? persistentParameters : new HashMap<>();
		this.outputCookies = outputCookies != null ? outputCookies : new ArrayList<>();
		this.temporaryParameters = temporaryParameters;
		this.dispatcher = dispatcher;
	}

	/**
	 * Konstruktor koji inicijalizira primjerak ovog razreda. Unutar
	 * konstruktora reference na sve predane parametre spremaju kao članske
	 * varijable. Ukoliko se kao parametar <b>outputStream</b> preda
	 * <code>null</code> baca se iznimka {@link IllegalArgumentException}
	 *
	 * @param outputStream
	 *            Članska varijabla koja predstavlja izlazni tok okteta u koji
	 *            je potrebno pisati
	 * @param parameters
	 *            varijabla koja predstavlja {@link Map} kojoj su ključevi
	 *            nazivi, a vrijednosti vrijednosti uz naziv parametara koje je
	 *            korisnik poslao
	 * @param persistentParameters
	 *            varijabla koja predstavlja {@link Map} kojoj su ključevi
	 *            nazivi, a vrijednosti vrijednosti uz naziv stalnih parametara
	 *            koji se koriste uz cookie tehnologiju, te može biti da su
	 *            izgenerirani u prijašnjim sesijama klijenta i ovog
	 *            poslužitelja modeliranog razredom {@link SmartHttpServer}
	 * @param outputCookies
	 *            varijabla koja predstavlja {@link List} cookija modeliranih
	 *            razredom {@link RCCookie}, a koji se šalju korisniku (nejgovom
	 *            pregledniku)
	 * @throws IllegalArgumentException
	 *             ukoliko se kao parametar <b>outputStream</b> preda
	 *             <code>null</code>
	 */
	public RequestContext(OutputStream outputStream, Map<String, String> parameters,
			Map<String, String> persistentParameters, List<RCCookie> outputCookies) {
		this(new HashMap<>(), null, outputStream, parameters, persistentParameters, outputCookies);
	}

	/**
	 * Metoda koja postavlja tekstualnu reprezentaiju koda s kojim treba
	 * enkodirati odgovor.
	 *
	 * @param encoding
	 *            tekstualna reprezentaija koda s kojim treba enkodirati odgovor
	 * @throws RuntimeException
	 *             ako je zaglavlje već izgenerirano
	 */
	public void setEncoding(String encoding) {
		checkIfHeaderGenerated();
		this.encoding = encoding;
	}

	/**
	 * Metoda koja postavlja statusni kod koji se šalje u odgovoru
	 *
	 * @param encoding
	 *            statusni kod koji se šalje u odgovoru
	 * @throws RuntimeException
	 *             ako je zaglavlje već izgenerirano
	 */
	public void setStatusCode(int statusCode) {
		checkIfHeaderGenerated();
		this.statusCode = statusCode;
	}

	/**
	 * Metoda koja postavlja tekst uz statusni kod koji se šalje u odgovoru
	 *
	 * @param encoding
	 *            tekst uz statusni kod koji se šalje u odgovoru
	 * @throws RuntimeException
	 *             ako je zaglavlje već izgenerirano
	 */
	public void setStatusText(String statusText) {
		checkIfHeaderGenerated();
		this.statusText = statusText;
	}

	/**
	 * Metoda koja postavlja mime-tip odgovora
	 *
	 * @param encoding
	 *            mime-tip odgovora
	 * @throws RuntimeException
	 *             ako je zaglavlje već izgenerirano
	 */
	public void setMimeType(String mimeType) {
		checkIfHeaderGenerated();
		this.mimeType = mimeType;
	}

	/**
	 * Metoda koja postavlja indikator šalju li se svi podaci od jednom
	 * korisniku
	 *
	 * @param encoding
	 *            indikator šalju li se svi podaci od jednom korisniku
	 * @throws RuntimeException
	 *             ako je zaglavlje već izgenerirano
	 */
	public void setFullContent(boolean fullContent) {
		checkIfHeaderGenerated();
		this.fullContent = fullContent;
	}

	/**
	 * Metoda koja dodaje primjerak razreda {@link RCCookie} u {@link List} svih
	 * primjeraka {@link RCCookie} koji se šalju kroz odogovor
	 *
	 * @param cookie
	 *            primjerak razreda {@link RCCookie}
	 * 
	 * @throws RuntimeException
	 *             ako je zaglavlje već izgenerirano
	 */
	public void addRCCookie(RCCookie cookie) {
		checkIfHeaderGenerated();
		outputCookies.add(cookie);
	}

	/**
	 * Pomoćna metoda koja baca {@link RuntimeException} ukoliko je pozvana, a
	 * pri tom je postavljena zastavica {@link #headerGenerated}
	 */
	private void checkIfHeaderGenerated() {
		if (headerGenerated) {
			throw new RuntimeException("Zaglavlje je izgenerirano, stoga se svojstvo više ne može mijenjati");
		}
	}

	/**
	 * Metoda koja dohvaća varijablu koja predstavlja implementaciju sučelja
	 * {@link IDispatcher} koja koristi ovaj primjerak razreda
	 *
	 * @return varijablu koja predstavlja implementaciju sučelja
	 *         {@link IDispatcher} koja koristi ovaj primjerak razreda
	 */
	public IDispatcher getDispatcher() {
		return dispatcher;
	}

	/**
	 * Metoda koja iz {@link Map} korisnikovih parametara dohvaća parametar pod
	 * ključem <b>name</b> ili vraća <code>null</code> ukoliko takav ključ ne
	 * postoji unutar parametara
	 *
	 * @param name
	 *            ključ čija se vrijednost dohvaća iz {@link Map}e parametara
	 *            koje je korisnik posalo
	 * @return parametar pod ključem <b>name</b> ili <code>null</code> ukoliko
	 *         takav ključ ne postoji unutar parametara
	 */
	public String getParameter(String name) {
		return parameters.get(name);
	}

	/**
	 * Metoda koja dohvaća sve ključeve parametara iz {@link Map} parametara
	 *
	 * @return sve ključeve parametara iz {@link Map} parametara
	 */
	public Set<String> getParameterNames() {
		return Collections.unmodifiableSet(parameters.keySet());
	}

	/**
	 * Metoda koja iz {@link Map} stalnih parametara dohvaća parametar pod
	 * ključem <b>name</b> ili vraća <code>null</code> ukoliko takav ključ ne
	 * postoji unutar stalnih parametara
	 *
	 * @param name
	 *            ključ čija se vrijednost dohvaća iz {@link Map}e stalnih
	 *            parametara
	 * @return parametar pod ključem <b>name</b> ili <code>null</code> ukoliko
	 *         takav ključ ne postoji unutar parametara
	 */
	public String getPersistentParameter(String name) {
		return persistentParameters.get(name);
	}

	/**
	 * Metoda koja dohvaća sve ključeve parametara iz {@link Map} stalnih
	 * parametara
	 *
	 * @return sve ključeve parametara iz {@link Map} stalnih parametara
	 */
	public Set<String> getPersistentParameterNames() {
		return Collections.unmodifiableSet(persistentParameters.keySet());
	}

	/**
	 * Metoda koja u {@link Map}u stalnih parametara dodaje unos sa ključem
	 * <b>name</b> i vrijednošću <b>value</b>. Moguće je da takav unos već
	 * postoji pa se samo vrijednost postavlja pod tim ključem, a stara
	 * vrijednost se briše
	 *
	 * @param name
	 *            ključ pod kojim se dodaje parametar
	 * @param value
	 *            vrijednost parametra
	 */
	public void setPersistentParameter(String name, String value) {
		persistentParameters.put(name, value);
	}

	/**
	 * Metoda briše parametar pod ključem <b>name</b> iz {@link Map}e stalnih
	 * parametara
	 *
	 * @param name
	 *            ključ parametra koji se briše iz {@link Map}e stalnih
	 *            parametara
	 */
	public void removePersistentParameter(String name) {
		persistentParameters.remove(name);
	}

	/**
	 * Metoda koja iz {@link Map} privremenih parametara dohvaća parametar pod
	 * ključem <b>name</b> ili vraća <code>null</code> ukoliko takav ključ ne
	 * postoji unutar privremenih parametara
	 *
	 * @param name
	 *            ključ čija se vrijednost dohvaća iz {@link Map}e privremenih
	 *            parametara
	 * @return parametar pod ključem <b>name</b> ili <code>null</code> ukoliko
	 *         takav ključ ne postoji unutar privremenih parametara
	 */
	public String getTemporaryParameter(String name) {
		return temporaryParameters.get(name);
	}

	/**
	 * Metoda koja dohvaća sve ključeve parametara iz {@link Map} privremenih
	 * parametara
	 *
	 * @return sve ključeve parametara iz {@link Map} privremenih parametara
	 */
	public Set<String> getTemporaryParameterNames() {
		return Collections.unmodifiableSet(temporaryParameters.keySet());
	}

	/**
	 * Metoda koja u {@link Map}u privremenih parametara dodaje unos sa ključem
	 * <b>name</b> i vrijednošću <b>value</b>. Moguće je da takav unos već
	 * postoji pa se samo vrijednost postavlja pod tim ključem, a stara
	 * vrijednost se briše
	 *
	 * @param name
	 *            ključ pod kojim se dodaje parametar
	 * @param value
	 *            vrijednost parametra
	 */
	public void setTemporaryParameter(String name, String value) {
		temporaryParameters.put(name, value);
	}

	/**
	 * Metoda briše parametar pod ključem <b>name</b> iz {@link Map}e
	 * privremenih parametara
	 *
	 * @param name
	 *            ključ parametra koji se briše iz {@link Map}e privremenih
	 *            parametara
	 */
	public void removeTemporaryParameter(String name) {
		temporaryParameters.remove(name);
	}

	/**
	 * Metoda koja upisuje podate predane kao polje okteta <b>data</b> u interni
	 * primjerak razreda {@link OutputStream} ovog primjerka razreda. Metoda
	 * također generira zaglavlje odgovora ukoliko ono već nije izgenerirano
	 *
	 * @param data
	 *            polje okteta koje se upisuje u interni primjerak razreda
	 *            {@link OutputStream} ovog primjerka razreda.
	 * @return ovaj primjerak ovog razreda
	 * @throws IOException
	 *             ukoliko nije moguće pisati u interni izlazni tok okteta
	 */
	public RequestContext write(byte[] data) throws IOException {
		if (data == null) {
			throw new IllegalArgumentException("Predano polje okteta je referenca na null");
		}

		if (!headerGenerated) {
			generateHeader(data.length);
		}

		outputStream.write(data);
		outputStream.flush();
		return this;
	}

	/**
	 * Pomoćna metoda koja generira zaglavlje. Metoda prima veličinu podataka
	 * unutar teksta odgovora <b>fileSize</b> te ukoliko je {@link #fullContent}
	 * postavljen dodaje i dio zaglavlja "Content-Length"
	 *
	 * @param fileSize
	 *            veličina podataka unutar teksta odgovora
	 * @throws IOException
	 *             ukoliko nije moguće pisati u interni izlazni tok okteta
	 */
	private void generateHeader(int fileSize) throws IOException {
		// postavi zastavicu
		headerGenerated = true;

		StringJoiner sj = new StringJoiner(REQUEST_NEWLINE, EMPTY, REQUEST_NEWLINE + REQUEST_NEWLINE);
		sj.add(ServerUtil.generateFirstLineOfResponse("HTTP/1.1", statusCode, statusText));
		sj.add("Content-Type: " + mimeType + (mimeType.startsWith("text/") ? "; charset=" + encoding : EMPTY));
		if (fullContent) {
			sj.add("Content-Length: " + fileSize);
		}

		generateCookieDescriptions(sj);

		outputStream.write(sj.toString().getBytes(HEADER_CHARSET));
		outputStream.flush();
	}

	/**
	 * Pomoćna metoda koja dodaje sve cookie modelirane razredom
	 * {@link RCCookie} iz {@link #outputCookies} u zaglavlje odgovora
	 *
	 * @param sj
	 *            primjerak razreda {@link StringJoiner} unutar kojeg se upisuju
	 *            svi cookiji modelirani razredom {@link RCCookie}
	 */
	private void generateCookieDescriptions(StringJoiner sj) {
		for (RCCookie cookie : outputCookies) {
			sj.add(cookie.toString());
		}
	}

	/**
	 * Metoda koja upisuje podatke predane kao primjerak razreda {@link String}
	 * <b>text</b> u interni primjerak razreda {@link OutputStream} ovog
	 * primjerka razreda. Metoda također generira zaglavlje odgovora ukoliko ono
	 * već nije izgenerirano
	 *
	 * @param text
	 *            primjerak razreda {@link String} koji se upisuje u interni
	 *            primjerak razreda {@link OutputStream} ovog primjerka razreda.
	 * @return ovaj primjerak ovog razreda
	 * @throws IOException
	 *             ukoliko nije moguće pisati u interni izlazni tok okteta
	 */
	public RequestContext write(String text) throws IOException {
		if (!headerGenerated) {
			charset = Charset.forName(encoding);
		}
		return write(text.getBytes(charset));
	}

	/**
	 * Razred koji predstavlja jedan cookie iz tehnologija cookia o kojima
	 * možete više pročitati na
	 * <a href = "https://en.wikipedia.org/wiki/HTTP_cookie">linku</a>
	 * 
	 * @author Davor Češljaš
	 */
	public static class RCCookie {

		/**
		 * Konstanta koja predstavlja niz znakova koji se dodaje ako je cookie
		 * HttpOnly.
		 */
		private static final String HTTP_ONLY = "HttpOnly";

		/** Članska varijabla koja predstavlja naziv parametra cookia */
		private String name;

		/** Članska varijabla koja predstavlja vrijednost parametra cookia */
		private String value;

		/** Članska varijabla koja predstavlja domenu za koji vrijedi cookie */
		private String domain;

		/** Članska varijabla koja predstavlja putanju za koji vrijedi cookie */
		private String path;

		/** Članska varijabla koja predstavlja maksimalnu starost cookia */
		private Integer maxAge;

		/**
		 * Članska varijabla koja predstavlja indikator je li cookie HttpOnly
		 */
		private boolean httpOnly = false;

		/**
		 * Konstruktor koji inicijalizira primjerak ovog razreda. Unutar
		 * konstruktora samo se spremaju reference u privatne članske varijable
		 *
		 * @param name
		 *            varijabla koja predstavlja naziv parametra cookia
		 * @param value
		 *            varijabla koja predstavlja vrijednost parametra cookia
		 * @param maxAge
		 *            varijabla koja predstavlja maksimalnu starost cookia
		 * @param domain
		 *            varijabla koja predstavlja domenu za koji vrijedi cookie
		 * @param path
		 *            varijabla koja predstavlja putanju za koji vrijedi cookie
		 */
		public RCCookie(String name, String value, Integer maxAge, String domain, String path) {
			this.name = name;
			this.value = value;
			this.domain = domain;
			this.path = path;
			this.maxAge = maxAge;
		}

		/**
		 * Metoda koja dohvaća varijablu koja predstavlja naziv parametra cookia
		 *
		 * @return varijablu koja predstavlja naziv parametra cookia
		 */
		public String getName() {
			return name;
		}

		/**
		 * Metoda koja dohvaća varijablu koja predstavlja vrijednost parametra
		 * cookia
		 *
		 * @return varijablu koja predstavlja vrijednost parametra cookia
		 */
		public String getValue() {
			return value;
		}

		/**
		 * Metoda koja dohvaća varijablu koja predstavlja domenu za koji vrijedi
		 * cookie
		 *
		 * @return varijablu koja predstavlja domenu za koji vrijedi cookie
		 */
		public String getDomain() {
			return domain;
		}

		/**
		 * Metoda koja dohvaća varijablu koja predstavlja putanju za koji
		 * vrijedi cookie.
		 *
		 * @return varijablu koja predstavlja putanju za koji vrijedi cookie.
		 *
		 */
		public String getPath() {
			return path;
		}

		/**
		 * Metoda koja dohvaća varijablu koja predstavlja maksimalnu starost
		 * cookia
		 *
		 * @return varijablu koja predstavlja maksimalnu starost cookia
		 *
		 */
		public Integer getMaxAge() {
			return maxAge;
		}

		/**
		 * Metoda koja postavlja indikator je li cookie HttpOnly na vrijednost
		 * <b>httpOnly</b>
		 *
		 * @param httpOnly
		 *            nova vrijednost indikatora je li cookie HttpOnly
		 */
		public void setHttpOnly(boolean httpOnly) {
			this.httpOnly = httpOnly;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((domain == null) ? 0 : domain.hashCode());
			result = prime * result + (httpOnly ? 1231 : 1237);
			result = prime * result + ((maxAge == null) ? 0 : maxAge.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((path == null) ? 0 : path.hashCode());
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RCCookie other = (RCCookie) obj;
			if (domain == null) {
				if (other.domain != null)
					return false;
			} else if (!domain.equals(other.domain))
				return false;
			if (httpOnly != other.httpOnly)
				return false;
			if (maxAge == null) {
				if (other.maxAge != null)
					return false;
			} else if (!maxAge.equals(other.maxAge))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (path == null) {
				if (other.path != null)
					return false;
			} else if (!path.equals(other.path))
				return false;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return String.format("Set-Cookie: %s=\"%s\";", name, value)
					+ (domain != null ? String.format(" Domain=%s;", domain) : EMPTY)
					+ (path != null ? String.format(" Path=%s;", path) : EMPTY)
					+ (maxAge != null ? String.format(" Max-Age=%d;", maxAge) : EMPTY) + (httpOnly ? HTTP_ONLY : EMPTY);
		}
	}
}
