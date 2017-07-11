package hr.fer.zemris.java.custom.scripting.exec;

import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;

/**
 * Razred predstavlja kolekciju čiji su ključevi primjerci razreda
 * {@link String}, a vrijednosti stogovne strukture primjeraka razreda
 * {@link ValueWrapper}. Stogovno struktura interno se ostvaruje listom te ime
 * složenosti skidanja (ili pogleda) i dodavanja O(1). Ukratko na primjerke ovog
 * razreda može se gledati kao na mapu <{@link String}, Stog
 * ({@link ValueWrapper})>. Primjerci ovog razreda ne mogu imati niti ključ niti
 * vrijednost <code>null</code>. Razred nudi sljedeće metode:
 * 
 * <ul>
 * <li>{@link #push(String, ValueWrapper)}</li>
 * <li>{@link #pop(String)}</li>
 * <li>{@link #peek(String)}</li>
 * <li>{@link #isEmpty(String)}</li>
 * </ul>
 * 
 * Konstruktor {@link #ObjectMultistack()} inicijalizira unutarnje strukture
 * ovog razreda
 * 
 * @see ValueWrapper
 * 
 * @author Davor Češljaš
 */
public class ObjectMultistack {

	/**
	 * Članska varijabla koja predstavlja internu strukturu ovog razreda. Sadrži
	 * ključeve tipa {@link String} koji nisu <code>null</code> i vrijednosti
	 * {@link MultistackEntry} koji predstavljaju vrhove stogova i također nisu
	 * <code>null</code>
	 */
	private Map<String, MultistackEntry> multistack;

	/**
	 * Konstruktor koji inicijalizira primjerak ovog razreda. Ovaj konstruktor
	 * samo inicijalizira internu strukturu ovog primjerka.
	 */
	public ObjectMultistack() {
		// hash tablica za dohvat sa složenosti O(1)
		multistack = new HashMap<>();
	}

	/**
	 * Metoda koja služi za dodavanje elemenata. Predana vrijednost <b>name</b>
	 * ukoliko postoji karakteristična je za pojedini stog. Ukoliko <b>name</b>
	 * postoji u internoj strukturi vrijednost <b>valueWrapper</b> dodaje se na
	 * vrh stoga. Ukoliko <b>name</b> ne postoji u internoj strukturi stvara se
	 * novi stog s tim ključem. Niti <b>name</b> niti <b>valueWrapper</b> ne
	 * smiju biti <code>null</code>
	 *
	 * @param name
	 *            ključ(već postojeći ili budući) unutar interne strukture
	 * @param valueWrapper
	 *            vrijednost koja se dodaje na stog , ako stog za taj ključ ne
	 *            postoji on se stvara
	 * 
	 * @throws IllegalArgumentException
	 *             ukoliko su <b>name</b> ili <b>valueWrapper</b>
	 *             <code>null</code>
	 */
	public void push(String name, ValueWrapper valueWrapper) {
		if (name == null || valueWrapper == null) {
			throw new IllegalArgumentException("Niti ključ niti vrijednost ne smiju biti null! Predani: ključ: " + name
					+ " vrijednost: " + valueWrapper);
		}
		MultistackEntry entry = new MultistackEntry(valueWrapper,
				multistack.containsKey(name) ? multistack.get(name) : null);
		multistack.put(name, entry);
	}

	/**
	 * Metoda koja zadanom ključu<b>name</b> sa stoga skida element s vrha i
	 * vraća ga kroz povratnu vrijednost. Ukoliko je stog prazan baca se
	 * {@link IllegalArgumentException}. Ista iznimka baca se ukoliko je ključ
	 * <code>null</code> ili takav ključ ne postoji u internoj strukturi
	 *
	 * @param name
	 *            ključ sa čijeg stoga skidamo element
	 * @return referenca na primjerak razreda {@link ValueWrapper} skinuta sa
	 *         vrha stoga
	 * 
	 * @throws EmptyStackException
	 *             ukoliko je stog prazan
	 * @throws IllegalArgumentException
	 *             ukoliko je ključ <code>null</code> ili takav ključ ne postoji
	 *             u internoj strukturi
	 */
	public ValueWrapper pop(String name) {
		checkGivenKey(name);
		MultistackEntry toReturn = multistack.remove(name);
		checkIfStackEmpty(toReturn, name);
		multistack.put(name, toReturn.next);
		return toReturn.valueWrapper;
	}

	/**
	 * Metoda koja zadanom ključu<b>name</b> sa stoga uzima referencu na element
	 * s vrha (stog ostaje nepromijenjen) i vraća ga kroz povratnu vrijednost.
	 * Ukoliko je stog prazan baca se {@link IllegalArgumentException}. Ista
	 * iznimka baca se ukoliko je ključ <code>null</code> ili takav ključ ne
	 * postoji u internoj strukturi
	 *
	 * @param name
	 *            ključ sa čijeg stoga skidamo element
	 * @return referenca na primjerak razreda {@link ValueWrapper} sa vrha stoga
	 * 
	 * @throws EmptyStackException
	 *             ukoliko je stog prazan
	 * @throws IllegalArgumentException
	 *             ukoliko je ključ <code>null</code> ili takav ključ ne postoji
	 *             u internoj strukturi
	 */
	public ValueWrapper peek(String name) {
		checkGivenKey(name);
		MultistackEntry toReturn = multistack.get(name);
		checkIfStackEmpty(toReturn, name);
		return toReturn.valueWrapper;
	}

	/**
	 * Metoda koja provjerava je li stog za predani ključ <b>name</b> prazan.
	 * Metoda će vratiti <b>true</b> i ukoliko se kao ključ preda
	 * <code>null</code> ili ako predani ključ <b>name</b> ne postoji u internoj
	 * strukturi, jer zaiste su njihovi stogovi prazni
	 *
	 * @param name
	 *            ključ čiji se stog provjerava
	 * @return <b>true</b> ako je stog prazan, <b>false</b> inače
	 */
	public boolean isEmpty(String name) {
		return multistack.get(name) == null;
	}

	/**
	 * Pomoćna metoda koja vrši provjeru je li stog prazan. Metoda baca
	 * {@link IllegalArgumentException} ukoliko je stog prazan
	 *
	 * @param firstNode
	 *            vrh stoga koji se provjerava je li prazan
	 * @param name
	 *            ključ kojem je pridjeljen vrh stoga <b>firstNode</b>(služi
	 *            isključivo za ispis poruke)
	 * @throws EmptyStackException
	 *             ukoliko je stog prazan
	 */
	private void checkIfStackEmpty(MultistackEntry firstNode, String name) {
		if (firstNode == null) {
			throw new EmptyStackException();
		}
	}

	/**
	 * Pomoćna metoda koja vrši provjeru je li predani ključ <code>null</code>
	 * te ako nije postoji li takav ključ u internoj strukturi.Metoda baca
	 * {@link IllegalArgumentException} ukoliko je predani ključ <b>name</b>
	 * <code>null</code> ili ključ <b>name</b> ne postoji u internoj strukturi
	 * 
	 * @param name
	 *            ključ koji se provjerava
	 * @throws IllegalArgumentException
	 *             ukoliko je predani ključ <b>name</b> <code>null</code> ili
	 *             ključ <b>name</b> ne postoji u internoj strukturi
	 */
	private void checkGivenKey(String name) {
		if (name == null || !multistack.containsKey(name)) {
			throw new IllegalArgumentException("Predani ključ ne postoji u ovoj kolekciji!");
		}
	}

	/**
	 * Razred koji predstavlja čvor stoga za razred {@link ObjectMultistack}.
	 * Primjerci ovog razreda su nepromijenjivi. Svaki primjerak ovog razreda
	 * drži referencu na primjerak razreda {@link ValueWrapper}(ta referenca
	 * neće biti <code>null</code>) te referencu na idući čvor stoga ( također
	 * primjerak razreda {@link MultistackEntry}). Na razred se može gledati kao
	 * na omotač primjerka razreda {@link ValueWrapper}. Razred se zna ispisati
	 * i razred ({@link #toString()}) može ustvrditi je li jednak nekom drugom
	 * objektu ({@link #equals(Object)} i {@link #hashCode()})
	 * 
	 * @see ObjectMultistack
	 * 
	 * @author Davor Češljaš
	 */
	private static class MultistackEntry {

		/** Članska varijabla koju primjerak ovog razreda omata */
		private ValueWrapper valueWrapper;

		/** Članska varijabla koja predstavlja idući čvor stoga */
		private MultistackEntry next;

		/**
		 * Konstruktor koji inicijalizira vrijednosti članskih varijabli
		 * {@link #valueWrapper} i {@link #next} na predane vrijednosti.
		 *
		 * @param valueWrapper
		 *            Vrijednost na koju se inicijalizira {@link #valueWrapper}
		 * @param next
		 *            Vrijednost na koju se inicijalizira {@link #next}
		 */
		public MultistackEntry(ValueWrapper valueWrapper, MultistackEntry next) {
			this.valueWrapper = valueWrapper;
			this.next = next;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((valueWrapper == null) ? 0 : valueWrapper.hashCode());
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
			MultistackEntry other = (MultistackEntry) obj;
			if (valueWrapper == null) {
				if (other.valueWrapper != null)
					return false;
			} else if (!valueWrapper.equals(other.valueWrapper))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return valueWrapper.toString();
		}

	}
}
