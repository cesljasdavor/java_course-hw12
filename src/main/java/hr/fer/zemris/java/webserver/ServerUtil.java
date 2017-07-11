package hr.fer.zemris.java.webserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;

import hr.fer.zemris.java.webserver.RequestContext.RCCookie;

/**
 * Pomoćni razred koji koristi poslužitelj modeliran razredom
 * {@link SmartHttpServer}. Razred sadrži sljedeće metode:
 * <ul>
 * <li>{@link #generateFirstLineOfResponse(String, int, String)}</li>
 * <li>{@link #sendErrorResponse(String, int, String, OutputStream)}</li>
 * <li>{@link #sendBadRequestResponse(String, OutputStream)}</li>
 * <li>{@link #readFromDisk(Path, Charset)}</li>
 * <li>{@link #loadProperties(String, BiConsumer)}</li>
 * <li>{@link #readRequest(PushbackInputStream)}</li>
 * <li>{@link #parseCookies(String)}</li>
 * <li>{@link #parseHost(String)}</li>
 * </ul>
 * Razred nudi i mnoštvo konstanti koje se često koriste kao argumetni gore
 * napisanih metoda.
 * 
 * @see SmartHttpServer
 * 
 * @author Davor Češljaš
 */
public class ServerUtil {

	/**
	 * Konstanta koja predstavlja naziv u zahtjevu kojem je značenje metoda get
	 */
	public static final String GET_METHOD = "GET";

	/**
	 * Konstanta koja predstavlja predpostavljenu vrijednost verzije protokola
	 * HTTP
	 */
	public static final String DEFAULT_VERISON = "HTTP/1.1";

	/**
	 * Konstanta koja predstavlja nepromijenjivi {@link Set} svih podržanih
	 * verzija protokola HTTP
	 */
	public static final Set<String> VERSIONS = Collections
			.unmodifiableSet(new HashSet<>(Arrays.asList("HTTP/1.0", DEFAULT_VERISON)));

	/** Konstanta koja predstavlja status za "Bad Request" pogrešku */
	public static final int BAD_REQUEST_STATUS = 400;

	/** Konstanta koja predstavlja tekst za "Bad Request" pogrešku */
	public static final String BAD_REQUEST_TEXT = "Bad Request";

	/** Konstanta koja predstavlja status za "FORBIDDEN" pogrešku */
	public static final int FORBIDDEN_STATUS = 403;

	/** Konstanta koja predstavlja tekst za "FORBIDDEN" pogrešku */
	public static final String FORBIDDEN_TEXT = "FORBIDDEN";

	/** Konstanta koja predstavlja status za "Not Found" pogrešku */
	public static final int NOT_FOUND_STATUS = 404;

	/** Konstanta koja predstavlja tekst za "Not Found" pogrešku */
	public static final String NOT_FOUND_TEXT = "Not Found";

	/** Konstanta koja predstavlja terminator odgovora/zahtjeva */
	public static final String HEADER_TERMINATOR = "\r\n\r\n";

	/**
	 * Privatni predpostavljeni konstruktor koja je izrađen kako se ne bi mogli
	 * instancirati primjerci ovog razreda
	 */
	private ServerUtil() {
	}

	/**
	 * Pomoćna metoda koja generira prvi redak odgovora na zahtjev poslan
	 * poslužitelju koji je modeliran razredom {@link SmartHttpServer}
	 *
	 * @param version
	 *            verzija HTTP protokola
	 * @param statusCode
	 *            status odgovora
	 * @param statusText
	 *            tekst uz predani status
	 * @return primjerak razreda {@link String} koji predstavlja prvi redak
	 *         odgovora na zahtjev
	 */
	public static String generateFirstLineOfResponse(String version, int statusCode, String statusText) {
		return String.format("%s %d %s", version, statusCode, statusText);
	}

	/**
	 * Metoda koja na primjerak razreda {@link OutputStream} ispisuje poruku o
	 * pogrešci koja je definirana predanim parametrima <b>statusCode</b> i
	 * <b>statusText</b>. Kao posljednji parametar potrebno je dati verziju HTTP
	 * protokola <b>verison</b>
	 *
	 * @param version
	 *            verzija HTTP protokola
	 * @param statusCode
	 *            statusni kod odgovora na zahtjev
	 * @param statusText
	 *            pripadni tekst uz statusni kod odgovora na zahtjev
	 * @param os
	 *            primjerak razreda {@link OutputStream} u koji se upisuje
	 *            poruka o pogrešci
	 * @throws IOException
	 *             ukoliko nije moguće pisati u predani primjerak razreda
	 *             {@link OutputStream} <b>os</b>
	 */
	public static void sendErrorResponse(String version, int statusCode, String statusText, OutputStream os)
			throws IOException {
		String response = generateFirstLineOfResponse(version, statusCode, statusText).concat(HEADER_TERMINATOR);

		os.write(response.getBytes());
		os.flush();
	}

	/**
	 * Metoda koja je izrađena zbog čestog slanja status
	 * {@value #BAD_REQUEST_TEXT}. Ova metoda interno poziva
	 * {@link #sendErrorResponse(String, int, String, OutputStream)} sa statusom
	 * {@link #BAD_REQUEST_STATUS} i pripadnim tekstom {@link #BAD_REQUEST_TEXT}
	 *
	 * @param version
	 *            verzija HTTP protokola
	 * @param os
	 *            primjerak razreda {@link OutputStream} u koji se upisuje
	 *            poruka o pogrešci
	 * @throws IOException
	 *             ukoliko nije moguće pisati u predani primjerak razreda
	 *             {@link OutputStream} <b>os</b>
	 */
	public static void sendBadRequestResponse(String version, OutputStream os) throws IOException {
		sendErrorResponse(version, BAD_REQUEST_STATUS, BAD_REQUEST_TEXT, os);
	}

	/**
	 * Metoda koja vrši čitanje podataka iz datoteke predstavljene primjerkom
	 * sučelja {@link Path} <b>filePath</b>. Metodi se može predati i specijalni
	 * skup znakova kojim se želi enkodirati sadržaj datoteke <b>charset</b>.
	 * Ukoliko se za <b>charset</b> preda <code>null</code> metoda koristi
	 * {@link StandardCharsets#UTF_8}.
	 * 
	 * @param filePath
	 *            primjerak sučelja {@link Path} koji predstavlja putanju do
	 *            datoteke koju treba pročitati
	 * @param charset
	 *            primjerak razreda {@link Charset} koji predstavlja skup
	 *            znakova kojim se treba enkodirati sadržaj datoteke. Ukoliko se
	 *            preda <code>null</code> datoteka će biti enkodirana sa
	 *            {@link StandardCharsets#UTF_8}
	 * @return primjerak razreda {@link String} koji predstavlja sadržaj
	 *         datoteke sa putanjom <b>filePath</b> enkodiran skupom znakova
	 *         <b>charset</b>
	 * @throws IOException
	 *             ukoliko se ne može čitati iz datoteke sa putanjom
	 *             <b>filePath</b> ili takva ne postoji u datotečnom sustavu
	 */
	public static String readFromDisk(Path filePath, Charset charset) throws IOException {
		if (charset == null) {
			charset = StandardCharsets.UTF_8;
		}

		return new String(Files.readAllBytes(filePath), charset);
	}

	/**
	 * Metoda koja dohvaća ulaz iz datoteke čiji unosi su oblika "ključ =
	 * vrijednost" te nad svakim ključem i vrijednosti poziva strategiju
	 * predstavljenu sučeljem {@link BiConsumer} <b>consumer</b>.
	 *
	 * @param filePath
	 *            putanja do datoteke koja se treba učitati
	 * @param consumer
	 *            strategija koja se primjenjuje nad svakim ključem i
	 *            vrijednošću unutar datoteke
	 * @throws IOException
	 *             ukoliko se ne može čitati iz datoteke sa putanjom
	 *             <b>filePath</b> ili takva ne postoji u datotečnom sustavu
	 */
	public static void loadProperties(String filePath, BiConsumer<Object, Object> consumer) throws IOException {
		Properties properties = new Properties();
		properties.load(Files.newBufferedReader(Paths.get(filePath)));
		properties.forEach(consumer);
	}

	/**
	 * Metoda koja čita zaglavlje korisnikovog zahtjev i parsira ga u polje
	 * okteta. Metoda je preuzeta iz knjige profesora Marka Čupića. Metoda čita
	 * oktete sve do pojave neke od sljedećih nizova znakova:
	 * <ul>
	 * <li>'\r\n\r\n'</li>
	 * <li>'\n\n'</li>
	 * <li>'\r\r'</li>
	 * <ul>
	 *
	 * @param is
	 *            ulazni tok okteta iz kojeg se čita korisnikova zahtjev
	 * @return polje okteta koje predstavlja zaglavlje korisnikova zahtjeva
	 * @throws IOException
	 *             ukoliko se ne može čitati iz ulaznog toka podataka <b>is</b>
	 */
	public static byte[] readRequest(PushbackInputStream is) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		int state = 0;
		l: while (true) {
			int b = is.read();
			if (b == -1)
				return null;
			if (b != 13) {
				bos.write(b);
			}
			switch (state) {
			case 0:
				if (b == 13) {
					state = 1;
				} else if (b == 10)
					state = 4;
				break;
			case 1:
				if (b == 10) {
					state = 2;
				} else
					state = 0;
				break;
			case 2:
				if (b == 13) {
					state = 3;
				} else
					state = 0;
				break;
			case 3:
				if (b == 10) {
					break l;
				} else
					state = 0;
				break;
			case 4:
				if (b == 10) {
					break l;
				} else
					state = 0;
				break;
			}
		}
		return bos.toByteArray();
	}

	/**
	 * Metoda koja iz predane linije predstavljene primjerkom razreda
	 * {@link String}, a koja MORA započeti sa "Cookie: " čita i parsira sve
	 * cookie koji postoje unutar tog redka. Predpostavka je da su svi cookiji
	 * rastavljeni po ";" te da je između ključa i vrijednosti cookia niz "="
	 *
	 * @param cookieLine
	 *            primjerak razreda {@link String} koji predstavlja liniju koja
	 *            započinje sa "Cookie: "
	 * @return {@link List} svih parsiranih cookija koji su unutar poslužitelja
	 *         modeliranog razredom {@link SmartHttpServer} predstavljeni sa
	 *         razredom {@link RCCookie}
	 */
	public static List<RequestContext.RCCookie> parseCookies(String cookieLine) {
		String[] splittedLine = cookieLine.split(":");

		if (splittedLine.length != 2) {
			return null;
		}

		List<RCCookie> cookies = new ArrayList<>();
		for (String cookieString : splittedLine[1].split(";")) {
			RCCookie cookie = extractCookie(cookieString);
			if (cookie != null) {
				cookies.add(cookie);
			}
		}

		return cookies.isEmpty() ? null : cookies;
	}

	/**
	 * Pomoćna metoda koja iz predanog primjerka razreda {@link String} vadi
	 * ključ i vrijednost cookija te stvara novi primjerak razreda
	 * {@link RCCookie}.
	 *
	 * @param cookieString
	 *            primjerak razreda {@link String} iz kojeg se parsira novi
	 *            primjerak razreda {@link RCCookie}
	 * @return novi primjerak razreda {@link RCCookie} nastao parsiranjem
	 *         <b>cookieString</b>
	 */
	private static RCCookie extractCookie(String cookieString) {
		String[] keyValue = cookieString.split("=");

		if (keyValue.length != 2) {
			return null;
		}

		for (int i = 0; i < keyValue.length; i++) {
			keyValue[i] = keyValue[i].trim();
		}

		return new RCCookie(keyValue[0], keyValue[1].substring(1, keyValue[1].length() - 1), null, null, null);
	}

	/**
	 * Metoda koja iz predane linije predstavljene primjerkom razreda
	 * {@link String}, a koja MORA započeti sa "Host: " čita i parsira adresu
	 * poslužitelja na koju je poslan zahtjev. Metoda se koristi jer poslužitelj
	 * može imati više načina preko kojih se pristupa poslužitelju (npr.
	 * localhost i 127.0.0.1)
	 *
	 * @param cookieLine
	 *            primjerak razreda {@link String} koji predstavlja liniju koja
	 *            započinje sa "Host: "
	 * @return primjerak razreda {@link String} koji predstavlja adresu
	 *         poslužitelja koju je korisnik poslao zahtjev
	 */
	public static String parseHost(String hostLine) {
		String[] splittedLine = hostLine.split(":");

		if (splittedLine.length < 2) {
			return null;
		}

		return splittedLine[1];
	}
}
