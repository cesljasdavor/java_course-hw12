package hr.fer.zemris.java.custom.scripting.exec;

import java.util.Stack;

import hr.fer.zemris.java.webserver.RequestContext;

/**
 * Sučelje koje predstavlja apstraktnu akciju semantičkog analizatora
 * {@link SmartScriptEngine}. Sučelje nudi samo jednu metodu
 * {@link #calculate(RequestContext, Stack)}, te se time može pisati kroz lambda
 * izraze
 */
public interface IStackOperation {

	/**
	 * Metoda koja vrši izračun na temelju predanih parametara
	 * <b>requestContext</b> i <b>echoStack</b>
	 *
	 * @param requestContext
	 *            primjerak razreda {@link RequestContext} koji se koristi za
	 *            izračun
	 * @param echoStack
	 *            primjerak razreda {@link Stack} koji sadrži primjerke razreda
	 *            {@link ValueWrapper}. S ovog stoga uzimaju se argumenti
	 *            operacije i na njega se , ako operacija generira rezultat,
	 *            stavlja rezultat
	 */
	void calculate(RequestContext requestContext, Stack<ValueWrapper> echoStack);
}
