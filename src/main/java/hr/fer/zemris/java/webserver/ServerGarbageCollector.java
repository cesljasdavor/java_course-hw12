package hr.fer.zemris.java.webserver;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Razred implementira sučelje {@link Runnable}. Primjerak ovog razreda obavlja
 * posao čišćenja tablice svih sesija koje su zapamćene, a koje su istekle.
 * Primjerak ovog razreda zaustavlja rad koristeći metodu
 * {@link Thread#sleep(long)} na {@value #TIMEOUT}ms. Po buđenju prolazi kroz
 * čitavu mapu koja mu se preda kroz konstuktor i vrši brisanje isteklih sesija.
 * 
 * @see Thread
 * @see Runnable
 * @see SmartHttpServer
 * 
 * @author Davor Češljaš
 */
public class ServerGarbageCollector implements Runnable {

	/**
	 * Konstanta koja predstavlja vrijeme koje je primjerak ovog razreda
	 * zaustavljen
	 */
	private static final int TIMEOUT = 1000 * 60 * 5;

	/** Članska varijabla koja predstavlja sve aktivne sesije. */
	private final Map<String, SmartHttpServer.SessionMapEntry> sessions;

	/**
	 * Konstruktor koji inicijalizira primjerak ovo razreda. Konstruktor prima
	 * {@link Map} sesija koje su trenutno aktivne na poslužitelju koji je
	 * primjerak razreda {@link SmartHttpServer} te pamti referencu na nju.
	 *
	 * @param sessions
	 *            referenca na sesija koje su trenutno aktivne na poslužitelju
	 */
	public ServerGarbageCollector(Map<String, SmartHttpServer.SessionMapEntry> sessions) {
		this.sessions = Objects.requireNonNull(sessions, "Nemam što čistiti");
	}

	@Override
	public void run() {
		while (true) {
			long time = Calendar.getInstance().getTimeInMillis();
			new HashMap<>(sessions).forEach((sid, entry) -> {
				if (entry.getValidUntil() < time) {
					sessions.remove(sid);
				}
			});

			try {
				Thread.sleep(TIMEOUT);
			} catch (InterruptedException e) {
			}
		}
	}

}
