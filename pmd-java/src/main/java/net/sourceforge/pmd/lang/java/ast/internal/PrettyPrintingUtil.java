/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.ast.internal;

import net.sourceforge.pmd.lang.java.ast.ASTAnnotationTypeDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTAnyTypeDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTArrayType;
import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceType;
import net.sourceforge.pmd.lang.java.ast.ASTConstructorDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTEnumDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTFieldDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTFormalParameter;
import net.sourceforge.pmd.lang.java.ast.ASTFormalParameters;
import net.sourceforge.pmd.lang.java.ast.ASTImportDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTList;
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTMethodOrConstructorDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTPrimitiveType;
import net.sourceforge.pmd.lang.java.ast.ASTRecordDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTReferenceType;
import net.sourceforge.pmd.lang.java.ast.ASTResource;
import net.sourceforge.pmd.lang.java.ast.ASTType;
import net.sourceforge.pmd.lang.java.ast.ASTTypeArguments;
import net.sourceforge.pmd.lang.java.ast.ASTVariableDeclaratorId;
import net.sourceforge.pmd.lang.java.ast.ASTVoidType;
import net.sourceforge.pmd.lang.java.ast.ASTWildcardType;
import net.sourceforge.pmd.lang.java.ast.JavaNode;

/**
 * @author Clément Fournier
 */
public final class PrettyPrintingUtil {

    private PrettyPrintingUtil() {
        // util class
    }

    /**
     * Returns a normalized method name. This just looks at the image of the types of the parameters.
     */
    public static String displaySignature(String methodName, ASTFormalParameters params) {

        StringBuilder sb = new StringBuilder();
        sb.append(methodName);
        sb.append('(');

        boolean first = true;
        for (ASTFormalParameter param : params) {
            if (!first) {
                sb.append(", ");
            }
            first = false;

            prettyPrintTypeNode(param.getTypeNode(), sb, /*withTypeArgs*/false);
            int extraDimensions = ASTList.sizeOrZero(param.getVarId().getExtraDimensions());
            while (extraDimensions-- > 0) {
                sb.append("[]");
            }
        }

        sb.append(')');

        return sb.toString();
    }

    private static void prettyPrintTypeNode(ASTType t, StringBuilder sb, boolean withTargs) {
        if (t instanceof ASTPrimitiveType) {
            sb.append(((ASTPrimitiveType) t).getKind().getSimpleName());
        } else if (t instanceof ASTClassOrInterfaceType) {
            sb.append(((ASTClassOrInterfaceType) t).getSimpleName());
            if (withTargs) {
                prettyPrintTypeArguments((ASTClassOrInterfaceType) t, sb);
            }
        } else if (t instanceof ASTArrayType) {
            prettyPrintTypeNode(((ASTArrayType) t).getElementType(), sb, withTargs);
            int depth = ((ASTArrayType) t).getArrayDepth();
            for (int i = 0; i < depth; i++) {
                sb.append("[]");
            }
        } else if (t instanceof ASTVoidType) {
            sb.append("void");
        } else if (t instanceof ASTWildcardType) {
            sb.append("?");
            ASTReferenceType bound = ((ASTWildcardType) t).getTypeBoundNode();
            if (bound != null) {
                sb.append(((ASTWildcardType) t).hasLowerBound() ? " super " : " extends ");
                prettyPrintTypeNode(bound, sb, withTargs);
            }
        }
    }

    private static void prettyPrintTypeArguments(ASTClassOrInterfaceType t, StringBuilder sb) {
        ASTTypeArguments targs = t.getTypeArguments();
        if (targs != null) {
            boolean first = true;
            sb.append('<');
            for (ASTType targ : targs) {
                prettyPrintTypeNode(targ, sb, true);
                if (!first) {
                    sb.append(", ");
                }
                first = false;
            }
            sb.append('>');
        }
    }

    public static String prettyPrintType(ASTType t) {
        StringBuilder sb = new StringBuilder();
        prettyPrintTypeNode(t, sb, /*withTypeArgs*/false);
        return sb.toString();
    }

    public static String prettyPrintTypeWithTargs(ASTType t) {
        StringBuilder sb = new StringBuilder();
        prettyPrintTypeNode(t, sb, /*withTypeArgs*/ true);
        return sb.toString();
    }

    /**
     * Returns a normalized method name. This just looks at the image of the types of the parameters.
     */
    public static String displaySignature(ASTMethodOrConstructorDeclaration node) {
        ASTFormalParameters params = node.getFormalParameters();
        String name = node instanceof ASTMethodDeclaration ? node.getName() : node.getImage();

        return displaySignature(name, params);
    }

    /**
     * Returns the generic kind of declaration this is, eg "enum" or "class".
     */
    public static String getPrintableNodeKind(ASTAnyTypeDeclaration decl) {
        if (decl instanceof ASTClassOrInterfaceDeclaration && decl.isInterface()) {
            return "interface";
        } else if (decl instanceof ASTAnnotationTypeDeclaration) {
            return "annotation";
        } else if (decl instanceof ASTEnumDeclaration) {
            return "enum";
        } else if (decl instanceof ASTRecordDeclaration) {
            return "record";
        }
        return "class";
    }

    /**
     * Returns the "name" of a node. For methods and constructors, this
     * may return a signature with parameters.
     */
    public static String getNodeName(JavaNode node) {
        // constructors are differentiated by their parameters, while we only use method name for methods
        if (node instanceof ASTMethodDeclaration) {
            return ((ASTMethodDeclaration) node).getName();
        } else if (node instanceof ASTMethodOrConstructorDeclaration) {
            // constructors are differentiated by their parameters, while we only use method name for methods
            return displaySignature((ASTConstructorDeclaration) node);
        } else if (node instanceof ASTFieldDeclaration) {
            return ((ASTFieldDeclaration) node).getVarIds().firstOrThrow().getName();
        } else if (node instanceof ASTResource) {
            return ((ASTResource) node).getStableName();
        } else if (node instanceof ASTAnyTypeDeclaration) {
            return ((ASTAnyTypeDeclaration) node).getSimpleName();
        } else if (node instanceof ASTVariableDeclaratorId) {
            return ((ASTVariableDeclaratorId) node).getName();
        } else {
            return node.getImage(); // todo get rid of this
        }
    }


    /**
     * Returns the 'kind' of node this is. For instance for a {@link ASTFieldDeclaration},
     * returns "field".
     *
     * @throws UnsupportedOperationException If unimplemented for a node kind
     * @see #getPrintableNodeKind(ASTAnyTypeDeclaration)
     */
    public static String getPrintableNodeKind(JavaNode node) {
        if (node instanceof ASTAnyTypeDeclaration) {
            return getPrintableNodeKind((ASTAnyTypeDeclaration) node);
        } else if (node instanceof ASTMethodDeclaration) {
            return "method";
        } else if (node instanceof ASTConstructorDeclaration) {
            return "constructor";
        } else if (node instanceof ASTFieldDeclaration) {
            return "field";
        } else if (node instanceof ASTResource) {
            return "resource specification";
        }
        throw new UnsupportedOperationException("Node " + node + " is unaccounted for");
    }

    public static String prettyImport(ASTImportDeclaration importDecl) {
        String name = importDecl.getImportedName();
        if (importDecl.isImportOnDemand()) {
            return name + ".*";
        }
        return name;
    }

}
