package dataflow.cfg.node;

import java.util.Collection;
import java.util.Collections;

import javax.lang.model.element.Element;

import javacutils.ElementUtils;
import javacutils.InternalUtils;
import javacutils.TreeUtils;

import dataflow.util.HashCodeUtils;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;

/**
 * A node for a field access, including a method accesses:
 *
 * <pre>
 *   <em>expression</em> . <em>field</em>
 * </pre>
 *
 * @author Stefan Heule
 *
 */
public class FieldAccessNode extends Node {

    protected Tree tree;
    protected Element element;
    protected String field;
    protected Node receiver;

    // TODO: add method to get modifiers (static, access level, ..)

    public FieldAccessNode(Tree tree, Node receiver) {
        super(InternalUtils.typeOf(tree));
        assert TreeUtils.isFieldAccess(tree);
        this.tree = tree;
        this.receiver = receiver;
        this.field = TreeUtils.getFieldName(tree);

        if (tree instanceof MemberSelectTree) {
            this.element = TreeUtils.elementFromUse((MemberSelectTree) tree);
        } else {
            assert tree instanceof IdentifierTree;
            this.element =  TreeUtils.elementFromUse((IdentifierTree) tree);
        }
    }

    public FieldAccessNode(Element element, Node receiver) {
        super(element.asType());
        this.tree = null;
        this.element = element;
        this.receiver = receiver;
        this.field = element.getSimpleName().toString();
    }

    public Element getElement() {
        return element;
    }

    public Node getReceiver() {
        return receiver;
    }

    public String getFieldName() {
        return field;
    }

    @Override
    public Tree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitFieldAccess(this, p);
    }

    @Override
    public String toString() {
        return getReceiver() + "." + field;
    }

    /**
     * Is this a static field?
     */
    public boolean isStatic() {
        return ElementUtils.isStatic(getElement());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof FieldAccessNode)) {
            return false;
        }
        FieldAccessNode other = (FieldAccessNode) obj;
        return getReceiver().equals(other.getReceiver())
                && getFieldName().equals(other.getFieldName());
    }

    @Override
    public int hashCode() {
        return HashCodeUtils.hash(getReceiver(), getFieldName());
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.singletonList(receiver);
    }
}