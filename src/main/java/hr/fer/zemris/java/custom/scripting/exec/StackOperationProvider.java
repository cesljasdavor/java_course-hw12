package hr.fer.zemris.java.custom.scripting.exec;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import hr.fer.zemris.java.webserver.RequestContext;

/**
 * Razred koji predstavlja pomoćni razred za semantičku analizu primjerkom
 * razreda {@link SmartScriptEngine}. Razred sadrži sve moguće operacije koje
 * ovaj semantički analizator može izvršiti. Razred je također izrađen u skladu
 * s oblikovnim obrascem
 * <a href = "https://en.wikipedia.org/wiki/Singleton_pattern">jedinstveni
 * objekt</a>. Za izvršavanje operacije korisniku se nudi metoda
 * {@link #calculateOperation(String, RequestContext, Stack)} koja prima naziv
 * funkcije koju treba izvršiti i primjerke razreda {@link RequestContext} i
 * {@link Stack} pomoću kojih se operacija izvršava.
 * 
 * 
 * @see RequestContext
 * @see IStackOperation
 * @see SmartScriptEngine
 * 
 * @author Davor Češljaš
 */
public class StackOperationProvider {

	/**
	 * Konstanta koja predstavlja jedini primjerak razreda
	 * {@link StackOperationProvider} unutar oblikovonog obrasca
	 * <a href = "https://en.wikipedia.org/wiki/Singleton_pattern">jedinstveni
	 * objekt</a>
	 */
	private static final StackOperationProvider INSTANCE = new StackOperationProvider();

	/**
	 * Konstanta koja predstavlja znak s kojim započinje naziv svake funkcije
	 */
	private static final String FUNCTION_START = "@";

	/**
	 * Članska varijabla koja sadrži (nakon inicijalizacije) sve operacije koje
	 * primjerak ovog razreda može izvršiti
	 */
	private Map<String, IStackOperation> operations;

	/**
	 * Privatni konstruktor koji inicijalizira primjerak ovog razreda. Ovaj
	 * konstruktor inicijalizira člansku varijablu {@link #operations} pozivom
	 * metode {@link #initOperations()}
	 */
	private StackOperationProvider() {
		operations = new HashMap<>();

		initOperations();
	}

	/**
	 * Metoda koja se koristi za izvršavanjem operacije pod nazivom
	 * <b>opName</b>. Predani parametri <b>requestContext</b> i <b>echoStack</b>
	 * koriste se isključivo za izračun ove operacije. Operacija uzima sve
	 * parametre sa predanog primjerka razreda {@link Stack}, te na njega,
	 * ukoliko generira rezultat, sprema i rezultat. Primjerak razreda
	 * {@link RequestContext} ovdje se koristi za izmjenu ili dohvaćanje
	 * parametara iz nejgovih struktura
	 *
	 * @param opName
	 *            primjerak razreda {@link String} koji sadrži naziv operacije
	 *            koja se treba izvesti
	 * @param requestContext
	 *            primjerak razreda {@link RequestContext} koja se koristi za
	 *            uzimanje i stavljanje parametara u njegove interne strukture
	 * @param echoStack
	 *            primjerak razreda {@link Stack} iz kojeg se uzimaju parametri,
	 *            te ukoliko operacija generira rezultat za stog, taj rezultat
	 *            se postavlja na ovaj stog
	 * 
	 * @see RequestContext
	 * 
	 * @throws UnsupportedOperationException
	 *             ukoliko operacija s imenom <b>opName</b> nije podržana
	 */
	public void calculateOperation(String opName, RequestContext requestContext, Stack<ValueWrapper> echoStack) {
		IStackOperation stackOperation = operations.get(opName);
		if (stackOperation == null) {
			throw new UnsupportedOperationException(String.format("Operacija '%s' nije podržana", opName));
		}
		stackOperation.calculate(requestContext, echoStack);
	}

	/**
	 * Pomoćna metoda unutar koje se inicijaliziraju sve operacije, koje su
	 * primjerci razreda koji implementira sučelje {@link IStackOperation}. Po
	 * stvaranju svake od operacije ona se pod nazivom operacije mapira u
	 * {@link Map} {@link #operations}
	 */
	private void initOperations() {
		IStackOperation add = new IStackOperation() {
			private BiConsumer<ValueWrapper, Object> addOperation = ValueWrapper::add;

			@Override
			public void calculate(RequestContext requestContext, Stack<ValueWrapper> echoStack) {
				binaryOperation(addOperation, echoStack);
			}
		};
		operations.put("+", add);

		IStackOperation sub = new IStackOperation() {
			private BiConsumer<ValueWrapper, Object> subOperation = ValueWrapper::subtract;

			@Override
			public void calculate(RequestContext requestContext, Stack<ValueWrapper> echoStack) {
				binaryOperation(subOperation, echoStack);
			}
		};
		operations.put("-", sub);

		IStackOperation mul = new IStackOperation() {
			private BiConsumer<ValueWrapper, Object> mulOperation = ValueWrapper::multiply;

			@Override
			public void calculate(RequestContext requestContext, Stack<ValueWrapper> echoStack) {
				binaryOperation(mulOperation, echoStack);
			}
		};
		operations.put("*", mul);

		IStackOperation div = new IStackOperation() {
			private BiConsumer<ValueWrapper, Object> divOperation = ValueWrapper::divide;

			@Override
			public void calculate(RequestContext requestContext, Stack<ValueWrapper> echoStack) {
				binaryOperation(divOperation, echoStack);
			}
		};
		operations.put("/", div);

		IStackOperation sin = (requestContext, echoStack) -> {
			String argument = echoStack.pop().toString();
			echoStack.push(new ValueWrapper(Math.sin(Double.parseDouble(argument))));
		};
		operations.put(FUNCTION_START + "sin", sin);

		IStackOperation decfmt = (requestContext, echoStack) -> {
			DecimalFormat formatter = new DecimalFormat(echoStack.pop().toString());
			echoStack.push(new ValueWrapper(formatter.format(echoStack.pop().getValue())));
		};
		operations.put(FUNCTION_START + "decfmt", decfmt);

		IStackOperation dup = (requestContext, echoStack) -> {
			ValueWrapper toClone = echoStack.pop();
			echoStack.push(toClone);
			echoStack.push(new ValueWrapper(toClone.getValue()));
		};
		operations.put(FUNCTION_START + "dup", dup);

		IStackOperation swap = (requestContext, echoStack) -> {
			ValueWrapper first = echoStack.pop();
			ValueWrapper second = echoStack.pop();
			echoStack.push(first);
			echoStack.push(second);
		};
		operations.put(FUNCTION_START + "swap", swap);

		IStackOperation setMimeType = (requestContext, echoStack) -> {
			requestContext.setMimeType((String) echoStack.pop().getValue());
		};
		operations.put(FUNCTION_START + "setMimeType", setMimeType);

		IStackOperation paramGet = (requestContext, echoStack) -> {
			Supplier<String> valueSupplier = () -> requestContext.getParameter((String) echoStack.pop().getValue());
			getParameter(echoStack, valueSupplier);
		};
		operations.put(FUNCTION_START + "paramGet", paramGet);

		IStackOperation pparamGet = (requestContext, echoStack) -> {
			Supplier<String> valueSupplier = () -> requestContext
					.getPersistentParameter((String) echoStack.pop().getValue());
			getParameter(echoStack, valueSupplier);
		};
		operations.put(FUNCTION_START + "pparamGet", pparamGet);

		IStackOperation pparamSet = (requestContext, echoStack) -> {
			BiConsumer<String, String> valueSetter = (name, value) -> requestContext.setPersistentParameter(name,
					value);
			setParameter(echoStack, valueSetter);
		};
		operations.put(FUNCTION_START + "pparamSet", pparamSet);

		IStackOperation pparamDel = (requestContext, echoStack) -> {
			Consumer<String> paramRemover = (name) -> requestContext.removePersistentParameter(name);
			deleteParameter(echoStack, paramRemover);
		};
		operations.put(FUNCTION_START + "pparamDel", pparamDel);

		IStackOperation tparamGet = (requestContext, echoStack) -> {
			Supplier<String> valueSupplier = () -> requestContext
					.getTemporaryParameter((String) echoStack.pop().getValue());
			getParameter(echoStack, valueSupplier);
		};
		operations.put(FUNCTION_START + "tparamGet", tparamGet);

		IStackOperation tparamSet = (requestContext, echoStack) -> {
			BiConsumer<String, String> valueSetter = (name, value) -> requestContext.setTemporaryParameter(name, value);
			setParameter(echoStack, valueSetter);
		};
		operations.put(FUNCTION_START + "tparamSet", tparamSet);

		IStackOperation tparamDel = (requestContext, echoStack) -> {
			Consumer<String> paramRemover = (name) -> requestContext.removeTemporaryParameter(name);
			deleteParameter(echoStack, paramRemover);
		};
		operations.put(FUNCTION_START + "tparamDel", tparamDel);
	}

	/**
	 * Metoda koja obrađuje binarne operacije. Metoda prima starategiju koja
	 * obavlja izračun rezultata, a implementira sučelje {@link BiConsumer}
	 * <b>consumer</b>. Metoda standardno prima već opisan primjerak razred
	 * {@link Stack} (za detalje pogledati
	 * {@link #calculateOperation(String, RequestContext, Stack)}). Operacija
	 * sve parametre dohvaća sa predanog stoga, te rezultat sprema na njega
	 *
	 * @param consumer
	 *            strategija koja implementira sučelje {@link BiConsumer}, a
	 *            koja predstavlja konkretnu operaciju
	 * @param echoStack
	 *            primjerak razreda {@link Stack} s kojeg se dohvaćaju parametri
	 *            i na koji se spremaju rezultati
	 */
	private void binaryOperation(BiConsumer<ValueWrapper, Object> consumer, Stack<ValueWrapper> echoStack) {
		ValueWrapper firstArgument = echoStack.pop();
		consumer.accept(firstArgument, echoStack.pop().getValue());
		echoStack.push(firstArgument);
	}

	/**
	 * Pomoćna metod koja pomoću strategije koja implementira sučelje
	 * {@link Supplier} dohvaća rezultat i sprema ga na stog.
	 *
	 * @param echoStack
	 *            primjerak razreda {@link Stack} na koji se sprema rezultat
	 * @param valueSupplier
	 *            strategija koja se koristi za dohvat rezultata operacije.
	 *            Strategija implementira sučelje {@link Supplier}
	 */
	private void getParameter(Stack<ValueWrapper> echoStack, Supplier<String> valueSupplier) {
		Object defaultValue = echoStack.pop().getValue();
		String value = valueSupplier.get();
		echoStack.push(new ValueWrapper(value == null ? defaultValue : value));
	}

	/**
	 * Pomoćna metoda koja sa predanog primjerka razreda {@link Stack}
	 * <b>echoStack</b> dohvaća dva parametra i predaje ih predanoj strategiji
	 * koja implementira sučelje {@link BiConsumer} <b>valueSetter</b>.
	 *
	 * @param echoStack
	 *            primjerak razreda {@link Stack} s kojeg se dohvaćaju parametri
	 * @param valueSetter
	 *            strategija koja implementira sučelje {@link BiConsumer}, a
	 *            koja konzumira dva parametra dohvaćena sa stoga
	 */
	private void setParameter(Stack<ValueWrapper> echoStack, BiConsumer<String, String> valueSetter) {
		String name = (String) echoStack.pop().getValue();
		// toString u slučaju da se ne radi o String konstanti
		String value = echoStack.pop().getValue().toString();

		valueSetter.accept(name, value);
	}

	/**
	 * Pomoćna metoda koja sa predanog primjerka razreda {@link Stack} dohvaća
	 * parametar i predaje ga predanoj strategiji za brisanje koja implementira
	 * sučelje {@link Consumer}.
	 *
	 * @param echoStack
	 *            primjerak razreda {@link Stack} s kojeg se dohvaćaju parametri
	 * @param paramRemover
	 *            strategija koja implementira sučelje {@link Consumer}, a koja
	 *            vrši brisanje
	 */
	private void deleteParameter(Stack<ValueWrapper> echoStack, Consumer<String> paramRemover) {
		String name = (String) echoStack.pop().getValue();
		paramRemover.accept(name);
	}

	/**
	 * Metoda koja dohvaća jedini primjerak ovog razreda. Metoda je također
	 * sastavni dio oblikovnog obrasca
	 * <a href = "https://en.wikipedia.org/wiki/Singleton_pattern">jedinstveni
	 * objekt</a>.
	 *
	 * @return dohvaća jedini primjerak ovog razreda
	 */
	public static StackOperationProvider getInstance() {
		return INSTANCE;
	}
}
