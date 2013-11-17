/* $Revision$ $Author$ $Date$ 
 *
 * Copyright (C) 2004-2007  The Chemistry Development Kit (CDK) project
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * (or see http://www.gnu.org/copyleft/lesser.html)
 */
package org.openscience.cdk.isomorphism.matchers.smarts;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.isomorphism.matchers.IQueryAtom;

import java.util.EnumSet;
import java.util.Set;

/**
 * This class matches a logical operator that connects two query atoms
 *
 * @cdk.module  smarts
 * @cdk.githash
 * @cdk.keyword SMARTS 
 */
public final class LogicalOperatorAtom extends SMARTSAtom {

	/**
	 * Left child
	 */
	private IQueryAtom left;

    /**
     * Name of operator
     */
    private String operator;

    /**
     * Right child
     */
    private IQueryAtom right;

    public LogicalOperatorAtom(IChemObjectBuilder builder){
        super(builder);
    }
    
    @Deprecated
    public IQueryAtom getLeft() {
        return left;
    }
    
    @Deprecated
    public String getOperator() {
        return operator;
    }
    
    @Deprecated
    public IQueryAtom getRight() {
        return right;
    }
    
    @Deprecated
    public void setLeft(IQueryAtom left) {
        this.left = left;
    }
    
    @Deprecated
    public void setOperator(String name) {
        this.operator = name;
    }
    
    @Deprecated
    public void setRight(IQueryAtom right) {
        this.right = right;
    }
    
    /* (non-Javadoc)
     * @see org.openscience.cdk.isomorphism.matchers.smarts.SMARTSAtom#matches(org.openscience.cdk.interfaces.IAtom)
     */
    @Deprecated
    public boolean matches(IAtom atom) {
    	boolean val = false;
    	boolean matchesLeft = left.matches(atom);
    	if (right != null) {
    		if ("and".equals(operator) && matchesLeft) {
        		boolean matchesRight = right.matches(atom);
    			val = matchesLeft && matchesRight;
    		} else if ("or".equals(operator)) {
        		boolean matchesRight = right.matches(atom);
    			val = matchesLeft || matchesRight;
    		}
    	} else {
    		if ("not".equals(operator)) {
    			val = (!matchesLeft);
    		} else {
    			val = matchesLeft;
    		}
    	}
    	return val;
    }

    /* (non-Javadoc)
     * @see org.openscience.cdk.ChemObject#getFlag(int)
     */
    @Deprecated
    public boolean getFlag(int flagType) {
    	boolean val = false;
    	boolean leftFlag = left.getFlag(flagType);
    	if (right != null) {
    		if ("and".equals(operator) && leftFlag) {
        		boolean rightFlag = right.getFlag(flagType);
    			val = leftFlag && rightFlag;
    		} else if ("or".equals(operator)) {
        		boolean rightFlag = right.getFlag(flagType);
    			val = leftFlag || rightFlag;
    		}
    	} else {
    		if ("not".equals(operator)) {
    			val = (!leftFlag);
    		} else {
    			val = leftFlag;
    		}
    	}
    	return val;
    }

    /**
     * Conjunction the provided expressions.
     *
     * @param left expression
     * @param right expression
     * @return conjunction of the left and right expressions
     */
    public static SMARTSAtom and(IQueryAtom left, IQueryAtom right) {
        return new Conjunction(left.getBuilder(), left, right);
    }

    /**
     * Disjunction the provided expressions.
     *
     * @param left expression
     * @param right expression
     * @return disjunction of the left and right expressions
     */
    public static SMARTSAtom or(IQueryAtom left, IQueryAtom right) {
        return new Disjunction(left.getBuilder(), left, right);
    }

    /**
     * Negate the provided expression.
     * 
     * @param expr expression to negate
     * @return a SMARTS atom which is the negation of the expression
     */
    public static SMARTSAtom not(IQueryAtom expr) {
        return new Negation(expr.getBuilder(), expr);
    }

    /** Defines a conjunction (AND) between two query atoms. */
    private static class Conjunction extends SMARTSAtom {

        /** left and right of the operator. */
        private IQueryAtom left, right;

        /**
         * Create a disjunction of {@code left} or {@code right}.
         *
         * @param builder chem object builder
         * @param left    the expression to negate
         * @param right   the expression to negate
         */
        private Conjunction(IChemObjectBuilder builder, IQueryAtom left, IQueryAtom right) {
            super(builder);
            this.left = left;
            this.right = right;
        }

        /** @inheritDoc */
        @Override public boolean matches(IAtom atom) {
            return left.matches(atom) && right.matches(atom);
        }

        /**
         * @inheritDoc
         */
        @Override public void chirality(IAtom target, EnumSet<Chirality> chiralities) {
            
            // determine the causalities of either side and then work out if it
            // was a contradiction or if the match was valid
            EnumSet<Chirality> leftChiralities  = EnumSet.noneOf(Chirality.class);
            EnumSet<Chirality> rightChiralities = EnumSet.noneOf(Chirality.class);
            
            ((SMARTSAtom) left).chirality(target, leftChiralities);
            ((SMARTSAtom) right).chirality(target, rightChiralities);
            
            // contradictions => None
            if (leftChiralities.contains(Chirality.Clockwise) && rightChiralities.contains(Chirality.Anticlockwise))
                chiralities.add(Chirality.None);
            if (leftChiralities.contains(Chirality.Anticlockwise) && rightChiralities.contains(Chirality.Clockwise))
                chiralities.add(Chirality.None);
            
            chiralities.addAll(leftChiralities);
            chiralities.addAll(rightChiralities);

            if (leftChiralities.contains(Chirality.Clockwise) || leftChiralities.contains(Chirality.Anticlockwise)
                    || rightChiralities.contains(Chirality.Clockwise) || rightChiralities.contains(Chirality.Anticlockwise))
                chiralities.remove(Chirality.Any);
        }
    }

    /** Defines a disjunction (or) between two query atoms. */
    private static class Disjunction extends SMARTSAtom {

        /** left and right of the operator. */
        private IQueryAtom left, right;

        /**
         * Create a disjunction of {@code left} or {@code right}.
         *
         * @param builder chem object builder
         * @param left    the expression to negate
         * @param right   the expression to negate
         */
        private Disjunction(IChemObjectBuilder builder, IQueryAtom left, IQueryAtom right) {
            super(builder);
            this.left = left;
            this.right = right;
        }

        /** @inheritDoc */
        @Override public boolean matches(IAtom atom) {
            return left.matches(atom) || right.matches(atom);
        }

        /**
         * @inheritDoc
         */
        @Override public void chirality(IAtom target, EnumSet<Chirality> chiralities) {
            // 'or' the sides of what matched
            if (left.matches(target))
                ((SMARTSAtom) left).chirality(target, chiralities);
            if (right.matches(target))
                ((SMARTSAtom) right).chirality(target, chiralities);
        }
    }

    /** Defines a negation (not) of a query atom. */
    private static class Negation extends SMARTSAtom {

        /** Expression to negate. */
        private IQueryAtom expression;

        /**
         * Create a negation of {@code expression}.
         *
         * @param builder    chem object builder
         * @param expression the expression to negate
         */
        private Negation(IChemObjectBuilder builder, IQueryAtom expression) {
            super(builder);
            this.expression = expression;
        }

        /** @inheritDoc */
        @Override public boolean matches(IAtom atom) {
            return !expression.matches(atom);
        }

        @Override public void chirality(IAtom target, EnumSet<Chirality> chiralities) {
            chiralities.clear(); // not sure what logic to apply here?
        }
    }
}
