/* Copyright (C) 2009  Egon Willighagen <egonw@users.sf.net>
 *
 * Contact: jchempaint-devel@lists.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * All we ask is that proper credit is given for our work, which includes
 * - but is not limited to - adding the above copyright notice to the beginning
 * of your source code files, and to any copyright notice that you may distribute
 * with programs based on this work.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.openscience.cdk.libio.jena;

import java.util.HashMap;
import java.util.Map;

import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomType;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IElectronContainer;
import org.openscience.cdk.interfaces.IElement;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.interfaces.IPseudoAtom;
import org.openscience.cdk.interfaces.IAtomType.Hybridization;
import org.openscience.cdk.interfaces.IBond.Order;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Helper class that converts a CDK {@link IChemObject} into RDF using a
 * Jena model and the CDK data model ontology.
 *
 * @cdk.module       iordf
 * @cdk.githash
 * @cdk.keyword      Resource Description Framework
 * @cdk.keyword      Jena
 * @cdk.keyword      RDF
 * @cdk.keyword      Web Ontology Language
 * @cdk.keyword      OWL
 */
public class Convertor {

    public static Model molecule2Model(IMolecule molecule) {
        Model model = createCDKModel();
        Resource subject = model.createResource(
            createIdentifier(model, molecule)
        );
        model.add(subject, RDF.type, CDK.MOLECULE);
        Map<IAtom,Resource> cdkToRDFAtomMap = new HashMap<IAtom, Resource>();
        for (IAtom atom : molecule.atoms()) {
            Resource rdfAtom = model.createResource(
                createIdentifier(model, atom)
            );
            cdkToRDFAtomMap.put(atom, rdfAtom);
            model.add(subject, CDK.HASATOM, rdfAtom);
            if (atom instanceof IPseudoAtom) {
                model.add(rdfAtom, RDF.type, CDK.PSEUDOATOM);
                serializePseudoAtomFields(model, rdfAtom, (IPseudoAtom)atom);
            } else {
                model.add(rdfAtom, RDF.type, CDK.ATOM);
                serializeAtomFields(model, rdfAtom, atom);
            }
        }
        for (IBond bond : molecule.bonds()) {
            Resource rdfBond = model.createResource(
                createIdentifier(model, bond)
            );
            model.add(rdfBond, RDF.type, CDK.BOND);
            for (IAtom atom : bond.atoms()) {
                model.add(rdfBond, CDK.BINDSATOM, cdkToRDFAtomMap.get(atom));
            }
            if (bond.getOrder() != null) {
                model.add(
                    rdfBond, CDK.HASORDER,
                    order2Resource(bond.getOrder())
                );
            }
            model.add(subject, CDK.HASBOND, rdfBond);
            serializeElectronContainerFields(model, rdfBond, bond);
        }
        return model;
    }

    private static void serializePseudoAtomFields(Model model,
            Resource rdfAtom, IPseudoAtom atom) {
        serializeAtomFields(model, rdfAtom, atom);
        if (atom.getLabel() != CDKConstants.UNSET)
            model.add(rdfAtom, CDK.HASLABEL, atom.getLabel());
    }

    private static void serializeAtomFields(Model model, Resource rdfAtom,
            IAtom atom) {
        serializeAtomTypeFields(model, rdfAtom, atom);
        model.add(rdfAtom, RDF.type, CDK.ATOM);
        if (atom.getSymbol() != CDKConstants.UNSET)
            model.add(rdfAtom, CDK.SYMBOL, atom.getSymbol());
    }

    private static void serializeElectronContainerFields(Model model,
            Resource rdfBond, IElectronContainer bond) {
        serializeChemObjectFields(model, rdfBond, bond);
        if (bond.getElectronCount() != null)
            model.add(
                rdfBond, CDK.HASELECTRONCOUNT,
                bond.getElectronCount().toString()
            );
    }

    private static void serializeChemObjectFields(Model model,
            Resource rdfObject, IChemObject object) {
        if (object.getID() != null)
            model.add(rdfObject, CDK.IDENTIFIER, object.getID());
    }

    private static void deserializeChemObjectFields(
            Resource rdfObject, IChemObject object) {
        Statement id = rdfObject.getProperty(CDK.IDENTIFIER);
        if (id != null) object.setID(id.getString());
    }

    private static void serializeElementFields(Model model,
            Resource rdfObject, IElement element) {
        serializeChemObjectFields(model, rdfObject, element);
        if (element.getSymbol() != null)
            model.add(rdfObject, CDK.SYMBOL, element.getSymbol());
        if (element.getAtomicNumber() != null)
            model.add(rdfObject, CDK.HASATOMICNUMBER,
                element.getAtomicNumber().toString());
    }

    private static void deserializeElementFields(
            Resource rdfObject, IElement element) {
        deserializeChemObjectFields(rdfObject, element);
        Statement symbol = rdfObject.getProperty(CDK.SYMBOL);
        if (symbol != null) element.setSymbol(symbol.getString());
        Statement atomicNumber = rdfObject.getProperty(CDK.HASATOMICNUMBER);
        if (atomicNumber != null)
            element.setAtomicNumber(atomicNumber.getInt());
    }

    private static void serializeAtomTypeFields(Model model,
            Resource rdfObject, IAtomType type) {
        serializeIsotopeFields(model, rdfObject, type);
        if (type.getHybridization() != null) {
            Hybridization hybrid = type.getHybridization(); 
            if (hybrid == Hybridization.S) {
                model.add(rdfObject, CDK.HASHYBRIDIZATION, CDK.HYBRID_S);
            } else if (hybrid == Hybridization.SP1) {
                model.add(rdfObject, CDK.HASHYBRIDIZATION, CDK.HYBRID_SP1);
            } else if (hybrid == Hybridization.SP2) {
                model.add(rdfObject, CDK.HASHYBRIDIZATION, CDK.HYBRID_SP2);
            } else if (hybrid == Hybridization.SP3) {
                model.add(rdfObject, CDK.HASHYBRIDIZATION, CDK.HYBRID_SP3);
            } else if (hybrid == Hybridization.PLANAR3) {
                model.add(rdfObject, CDK.HASHYBRIDIZATION, CDK.HYBRID_PLANAR3);
            } else if (hybrid == Hybridization.SP3D1) {
                model.add(rdfObject, CDK.HASHYBRIDIZATION, CDK.HYBRID_SP3D1);
            } else if (hybrid == Hybridization.SP3D2) {
                model.add(rdfObject, CDK.HASHYBRIDIZATION, CDK.HYBRID_SP3D2);
            } else if (hybrid == Hybridization.SP3D3) {
                model.add(rdfObject, CDK.HASHYBRIDIZATION, CDK.HYBRID_SP3D3);
            } else if (hybrid == Hybridization.SP3D4) {
                model.add(rdfObject, CDK.HASHYBRIDIZATION, CDK.HYBRID_SP3D4);
            } else if (hybrid == Hybridization.SP3D5) {
                model.add(rdfObject, CDK.HASHYBRIDIZATION, CDK.HYBRID_SP3D5);
            }
        }
        if (type.getAtomTypeName() != null) {
            model.add(rdfObject, CDK.HASATOMTYPENAME, type.getAtomTypeName());
        }
        if (type.getFormalCharge() != null) {
            model.add(
                rdfObject, CDK.HASFORMALCHARGE,
                type.getFormalCharge().toString()
            );
        }
        if (type.getMaxBondOrder() != null) {
            model.add(
                rdfObject, CDK.HASMAXBONDORDER,
                order2Resource(type.getMaxBondOrder())
            );
        }
    }

    private static void serializeIsotopeFields(Model model, Resource rdfObject,
            IIsotope isotope) {
        serializeElementFields(model, rdfObject, isotope);
        if (isotope.getMassNumber() != CDKConstants.UNSET) {
            model.add(
                 rdfObject, CDK.HASMASSNUMBER,
                 isotope.getMassNumber().toString()
            );
        }
        if (isotope.getExactMass() != CDKConstants.UNSET) {
            model.add(
                 rdfObject, CDK.HASEXACTMASS,
                 isotope.getExactMass().toString()
            );
        }
        if (isotope.getNaturalAbundance() != CDKConstants.UNSET) {
            model.add(
                 rdfObject, CDK.HASNATURALABUNDANCE,
                 isotope.getNaturalAbundance().toString()
            );
        }
    }

    private static void deserializeAtomTypeFields(
            Resource rdfObject, IAtomType element) {
        deserializeIsotopeFields(rdfObject, element);
        Statement hybrid = rdfObject.getProperty(CDK.HASHYBRIDIZATION);
        if (hybrid != null) {
            Resource rdfHybrid = (Resource)hybrid.getObject();
            if (rdfHybrid.equals(CDK.HYBRID_S)) {
                element.setHybridization(Hybridization.S);
            } else if (rdfHybrid.equals(CDK.HYBRID_SP1)) {
                element.setHybridization(Hybridization.SP1);
            } else if (rdfHybrid.equals(CDK.HYBRID_SP2)) {
                element.setHybridization(Hybridization.SP2);
            } else if (rdfHybrid.equals(CDK.HYBRID_SP3)) {
                element.setHybridization(Hybridization.SP3);
            } else if (rdfHybrid.equals(CDK.HYBRID_PLANAR3)) {
                element.setHybridization(Hybridization.PLANAR3);
            } else if (rdfHybrid.equals(CDK.HYBRID_SP3D1)) {
                element.setHybridization(Hybridization.SP3D1);
            } else if (rdfHybrid.equals(CDK.HYBRID_SP3D2)) {
                element.setHybridization(Hybridization.SP3D2);
            } else if (rdfHybrid.equals(CDK.HYBRID_SP3D3)) {
                element.setHybridization(Hybridization.SP3D3);
            } else if (rdfHybrid.equals(CDK.HYBRID_SP3D4)) {
                element.setHybridization(Hybridization.SP3D4);
            } else if (rdfHybrid.equals(CDK.HYBRID_SP3D5)) {
                element.setHybridization(Hybridization.SP3D5);
            }
        }
        Statement name = rdfObject.getProperty(CDK.HASATOMTYPENAME);
        if (name != null) {
            element.setAtomTypeName(name.getString());
        }
        Statement order = rdfObject.getProperty(CDK.HASMAXBONDORDER);
        if (order != null) {
            Resource maxOrder = (Resource)order.getResource();
            element.setMaxBondOrder(resource2Order(maxOrder));
        }
        Statement formalCharge = rdfObject.getProperty(CDK.HASFORMALCHARGE);
        if (formalCharge != null)
            element.setFormalCharge(formalCharge.getInt());
    }

    private static void deserializeIsotopeFields(Resource rdfObject,
            IIsotope isotope) {
        deserializeElementFields(rdfObject, isotope);
        Statement massNumber = rdfObject.getProperty(CDK.HASMASSNUMBER);
        if (massNumber != null)
            isotope.setMassNumber(massNumber.getInt());
        Statement exactMass = rdfObject.getProperty(CDK.HASEXACTMASS);
        if (exactMass != null)
            isotope.setExactMass(exactMass.getDouble());
        Statement naturalAbundance =
            rdfObject.getProperty(CDK.HASNATURALABUNDANCE);
        if (naturalAbundance != null)
            isotope.setNaturalAbundance(naturalAbundance.getDouble());
    }

    public static Order resource2Order(Resource rdfOrder) {
        if (rdfOrder.equals(CDK.SINGLEBOND)) {
            return Order.SINGLE;
        } else if (rdfOrder.equals(CDK.DOUBLEBOND)) {
            return Order.DOUBLE;
        } else if (rdfOrder.equals(CDK.TRIPLEBOND)) {
            return Order.TRIPLE;
        } else if (rdfOrder.equals(CDK.QUADRUPLEBOND)) {
            return Order.QUADRUPLE;
        }
        return null;
    }

    public static Resource order2Resource(Order order) {
        if (order == Order.SINGLE) {
            return CDK.SINGLEBOND;
        } else if (order == Order.DOUBLE) {
            return CDK.DOUBLEBOND;
        } else if (order == Order.TRIPLE) {
            return CDK.TRIPLEBOND;
        } else if (order == Order.QUADRUPLE) {
            return CDK.QUADRUPLEBOND;
        }
        return null;
    }

    private static String createIdentifier(Model model, IChemObject object) {
        StringBuilder result = new StringBuilder();
        result.append("http://example.com/");
        result.append(model.hashCode()).append('/');
        result.append(object.getClass().getSimpleName()).append('/');
        result.append(object.hashCode());
        return result.toString();
    }

    private static void deserializeElectronContainerFields(
            Resource rdfObject, IElectronContainer bond) {
        deserializeChemObjectFields(rdfObject, bond);
        Statement count = rdfObject.getProperty(CDK.HASELECTRONCOUNT);
        if (count != null)
            bond.setElectronCount(count.getInt());
    }

    public static IMolecule model2Molecule(Model model,
        IChemObjectBuilder builder) {
        ResIterator mols =
            model.listSubjectsWithProperty(RDF.type, CDK.MOLECULE);
        IMolecule mol = null;
        if (mols.hasNext()) {
            Resource rdfMol = mols.next();
            mol = builder.newMolecule();
            Map<Resource,IAtom> rdfToCDKAtomMap = new HashMap<Resource,IAtom>();
            StmtIterator atoms = rdfMol.listProperties(CDK.HASATOM);
            while (atoms.hasNext()) {
                Resource rdfAtom = atoms.nextStatement().getResource();
                IAtom atom;
                if (rdfAtom.hasProperty(RDF.type, CDK.PSEUDOATOM)) {
                    atom = builder.newPseudoAtom();
                    atom.setStereoParity(0);
                    Statement label = rdfAtom.getProperty(CDK.HASLABEL);
                    if (label != null)
                        ((IPseudoAtom)atom).setLabel(label.getString());
                } else {
                    atom = builder.newAtom();
                }
                Statement symbol = rdfAtom.getProperty(CDK.SYMBOL);
                if (symbol != null) atom.setSymbol(symbol.getString());
                rdfToCDKAtomMap.put(rdfAtom, atom);
                deserializeAtomTypeFields(rdfAtom, atom);
                mol.addAtom(atom);
            }
            StmtIterator bonds = rdfMol.listProperties(CDK.HASBOND);
            while (bonds.hasNext()) {
                Resource rdfBond = bonds.nextStatement().getResource();
                IBond bond = builder.newBond();
                StmtIterator bondAtoms = rdfBond.listProperties(CDK.BINDSATOM);
                int atomCounter = 0;
                while (bondAtoms.hasNext()) {
                    Statement rdfAtom = bondAtoms.nextStatement();
                    IAtom atom = rdfToCDKAtomMap.get(rdfAtom.getResource());
                    bond.setAtom(atom, atomCounter);
                    atomCounter++;
                }
                Resource order = rdfBond.
                    getProperty(CDK.HASORDER).getResource();
                bond.setOrder(resource2Order(order));
                mol.addBond(bond);
                deserializeElectronContainerFields(rdfBond, bond);
            }
        }
        return mol;
    }

    private static Model createCDKModel() {
        Model model = ModelFactory.createOntologyModel();
        model.setNsPrefix("cdk", "http://cdk.sourceforge.net/model.owl#");
        return model;
    }

}