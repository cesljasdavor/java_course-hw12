package hr.fer.zemris.java.custom.scripting.demo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import hr.fer.zemris.java.custom.scripting.elems.Element;
import hr.fer.zemris.java.custom.scripting.nodes.DocumentNode;
import hr.fer.zemris.java.custom.scripting.nodes.EchoNode;
import hr.fer.zemris.java.custom.scripting.nodes.ForLoopNode;
import hr.fer.zemris.java.custom.scripting.nodes.INodeVisitor;
import hr.fer.zemris.java.custom.scripting.nodes.Node;
import hr.fer.zemris.java.custom.scripting.nodes.TextNode;
import hr.fer.zemris.java.custom.scripting.parser.SmartScriptParser;

//pitati Čupića sutra jel možeš samo nad document pozvat da se ispiše
public class TreeWriter {

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			throw new IllegalArgumentException("Tražio sam putanju do datoteke");
		}

		String docBody = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
		SmartScriptParser p = new SmartScriptParser(docBody);
		WriteVisitor visitor = new WriteVisitor();
		p.getDocumentNode().accept(visitor);

	}

	public static class WriteVisitor implements INodeVisitor {
		private StringBuilder sb = new StringBuilder();

		@Override
		public void visitTextNode(TextNode node) {
			sb.append(node);

			visitChildren(node);
		}

		@Override
		public void visitForLoopNode(ForLoopNode node) {
			sb.append("{$ FOR " + node.getVariable() + " " + node.getStartExpression() + " " + node.getEndExpression()
					+ " ");
			Element stepExpression = node.getStepExpression();
			sb.append(stepExpression != null ? stepExpression : "").append("$}");

			visitChildren(node);

			sb.append("{$ END $}");
		}

		@Override
		public void visitEchoNode(EchoNode node) {
			sb.append("{$= ");
			for (Element element : node.getElements()) {
				sb.append(element).append(" ");
			}			
			sb.append("$}");
			
			visitChildren(node);
		}

		@Override
		public void visitDocumentNode(DocumentNode node) {
			visitChildren(node);
			
			System.out.println(sb.toString());
		}

		private void visitChildren(Node node) {
			for (Object obj : node) {
				((Node)obj).accept(this);
			}
		}
	}
}
