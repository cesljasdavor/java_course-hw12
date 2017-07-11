package hr.fer.zemris.java.custom.scripting.exec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Stack;
import java.util.StringJoiner;

import hr.fer.zemris.java.custom.scripting.elems.Element;
import hr.fer.zemris.java.custom.scripting.elems.ElementConstantDouble;
import hr.fer.zemris.java.custom.scripting.elems.ElementConstantInteger;
import hr.fer.zemris.java.custom.scripting.elems.ElementFunction;
import hr.fer.zemris.java.custom.scripting.elems.ElementOperator;
import hr.fer.zemris.java.custom.scripting.elems.ElementString;
import hr.fer.zemris.java.custom.scripting.elems.ElementVariable;
import hr.fer.zemris.java.custom.scripting.lexer.SmartScriptLexer;
import hr.fer.zemris.java.custom.scripting.nodes.DocumentNode;
import hr.fer.zemris.java.custom.scripting.nodes.EchoNode;
import hr.fer.zemris.java.custom.scripting.nodes.ForLoopNode;
import hr.fer.zemris.java.custom.scripting.nodes.INodeVisitor;
import hr.fer.zemris.java.custom.scripting.nodes.Node;
import hr.fer.zemris.java.custom.scripting.nodes.TextNode;
import hr.fer.zemris.java.custom.scripting.parser.SmartScriptParser;
import hr.fer.zemris.java.webserver.RequestContext;

/**
 * Razred predstavlja semantički analizator koji se koristi po završetku analize
 * teksta leksičkim analizatorom {@link SmartScriptLexer} i sintaksnim
 * analizatorom {@link SmartScriptParser}. Pozivom metode {@link #execute()}
 * pokreće se analiza generativnog stabla koje je nastalo parsiranjem primjerkom
 * razreda {@link SmartScriptParser}.
 * <p>
 * Prilikom obilaska koristi se oblikovni obrazrac
 * <a href = "https://en.wikipedia.org/wiki/Visitor_pattern">posjetitelj</a>
 * budući da su svi čvorovi izvedeni iz {@link Node} te time implementiraju
 * metodu {@link Node#accept(INodeVisitor)}.
 * <p>
 * 
 * @see SmartScriptLexer
 * @see SmartScriptParser
 * @see Node
 * @see RequestContext
 * @see StackOperationProvider
 * 
 * @author Davor Češljaš
 */
public class SmartScriptEngine {

	/**
	 * Članska varijabla koja predstavlja vršni čvor generativnog stabla,
	 * nastalog parsiranjem prijerkom razreda {@link SmartScriptParser}
	 */
	private DocumentNode documentNode;

	/**
	 * Člasnka varijabla koja se koristi za ispis semantički analiziranog
	 * stabla.
	 */
	private RequestContext requestContext;

	/**
	 * Članska varijabla koja predstavlja pomoćni stog na koji se postavljaju
	 * sve varijable koje se moraju globalno koristiti.
	 */
	private ObjectMultistack multistack = new ObjectMultistack();

	/**
	 * Članska varijabla koja predstavlja konkretan posjetitelj koji
	 * implementira sučelje {@link INodeVisitor}
	 */
	private INodeVisitor visitor = new INodeVisitor() {
		/**
		 * Članska varijabla koja predstavlja pomoćni spremnik koji se koristi
		 * prilikom semantičke analize
		 */
		private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		@Override
		public void visitTextNode(TextNode node) {
			writeToBuffer(node.getText());
		}

		@Override
		public void visitForLoopNode(ForLoopNode node) {
			ValueWrapper variableValue = new ValueWrapper(extractElementValue(node.getStartExpression()));
			multistack.push(node.getVariable().toString(), variableValue);

			Element step = node.getStepExpression();
			Object stepValue = step == null ? Integer.valueOf(0) : extractElementValue(step);
			Object endValue = extractElementValue(node.getEndExpression());

			for (; variableValue.numCompare(endValue) <= 0; variableValue.add(stepValue)) {
				visitChildren(node);
			}
		}

		@Override
		public void visitEchoNode(EchoNode node) {
			StackOperationProvider opProvider = StackOperationProvider.getInstance();
			Stack<ValueWrapper> echoStack = new Stack<>();

			for (Element element : node.getElements()) {
				if (element instanceof ElementFunction || element instanceof ElementOperator) {
					opProvider.calculateOperation(element.asText(), requestContext, echoStack);
				} else {
					echoStack.push(new ValueWrapper(extractElementValue(element)));
				}
			}

			StringJoiner sj = new StringJoiner(" ");
			printStack(sj, echoStack);

			writeToBuffer(sj.toString());
		}

		@Override
		public void visitDocumentNode(DocumentNode node) {
			visitChildren(node);

			writeToRequestContextOutput();
		}

		/**
		 * Pomoćna metoda koja sav sadržaj članske varijable {@link #buffer}
		 * piše u izlaz od članske varijable
		 * {@link SmartScriptEngine#requestContext}
		 */
		private void writeToRequestContextOutput() {
			try {
				requestContext.write(buffer.toByteArray());
			} catch (IOException e) {
				System.err.println("Nisam u mogućnosti pisati u tok podataka");
			}
		}

		/**
		 * Pomoćna metoda koja ispisuje sadržaj primjerka razreda {@link Stack}
		 * <b>stack</b> od dna prema vrhu u predani primjerak razreda
		 * {@link StringJoiner}
		 * 
		 * @param sj
		 *            primjerak razreda {@link StringJoiner} u koji se ispisuje
		 *            stog
		 * @param stack
		 *            primjerak razreda {@link Stack} koji se ispisuje
		 */
		private void printStack(StringJoiner sj, Stack<ValueWrapper> stack) {
			if (stack.isEmpty()) {
				return;
			}

			ValueWrapper value = stack.pop();

			printStack(sj, stack);

			sj.add(value.toString());
		}

		/**
		 * Pomoćna metoda koji sadržaj predanog primjerak razreda {@link String}
		 * <b>text</b> upisuje u {@link #buffer}
		 * 
		 * @param text
		 *            primjerak razreda {@link String} čiji se sadržaj upisuje u
		 *            {@link #buffer}
		 */
		private void writeToBuffer(String text) {
			try {
				buffer.write(text.getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				System.out.println("Ne mogu pisati u pomoćni spremnik");
			}
		}

		/**
		 * Pomoćna metoda koja ispituje predani primjerak razreda
		 * {@link Element} <b>element</b> i vadi vrijednost na neka od 4 načina,
		 * a ovisno kojem izvedenom razredu pripada <b>element</b>:
		 * <ul>
		 * <li>{@link ElementVariable} - traži varijablu s tim nazivom u
		 * {@link SmartScriptEngine#multistack} te vraća njenu vrijednost</li>
		 * <li>{@link ElementConstantInteger} - vraća {@link Integer}
		 * vrijednosti</li>
		 * <li>{@link ElementConstantDouble} - vraća {@link Double}
		 * vrijednost</li>
		 * <li>{@link ElementString} - vraća {@link String} vrijednost</li>
		 * </ul>
		 * 
		 * @param element
		 *            primjerak razreda {@link Element} koji se isputuje
		 * @return neku od gore navedenih vrijednosti
		 * 
		 * @throws IllegalArgumentException
		 *             ako primjerak razreda {@link Element} nije nadrazred
		 *             nekim od gore navedenih
		 */
		private Object extractElementValue(Element element) {
			Object value;
			if (element instanceof ElementVariable) {
				value = multistack.peek(((ElementVariable) element).getName()).getValue();
			} else if (element instanceof ElementConstantInteger) {
				value = ((ElementConstantInteger) element).getValue();
			} else if (element instanceof ElementConstantDouble) {
				value = ((ElementConstantDouble) element).getValue();
			} else if (element instanceof ElementString) {
				value = ((ElementString) element).getValue();
			} else {
				throw new IllegalArgumentException(
						String.format("Ne mogu izvaditi vrijednost iz '%s'", element.toString()));
			}

			return value;
		}

		/**
		 * Pomoćna metoda koja poziva metodu {@link Node#accept(INodeVisitor)}
		 * nad svom djecom primjerka razreda {@link Node}
		 * 
		 * @param node
		 *            primjerak razreda {@link Node} čija se djeca obilaze
		 */
		private void visitChildren(Node node) {
			for (Object obj : node) {
				((Node) obj).accept(this);
			}
		}
	};

	/**
	 * Konstruktor koji inicijalizira primjerak ovog razreda. Unutar
	 * konstruktora interno se pospremaju predani parametri ukoliko niti jedan
	 * od njih nije <code>null</code>
	 *
	 * @param documentNode
	 *            primjerak razreda {@link DocumentNode} koji predstavlja vršni
	 *            čvor generativnog stabla, nastalog parsiranjem primjerkom
	 *            razreda {@link SmartScriptParser}
	 * @param requestContext
	 *            primjerak razreda {@link RequestContext} u čiji se izlazni tok
	 *            okteta piše, pozivom njegove metode
	 *            {@link RequestContext#write(byte[])}
	 * 
	 * @throws NullPointerException
	 *             ukliko je neki od predanih parametara <code>null</code>
	 */
	public SmartScriptEngine(DocumentNode documentNode, RequestContext requestContext) {
		this.documentNode = Objects.requireNonNull(documentNode, "Dokument ne smije biti null");
		this.requestContext = Objects.requireNonNull(requestContext, "Kontekst zahtjeva ne smije biti null");
	}

	/**
	 * Metoda čijim pozivom započinje semantička analiza. Ova metoda rezultira
	 * ili time da je u predani primjerak razreda {@link RequestContext} (u
	 * konstruktoru) upisan semantički analizirani sadržaj ili se bacila iznimka
	 *
	 * @throws IllegalArgumentException
	 *             ukoliko semantička analiza nije uspješno završila
	 */
	public void execute() {
		documentNode.accept(visitor);
	}
}