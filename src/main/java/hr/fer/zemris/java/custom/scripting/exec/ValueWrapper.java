package hr.fer.zemris.java.custom.scripting.exec;

import java.util.Comparator;
import java.util.function.BiConsumer;

/**
 * Razred koji predstavlja omotač oko reference na primjerak bilo kojeg razreda.
 * Općenito čuva se referenca na {@link Object}. Razred nudi metode za
 * aritmetiku nad spremljenom referencom ukoliko je ona primjerak razreda
 * {@link Integer}, {@link Double} ili u određenim slučajevima {@link String}.
 * razred može raditi aritmetičke operacije nad primjerkom razreda
 * {@link String} ukoliko je unutar tog primjerka spremljena vrijednost koja se
 * može parsirati u {@link Integer} ili {@link Double} pozivom
 * {@link Integer#parseInt(String)} ,odnosno {@link Double#parseDouble(String)}.
 * Rezultat operacija sprema se u referencu kao {@link Integer} ukoliko su oba
 * argumenta pretvorena u {@link Integer}, inače se rezultat sprema kao
 * {@link Double}. Razred nudi iduće metode koje rade nad spremljenim objektom:
 * <ul>
 * <li>{@link #getValue()}</li>
 * <li>{@link #setValue(Object)}</li>
 * <li>{@link #add(Object)}</li>
 * <li>{@link #subtract(Object)}</li>
 * <li>{@link #multiply(Object)}</li>
 * <li>{@link #divide(Object)}</li>
 * <li>{@link #numCompare(Object)}</li>
 * </ul>
 * 
 * Razreda nudi jedan konstruktor koji inicijalizira člansku varijablu na
 * predanu vrijednost {@link #ValueWrapper(Object)}. Razred nadjačava metode:
 * <ul>
 * <li>{@link #equals(Object)}</li>
 * <li>{@link #hashCode()}</li>
 * <li>{@link #toString()}</li>
 * </ul>
 * 
 * @see Integer
 * @see Double
 * 
 * @author Davor Češljaš
 */
public class ValueWrapper {

	private static final double DIFFERENCE =  0.000001;
	
	/** Vrijednost koju ovaj razred omotava */
	private Object value;

	/**
	 * Konstruktor koji inicijalizira vrijednost koju primjerak ovog razreda
	 * omotava na <b>value</b>
	 *
	 * @param value
	 *            vrijednost na koju se inicijalizira primjerak ovog razreda
	 */
	public ValueWrapper(Object value) {
		this.value = value;
	}

	/**
	 * Metoda koja dohvaća referncu koju ovaj razred omotava
	 *
	 * @return referncu koju ovaj razred omotava
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Metoda koja mijenja referencu koju ovaj razred omotava na predna
	 * <b>value</b>
	 *
	 * @param value
	 *            nova referenca koju ovaj razred omotava
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * Metoda koja vrši aritmetičku operaciju zbrajanja nad referencom koju
	 * omotava primjerak ovog razreda i predanom vrijednosti <b>incValue</b>.
	 * Operacija se vrši na sljedeći način:
	 * <ol>
	 * <li>Ukoliko su obe reference reference na {@link Integer} rezultat se
	 * sprema kao primjerak razreda {@link Integer}</li>
	 * <li>Ukoliko je barem jedna od referenci referenca na {@link Double} tada
	 * se rezultat sprema kao primjerak razreda {@link Double}</li>
	 * <li>Ukoliko je bilo koja referenca referenca na {@link String} pokušava
	 * se vrijednost niza pretvoriti u {@link Double} ukoliko to ne uspije
	 * pokušava se vrijednost niza pretvoriti u {@link Integer}, ako niti to ne
	 * uspije baca se iznimka {@link IllegalArgumentException}</li>
	 * <li>Ako je neka od referenca referenca na bilo koji drugi razred baca se
	 * {@link IllegalArgumentException}</li>
	 * </ol>
	 * NAPOMENA: Referenca se sprema kao referenca koju omata <b>ovaj primjerak
	 * razreda</b> {@link ValueWrapper}
	 *
	 * @param incValue
	 *            drugi operand (referenca) u opisanoj operaciji zbrajanja
	 * 
	 * @throws IllegalArgumentException
	 *             ukoliko bilo koji od uvjeta 1, 2 ili 3 nisu zadovoljeni
	 */
	public void add(Object incValue) {
		BiConsumer<Number, Number> add = (first, second) -> {
			if (checkForDoubles(first, second)) {
				value = first.doubleValue() + second.doubleValue();
				return;
			}
			value = first.intValue() + second.intValue();
		};
		calculateResult(value, incValue, add);
	}

	/**
	 * Metoda koja vrši aritmetičku operaciju oduzimanja nad referencom koju
	 * omotava primjerak ovog razreda i predanom vrijednosti <b>decValue</b>.
	 * Operacija se vrši na sljedeći način:
	 * <ol>
	 * <li>Ukoliko su obe reference reference na {@link Integer} rezultat se
	 * sprema kao primjerak razreda {@link Integer}</li>
	 * <li>Ukoliko je barem jedna od referenci referenca na {@link Double} tada
	 * se rezultat sprema kao primjerak razreda {@link Double}</li>
	 * <li>Ukoliko je bilo koja referenca referenca na {@link String} pokušava
	 * se vrijednost niza pretvoriti u {@link Double} ukoliko to ne uspije
	 * pokušava se vrijednost niza pretvoriti u {@link Integer}, ako niti to ne
	 * uspije baca se iznimka {@link IllegalArgumentException}</li>
	 * <li>Ako je neka od referenca referenca na bilo koji drugi razred baca se
	 * {@link IllegalArgumentException}</li>
	 * </ol>
	 * NAPOMENA: Referenca se sprema kao referenca koju omata <b>ovaj primjerak
	 * razreda</b> {@link ValueWrapper}
	 *
	 * @param decValue
	 *            drugi operand (referenca) u opisanoj operaciji oduzimanja
	 * 
	 * @throws IllegalArgumentException
	 *             ukoliko bilo koji od uvjeta 1, 2 ili 3 nisu zadovoljeni
	 */
	public void subtract(Object decValue) {
		BiConsumer<Number, Number> sub = (first, second) -> {
			if (checkForDoubles(first, second)) {
				value = first.doubleValue() - second.doubleValue();
				return;
			}
			value = first.intValue() - second.intValue();
		};
		calculateResult(value, decValue, sub);
	}

	/**
	 * Metoda koja vrši aritmetičku operaciju množenja nad referencom koju
	 * omotava primjerak ovog razreda i predanom vrijednosti <b>mulValue</b>.
	 * Operacija se vrši na sljedeći način:
	 * <ol>
	 * <li>Ukoliko su obe reference reference na {@link Integer} rezultat se
	 * sprema kao primjerak razreda {@link Integer}</li>
	 * <li>Ukoliko je barem jedna od referenci referenca na {@link Double} tada
	 * se rezultat sprema kao primjerak razreda {@link Double}</li>
	 * <li>Ukoliko je bilo koja referenca referenca na {@link String} pokušava
	 * se vrijednost niza pretvoriti u {@link Double} ukoliko to ne uspije
	 * pokušava se vrijednost niza pretvoriti u {@link Integer}, ako niti to ne
	 * uspije baca se iznimka {@link IllegalArgumentException}</li>
	 * <li>Ako je neka od referenca referenca na bilo koji drugi razred baca se
	 * {@link IllegalArgumentException}</li>
	 * </ol>
	 * NAPOMENA: Referenca se sprema kao referenca koju omata <b>ovaj primjerak
	 * razreda</b> {@link ValueWrapper}
	 *
	 * @param mulValue
	 *            drugi operand (referenca) u opisanoj operaciji množenja
	 * 
	 * @throws IllegalArgumentException
	 *             ukoliko bilo koji od uvjeta 1, 2 ili 3 nisu zadovoljeni
	 */
	public void multiply(Object mulValue) {
		BiConsumer<Number, Number> mul = (first, second) -> {
			if (checkForDoubles(first, second)) {
				value = first.doubleValue() * second.doubleValue();
				return;
			}
			value = first.intValue() * second.intValue();
		};
		calculateResult(value, mulValue, mul);
	}

	/**
	 * Metoda koja vrši aritmetičku operaciju dijeljenja nad referencom koju
	 * omotava primjerak ovog razreda i predanom vrijednosti <b>divValue</b>.
	 * Operacija se vrši na sljedeći način:
	 * <ol>
	 * <li>Ukoliko su obe reference reference na {@link Integer} rezultat se
	 * sprema kao primjerak razreda {@link Integer}</li>
	 * <li>Ukoliko je barem jedna od referenci referenca na {@link Double} tada
	 * se rezultat sprema kao primjerak razreda {@link Double}</li>
	 * <li>Ukoliko je bilo koja referenca referenca na {@link String} pokušava
	 * se vrijednost niza pretvoriti u {@link Double} ukoliko to ne uspije
	 * pokušava se vrijednost niza pretvoriti u {@link Integer}, ako niti to ne
	 * uspije baca se iznimka {@link IllegalArgumentException}</li>
	 * <li>Ako je neka od referenca referenca na bilo koji drugi razred baca se
	 * {@link IllegalArgumentException}</li>
	 * </ol>
	 * NAPOMENA: Referenca se sprema kao referenca koju omata <b>ovaj primjerak
	 * razreda</b> {@link ValueWrapper}
	 *
	 * @param divValue
	 *            drugi operand (referenca) u opisanoj operaciji množenja
	 * @throws IllegalArgumentException
	 *             ukoliko bilo koji od uvjeta 1, 2 ili 3 nisu zadovoljeni
	 */
	public void divide(Object divValue) {
		BiConsumer<Number, Number> div = (first, second) -> {
			if (Math.abs(second.doubleValue() - 0) < DIFFERENCE) {
				throw new ArithmeticException("Dijeljenje s nulom!");
			}

			if (checkForDoubles(first, second)) {
				value = first.doubleValue() / second.doubleValue();
				return;
			}
			value = first.intValue() / second.intValue();
		};
		calculateResult(value, divValue, div);
	}

	/**
	 * Metoda koja vrši operaciju usporedbe nad referencom koju omotava
	 * primjerak ovog razreda i predanom vrijednosti <b>withValue</b>. Operacija
	 * se vrši na sljedeći način:
	 * <ol>
	 * <li>Ukoliko su obe reference reference na {@link Integer} rezultat se
	 * sprema kao primjerak razreda {@link Integer}</li>
	 * <li>Ukoliko je barem jedna od referenci referenca na {@link Double} tada
	 * se rezultat sprema kao primjerak razreda {@link Double}</li>
	 * <li>Ukoliko je bilo koja referenca referenca na {@link String} pokušava
	 * se vrijednost niza pretvoriti u {@link Double} ukoliko to ne uspije
	 * pokušava se vrijednost niza pretvoriti u {@link Integer}, ako niti to ne
	 * uspije baca se iznimka {@link IllegalArgumentException}</li>
	 * <li>Ako je neka od referenca referenca na bilo koji drugi razred baca se
	 * {@link IllegalArgumentException}</li>
	 * </ol>
	 * Ukoliko je bilo koji od uvjeta 1, 2 ili 3 zadovoljen metoda vraća
	 * sljedeće:
	 * <ul>
	 * <li>1 ako je omotana referenca veća od <b>withValue</b></li>
	 * <li>-1 ako je omotana referenca manja <b>withValue</b></li>
	 * <li>0 ukoliko su reference jednake po sadržaju</li>
	 * </ul>
	 * NAPOMENA: Referenca se sprema kao referenca koju omata <b>ovaj primjerak
	 * razreda</b> {@link ValueWrapper}
	 *
	 * @param withValue
	 *            drugi operand (referenca) u opisanoj operaciji množenja
	 * @return
	 *         <ul>
	 *         <li>1 ako je omotana referenca veća od <b>withValue</b></li>
	 *         <li>-1 ako je omotana referenca manja <b>withValue</b></li>
	 *         <li>0 ukoliko su reference jednake po sadržaju</li>
	 *         </ul>
	 *
	 * @throws IllegalArgumentException
	 *             ukoliko bilo koji od uvjeta 1, 2 ili 3 nisu zadovoljeni
	 * 
	 * 
	 */
	public int numCompare(Object withValue) {
		Comparator<Number> comparator = (first, second) -> {
			if (checkForDoubles(first, second)) {
				return Double.compare(first.doubleValue(), second.doubleValue());
			}
			return Integer.compare(first.intValue(), second.intValue());
		};
		return comparator.compare(extractArgument(value), extractArgument(withValue));
	}

	/**
	 * Generička pomoćna metoda koja nad parsiranim vrijednostima <b>first</b> i
	 * <b>second</b> izvršava {@link BiConsumer#accept(Object, Object)} od
	 * predanog <b>consumer</b>
	 *
	 * @param first
	 *            prvi operand
	 * @param second
	 *            drugi operand
	 * @param consumer
	 *            primjerak razreda koji imlementira sučelje {@link BiConsumer}
	 * @see BiConsumer
	 * 
	 * @see ValueWrapper#extractArgument(Object)
	 */
	private void calculateResult(Object first, Object second, BiConsumer<Number, Number> consumer) {
		consumer.accept(extractArgument(first), extractArgument(second));
	}

	/**
	 * Pomoćna metoda koja provjerava je li bilo koji od objekata primjerak
	 * razreda {@link Double}
	 *
	 * @param firstArgument
	 *            prvi operand
	 * @param secondArgument
	 *            drgui operand
	 * @return <b>true</b> ukoliko je barem jedan od predanih operanada
	 *         primjerak razreda {@link Double}
	 */
	private boolean checkForDoubles(Object firstArgument, Object secondArgument) {
		return firstArgument instanceof Double || secondArgument instanceof Double;
	}

	/**
	 * Pomoćna metoda koja parsira predanu referencu te je pokušava pretvoriti u
	 * minimalno tip {@link Number}
	 *
	 * @param argument
	 *            argument koji se parsira
	 * @return primjerak razreda koji je castablilan u {@link Number}
	 * 
	 * @throws IllegalArgumentException
	 *             ukoliko nije moguće iz argumenta izlučiti primjerak razreda
	 *             koji je minimalno {@link Number}
	 * 
	 * @see ValueWrapper#extractArgumentFromString(String)
	 */
	private Number extractArgument(Object argument) {
		if (argument == null) {
			return Integer.valueOf(0);
		}
		if (argument instanceof Integer || argument instanceof Double) {
			// integeri i doublovi su nepormijenjivi!
			return (Number) argument;
		}
		if (argument instanceof String) {
			return extractArgumentFromString((String) argument);
		}
		throw new IllegalArgumentException("Predali ste argument koji je primjerak razreda: " + argument.getClass());
	}

	/**
	 * Pomoćna metoda koja predani argument <b>argument</b> pokušava parsirati
	 * ili u {@link Integer} ili u {@link Double}. Budući i {@link Integer} i
	 * {@link Double} izvedeni iz razreda {@link Number} povratna vrijednost je
	 * tipa {@link Number}
	 *
	 * @param argument
	 *            argument koji se parsira
	 * @return vrijednost koji je sadržan u {@link String}u castan u razred
	 *         {@link Number}
	 * 
	 * @throws NumberFormatException
	 *             ukoliko nije moguće parsirati predani argument
	 *             <b>argument</b>
	 */
	private Number extractArgumentFromString(String argument) {
		// ako se argument nemože parsirati baca se
		// NumberFormatException(RuntimeException)
		if (argument.contains(".") || argument.contains("E") || argument.contains("e")) {
			return Double.parseDouble(argument);
		}
		return Integer.parseInt(argument);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		ValueWrapper other = (ValueWrapper) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
