package hr.fer.zemris.java.webserver;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

import hr.fer.zemris.java.custom.scripting.exec.SmartScriptEngine;
import hr.fer.zemris.java.custom.scripting.parser.SmartScriptParser;
import hr.fer.zemris.java.webserver.RequestContext.RCCookie;

/**
 * Razred koji predstavlja poslužitelj. Ovaj poslužitelj ima svega nekoliko
 * funkcionalnsoti, a uglavnom služi za demonstarcijske potrebe raznih pristupa.
 * Tako ovdje postoje:
 * <ul>
 * <li>klasičan dohvat resursa sa poslužitelja, gdje upisana adresa odgovara
 * resursu u javno ponuđenom direktoriju</li>
 * <li>čitanje skripti i parsiranje tih skripti sa primjerkom razreda
 * {@link SmartScriptEngine}</li>
 * <li>posluživanje korisnika na način da korisnikova adresa odgovara adresi
 * primjerka razreda koji mora obaviti pojedinu funkcionalnost. Ovdje je razred
 * koji vrši obradu stvoren na jednom mjestu te se samo poziva njegova
 * metoda.</li>
 * <li>posluživanje korisnika na način da korisnikova adresa odgovara adresi
 * primjerka razreda koji je potrebno stvoriti te potom izvršiti njegov
 * posao.</li>
 * </ul>
 * 
 * Za zadnja dva pristupa koriste se razredi koji implementiraju sučelje
 * {@link IWebWorker}. Primjerci tih razreda stvaraju se dinamički (ili prilikom
 * inicijalizacije poslužitelja ili prilikom obrade zahtjeva).
 * <p>
 * Ovaj poslužitelj ima i podršku za tehnologiju cookia o kojoj možete više
 * pročitati na <a href = "https://en.wikipedia.org/wiki/HTTP_cookie">linku</a>.
 * Cookiji su ovdje oblikovani razredom {@link RCCookie}. Konkretno ovaj
 * poslužitelj predaje cookije sesije tako da neki parametri mogu dolaziti i iz
 * starijih zahtjeva istog korisnika
 * </p>
 * 
 * @see SmartScriptEngine
 * @see IWebWorker
 * @see RCCookie
 * 
 * @author Davor Češljaš
 */
public class SmartHttpServer {

	/**
	 * Konstanta koja predstavlja dio ključa u postavkama poslužitelja koji se
	 * koristi za sam proslužitelj.
	 */
	private static final String SERVER = "server.";

	/**
	 * Konstanta koja predstavlja ključ adrese na kojoj se nalazi poslužitelj
	 */
	private static final String SERVER_ADDRESS = SERVER + "address";

	/**
	 * Konstanta koja predstavlja ključ vrata na kojima se nalazi poslužitelj
	 */
	private static final String SERVER_PORT = SERVER + "port";

	/**
	 * Konstanta koja predstavlja ključ za broj dretvi radinika na ovom
	 * poslužitelju
	 */
	private static final String SERVER_WORKER_THREADS = SERVER + "workerThreads";

	/**
	 * Konstanta koja predstavlja ključ za putanju do direktorija unutar kojeg
	 * se nalaze svi dostupni resursi
	 */
	private static final String SERVER_DOCUMENT_ROOT = SERVER + "documentRoot";

	/**
	 * Konstanta koja predstavlja ključ za putanju do konfiguracijske datoteke
	 * za mime-tipove
	 */
	private static final String SERVER_MIME_CONFIG = SERVER + "mimeConfig";

	/**
	 * Konstanta koja predstavlja ključ za trajanje važenja cookia jedne sesije
	 */
	private static final String SESSION_TIMEOUT = "session.timeout";

	/**
	 * Konstanta koja predstavlja ključ za putanju do konfiguracijske datoteke
	 * radnika za obradu određemih zahtjeva
	 */
	private static final String SERVER_WORKERS = SERVER + "workers";

	/**
	 * Konstanta koja predstavlja predpostavljeni mime-tip
	 * "application/octet-stream"
	 */
	private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

	/**
	 * Konstanta koja predstavlja ekstenzije skripti koje se izvršavaju
	 * primjerkom razreda {@link SmartScriptEngine}
	 */
	private static final String SMART_SCRIPT_EXTENSION = "smscr";

	/**
	 * Konstanta koja predstavlja dogovorenu putanju na kojoj se (ne fizički)
	 * nalaze svi radnici koji implementiraju sučelje {@link IWebWorker}
	 */
	private static final String WORKERS_PATH = "/ext/";

	/**
	 * Konstanta koja predstavlja direktorij do privatnih skripti (onih koje
	 * korisnik ne može direktno izvesti)
	 */
	private static final String PRIVATE_SCRIPTS_PATH = "/private/";

	/**
	 * Konstanta koja predstavlja naziv paketa u kojem se nalaze svi radnici
	 * oblikovani sučeljem {@link IWebWorker}
	 */
	private static final String WORKERS_PACKAGE = "hr.fer.zemris.java.webserver.workers.";

	/** Konstanta koja predstavlja veličinu Session ID-a */
	private static final int SID_SIZE = 20;

	/**
	 * Konstanta koja predstavlja broj znakova koji se mogu naći u Session ID-u
	 */
	private static final int SID_RANGE = 26;

	/**
	 * Konstanta koja predstavlja prvi znak koji ulazi u raspon znakova za
	 * Session ID
	 */
	private static final int SID_FIRST = 65;

	/** Konstanta koja predstavlja ključ cookia koji sadrži Session ID */
	private static final String SID_COOKIE_KEY = "sid";

	/**
	 * Članska varijabla koja predstavlja adresu na kojoj se nalazi ova
	 * poslužitelj
	 */
	private String address;

	/**
	 * Članska varijabla koja predstavlja vrata na kojima se nalazi ovaj
	 * poslužitelj
	 */
	private int port;

	/**
	 * Članska varijabla koja predstavlja broj dretvi radnika koje je potrebno
	 * izraditi
	 */
	private int workerThreads;

	/**
	 * Članska varijabla koja predstavlja vrijeme trajanje jedne cookie sesije
	 */
	private int sessionTimeout;

	/**
	 * Članska varijabla koja predstavlja {@link Map} svih podržanih mime-tipova
	 */
	private Map<String, String> mimeTypes = new HashMap<>();

	/**
	 * Članska varijabla koja predstavlja referencu na dretvu radnika koji
	 * prihvaća zahtjeve od korisnika
	 */
	private ServerThread serverThread;

	/**
	 * Članska varijabla koja predstavlja thread pool (bazen dretvi) radnika
	 * koji poslužuju klijente
	 */
	private ExecutorService threadPool;

	/**
	 * Članska varijabla koja predstavlja putanju do javnog direktorija
	 * (direktorija koji jedino korisnik "vidi")
	 */
	private Path documentRoot;

	/**
	 * Članska varijabla koja predstavlja {@link Map} svih radnika koji stvaraju
	 * odgovor za korisnika , ukoliko se zahtjev pošalje na adresu koja je ključ
	 * ove mape
	 */
	private Map<String, IWebWorker> workersMap = new HashMap<>();

	/**
	 * Članska varijabla koja predstavlja {@link Map} svih aktivnih cookie
	 * sesija
	 */
	private Map<String, SessionMapEntry> sessions = new ConcurrentHashMap<String, SmartHttpServer.SessionMapEntry>();

	/**
	 * Članska varijabla koja predstavlja primjerak razreda {@link Random} koji
	 * se koristi za gradnju Session ID -a
	 */
	private Random sessionRandom = new Random();

	/**
	 * Konstruktor koji inicijalizira primjerak ovog razreda. Konstruktoru se
	 * kao jedini parametar šalje putanja (u obliku primjerka razreda
	 * {@link String}) do konfiguracijske datoteke poslužitelja
	 *
	 * @param configFileName
	 *            putanja (u obliku primjerka razreda {@link String}) do
	 *            konfiguracijske datoteke poslužitelja
	 * @throws IOException
	 *             ukoliko se ne može pročitati konfiguracijska datoteka
	 */
	public SmartHttpServer(String configFileName) throws IOException {
		Properties serverProperties = new Properties();
		serverProperties.load(Files.newBufferedReader(Paths.get(configFileName)));

		this.address = serverProperties.getProperty(SERVER_ADDRESS);
		this.port = Integer.parseInt(serverProperties.getProperty(SERVER_PORT));
		this.workerThreads = Integer.parseInt(serverProperties.getProperty(SERVER_WORKER_THREADS));
		this.sessionTimeout = Integer.parseInt(serverProperties.getProperty(SESSION_TIMEOUT));

		loadMimeTypes(serverProperties.getProperty(SERVER_MIME_CONFIG));

		loadWorkers(serverProperties.getProperty(SERVER_WORKERS));

		this.documentRoot = Paths.get(serverProperties.getProperty(SERVER_DOCUMENT_ROOT)).toRealPath();
		this.serverThread = new ServerThread();

	}

	/**
	 * Pomoćna metoda koja učitava sve radnike koji se koriste za stvaranja
	 * odgovora klijentima koji pošalju zahtjeva na njihovu adresu (ključ
	 * {@link Map} {@link #workersMap})
	 * 
	 *
	 * @param filePath
	 *            primjerak razreda {@link String} koji predstavlja putanju do
	 *            konfiguracijske datoteke radnika
	 * @throws IOException
	 *             ukoliko se ne može čitati iz konfiguracijske datoteke
	 */
	private void loadWorkers(String filePath) throws IOException {
		BiConsumer<Object, Object> consumer = (key, value) -> {
			if (workersMap.containsKey(key)) {
				throw new IllegalArgumentException(
						String.format("Svojstvo pod ključem '%s' već postoji", key.toString()));
			}

			try {
				Class<?> referenceToClass = this.getClass().getClassLoader().loadClass(value.toString());
				workersMap.put((String) key, (IWebWorker) referenceToClass.newInstance());
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("Ne mogu pronaći razred: " + value.toString());
			} catch (InstantiationException e) {
				throw new IllegalArgumentException("Ne mogu stvoriti primjerak razreda: " + value.toString());
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException(
						"Nemam pristup razredu ili konstruktoru razreda: " + value.toString());
			}

		};

		ServerUtil.loadProperties(filePath, consumer);
	}

	/**
	 * Pomoćna metoda koja učitava sve mime-tipove koji podržava ovaj
	 * poslužitelj, a koji su mapirani na ekstenzije datoteka
	 * 
	 *
	 * @param filePath
	 *            primjerak razreda {@link String} koji predstavlja putanju do
	 *            konfiguracijske datoteke mime-tipova
	 * @throws IOException
	 *             ukoliko se ne može čitati iz konfiguracijske datoteke
	 */
	private void loadMimeTypes(String filePath) throws IOException {
		ServerUtil.loadProperties(filePath, (key, value) -> mimeTypes.put((String) key, (String) value));
	}

	/**
	 * Metoda čijim pozivom započinje rad ovog poslužitelja
	 */
	protected synchronized void start() {
		if (serverThread.isAlive()) {
			return;
		}

		threadPool = Executors.newFixedThreadPool(workerThreads);
		serverThread.start();

		startCleaning();
	}

	/**
	 * Pomoćna metoda koja inicijalizira sakupljača smeća oblikovanog razredom
	 * {@link ServerGarbageCollector} sa parametrom {@link #sessions}, predaje
	 * ga demonskoj dretvi i započinje rad te dretve
	 */
	private void startCleaning() {
		Thread cleaner = new Thread(new ServerGarbageCollector(sessions));
		cleaner.setDaemon(true);

		cleaner.start();
	}

	/**
	 * Metoda koja zaustavlja rad ovog poslužitelja
	 */
	protected synchronized void stop() {
		serverThread.interrupt();
		threadPool.shutdown();
	}

	/**
	 * Zaštićeni razred koji nasljeđuje razred {@link Thread}. Ovaj razred
	 * predstavlja dretvu koja čeka na zahtjeve korisnika, te kada ga dobije
	 * delegira posao primjerku razreda {@link ClientWorker} koji onda taj
	 * zahtjev obrađuje
	 * 
	 * @see ClientWorker
	 * @see Thread
	 * 
	 * @author Davor Češljaš
	 */
	protected class ServerThread extends Thread {

		@Override
		public void run() {
			try (ServerSocket serverSocket = new ServerSocket(port)) {
				while (true) {
					Socket client = serverSocket.accept();
					ClientWorker clientWorker = new ClientWorker(client);
					threadPool.submit(clientWorker);
				}
			} catch (IOException e) {
				System.out.println("Server se ne može slušati na portu: " + port);
				System.out.println("Zatvaram poslužitelja...");
				System.exit(-1);
			}
		}
	}

	/**
	 * Privatni statički razred koji implementira sučelja {@link Runnable} i
	 * {@link IDispatcher}. Primjerci ovog razreda vrše obradu zahtjeva
	 * klijenata i slanje odgovora na zahtjev.
	 * 
	 * @see Runnable
	 * @see IDispatcher
	 * 
	 * @author Davor Češljaš
	 */
	private class ClientWorker implements Runnable, IDispatcher {

		/**
		 * Članska varijabla koja predsavlja primjerak razreda {@link Socket}
		 * kojim je opisana veza između klijenta i poslužitelja
		 */
		private Socket csocket;

		/**
		 * Članska varijabla koja predstavlja ulazni niz okteta iz kojeg se čita
		 * zahtjev
		 */
		private PushbackInputStream istream;

		/**
		 * Članska varijabla koja predstavlja izlazni niz okteta u koji se piše
		 * odgovor
		 */
		private OutputStream ostream;

		/** Članska varijabla koja predstavlja verziju HTTP protokola */
		private String version;

		/** Članska varijabla koja predstavlja HTTP metodu zahtjeva */
		private String method;

		/**
		 * Članska varijabla koja predstavlja {@link Map} svih parametara koje
		 * je korisnik predao
		 */
		private Map<String, String> params = new HashMap<String, String>();

		/**
		 * Članska varijabla koja predstavlja {@link Map} privremenih parametara
		 * sesije (brišu se čim se pošalje odgovor)
		 */
		private Map<String, String> tempParams = new HashMap<String, String>();

		/**
		 * Članska varijabla koja predstavlja {@link Map} stalnih parametara
		 * sesije (brišu se tek kada istekne cookie sesija)
		 */
		private Map<String, String> persParams = new HashMap<String, String>();

		/**
		 * Članska varijabla koja predstavlja {@link List} cookia oblikovanih
		 * razredom {@link RCCookie} koji se šalju korisniku u odgovoru
		 */
		private List<RCCookie> outputCookies = new ArrayList<RequestContext.RCCookie>();

		/**
		 * Članska varijabla koja predstavlja kontekst zahtjeva i odgovora na
		 * zahtjev.
		 */
		private RequestContext context;

		/**
		 * Članska varijabla koja predstavlja parsiranu putanju koju je korisnik
		 * zatražio, a koja je predstavljena primjerkom sučelja {@link Path}
		 */
		private Path parsedPath;

		/** Članska varijabla koja predstavlja Session ID */
		private String SID;

		/**
		 * Konstruktor koji inicijalizira primjerak ovog razreda. Konstruktor
		 * interno sprema predanu referencu na primjerak razreda {@link Socket}
		 * <b>csocket</b>
		 *
		 * @param csocket
		 *            primjerak razreda {@link Socket} kojim se modelira veza
		 *            između klijenta i poslužitelja
		 */
		public ClientWorker(Socket csocket) {
			this.csocket = csocket;
		}

		@Override
		public void run() {
			List<String> request = null;
			try {
				istream = new PushbackInputStream(csocket.getInputStream());
				ostream = csocket.getOutputStream();

				request = readRequest();
				if (request.isEmpty()) {
					sendErrorResponse(ServerUtil.DEFAULT_VERISON, ServerUtil.BAD_REQUEST_STATUS,
							ServerUtil.BAD_REQUEST_TEXT);
				}

				SessionMapEntry entry = checkSession(request);
				persParams = entry.map;

				String firstLine = request.get(0);
				String filepath = extractFirstLineParams(firstLine);

				internalDispatchRequest(filepath, true);
			} catch (IOException e) {
				System.out.println("Klijent je prekinuo konekciju.");
				return;
			} catch (IllegalArgumentException e) {
				System.out.println("Klijent je poslao neispravan zahtjev, šaljem poruku o pogrešci...");
				return;
			} catch (Exception e) {
				System.out.println("Ne mogu poslati odgovor");
				return;
			} finally {
				try {
					istream.close();
					ostream.close();
				} catch (IOException ignorable) {
				}
			}
		}

		/**
		 * Pomoćna metoda koja čita zahtjev korisnika i iz njega gradi
		 * {@link List} primjeraka razreda {@link String} koji predstavljaju
		 * linije zahtjeva. Za ovo čitanje koristi se metoda
		 * {@link ServerUtil#readRequest(PushbackInputStream)}
		 *
		 * @return {@link List} primjeraka razreda {@link String} koji
		 *         predstavljaju linije zahtjeva
		 * @throws IOException
		 *             Ukoliko nije moguće pročitati zahtjev od klijenta
		 */
		private List<String> readRequest() throws IOException {
			// zahtjevi nemaju specijalnih znakova tako da se mogu
			// čitati sa bilo kojim charsetom, pa tako i sa defaultnim
			String string = new String(ServerUtil.readRequest(istream));
			return new ArrayList<>(Arrays.asList(string.split("[\r\n]+")));
		}

		/**
		 * Pomoćna metoda koja provjerava unutar predanog parametra
		 * <b>request</b> postoji li cookie sa ključem {@value #SID_COOKIE_KEY}.
		 * Te ukoliko postoji i ukoliko je primjerak u {@link Map}
		 * {@link SmartHttpServer#sessions} važeći dohvaća primjerak razreda
		 * {@link SessionMapEntry}. Ukoliko to nije točno ili ukoliko uopće
		 * takav cookie ne postoji stvara se novi SID i novi primjerak razreda
		 * {@link SessionMapEntry} te se on vraća
		 *
		 * @param request
		 *            {@link List} svih linija predstavljenih primjercima
		 *            razreda {@link String} koje je korisnik poslao
		 * @return primjerak razreda {@link SessionMapEntry}, a ovisno o ishodu
		 *         gore opisanog scenarija
		 */
		private SessionMapEntry checkSession(List<String> request) {
			String host = null;
			List<RCCookie> cookies = null;

			for (String line : request) {
				if (line.startsWith("Host:")) {
					host = ServerUtil.parseHost(line);
				} else if (line.startsWith("Cookie:")) {
					cookies = ServerUtil.parseCookies(line);
				}
			}

			host = host == null ? address : host;

			String sidCandidate = findSIDCandidate(cookies);

			return findSessionMapEntry(sidCandidate, host);
		}

		/**
		 * Pomoćna metoda koja u unutar {@link List} svih parsiranih cookija
		 * koji su primjerci razreda {@link RCCookie} <b>cookies</b> pokušava
		 * pronaći onaj sa ključem {@value #SID_COOKIE_KEY} te vraća njegovu
		 * vrijednost ukoliko je nađe ili <code>null</code> ukoliko takvog
		 * cookia nema
		 *
		 * @param cookies
		 *            {@link List} svih parsiranih cookija koji su primjerci
		 *            razreda {@link RCCookie}
		 * @return pronađenu vrijednost cookia sa ključem
		 *         {@value #SID_COOKIE_KEY} ili <code>null</code> ukoliko cookie
		 *         pod tim ključem ne postoji
		 */
		private String findSIDCandidate(List<RCCookie> cookies) {
			if (cookies == null) {
				return null;
			}

			String sidCandidate = null;

			for (RCCookie cookie : cookies) {
				if (cookie.getName().equals(SID_COOKIE_KEY)) {
					sidCandidate = cookie.getValue();
				}
			}

			return sidCandidate;
		}

		/**
		 * Pomoćna metoda koja iz predanih parametara pronalazi već postojeći
		 * primjerak razreda {@link SessionMapEntry} ili stvara novi.
		 *
		 * @param sidCandidate
		 *            kandidat za session ID parsiran iz cookia
		 * @param host
		 *            host na koji je klijent poslao zahtjev ili
		 *            {@link SmartHttpServer#address}
		 * @return primjerak razreda {@link SessionMapEntry} (novi ili već
		 *         postojeći)
		 */
		private SessionMapEntry findSessionMapEntry(String sidCandidate, String host) {
			SessionMapEntry entry = null;
			if (sidCandidate == null || (entry = sessions.get(sidCandidate)) == null) {
				return generateSessionMapEntry(host);
			}

			if (entry.validUntil >= Calendar.getInstance().getTimeInMillis()) {
				SID = sidCandidate;
				entry.validUntil = getSessionTimeoutInMilis();
				return entry;
			}

			sessions.remove(SID);
			return generateSessionMapEntry(host);
		}

		/**
		 * Pomoćna metoda koja na temelju predanog parametra <b>host</b> stvara
		 * novi primjerak razreda {@link SessionMapEntry}
		 *
		 * @param host
		 *            host na koji je klijent poslao zahtjev ili
		 *            {@link SmartHttpServer#address}
		 * @return novi primjerak razreda {@link SessionMapEntry}
		 */
		private SessionMapEntry generateSessionMapEntry(String host) {
			SID = generateSID();

			RCCookie sidCookie = new RCCookie(SID_COOKIE_KEY, SID, null, host, "/");
			sidCookie.setHttpOnly(true);
			outputCookies.add(sidCookie);

			SessionMapEntry entry = new SessionMapEntry(SID, getSessionTimeoutInMilis());
			sessions.put(SID, entry);

			return entry;
		}

		/**
		 * Pomoćna metoda koja generira novi Session ID
		 *
		 * @return novi Session ID
		 */
		private String generateSID() {
			while (true) {
				StringBuilder sb = new StringBuilder();

				for (int i = 0; i < SID_SIZE; i++) {
					int rand = SID_FIRST + sessionRandom.nextInt(SID_RANGE);
					sb.append((char) rand);
				}

				String newSID = sb.toString();

				if (!sessions.containsKey(newSID)) {
					return newSID;
				}
			}
		}

		/**
		 * Metoda koja vraća vrijeme trajanja novog Session ID-a
		 *
		 * @return vrijeme trajanja novog Session ID-a
		 */
		private long getSessionTimeoutInMilis() {
			return Calendar.getInstance().getTimeInMillis() + sessionTimeout * 1000;
		}

		/**
		 * Pomoćna metoda koja ekstrahira sve parametre prve linije zahtjeva
		 * (HTTP metoda, HTTP verzija i putanja)
		 *
		 * @param firstLine
		 *            primjerak razreda {@link String} koji predstavlja prvu
		 *            linija zahtjeva
		 * @return ekstrahiranu putanju koja je oblikovana primjerkom razreda
		 *         {@link String}
		 * @throws IOException
		 *             ukoliko se dogodi pogreška, a koja se ne može poslati,
		 *             jer se ne može pisati u izlazni tok podataka
		 */
		private String extractFirstLineParams(String firstLine) throws IOException {
			String[] splittedLine = firstLine.split(" ");
			if (splittedLine.length != 3) {
				sendErrorResponse(ServerUtil.DEFAULT_VERISON, ServerUtil.BAD_REQUEST_STATUS,
						ServerUtil.BAD_REQUEST_TEXT);
			}

			method = splittedLine[0].trim();
			version = splittedLine[2].trim();
			if (!method.equals(ServerUtil.GET_METHOD) || !ServerUtil.VERSIONS.contains(version)) {
				sendErrorResponse(ServerUtil.DEFAULT_VERISON, ServerUtil.BAD_REQUEST_STATUS,
						ServerUtil.BAD_REQUEST_TEXT);
			}

			return parsePath(splittedLine[1]);
		}

		/**
		 * Pomoćna metoda koja prasira putanju <b>requestedPath</b>
		 * rastavljajući je na parametre(ukoliko oni postoje) i samu putanju
		 *
		 * @param requestedPath
		 *            putanja do resursa koja opcionalno uključuje i parametre
		 * @return ekstrahirana putanja koja je primjerak razreda {@link String}
		 *         (dakle bez parametara)
		 * @throws IOException
		 *             ukoliko se dogodi pogreška, a koja se ne može poslati,
		 *             jer se ne može pisati u izlazni tok podataka
		 */
		private String parsePath(String requestedPath) throws IOException {
			String[] splittedPath = requestedPath.split("\\?");
			if (splittedPath.length < 1 || splittedPath.length > 2) {
				sendErrorResponse(version, ServerUtil.BAD_REQUEST_STATUS, ServerUtil.BAD_REQUEST_TEXT);
			}

			String pathString = splittedPath[0].trim();

			checkRequestPath(pathString);

			if (splittedPath.length == 2) {
				parseParameters(splittedPath[1]);
			}

			return pathString;
		}

		/**
		 * Pomoćna metoda koja provjerava ispravnost zatražene putanje
		 * <b>pathString</b>. Ukoliko ona nije dobro definirana metoda baca
		 * {@link IllegalArgumentException} i pri tome šalje klijentu poruku o
		 * pogrešci
		 *
		 * @param pathString
		 *            primjerak razreda {@link String} koji predstavlja putanju
		 *            do zatraženog resurasa
		 * @throws IOException
		 *             ukoliko se dogodi pogreška, a koja se ne može poslati,
		 *             jer se ne može pisati u izlazni tok podataka
		 */
		private void checkRequestPath(String pathString) throws IOException {
			if (workersMap.containsKey(pathString) || pathString.startsWith(WORKERS_PATH)
					|| pathString.startsWith(WORKERS_PATH)) {
				return;
			}
			// nema potrebe instancirati Path ako se radi sa workerom
			parsedPath = Paths.get(documentRoot.toString(), pathString).toAbsolutePath();
			if (!parsedPath.startsWith(documentRoot)) {
				sendErrorResponse(version, ServerUtil.FORBIDDEN_STATUS, ServerUtil.FORBIDDEN_TEXT);
			}
			if (!Files.isRegularFile(parsedPath) || !Files.isReadable(parsedPath)) {
				sendErrorResponse(version, ServerUtil.NOT_FOUND_STATUS, ServerUtil.NOT_FOUND_TEXT);
			}
		}

		/**
		 * Pomoćna metoda koja iz predanog primjerka razreda {@link String}
		 * <b>parameters</b> parsira parametre koje je klijent poslao u
		 * zahtjevu. Ukoliko parametar nema ključ i vrijednost, šalje se poruka
		 * o pogrešci status-a {@link ServerUtil#BAD_REQUEST_STATUS} i baca se
		 * iznimka
		 *
		 * @param parameters
		 *            primjerak razreda {@link String} iz kojeg se vade
		 *            paramteri
		 * @throws IOException
		 *             ukoliko se dogodi pogreška, a koja se ne može poslati,
		 *             jer se ne može pisati u izlazni tok podataka
		 */
		private void parseParameters(String parameters) throws IOException {
			for (String param : parameters.split("&")) {
				String[] keyValue = param.split("=");
				if (keyValue.length != 2) {
					sendErrorResponse(version, ServerUtil.BAD_REQUEST_STATUS, ServerUtil.BAD_REQUEST_TEXT);
				}

				this.params.put(keyValue[0], keyValue[1]);
			}
		}

		/**
		 * Pomoćna metoda koja šalje poruku o pogrešci klijentu. Koja se poruka
		 * i koji status šalju definirano je preko parametara <b>statusText</b>
		 * i <b>statusCode</b>.
		 *
		 * @param version
		 *            verzija protokola HTTP
		 * @param statusCode
		 *            statusni kod pogreške koji se šalje
		 * @param statusText
		 *            tekst uz statusni kod koji se šalje
		 * @throws IOException
		 *             ukoliko se dogodi pogreška, a koja se ne može poslati,
		 *             jer se ne može pisati u izlazni tok podataka
		 */
		private void sendErrorResponse(String version, int statusCode, String statusText) throws IOException {
			ServerUtil.sendErrorResponse(version, statusCode, statusText, ostream);
			throw new IllegalArgumentException();
		}

		@Override
		public void dispatchRequest(String urlPath) throws Exception {
			internalDispatchRequest(urlPath, false);
		}

		/**
		 * Metoda koja se koristi za stvaranje odgovora na korisnikov zahtjev, a
		 * ovisno o tome kako je zahtjev poslan (vidi dokumentaciju poslužitelja
		 * {@link SmartHttpServer} i sve moguće načine na koje se može stvoriti
		 * odgovor)
		 *
		 * @param urlPath
		 *            putanja do resursa koju je klijent zatražio
		 * @param directCall
		 *            zastavica koja ispituje je li poziv ove metode direktan
		 *            ili je ona pozvana iz metode
		 *            {@link #dispatchRequest(String)}
		 * @throws Exception
		 *             iznimka koja nastaje u slučaju pogreške, a ovisno o tipu
		 *             pogreške
		 */
		public void internalDispatchRequest(String urlPath, boolean directCall) throws Exception {
			if (context == null) {
				context = new RequestContext(tempParams, this, ostream, params, persParams, outputCookies);
			}

			createResponse(urlPath, directCall);
		}

		/**
		 * Pomoćna metoda koja odlučuje koji od u pristupa će se primjeniti za
		 * odgovor korisniku (vidi dokumentaciju poslužitelja
		 * {@link SmartHttpServer} i sve moguće načine na koje se može stvoriti
		 * odgovor)
		 *
		 * @param urlPath
		 *            putanja do resursa koju je klijent zatražio
		 * @param directCall
		 *            zastavica koja ispituje je li poziv ove metode direktan
		 *            ili je ona pozvana iz metode
		 *            {@link #dispatchRequest(String)}
		 * @throws Exception
		 *             iznimka koja nastaje u slučaju pogreške, a ovisno o tipu
		 *             pogreške
		 */
		private void createResponse(String urlPath, boolean directCall) throws Exception {
			if (urlPath.startsWith(PRIVATE_SCRIPTS_PATH) && directCall) {
				sendErrorResponse(version, ServerUtil.NOT_FOUND_STATUS, ServerUtil.NOT_FOUND_TEXT);
			}

			String extension = extractExtension(urlPath);

			if (urlPath.startsWith(WORKERS_PATH)) {
				createWorkerResponse(urlPath);
			} else if (workersMap.containsKey(urlPath)) {
				workersMap.get(urlPath).processRequest(context);
			} else if (extension.equals(SMART_SCRIPT_EXTENSION)) {
				createScriptResponse(urlPath);
			} else {
				createNormalResponse(extension);
			}
		}

		/**
		 * Pomoćna metoda koja stvara radnika koji implementira sučelje
		 * {@link IWebWorker} i potom stvara odgovor pomoću tog radnika, a
		 * ovisno o konkretnom radniku
		 *
		 * @param urlPath
		 *            putanja do resursa koju je klijent zatražio
		 * @throws Exception
		 *             iznimka koja nastaje u slučaju pogreške, a ovisno o tipu
		 *             pogreške
		 */
		private void createWorkerResponse(String urlPath) throws IOException {
			String workerName = urlPath.replace(WORKERS_PATH, "");
			Class<?> referenceToClass;
			try {
				referenceToClass = this.getClass().getClassLoader().loadClass(WORKERS_PACKAGE + workerName);
				((IWebWorker) referenceToClass.newInstance()).processRequest(context);
			} catch (Exception e) {
				ServerUtil.sendBadRequestResponse(version, ostream);
			}
		}

		/**
		 * Pomoćna metoda koja se koristi za vađenje ekstenzije resursa koji je
		 * klijent zatražio
		 *
		 * @param fileName
		 *            {@link String} reprezentacija putanje do traženog resursa
		 * @return primjerak razreda {@link String} koji predstavlja ekstenziju
		 *         ili <code>null</code> ukoliko resur nema ekstenziju
		 */
		private String extractExtension(String fileName) {
			int dotIndex = fileName.lastIndexOf('.');
			if (dotIndex == -1) {
				return null;
			}

			return fileName.substring(dotIndex + 1, fileName.length()).trim();
		}

		/**
		 * Pomoćna metoda koja stvara primjerak razreda
		 * {@link SmartScriptEngine}, njemu predaje putanju do tražene skripte
		 * koju treba izvršiti, a koju je korisnik zatražio. Metoda potom
		 * generirani rezultat šalje klijentu kao odgovor
		 *
		 * @param filePath
		 *            putanja do skripte koju je korisnik zatražio
		 * @throws IOException
		 *             Ukoliko tražena skripta ne postoji ili se ne može čitati
		 *             ili pak nije moguće korisniku poslati odgovor
		 */
		private void createScriptResponse(String filePath) throws IOException {
			if (parsedPath == null) {
				parsedPath = Paths.get(documentRoot.toString(), filePath);
			}

			String docBody = ServerUtil.readFromDisk(parsedPath, null);
			SmartScriptParser parser = new SmartScriptParser(docBody);

			new SmartScriptEngine(parser.getDocumentNode(), context).execute();
		}

		/**
		 * Pomoćna metoda koja stvara tzv. normalni odgovor. Dakle, metoda
		 * upravo klijentu vraća ono što je on zatražio, u smislu da vraća
		 * resurs kojemu je putanja spremljena u člansku varijablu
		 * {@link #parsedPath} sa mime-tipom koji odgovara ekstenziji
		 * <b>extension</b>
		 *
		 * @param extension
		 *            ekstenzija zatraženog resursa
		 * @throws Exception
		 *             Ukoliko nije moguće vratiti odgovor korisniku ili nije
		 *             moguće pročitati traženi resurs ili traženi resur uopće
		 *             ne postoji na toj lokaciji
		 */
		private void createNormalResponse(String extension) throws Exception {
			context.setMimeType(findMimeType(extension));

			context.write(Files.readAllBytes(parsedPath));
		}

		/**
		 * Pomoćna metoda koja pronalazi mime-tip iz predane ekstenzije. Metoda
		 * pretražuje {@link Map}u {@link SmartHttpServer#mimeTypes} sa predanom
		 * ekstenzijom <b>extension</b>. Ukoliko takav mime-tip ne postoji u
		 * gore navedenoj mapi vraća se mime-tip
		 * {@link SmartHttpServer#DEFAULT_MIME_TYPE}
		 *
		 * @param extension
		 *            primjerak razreda {@link String} koji predstavlja
		 *            ekstenziju čiji se mime-tip traži
		 * @return ili mime-tip iz podržanih mime-tipova u {@link Map}i
		 *         {@link SmartHttpServer#mimeTypes} ili
		 *         {@link SmartHttpServer#DEFAULT_MIME_TYPE}
		 */
		private String findMimeType(String extension) {
			String mimeType = DEFAULT_MIME_TYPE;

			if (extension != null && mimeTypes.containsKey(extension)) {
				mimeType = mimeTypes.get(extension);
			}

			return mimeType;
		}
	}

	/**
	 * Paketski statički razred koji predstavlja jednu cookie sessiju. Razred
	 * interno sadrži Session ID, do kada vrijedi i mapu zapamćenih parametara
	 * za sessiju. Razred nudi samo jedan konstruktor
	 * {@link SessionMapEntry#SessionMapEntry(String, long)}.
	 * 
	 * @author Davor Češljaš
	 */
	static class SessionMapEntry {

		/** Članska varijabla koja predstavlja Session ID */
		private String sid;

		/**
		 * Članska varijabla koja predstavalja vrijeme u milisekundama do kada
		 * traje ovaj primjerak razreda
		 */
		private long validUntil;

		/**
		 * Članska varijabla koje predstavlja {@link Map} svih spremljenih
		 * parametara sessije
		 */
		private Map<String, String> map = new ConcurrentHashMap<>();

		/**
		 * Konstrukotor koji inicijalizira primjerak ovog razreda. Unutar
		 * konstruktora se inicijaliziraju vrijednosti Session ID-a i vremena
		 * važenja na predane parametre <b>sid</b> i <b>validUntil</b>
		 *
		 * @param sid
		 *            varijabla koja predstavlja Session ID
		 * @param validUntil
		 *            varijabla koja predstavalja vrijeme u milisekundama do
		 *            kada traje ovaj primjerak razreda
		 */
		public SessionMapEntry(String sid, long validUntil) {
			this.sid = sid;
			this.validUntil = validUntil;
		}

		/**
		 * Metoda koja dohvaća vrijeme u milisekundama do kada traje primjerak
		 * ovog razreda
		 *
		 * @return vrijeme u milisekundama do kada traje primjerak ovog razreda
		 */
		public long getValidUntil() {
			return validUntil;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((sid == null) ? 0 : sid.hashCode());
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
			SessionMapEntry other = (SessionMapEntry) obj;
			if (sid == null) {
				if (other.sid != null)
					return false;
			} else if (!sid.equals(other.sid))
				return false;
			return true;
		}
	}

	/**
	 * Metoda od koje započinje rad ovog programa
	 *
	 * @param args
	 *            argumenti naredbenog redka. Ovdje mora biti samo jedan
	 *            argument koji predsavlja putanju do konfiguracijske datoteke
	 *            poslužitelja
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			throw new IllegalArgumentException("Očekivao sam putanju do konfiguracijske datoteke poslužitelja");
		}

		try {
			new SmartHttpServer(args[0]).start();
		} catch (IOException e) {
			System.out.printf("Konfiguracijska datoteka '%s' ne može se učitati", args[0]);
			System.out.println("Zatvaram poslužitelja...");
			System.exit(-1);
		}
	}
}