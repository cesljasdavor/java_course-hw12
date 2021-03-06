package hr.fer.java.zemris.hw02.collections;

/**
 * Razred koji nasljeđuje {@link RuntimeException}. Razred se koristi kroz
 * implementaciju stoga {@link ObjectStack}. Ovaj razred je neprovjeravana
 * iznimka i koristi se kako bi korisnika obavijestio da pokušava uzeti element
 * sa praznog stoga.
 * 
 *  @author Davor Češljaš
 */
public class EmptyStackException extends RuntimeException {

	/** Konstanta serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor koji inicijalizira primjerak ovog razreda. Korištenjem ovog
	 * konstruktora korisniku će prilikom pojave iznimke biti ispisan trag stoga
	 * bez ikakve dodatne poruke
	 */
	public EmptyStackException() {
		super();
	}

	/**
	 * Konstruktor koji inicijalizira primjerak ovog razreda. Korištenjem ovog
	 * konstruktora korisniku će prilikom pojave iznimke biti ispisan trag stoga
	 * uz dodatnu poruku
	 *
	 * @param message
	 *           poruka koju treba ispisati korisniku prilikom bacanja iznimke
	 */
	public EmptyStackException(String message) {
		super(message);
	}

	/**
	 * Konstruktor koji inicijalizira primjerak ovog razreda. Korištenjem ovog
	 * konstruktora korisniku će prilikom pojave iznimke biti ispisan trag stoga
	 * uz dodatan uzrok
	 *
	 * @param cause
	 *           Uzrok bacanja iznimke
	 */
	public EmptyStackException(Throwable cause) {
		super(cause);
	}

	/**
	 * Konstruktor koji inicijalizira primjerak ovog razreda. Korištenjem ovog
	 * konstruktora korisniku će prilikom pojave iznimke biti ispisan trag stoga
	 * uz dodatan uzrok i poruku 
	 *
	 *	 @param message
	 *           poruka koju treba ispisati korisniku prilikom bacanja iznimke
	 * @param cause
	 *           Uzrok bacanja iznimke
	 */
	public EmptyStackException(String message, Throwable cause) {
		super(message, cause);
	}

}
