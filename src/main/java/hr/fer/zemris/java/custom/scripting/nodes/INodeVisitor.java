package hr.fer.zemris.java.custom.scripting.nodes;

/**
 * Sučelje koje predstavlja sučelje posjetitelja unutar oblikovnog obrasca
 * <a href = "https://en.wikipedia.org/wiki/Visitor_pattern">Posjetitelj</a>.
 * Sučelje se koristi za obliazak konkretnih implementacija apstraktnog razreda
 * {@link Node}. Sučelje nudi 4 metode, za svaku od konkretne implementacije
 * apstraktnog razreda {@link Node}:
 * <ul>
 * <li>{@link #visitDocumentNode(DocumentNode)} - {@link DocumentNode}</li>
 * <li>{@link #visitEchoNode(EchoNode)} - {@link EchoNode}</li>
 * <li>{@link #visitTextNode(TextNode)} - {@link TextNode}</li>
 * <li>{@link #visitForLoopNode(ForLoopNode)} - {@link ForLoopNode}</li>
 * </ul>
 * 
 * @see Node
 * @see DocumentNode
 * @see ForLoopNode
 * @see EchoNode
 * @see TextNode
 * 
 * @author Davor Češljaš
 * 
 */
public interface INodeVisitor {

	/**
	 * Metoda koja se koristi za posjet primjerku razreda {@link TextNode}.
	 *
	 * @param node
	 *            primjerak razreda {@link TextNode} koji se posjećuje
	 */
	public void visitTextNode(TextNode node);

	/**
	 * Metoda koja se koristi za posjet primjerku razreda {@link ForLoopNode}.
	 * Budući da ovaj razred može imati djecu, korisnik je dužan u ovoj metodi
	 * implementirati i posjet djeci. Korisnika se podsjeća da je svaki
	 * primjerak razreda koji je nasljeđen iz {@link Node} iterabilan po djeci
	 *
	 * @param node
	 *            primjerak razreda {@link ForLoopNode} koji se posjećuje
	 */
	public void visitForLoopNode(ForLoopNode node);

	/**
	 * Metoda koja se koristi za posjet primjerku razreda {@link EchoNode}
	 *
	 * @param node
	 *            primjerak razreda {@link EchoNode} koji se posjećuje
	 */
	public void visitEchoNode(EchoNode node);

	/**
	 * Metoda koja se koristi za posjet primjerku razreda {@link DocumentNode}.
	 * Budući da ovaj razred može imati djecu (obično je ovo korijenski čvor
	 * generativnog stabla), korisnik je dužan u ovoj metodi implementirati i
	 * posjet djeci. Korisnika se podsjeća da je svaki primjerak razreda koji je
	 * nasljeđen iz {@link Node} iterabilan po djeci
	 *
	 * @param node
	 *            primjerak razreda {@link DocumentNode} koji se posjećuje
	 */
	public void visitDocumentNode(DocumentNode node);
}
