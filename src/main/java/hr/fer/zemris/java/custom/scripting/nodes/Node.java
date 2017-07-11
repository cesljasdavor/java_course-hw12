package hr.fer.zemris.java.custom.scripting.nodes;

import java.util.Iterator;

import hr.fer.java.zemris.hw02.collections.ArrayIndexedCollection;
import hr.fer.zemris.java.custom.scripting.parser.SmartScriptParser;

/**
 * Apstraktni razred predstavlja nepromjenjivi čvor generativnog stabla prilikom
 * parsiranja primjerkom razreda {@link SmartScriptParser}. Formalno, svaki
 * razred koji nasljeđuje ovaj razred predstavlja nezavršni znak gramatike
 * spomenutog parsera. Razred također implementira sučelje {@link Iterable} kako
 * bi se moglo iterirati po čvoru koji sadrži djecu.
 * <p>
 * Jedina apstraktna metoda je metoda {@link #accept(INodeVisitor)} koja služi
 * kao impementacija prilikom obilaska stabla primjerkom sučelja
 * {@link INodeVisitor}. Korisniku se savjetuje da unutar te metode samo pozove
 * prikladnu metodu, a unutar primjerka razreda koji implementira sučelje
 * {@link INodeVisitor} odradi posao obilaska
 * </p>
 * 
 * @author Davor Češljaš
 */
public abstract class Node implements Iterable<Object> {

	/** Čvorovi djeca ovog čvora. */
	private ArrayIndexedCollection children;

	/**
	 * Dodaje čvor u listu čvorova djece
	 *
	 * @param child
	 *            čvor dijete koji je potrebno dodati
	 */
	public void addChildNode(Node child) {
		if (children == null) {
			children = new ArrayIndexedCollection();
		}

		children.add(child);
	}

	/**
	 * Dohvaća broj djece ovog čvora
	 *
	 * @return broj djece ovog čvora
	 */
	public int numberOfChildren() {
		if (children == null) {
			return 0;
		}

		return children.size();
	}

	/**
	 * Dohvaća dijete na poziciji <b>index</b> unutar liste čvorova djece.
	 *
	 * @param index
	 *            pozicija u listi čvorova djece s koje se dohvaća referenca na
	 *            čvor dijete
	 * @return referencu na čvor dijete koje se nalazi na poziciji <b>index</b>
	 *         u listi čvorova djece
	 * 
	 * @throws IndexOutOfBoundsException
	 *             - ukoliko index nije unutar granica
	 */
	public Node getChild(int index) {
		return (Node) children.get(index);
	}

	/**
	 * Metoda koja prima kao parametar implementaciju sučelja
	 * {@link INodeVisitor} te nad njom poziva odgovarajuću metodu, šaljući sebe
	 * (<b>this</b>) kao argument metodi. Metoda je karakteristična metoda
	 * oblikovnog obrasca
	 * <a href="https://en.wikipedia.org/wiki/Visitor_pattern">Posjetitelj</a>
	 * 
	 * @param nodeVisitor
	 *            implementacija konkretnog posjetitelja oblikovanog sučeljem
	 *            {@link INodeVisitor}
	 */
	public abstract void accept(INodeVisitor nodeVisitor);

	@Override
	public Iterator<Object> iterator() {
		return new NodeIterator();
	}

	/**
	 * Pomoćni razred koji implementira sučelje {@link Iterator}.
	 * 
	 * @author Davor Češljaš
	 */
	private class NodeIterator implements Iterator<Object> {

		/** Trenutna pozicija elementa u listi čvorova djece */
		private int currentIndex;

		@Override
		public boolean hasNext() {
			return children != null && currentIndex < children.size();
		}

		/**
		 * @throws UnsupportedOperationException
		 *             ukoliko čvor nema djece
		 */
		@Override
		public Object next() {
			if (children == null) {
				throw new UnsupportedOperationException();
			}
			return children.get(currentIndex++);
		}
	}

}
